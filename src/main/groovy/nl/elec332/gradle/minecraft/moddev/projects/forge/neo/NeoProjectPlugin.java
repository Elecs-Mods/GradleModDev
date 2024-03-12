package nl.elec332.gradle.minecraft.moddev.projects.forge.neo;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ProjectHelper;
import nl.elec332.gradle.minecraft.moddev.ProjectType;
import nl.elec332.gradle.minecraft.moddev.projects.ModMetadata;
import nl.elec332.gradle.minecraft.moddev.projects.forge.ForgeBasedPlugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.JavaPlugin;

import java.util.function.Consumer;

/**
 * Created by Elec332 on 04-09-2023
 */
public class NeoProjectPlugin extends ForgeBasedPlugin<NeoExtension> {

    public NeoProjectPlugin() {
        super(ProjectType.NEO_FORGE);
    }

    @Override
    protected void addMixinDependencies(Project project) {
    }

    @Override
    protected void preparePlugins(Project project, Settings settings) {
        addPlugin(project, "net.neoforged.gradle.userdev", MLProperties.NEO_GRADLE_VERSION);
    }

    @Override
    protected void beforeProject(Project project) {
        project.getDependencies().add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, "net.neoforged:neoforge:" + ProjectHelper.getStringProperty(project, MLProperties.NEO_VERSION));
    }

    @Override
    protected void afterProject(Project project) {
        NeoGroovyHelper.setRunSettings(project, getExtension(project));
        NeoGroovyHelper.setMinecraftSettings(project);
    }

    @Override
    protected void addProperties(Consumer<String> pluginProps, Consumer<String> projectProps) {
        pluginProps.accept(MLProperties.NEO_GRADLE_VERSION);

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