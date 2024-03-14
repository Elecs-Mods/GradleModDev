package nl.elec332.gradle.minecraft.moddev.projects.fabric.quilt;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ProjectType;
import nl.elec332.gradle.minecraft.moddev.SettingsPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.ModMetadata;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.FabricBasedPlugin;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Project;

import java.util.function.Consumer;

/**
 * Created by Elec332 on 04-09-2023
 */
public class QuiltProjectPlugin extends FabricBasedPlugin<QuiltExtension> {

    public QuiltProjectPlugin() {
        super(ProjectType.QUILT);
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
    protected void addProperties(Consumer<String> projectProps) {
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