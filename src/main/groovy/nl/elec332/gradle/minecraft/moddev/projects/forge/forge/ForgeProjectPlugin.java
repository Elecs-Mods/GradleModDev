package nl.elec332.gradle.minecraft.moddev.projects.forge.forge;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ProjectType;
import nl.elec332.gradle.minecraft.moddev.projects.ModMetadata;
import nl.elec332.gradle.minecraft.moddev.projects.forge.ForgeBasedPlugin;
import nl.elec332.gradle.minecraft.moddev.tasks.GenerateEmptyRefMapTask;
import nl.elec332.gradle.minecraft.moddev.tasks.GenerateMcMetaTask;
import nl.elec332.gradle.minecraft.moddev.tasks.GenerateMixinJsonTask;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 04-09-2023
 */
public class ForgeProjectPlugin extends ForgeBasedPlugin<ForgeExtension> {

    public ForgeProjectPlugin() {
        super(ProjectType.FORGE);
    }

    public static final String REMAP_JAR_TASK = "reobfJar";
    private static final String GENERATE_PACKINFO_TASK = "generatePackInfo";

    @Override
    protected void beforeProject(Project project) {
        project.getTasks().withType(AbstractPublishToMaven.class, m -> m.dependsOn(REMAP_JAR_TASK));

        GenerateMcMetaTask mm = project.getTasks().create(GENERATE_PACKINFO_TASK, GenerateMcMetaTask.class);
        project.getTasks().named(GENERATE_METADATA, p -> p.dependsOn(mm));
    }

    @Override
    public void afterRuntimePluginsAdded(Project project) {
        project.getDependencies().add("minecraft", "net.minecraftforge:forge:" + ProjectHelper.getStringProperty(project, MLProperties.MC_VERSION) + "-" + ProjectHelper.getStringProperty(project, MLProperties.FORGE_VERSION));
        ForgeGroovyHelper.setMinecraftSettings(project);
    }

    @Override
    protected void afterProject(Project project) {
        ForgeExtension extension = getExtension(project);
        ForgeGroovyHelper.setRunSettings(project, extension);
        ForgeGroovyHelper.setMixinRunSettings(project);
        if (extension.mainModSource != null) {
            project.getTasks().named(extension.mainModSource.getProcessResourcesTaskName(), ProcessResources.class, r -> r.from(project.getTasks().named(GENERATE_PACKINFO_TASK)));
        }
    }

    @Override
    protected void addProperties(Consumer<String> projectProps) {
        projectProps.accept(MLProperties.FORGE_VERSION);
    }

    @Override
    protected void addMixinDependencies(Project project) {
        if (!ProjectHelper.hasProperty(project, MLProperties.MIXIN_VERSION)) {
            throw new RuntimeException("Missing property: " + MLProperties.MIXIN_VERSION);
        }
        project.getDependencies().add(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, "org.spongepowered:mixin:" + ProjectHelper.getProperty(project, MLProperties.MIXIN_VERSION) + ":processor");
        File rootDir = project.getLayout().getBuildDirectory().dir("generated/refmaps").get().getAsFile();
        project.afterEvaluate(p -> ProjectHelper.getSourceSets(p).forEach(ss -> {
            File ssRefMap = new File(rootDir,  ss.getName() + "/" + ProjectHelper.getMixinRefMap(project));
            TaskProvider<JavaCompile> compileTask = p.getTasks().named(ss.getCompileJavaTaskName(), JavaCompile.class, c -> {
                c.dependsOn("createMcpToSrg");
                c.getOptions().getCompilerArgs().addAll(List.of(
                        "-AreobfTsrgFile=" + p.getLayout().getBuildDirectory().file("createMcpToSrg/output.tsrg").get().getAsFile().getPath(),
                        "-AoutRefMapFile=" + ssRefMap.getPath(),
                        "-AmappingTypes=tsrg",
                        "-AdefaultObfuscationEnv=searge"
                ));
            });
            p.getTasks().named(ss.getProcessResourcesTaskName(), ProcessResources.class, r -> {
                r.from(ssRefMap);
                r.dependsOn(compileTask);
            });
        }));

        project.getTasks().withType(GenerateMixinJsonTask.class).forEach(t -> t.getVariants().add("named_"));
        ForgeExtension extension = getExtension(project);
        TaskProvider<GenerateEmptyRefMapTask> mm = project.getTasks().register(GenerateEmptyRefMapTask.DEFAULT_TASK_NAME, GenerateEmptyRefMapTask.class, "named_");
        project.getTasks().named(GENERATE_METADATA, p -> p.dependsOn(mm));
        if (extension.mainModSource != null) {
            project.getTasks().named(extension.mainModSource.getProcessResourcesTaskName(), ProcessResources.class, r -> r.from(mm));
        }
    }

    @Override
    protected void checkModMetadata(Project project, ModMetadata metadata) {
        if (metadata.getMixins() != null && getExtension(project).addMixinsToManifest) {
            project.getTasks().withType(Jar.class, jar -> {
                jar.manifest(manifest -> manifest.attributes(Map.of(
                        "MixinConfigs", String.join(",", metadata.getMixins())
                )));
            });
            ForgeGroovyHelper.addMixins(project, metadata.getMixins());
        }

        if (ProjectHelper.hasProperty(project, MLProperties.FORGE_LOADER_VERSION)) {
            metadata.loaderVersion(ProjectHelper.getStringProperty(project, MLProperties.FORGE_LOADER_VERSION));
        }
        String version = ProjectHelper.getStringProperty(project, MLProperties.FORGE_VERSION);
        if (metadata.getLoaderVersion() == null) {
            metadata.loaderVersion(">=" + version.split("\\.")[0]);
        }

        if (!metadata.hasDependency("forge")) {
            String dep = ProjectHelper.hasProperty(project, MLProperties.FORGE_VERSION_DEP) ? ProjectHelper.getStringProperty(project, MLProperties.FORGE_VERSION_DEP) : null;
            metadata.dependsOn("forge", dep != null ? dep : (">=" + version));
        }
    }

    @Override
    protected TaskProvider<? extends AbstractArchiveTask> setupRemapTask(Project project, Task task, TaskProvider<Jar> jarTask) {
        task.dependsOn(ForgeProjectPlugin.REMAP_JAR_TASK);
        return jarTask;
    }

    @Override
    protected Class<ForgeExtension> extensionType() {
        return ForgeExtension.class;
    }

}
