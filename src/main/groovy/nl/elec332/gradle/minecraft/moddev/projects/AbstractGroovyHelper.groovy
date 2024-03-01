package nl.elec332.gradle.minecraft.moddev.projects


import nl.elec332.gradle.minecraft.moddev.MLProperties
import nl.elec332.gradle.minecraft.moddev.ModLoader
import nl.elec332.gradle.minecraft.moddev.ProjectHelper
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources

/**
 * Created by Elec332 on 07-09-2023
 */
class AbstractGroovyHelper {

    public static String GENERATED_RESOURCES = "src/generated/resources"
//    public static String GENERATED_RESOURCE_FILES = "build/generated/genResources"
    public static String GENERATE_MIXIN_TASK = "generateMixinJson"
    public static String GENERATE_MODINFO_TASK = "generateModInfo"
    public static String CHECK_CLASSES_TASK = "checkClasses"
    public static String GENERATE_METADATA = "generateMetadata"

    private static Directory genResources(Project target) {
        return target.layout.buildDirectory.dir("generated/genResources").get()
    }

    static class GenerateMixinJson extends DefaultTask {

        GenerateMixinJson() {
            onlyIf {
                mixins != null
            }
            outputs.upToDateWhen(t -> {
                var rd = genResources(getProject()).dir(source)
                for (File f in rd.asFile.listFiles((FilenameFilter) { dir, name ->
                    return name.endsWith(".mixins.json")
                })) {
                    if (!mixins.containsKey(f.name)) {
                        f.delete()
                    }
                }
                for (e in mixins.entrySet()) {
                    File f = rd.file(e.key).asFile
                    if (!f.exists() || f.text != e.value.toJson(null, project)) {
                        return false
                    }
                }
                return true
            })
        }

        void source(SourceSet ss) {
            this.source = ss.name
        }

        @Input
        String source = SourceSet.MAIN_SOURCE_SET_NAME

        void addMixins(Set<CommonExtension.Mixin> nm) {
            if (nm == null || nm.size() == 0) {
                return
            }
            if (mixins == null) {
                mixins = new HashMap<>()
            }
            ModLoader modLoader = ProjectHelper.getPlugin(getProject()).getModLoader()
            String fileNameBase = (modLoader == null ? ".common" : "." + modLoader.name()) + ".mixins.json"
            var rd = genResources(getProject()).dir(source)
            Set<String> pubs = new HashSet<>()
            for (m in nm) {
                String file
                if (counter == 1) {
                    file = ProjectHelper.getStringProperty(getProject(), MLProperties.MOD_ID) + fileNameBase
                } else {
                    file = ProjectHelper.getStringProperty(getProject(), MLProperties.MOD_ID) + counter + fileNameBase
                }
                if (!m.generateOnly) {
                    pubs.add(file)
                }
                mixins[file] = m
                outputs.file(rd.file(file))
                counter++
            }
            metaMixinFiles = Collections.unmodifiableSet(pubs)
        }

        @Internal
        private int counter = 1

        @SuppressWarnings("unused") //Gradle made me do it
        int getCounter() {
            return counter
        }

        @Input
        private Map<String, CommonExtension.Mixin> mixins = null
        @Internal
        private Set<String> metaMixinFiles = null

        @SuppressWarnings("unused") //Gradle made me do it
        Map<String, CommonExtension.Mixin> getMixins() {
            return mixins
        }

        Set<String> getMetaMixinFiles() {
            return metaMixinFiles == null ? Collections.EMPTY_SET : metaMixinFiles
        }

        @TaskAction
        void run() {
            if (mixins == null) {
                throw new RuntimeException("Gradle fucked up")
            }
            File rootDir = genResources(getProject()).dir(source).asFile
            for (File f in rootDir.listFiles()) {
                if (f.file && f.name.endsWith(".mixins.json")) {
                    f.delete()
                }
            }
            for (e in mixins.entrySet()) {
                new File(rootDir,  e.key).text = Objects.requireNonNull(e.value.toJson(null, project))
            }
        }

    }

    static class GenerateModInfo extends DefaultTask {

        GenerateModInfo() {
            outputs.upToDateWhen(t -> {
                File f = getOutput()
                f.exists() && f.text == metadata.toString()
            })
        }

        @Input
        ModMetadata metadata;

        void source(SourceSet ss) {
            this.source = ss.name
        }

        @Input
        String source = SourceSet.MAIN_SOURCE_SET_NAME

        @OutputFile
        File getOutput() {
            genResources(getProject()).dir(source).file(metadata.getFileLocation()).asFile
        }

        @TaskAction
        void run() {
            File f = getOutput()
            if (f.exists()) {
                f.delete()
            }
            f.text = metadata.toString()
        }

    }

    static void setProperties(Project target, boolean hasMod, ModLoader ml) {
        target.ext.set("modProject", hasMod)
        target.ext.set("hasModLoader", hasMod && ml != null)
        if (hasMod && ml != null) {
            target.ext.set("modLoader", ml.name())
        }
    }

    static void importCommonProject(Project root, Project common, CommonMLExtension extension) {
        ProjectHelper.applyToProject(root, {

            dependencies {
                compileOnly common
            }

            if (extension.addCommonSourceToAll) {
                Spec<Task> notNeoTask = { Task t -> !t.name.startsWith("neo") } as Spec<Task>

                tasks.withType(JavaCompile).matching(notNeoTask).configureEach {
                    source(common.sourceSets.main.allSource)
                }

                tasks.withType(Javadoc).matching(notNeoTask).configureEach {
                    source(common.sourceSets.main.allJava)
                }

                tasks.withType(ProcessResources).matching(notNeoTask).configureEach {
                    from common.sourceSets.main.resources
                }
            } else {
                tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME, JavaCompile) {
                    source(common.sourceSets.main.allSource)
                }

                tasks.named(JavaPlugin.JAVADOC_TASK_NAME, Javadoc) {
                    source(common.sourceSets.main.allJava)
                }

                tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME, ProcessResources) {
                    from common.sourceSets.main.resources
                }
            }


            tasks.named("sourcesJar", Jar) {
                from(common.sourceSets.main.allSource)
            }

//            tasks.withType(JavaCompile) {
//                source(common.sourceSets.main.allSource)
//            }
//
//            tasks.withType(ProcessResources) {
//                from common.sourceSets.main.resources
//                if (common.getTasks().getNames().contains(GENERATE_MIXIN_TASK)) {
//                    dependsOn(common.tasks.named(GENERATE_MIXIN_TASK))
//                }
//            }

        })
    }

}
