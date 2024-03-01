package nl.elec332.gradle.minecraft.moddev.projects;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ProjectHelper;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

import java.util.Objects;
import java.util.Set;

/**
 * Created by Elec332 on 23-02-2024
 */
public abstract class AbstractPluginSC implements Plugin<Project> {

    @Override
    public final void apply(Project target) {
        ProjectHelper.checkProperties(target, Set.of(MLProperties.ELECLOADER_VERSION));
        target.getRepositories().mavenLocal();
        if (target.getRootProject() != target) {
            applyML(target);
        }
        applyPlugin(target, getSourceSet(target, SourceSet.MAIN_SOURCE_SET_NAME));
    }

    private void applyML(Project target) {
        target.getRootProject().afterEvaluate(root -> root.getConfigurations().getByName(COMMON_CONFIG_NAME).getDependencies().forEach(dep -> target.getDependencies().add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, dep)));
    }

    protected abstract void applyPlugin(Project target, SourceSet main);

    protected static final String COMMON_CONFIG_NAME = "commonImplementation";
    protected static final String JAR_TASK_NAME = "jar";
    protected static final String DEV_JAR_TASK_NAME = "devJar";
    protected static final String DEV_ALL_JAR_TASK_NAME = "devJarAll";
    protected static final String MOD_PUBLICATION = "modPublication";

    protected static void addToPublication(Project target, AbstractArchiveTask archive) {
        getModPublication(target).artifact(archive);
    }

    protected static MavenPublication getModPublication(Project target) {
        return Objects.requireNonNull(target.getExtensions().getByType(PublishingExtension.class)).getPublications().maybeCreate(MOD_PUBLICATION, MavenPublication.class);
    }

    protected static SourceSet getSourceSet(Project target, String name) {
        return target.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().maybeCreate(name);
    }

}
