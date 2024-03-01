package nl.elec332.gradle.minecraft.moddev.projects.common;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ProjectHelper;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.ModMetadata;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;

import java.util.function.Consumer;

/**
 * Created by Elec332 on 02-09-2023
 */
public class CommonProjectPlugin extends AbstractPlugin<CommonProjectExtension> {

    public CommonProjectPlugin() {
        super(null);
    }

    @Override
    protected void preparePlugins(Project project, Settings settings) {
        ProjectHelper.addPlugin(project, "org.spongepowered.gradle.vanilla", ProjectHelper.getStringProperty(project, MLProperties.MIXIN_GRADLE_VANILLA));
    }

    @Override
    protected void beforeProject(Project project) {
    }

    @Override
    protected void afterProject(Project project) {
        CommonProjectGroovyHelper.setMinecraft(project);
        CommonProjectGroovyHelper.addMixinDep(project);
    }

    @Override
    protected void addProperties(Consumer<String> pluginProps, Consumer<String> projectProps) {
        pluginProps.accept(MLProperties.MIXIN_GRADLE_VANILLA);

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
    protected String getArchiveAppendix() {
        return "Common";
    }

    @Override
    protected Class<CommonProjectExtension> extensionType() {
        return CommonProjectExtension.class;
    }

}
