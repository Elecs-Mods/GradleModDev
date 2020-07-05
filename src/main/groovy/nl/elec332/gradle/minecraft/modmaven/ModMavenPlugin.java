package nl.elec332.gradle.minecraft.modmaven;

import nl.elec332.gradle.minecraft.moddev.tasks.GenerateTomlTask;
import nl.elec332.gradle.minecraft.moddev.ModDevPlugin;
import nl.elec332.gradle.util.GroovyHooks;
import nl.elec332.gradle.util.PluginHelper;
import nl.elec332.gradle.util.maven.MavenHelper;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.artifacts.mvnsettings.LocalMavenRepositoryLocator;
import org.gradle.api.plugins.MavenPlugin;

import javax.inject.Inject;

import static nl.elec332.gradle.util.Utils.isNullOrEmpty;

/**
 * Created by Elec332 on 2-7-2020
 */
@NonNullApi
public class ModMavenPlugin implements Plugin<Project> {

    @Inject
    public ModMavenPlugin(LocalMavenRepositoryLocator mavenRepositoryLocator) {
        this.mavenRepositoryLocator = mavenRepositoryLocator;
    }

    private final LocalMavenRepositoryLocator mavenRepositoryLocator;

    public static final String EXTENSION_NAME = "modMaven";

    @Override
    public void apply(Project project) {
        PluginHelper.checkMinimumGradleVersion("4.9");
        ModMavenExtension settings = project.getExtensions().create(EXTENSION_NAME, ModMavenExtension.class, project);
        String localMaven = MavenHelper.getLocalMavenProperty();
        if (localMaven != null) {
            project.getPluginManager().apply(MavenPlugin.class);
            project.afterEvaluate(p -> GroovyHooks.configureMaven(project, mavenDeployer -> {
                Runnable run = () -> {
                    MavenHelper.setMavenRepo(mavenDeployer, MavenHelper.getLocalMavenUrl(mavenRepositoryLocator));
                    MavenHelper.configureMaven(project, mavenDeployer, settings, ext -> {
                        if (isNullOrEmpty(ext.description)) {
                            ext.description = ext.artifactId;
                        }
                    }, !settings.forceLicense);
                };
                Object o = project.getTasks().findByName(ModDevPlugin.TOML_TASK_NAME);
                if (o != null) {
                    GenerateTomlTask task = (GenerateTomlTask) o;
                    runCompat(task, settings, project, run);
                } else {
                    run.run();
                }
            }));
        }
    }


    private void runCompat(GenerateTomlTask toml, ModMavenExtension maven, Project project, Runnable post) {
        if (!toml.hasEvaluated()) {
            project.afterEvaluate(p -> runCompat(toml, maven, project, post));
            return;
        }
        System.out.println("Syncing TOML and Maven data...");
        runCompat(toml, maven);
        post.run();
    }

    private void runCompat(GenerateTomlTask toml, ModMavenExtension maven) {
        if (isNullOrEmpty(toml.githubUrl)) {
            toml.githubUrl = maven.githubUrl;
        }
        if (isNullOrEmpty(maven.githubUrl)) {
            maven.githubUrl = toml.githubUrl;
        }
        if (isNullOrEmpty(toml.displayURL)) {
            toml.displayURL = maven.url;
        }
        if (isNullOrEmpty(maven.url)) {
            maven.url = toml.displayURL;
        }
        GenerateTomlTask.Mod mod = toml.mods.getByName(ModDevPlugin.getExtension(toml.getProject()).modId);
        if (isNullOrEmpty(mod.description)) {
            mod.description = maven.description;
        }
//        if (isNullOrEmpty(maven.description)) {
//            maven.description = mod.description;
//        }
        if (isNullOrEmpty(toml.authors)) {
            toml.authors = MavenHelper.getDevelopers(toml.getProject()).toString();
        }
    }

}
