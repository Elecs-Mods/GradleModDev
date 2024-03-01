package nl.elec332.gradle.minecraft.moddev.projects.forge.forge;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.ProjectHelper;
import nl.elec332.gradle.minecraft.moddev.SettingsPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.ModMetadata;
import nl.elec332.gradle.minecraft.moddev.projects.forge.ForgeBasedPlugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven;
import org.gradle.jvm.tasks.Jar;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 04-09-2023
 */
public class ForgeProjectPlugin extends ForgeBasedPlugin<ForgeExtension> {

    public ForgeProjectPlugin() {
        super(ModLoader.FORGE);
    }

    @Override
    protected String getArchiveAppendix() {
        return "Forge";
    }

    @Override
    protected void preparePlugins(Project project, Settings settings) {
        addPlugin(project, "net.minecraftforge.gradle", MLProperties.FORGE_GRADLE_VERSION);
    }

    @Override
    protected void beforeProject(Project project) {
    }

    @Override
    protected void afterProject(Project project) {
        ForgeGroovyHelper.setRunSettings(project, getExtension(project));
        ForgeGroovyHelper.setMinecraftSettings(project, getExtension(project));
        ForgeGroovyHelper.setDependencies(project);
        project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME).finalizedBy("reobfJar");
        project.getTasks().withType(AbstractPublishToMaven.class, m -> m.dependsOn("reobfJar"));
    }

    @Override
    protected void addProperties(Consumer<String> pluginProps, Consumer<String> projectProps) {
        pluginProps.accept(MLProperties.FORGE_GRADLE_VERSION);

        projectProps.accept(MLProperties.FORGE_VERSION);
    }

    @Override
    protected void addMixinDependencies(Project project) {
        if (!ProjectHelper.hasProperty(project, MLProperties.MIXIN_VERSION)) {
            throw new RuntimeException("Missing property: " + MLProperties.MIXIN_VERSION);
        }
        ForgeGroovyHelper.addMixinAnnotationProcessor(project);
    }

    @Override
    protected void checkModMetadata(Project project, ModMetadata metadata) {
        if (metadata.getMixins() != null && !SettingsPlugin.isSuperCommonMode(project)) {
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
    protected Class<ForgeExtension> extensionType() {
        return ForgeExtension.class;
    }

}
