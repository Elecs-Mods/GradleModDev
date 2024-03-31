package nl.elec332.gradle.minecraft.moddev.projects.forge.neo;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ProjectType;
import nl.elec332.gradle.minecraft.moddev.projects.ModMetadata;
import nl.elec332.gradle.minecraft.moddev.projects.forge.ForgeBasedPlugin;
import nl.elec332.gradle.minecraft.moddev.tasks.GenerateEmptyRefMapTask;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.util.GradleVersion;

import java.util.function.Consumer;

/**
 * Created by Elec332 on 04-09-2023
 */
public class NeoProjectPlugin extends ForgeBasedPlugin<NeoExtension> {

    public NeoProjectPlugin() {
        super(ProjectType.NEO_FORGE);
    }

    private static final String GENERATE_REFMAP_TASK = "generateEmptyRefMap";

    @Override
    protected void addMixinDependencies(Project project) {
        NeoExtension extension = getExtension(project);
        GenerateEmptyRefMapTask mm = project.getTasks().create(GENERATE_REFMAP_TASK, GenerateEmptyRefMapTask.class);
        project.getTasks().named(GENERATE_METADATA, p -> p.dependsOn(mm));
        if (extension.mainModSource != null) {
            project.getTasks().named(extension.mainModSource.getProcessResourcesTaskName(), ProcessResources.class, r -> r.from(project.getTasks().named(GENERATE_REFMAP_TASK)));
        }
    }

    @Override
    protected void beforeProject(Project project) {
        if (GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("8.7")) < 0) {
            System.out.println("There's a bug with NeoGradle file locking on gradle versions <8.7, consider updating Gradle.");
        }
    }

    @Override
    public void afterRuntimePluginsAdded(Project project) {
        NeoGroovyHelper.setMinecraftSettings(project);
    }

    @Override
    protected void afterProject(Project project) {
        //Here because of https://github.com/neoforged/NeoGradle/issues/136
        project.getDependencies().add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, "net.neoforged:neoforge:" + ProjectHelper.getStringProperty(project, MLProperties.NEO_VERSION));

        NeoGroovyHelper.setRunSettings(project, getExtension(project));
    }

    @Override
    protected void addProperties(Consumer<String> projectProps) {
        projectProps.accept(MLProperties.NEO_VERSION);
    }

    @Override
    protected void checkModMetadata(Project project, ModMetadata metadata) {
        if (ProjectHelper.hasProperty(project, MLProperties.NEO_LOADER_VERSION)) {
            metadata.loaderVersion(ProjectHelper.getStringProperty(project, MLProperties.NEO_LOADER_VERSION));
        }
        String version = ProjectHelper.getStringProperty(project, MLProperties.NEO_VERSION);

        if (metadata.getLoaderVersion() == null) {
            metadata.loaderVersion(">=" + version.split("\\.")[0]);
        }

        if (!metadata.hasDependency("neoforge")) {
            String dep = ProjectHelper.hasProperty(project, MLProperties.NEO_VERSION_DEP) ? ProjectHelper.getStringProperty(project, MLProperties.NEO_VERSION_DEP) : null;
            metadata.dependsOn("neoforge", dep != null ? dep : (">=" + version));
        }
    }

    @Override
    protected Class<NeoExtension> extensionType() {
        return NeoExtension.class;
    }

}