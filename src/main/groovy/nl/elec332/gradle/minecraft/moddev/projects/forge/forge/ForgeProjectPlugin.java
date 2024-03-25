package nl.elec332.gradle.minecraft.moddev.projects.forge.forge;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ProjectType;
import nl.elec332.gradle.minecraft.moddev.projects.ModMetadata;
import nl.elec332.gradle.minecraft.moddev.projects.forge.ForgeBasedPlugin;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;

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

    @Override
    protected void beforeProject(Project project) {
        project.getTasks().withType(AbstractPublishToMaven.class, m -> m.dependsOn(REMAP_JAR_TASK));
    }

    @Override
    public void afterRuntimePluginsAdded(Project project) {
        project.getDependencies().add("minecraft", "net.minecraftforge:forge:" + ProjectHelper.getStringProperty(project, MLProperties.MC_VERSION) + "-" + ProjectHelper.getStringProperty(project, MLProperties.FORGE_VERSION));
        ForgeGroovyHelper.setMinecraftSettings(project);
        ForgeGroovyHelper.setRunSettings(project, getExtension(project));
    }

    @Override
    protected void afterProject(Project project) {
        ForgeGroovyHelper.setMixinRunSettings(project);
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
        project.afterEvaluate(p -> ProjectHelper.getSourceSets(p).forEach(ss -> p.getTasks().named(ss.getCompileJavaTaskName(), JavaCompile.class, c -> {
            c.dependsOn("createMcpToSrg");
            File destDir = c.getDestinationDirectory().getAsFile().get();
            c.getOptions().getCompilerArgs().addAll(List.of(
                    "-AreobfTsrgFile=" + p.getLayout().getBuildDirectory().file("createMcpToSrg/output.tsrg").get().getAsFile().getPath(),
                    "-AoutRefMapFile=" + new File(destDir, ProjectHelper.getMixinRefMap(project)).getPath(),
                    "-AmappingTypes=tsrg",
                    "-AdefaultObfuscationEnv=searge"
            ));
        })));
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
