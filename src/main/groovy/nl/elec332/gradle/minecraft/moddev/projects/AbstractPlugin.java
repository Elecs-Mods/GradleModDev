package nl.elec332.gradle.minecraft.moddev.projects;

import nl.elec332.gradle.minecraft.moddev.*;
import nl.elec332.gradle.minecraft.moddev.tasks.CheckCompileTask;
import nl.elec332.gradle.minecraft.moddev.tasks.GenerateMixinJsonTask;
import nl.elec332.gradle.minecraft.moddev.tasks.GenerateModInfoTask;
import nl.elec332.gradle.minecraft.moddev.tasks.RemapJarTask;
import nl.elec332.gradle.minecraft.moddev.util.J8Helper;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import nl.elec332.gradle.minecraft.moddev.util.ProjectPluginInitializer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.tasks.GenerateModuleMetadata;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskInputs;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 02-09-2023
 */
public abstract class AbstractPlugin<E extends CommonExtension> implements Plugin<Project>, ProjectPluginInitializer.Listener {

    protected AbstractPlugin(ProjectType projectType) {
        this.projectType = projectType;
        if (projectType == null) {
            throw new UnsupportedOperationException();
        }
        if (projectType.getPluginClass() != getClass()) {
            throw new IllegalStateException();
        }
    }

    public static String GENERATE_MIXIN_TASK = "generateMixinJson";
    public static String GENERATE_MODINFO_TASK = "generateModInfo";
    public static String CHECK_CLASSES_TASK = "checkClasses";
    public static String GENERATE_METADATA = "generateMetadata";
    protected static final String REMAPPED_JAR_TASK_NAME = "remappedJar";

    private final ProjectType projectType;

    @Override
    public final void apply(@NotNull Project target) {
        SettingsPlugin.ModDevDetails cfg = SettingsPlugin.getDetails(target);

        Set<String> projectProps = new HashSet<>(J8Helper.listOf(MLProperties.MC_VERSION, MLProperties.MOD_VERSION, MLProperties.MOD_ID, MLProperties.MOD_GROUP_ID, MLProperties.MOD_NAME, MLProperties.MOD_AUTHORS, MLProperties.MOD_LICENSE, MLProperties.MOD_DESCRIPTION));
        addProperties(projectProps::add);
        E extension = target.getExtensions().create(extensionType(), "modloader", extensionType());
        boolean isCommon = getProjectType() == ProjectType.COMMON;
        AllProjectsPlugin.setProperties(target, getProjectType());

        TaskContainer tasks = target.getTasks();
        SettingsPlugin.addRepositories(target.getRepositories());
        ProjectHelper.checkProperties(target, projectProps);
        target.setVersion(ProjectHelper.getStringProperty(target, MLProperties.MOD_VERSION));
        target.setGroup(ProjectHelper.getStringProperty(target, MLProperties.MOD_GROUP_ID));
        target.getExtensions().configure(BasePluginExtension.class, e -> e.getArchivesName().set(ProjectHelper.getStringProperty(target, MLProperties.MOD_NAME)));
        tasks.register(CHECK_CLASSES_TASK, CheckCompileTask.class);
        tasks.named(JavaPlugin.CLASSES_TASK_NAME, it -> it.dependsOn(CHECK_CLASSES_TASK));
        tasks.register(GENERATE_METADATA).configure(gm -> {
            tasks.withType(GenerateMixinJsonTask.class).forEach(gm::dependsOn);
            tasks.withType(GenerateModInfoTask.class).forEach(gm::dependsOn);
        });

        tasks.withType(JavaCompile.class).configureEach(t -> t.dependsOn(GENERATE_METADATA));
        tasks.withType(Jar.class).configureEach(t -> t.dependsOn(GENERATE_METADATA));
        tasks.withType(ProcessResources.class).configureEach(t -> t.dependsOn(GENERATE_METADATA));

        if (!isCommon) {
            tasks.create(GENERATE_MIXIN_TASK, GenerateMixinJsonTask.class);
            tasks.create(GENERATE_MODINFO_TASK, GenerateModInfoTask.class).onlyIf(t -> cfg.generateModInfo());
        } else {
            extension.metadata(md -> {
                String id = ProjectHelper.getStringProperty(target, MLProperties.MOD_ID);
                md.mod(id, mi -> {
                    mi.modVersion(ProjectHelper.getStringProperty(target, MLProperties.MOD_VERSION));
                    mi.modName(ProjectHelper.getStringProperty(target, MLProperties.MOD_NAME));
                    mi.setAuthors(J8Helper.listOf(ProjectHelper.getStringProperty(target, MLProperties.MOD_AUTHORS).split(",")));
                    mi.modDescription(ProjectHelper.getStringProperty(target, MLProperties.MOD_DESCRIPTION));
                });
                md.modGroupId(ProjectHelper.getStringProperty(target, MLProperties.MOD_GROUP_ID));
                md.modLicense(ProjectHelper.getStringProperty(target, MLProperties.MOD_LICENSE));
            });
        }

        tasks.withType(GenerateModInfoTask.class).configureEach(gm -> tasks.withType(GenerateMixinJsonTask.class).forEach(gm::dependsOn));

        TaskProvider<Jar> jarTask = tasks.named(JavaPlugin.JAR_TASK_NAME, Jar.class, j -> j.getArchiveClassifier().set(getArchiveClassifier()));
        TaskProvider<RemapJarTask> remapTask = target.getTasks().register(REMAPPED_JAR_TASK_NAME, RemapJarTask.class, t -> {
            TaskProvider<? extends AbstractArchiveTask> ret = setupRemapTask(target, t, jarTask);
            if (ret != null && !ret.isPresent()) {
                throw new IllegalStateException();
            }
            t.setup(ret == null ? jarTask.get() : ret.get());
            t.getMapping().set(Optional.ofNullable(getProjectType().getModLoader()).map(ModLoader::getMapping).orElse(ModLoader.Mapping.NAMED));
        });
        target.getTasks().named(BasePlugin.ASSEMBLE_TASK_NAME, t -> t.dependsOn(remapTask));

        tasks.withType(Jar.class).configureEach(jar -> jar.manifest(manifest -> manifest.attributes(J8Helper.mapOf(
                J8Helper.entry("Specification-Title", ProjectHelper.getStringProperty(target, MLProperties.MOD_ID)),
                J8Helper.entry("Specification-Vendor", ProjectHelper.getStringProperty(target, MLProperties.MOD_AUTHORS)),
                J8Helper.entry("Specification-Version", '1'),
                J8Helper.entry("Implementation-Title", projectType.getName()),
                J8Helper.entry("Implementation-Version", jar.getArchiveVersion()),
                J8Helper.entry("Implementation-Vendor", ProjectHelper.getStringProperty(target, MLProperties.MOD_AUTHORS)),
                J8Helper.entry("Implementation-Timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()))
        ).entrySet().stream().filter(e -> !manifest.getAttributes().containsKey(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))));

        tasks.withType(ProcessResources.class).configureEach(pr -> {
            TaskInputs inputs = pr.getInputs();
            for (String s : MLProperties.ALL_PROPS) {
                if (ProjectHelper.hasProperty(target, s)) {
                    inputs.property(s, ProjectHelper.getStringProperty(target, s));
                }
            }
            inputs.property("version", target.getVersion());
            pr.filesMatching(J8Helper.listOf("/META-INF/mods.toml", "pack.mcmeta", "/fabric.mod.json", "/quilt.mod.json", "/*.txt"), f -> f.expand(inputs.getProperties(), d -> d.getEscapeBackslash().set(true)).filter(s -> s.replace("\\$", "$")));
        });

        target.afterEvaluate(p -> {
            if (extension.noModuleMetadata) {
                p.getTasks().withType(GenerateModuleMetadata.class).configureEach(meta -> meta.setEnabled(false));
            }

            p.getTasks().withType(GenerateMixinJsonTask.class).forEach(j -> j.addMixins(extension.getMixins()));
            if (extension instanceof CommonMLExtension) {
                ModMetadata md;
                if (!cfg.singleProject()) {
                    Project common = cfg.getCommonProject();
                    CommonExtension commonExtension = Objects.requireNonNull(common.getExtensions().getByType(CommonExtension.class), "Failed to find extension in common project");
                    md = ModMetadataImpl.generate(p, commonExtension.getMetaModifiers());
                    extension.getMetaModifiers().forEach(a -> a.execute(md));
                    if (((CommonMLExtension) extension).addCommonDependency) {
                        if (cfg.isSuperCommonMode()) {
                            throw new UnsupportedOperationException("Cannot add CommonDependency to SuperCommon project");
                        }
                        importCommonProject(p, common, (CommonMLExtension) extension);
                    }
                    if (((CommonMLExtension) extension).addCommonMixins) {
                        p.getTasks().withType(GenerateMixinJsonTask.class).forEach(j -> j.addMixins(commonExtension.getMixins()));
                    }
                } else {
                    md = ModMetadataImpl.generate(p, extension.getMetaModifiers());
                }
                p.getTasks().withType(GenerateMixinJsonTask.class).forEach(j -> j.getMetaMixinFiles().forEach(md::mixin));
                if (md.getMixins() != null) {
                    addMixinDependencies(p);
                }
                checkModMetadata(p, md);
                p.getTasks().withType(GenerateModInfoTask.class).configureEach(pr -> pr.getMetaData().set(md));
                for (Project dp : ((CommonMLExtension) extension).getMetaImports()) {
                    if (dp == null || dp == p) {
                        continue;
                    }
                    if (!ProjectHelper.hasEvaluated(dp)) {
                        dp.afterEvaluate(ddp -> importModMeta(ddp, p));
                    } else {
                        importModMeta(dp, p);
                    }
                }
            } else if (!extension.getMixins().isEmpty()) {
                addMixinDependencies(p);
            }
        });
        target.beforeEvaluate(this::beforeProject);
        target.afterEvaluate(this::afterProject);
    }

    private void importModMeta(Project from, Project to) {
        from.getTasks().withType(GenerateModInfoTask.class).configureEach(pr -> to.getTasks().withType(GenerateModInfoTask.class).configureEach(pr2 -> pr2.getMetaData().get().importFrom(pr.getMetaData().get())));
    }

    public final ProjectType getProjectType() {
        return this.projectType;
    }

    protected abstract void addMixinDependencies(Project project);

    protected String getArchiveClassifier() {
        return projectType.getName();
    }

    protected abstract void beforeProject(Project project);

    @Override
    public abstract void afterRuntimePluginsAdded(Project project);

    protected abstract void afterProject(Project project);

    protected abstract void addProperties(Consumer<String> projectProps);

    protected abstract void checkModMetadata(Project project, ModMetadata metadata);

    protected final E getExtension(Project project) {
        return project.getExtensions().getByType(extensionType());
    }

    protected abstract Class<E> extensionType();

    protected TaskProvider<? extends AbstractArchiveTask> setupRemapTask(Project project, Task task, TaskProvider<Jar> jarTask) {
        return null;
    }

    private static void importCommonProject(Project root, Project common, CommonMLExtension extension) {
        root.getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, common);
        TaskContainer tasks = root.getTasks();
        SourceSet commonMain = ProjectHelper.getSourceSets(root).maybeCreate(SourceSet.MAIN_SOURCE_SET_NAME);
        if (extension.addCommonSourceToAll) {
            Spec<Task> notNeoTask = t -> !t.getName().startsWith("neo");
            tasks.withType(JavaCompile.class).matching(notNeoTask).configureEach(c -> c.source(commonMain.getAllSource()));
            tasks.withType(Javadoc.class).matching(notNeoTask).configureEach(d -> d.source(commonMain.getAllJava()));
            tasks.withType(ProcessResources.class).matching(notNeoTask).configureEach(r -> r.from(commonMain.getResources()));
        } else {
            tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME, JavaCompile.class, c -> c.source(commonMain.getAllSource()));
            tasks.named(JavaPlugin.JAVADOC_TASK_NAME, Javadoc.class, d -> d.source(commonMain.getAllJava()));
            tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME, ProcessResources.class, r -> r.from(commonMain.getResources()));
        }
        tasks.named("sourcesJar", Jar.class, s -> s.from(commonMain.getAllSource()));
    }

}
