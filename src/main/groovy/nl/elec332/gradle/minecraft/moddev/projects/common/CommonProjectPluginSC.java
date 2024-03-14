package nl.elec332.gradle.minecraft.moddev.projects.common;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPluginSC;
import nl.elec332.gradle.minecraft.moddev.util.GradleInternalHelper;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.tasks.Jar;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Created by Elec332 on 23-02-2024
 */
public final class CommonProjectPluginSC extends AbstractPluginSC {

    private static final String LOADER_CONFIG_NAME = "elecLoader";

    @Override
    public void applyPlugin(Project target, SourceSet main) {
        target.getPluginManager().apply(MavenPublishPlugin.class);

        Configuration commonConfig = target.getConfigurations().create(COMMON_CONFIG_NAME);
        target.getConfigurations().getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(commonConfig);
        target.getDependencies().add(COMMON_CONFIG_NAME, "nl.elec332.minecraft.loader:ElecLoader:" + ProjectHelper.getStringProperty(target, MLProperties.ELECLOADER_VERSION));

        importProperties(target);

        target.getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class).configure(j -> {
            j.getArchiveClassifier().set("common-dev");
            j.getManifest().attributes(Map.of(MAPPINGS, ModLoader.Mapping.NAMED, "Implementation-Title", "common"));
        });
        target.getTasks().named("sourcesJar", Jar.class, j -> j.filesMatching("/META-INF/mods.toml", c -> c.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)));
        var devTask = target.getTasks().register(DEV_JAR_TASK_NAME, Jar.class, j -> {
            j.getArchiveClassifier().set("dev");
            j.from(main.getOutput());
            j.filesMatching("/META-INF/mods.toml", c -> c.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE));
            j.getManifest().attributes(Map.of(MAPPINGS, ModLoader.Mapping.NAMED, "Implementation-Title", "all"));
        });
        addToPublication(target, devTask);
        target.getArtifacts().add("archives", devTask);
        getModPublication(target).from(target.getComponents().getByName(GradleInternalHelper.JAVA_MAIN_COMPONENT_NAME));
    }

    private void importProperties(Project target) {
        Configuration loader = target.getConfigurations().create(LOADER_CONFIG_NAME);
        target.getDependencies().add(LOADER_CONFIG_NAME, "nl.elec332.minecraft.loader:ElecLoader:" + ProjectHelper.getStringProperty(target, MLProperties.ELECLOADER_VERSION));
        Set<File> loaders = loader.resolve();
        if (loaders.size() != 1) {
            throw new RuntimeException();
        }
        try (JarFile loaderFile = new JarFile(loaders.iterator().next())) {
            ZipEntry entry = Objects.requireNonNull(loaderFile.getEntry("targets.txt"), "Failed to find targets file!");
            Properties props = new Properties();
            props.load(loaderFile.getInputStream(entry));

            for (String s : MLProperties.ALL_PROPS) {
                if (props.containsKey(s) && !ProjectHelper.hasProperty(target, s)) {
                    ProjectHelper.setProperty(target, s, Objects.requireNonNull(props.getProperty(s)));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        target.getConfigurations().remove(loader);
    }

}
