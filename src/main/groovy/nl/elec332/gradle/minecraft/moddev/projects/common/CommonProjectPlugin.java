package nl.elec332.gradle.minecraft.moddev.projects.common;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ProjectType;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.ModMetadata;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

import java.util.function.Consumer;

/**
 * Created by Elec332 on 02-09-2023
 */
public class CommonProjectPlugin extends AbstractPlugin<CommonProjectExtension> {

    public CommonProjectPlugin() {
        super(ProjectType.COMMON);
    }

    @Override
    protected void beforeProject(Project project) {
        project.getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, "org.spongepowered:mixin:" + ProjectHelper.getStringProperty(project, MLProperties.MIXIN_VERSION));
    }

    @Override
    public void afterRuntimePluginsAdded(Project project) {
        CommonProjectGroovyHelper.setMinecraft(project);
    }

    @Override
    protected void afterProject(Project project) {
    }

    @Override
    protected void addProperties(Consumer<String> projectProps) {
        projectProps.accept(MLProperties.MIXIN_VERSION);
    }

    @Override
    protected void checkModMetadata(Project project, ModMetadata metadata) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void addMixinDependencies(Project project) {
    }

    @Override
    protected Class<CommonProjectExtension> extensionType() {
        return CommonProjectExtension.class;
    }

}
