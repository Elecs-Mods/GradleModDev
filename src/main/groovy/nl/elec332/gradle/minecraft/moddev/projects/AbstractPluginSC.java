package nl.elec332.gradle.minecraft.moddev.projects;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.SettingsPlugin;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenArtifact;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Created by Elec332 on 23-02-2024
 */
public abstract class AbstractPluginSC implements Plugin<Project> {

    @Override
    public final void apply(@NotNull Project target) {
        ProjectHelper.checkProperties(target, Set.of(MLProperties.ELECLOADER_VERSION));
        target.getRepositories().mavenLocal();

        Project commonProject = SettingsPlugin.getDetails(target).getCommonProject();
        target.beforeEvaluate(p -> {
            var devTask = target.getTasks().named(AbstractPlugin.DEV_JAR_TASK_NAME, Jar.class, j -> j.getManifest().attributes(Map.of(MAPPINGS, ModLoader.Mapping.NAMED)));
            addToPublication(commonProject, devTask);
        });

        applyPlugin(target, ProjectHelper.getSourceSets(target).maybeCreate(SourceSet.MAIN_SOURCE_SET_NAME));
    }

    protected abstract void applyPlugin(Project target, SourceSet main);

    protected static final String COMMON_CONFIG_NAME = "commonImplementation";
    protected static final String DEV_ALL_JAR_TASK_NAME = "devAllJar";
    protected static final String REMAPPED_JAR_TASK_NAME = "remappedJar";
    protected static final String MOD_PUBLICATION = "modPublication";
    protected static final String MAPPINGS = "Mappings";

    protected static void addToPublication(Project target, Object archive) {
        addToPublication(target, archive, null);
    }

    protected static void addToPublication(Project target, Object archive, Action<? super MavenArtifact> config) {
        MavenArtifact ma = getModPublication(target).artifact(archive);
        if (config != null) {
            config.execute(ma);
        }
    }

    protected static MavenPublication getModPublication(Project target) {
        return Objects.requireNonNull(target.getExtensions().getByType(PublishingExtension.class)).getPublications().maybeCreate(MOD_PUBLICATION, MavenPublication.class);
    }

}
