package nl.elec332.gradle.minecraft.moddev.projects.common;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPluginSC;
import nl.elec332.gradle.minecraft.moddev.tasks.AllModJarSetupTask;
import nl.elec332.gradle.minecraft.moddev.util.GradleInternalHelper;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlatformPlugin;
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
import java.util.stream.Stream;
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
        Configuration commonJarConfig = target.getConfigurations().create(COMMON_JAR_CONFIG_NAME);
        target.getConfigurations().getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(commonConfig);
        target.getDependencies().add(COMMON_CONFIG_NAME, "nl.elec332.minecraft.loader:ElecLoader:" + ProjectHelper.getStringProperty(target, MLProperties.ELECLOADER_VERSION) + ":dev");

        importProperties(target);

        var jarTask = target.getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class, j -> {
            j.getManifest().attributes(Map.of(MAPPINGS, ModLoader.Mapping.NAMED));
        });
        commonJarConfig.getOutgoing().artifact(jarTask);
        target.getTasks().named("sourcesJar", Jar.class, j -> j.filesMatching("/META-INF/mods.toml", c -> c.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)));

        var devAllTask = target.getTasks().register(DEV_ALL_JAR_TASK_NAME, Jar.class, j -> {
            j.getArchiveClassifier().set("all-dev");
            j.from(main.getOutput());
            j.filesMatching("/META-INF/mods.toml", c -> c.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE));
            j.getManifest().attributes(Map.of(MAPPINGS, ModLoader.Mapping.NAMED, "Implementation-Title", "all"));
        });
        target.getTasks().named(BasePlugin.ASSEMBLE_TASK_NAME, t -> t.dependsOn(devAllTask));

        var allTask = AllModJarSetupTask.createTaskChain(target, ALL_JAR_TASK_NAME);
        allTask.configure(j -> {
            j.getArchiveClassifier().set("all");
            j.getManifest().attributes(Map.of("Implementation-Title", "all"));
        });
        addToPublication(target, allTask);
        target.getTasks().named(BasePlugin.ASSEMBLE_TASK_NAME, t -> t.dependsOn(allTask));

        target.beforeEvaluate(p -> {
            p.getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class, j -> j.getDestinationDirectory().set(j.getDestinationDirectory().dir("common").get()));
            p.getTasks().named(AbstractPlugin.DEV_JAR_TASK_NAME, Jar.class, j -> j.getDestinationDirectory().set(j.getDestinationDirectory().dir("common").get()));
        });
        Stream.of(JavaPlatformPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME, JavaPlatformPlugin.API_ELEMENTS_CONFIGURATION_NAME)
                .map(name -> target.getConfigurations().getByName(name))
                .map(Configuration::getOutgoing)
                .forEach(pub -> {
                    pub.getArtifacts().clear(); //Remove "common" jar
                    pub.artifact(devAllTask);
                });

        getModPublication(target).from(target.getComponents().getByName(GradleInternalHelper.JAVA_MAIN_COMPONENT_NAME));
    }

    private void importProperties(Project target) {
        ScriptHandler scriptHandler = GradleInternalHelper.getGradleSettings(target).getBuildscript();
        scriptHandler.getRepositories().maven(m -> m.setUrl("https://modmaven.dev"));
        Configuration loader = scriptHandler.getConfigurations().create(LOADER_CONFIG_NAME);
        scriptHandler.getDependencies().add(LOADER_CONFIG_NAME, "nl.elec332.minecraft.loader:ElecLoader:" + ProjectHelper.getStringProperty(target, MLProperties.ELECLOADER_VERSION));

//        Configuration loader = target.getConfigurations().create(LOADER_CONFIG_NAME);
//        target.getDependencies().add(LOADER_CONFIG_NAME, "nl.elec332.minecraft.loader:ElecLoader:" + ProjectHelper.getStringProperty(target, MLProperties.ELECLOADER_VERSION));
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
