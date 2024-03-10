package nl.elec332.gradle.minecraft.moddev.projects.fabric.quilt;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.ProjectHelper;
import nl.elec332.gradle.minecraft.moddev.SettingsPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.ModMetadata;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.FabricBasedPlugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;

import java.util.function.Consumer;

/**
 * Created by Elec332 on 04-09-2023
 */
public class QuiltProjectPlugin extends FabricBasedPlugin<QuiltExtension> {

    public QuiltProjectPlugin() {
        super(ModLoader.QUILT);
    }

    @Override
    protected String getArchiveAppendix() {
        return "Quilt";
    }

    @Override
    protected void preparePlugins(Project project, Settings settings) {
        addPlugin(project, "org.quiltmc.loom", MLProperties.QUILT_LOOM_VERSION);
    }

    @Override
    protected void beforeProject(Project project) {
    }

    @Override
    protected void afterProject(Project project) {
        boolean api = getExtension(project).addApiDependency;
        if (api && !ProjectHelper.hasProperty(project, MLProperties.QUILT_VERSION)) {
            throw new RuntimeException("Missing property: " + MLProperties.QUILT_VERSION);
        }
        QuiltGroovyHelper.setDependencies(project, api);
        QuiltGroovyHelper.disableTracking(project); //Fuck you Quilt
        super.afterProject(project);
    }

    @Override
    protected void addProperties(Consumer<String> pluginProps, Consumer<String> projectProps) {
        pluginProps.accept(MLProperties.QUILT_LOOM_VERSION);

        projectProps.accept(MLProperties.QUILT_LOADER_VERSION);
    }

    @Override
    protected void checkModMetadata(Project project, ModMetadata metadata) {
        if (getExtension(project).addApiDependency && !metadata.hasDependency("quilted_fabric_api")) {
            String version = ProjectHelper.hasProperty(project, MLProperties.QUILT_VERSION_DEP) ? ProjectHelper.getStringProperty(project, MLProperties.QUILT_VERSION_DEP) : null;
            metadata.dependsOn("quilted_fabric_api", version != null ? version : (">=" + getApiVersion(ProjectHelper.getStringProperty(project, MLProperties.QUILT_VERSION))));
        }
        if (!metadata.hasDependency("quilt_loader") && !SettingsPlugin.isSuperCommonMode(project)) {
            metadata.dependsOn("quilt_loader", ">=" + ProjectHelper.getStringProperty(project, MLProperties.QUILT_LOADER_VERSION));
        }
    }

    @Override
    protected Class<QuiltExtension> extensionType() {
        return QuiltExtension.class;
    }

}