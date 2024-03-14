package nl.elec332.gradle.minecraft.moddev.projects.fabric.fabric;

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
public class FabricProjectPlugin extends FabricBasedPlugin<FabricExtension> {

    public FabricProjectPlugin() {
        super(ProjectType.FABRIC);
    }

    @Override
    protected void afterProject(Project project) {
        boolean api = getExtension(project).addApiDependency;
        if (api && !ProjectHelper.hasProperty(project, MLProperties.FABRIC_VERSION)) {
            throw new RuntimeException("Missing property: " + MLProperties.FABRIC_VERSION);
        }
        FabricGroovyHelper.setDependencies(project, api);
        super.afterProject(project);
    }

    @Override
    protected void addProperties(Consumer<String> projectProps) {
        projectProps.accept(MLProperties.FABRIC_LOADER_VERSION);
    }

    @Override
    protected void checkModMetadata(Project project, ModMetadata metadata) {
        if (getExtension(project).addApiDependency && !metadata.hasDependency("fabric-api")) {
            String version = ProjectHelper.hasProperty(project, MLProperties.FABRIC_VERSION_DEP) ? ProjectHelper.getStringProperty(project, MLProperties.FABRIC_VERSION_DEP) : null;
            metadata.dependsOn("fabric-api", version != null ? version : (">=" + getApiVersion(ProjectHelper.getStringProperty(project, MLProperties.FABRIC_VERSION))));
        }
        if (!metadata.hasDependency("fabricloader") && !SettingsPlugin.isSuperCommonMode(project)) {
            metadata.dependsOn("fabricloader", ">=" + ProjectHelper.getStringProperty(project, MLProperties.FABRIC_LOADER_VERSION));
        }
    }

    @Override
    protected Class<FabricExtension> extensionType() {
        return FabricExtension.class;
    }

}
