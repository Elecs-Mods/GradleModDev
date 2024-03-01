package nl.elec332.gradle.minecraft.moddev.projects;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.jvm.tasks.Jar;

import java.util.Collections;
import java.util.Objects;

/**
 * Created by Elec332 on 26-02-2024
 */
public abstract class AbstractPluginMLSC extends AbstractPluginSC {

    protected boolean addJarToMaven = true;

    @Override
    protected final void applyPlugin(Project target, SourceSet main) {
        SourceSet rootMain = getSourceSet(target.getRootProject(), SourceSet.MAIN_SOURCE_SET_NAME);
        target.afterEvaluate(trgt -> {
            trgt.getTasks().named(AbstractGroovyHelper.CHECK_CLASSES_TASK, CheckCompileTask.class, t -> {
                t.checkSource(rootMain);
            });
        });
        target.getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, target.getRootProject());
        String classifier = target.getName();

        target.getRootProject().getTasks().named(JavaPlugin.JAVADOC_TASK_NAME, Javadoc.class, t -> t.source(main.getJava()));
        target.getRootProject().getTasks().named("sourcesJar", Jar.class, t -> {
            Copy pr = (Copy) target.getTasks().getByName(main.getProcessResourcesTaskName());
            t.from(pr.getDestinationDir());
            t.from(main.getJava());
            t.dependsOn(pr);
        });
        target.getRootProject().getTasks().named(DEV_JAR_TASK_NAME, Jar.class, t -> t.from(main.getOutput()));

        target.getTasks().register(DEV_JAR_TASK_NAME, Jar.class, j -> {
            j.getArchiveClassifier().set(classifier + "-dev");
            j.from(main.getOutput());
            addToPublication(target.getRootProject(), j);
        });

        target.getTasks().register(DEV_ALL_JAR_TASK_NAME, Jar.class, j -> {
            j.getArchiveClassifier().set(classifier + "-all-dev");
            j.from(main.getOutput());
            j.from(rootMain.getOutput());
            addToPublication(target.getRootProject(), j);
        });

        target.getTasks().named(JAR_TASK_NAME, Jar.class, j -> {
            j.dependsOn(DEV_JAR_TASK_NAME, DEV_ALL_JAR_TASK_NAME);
            j.getArchiveClassifier().set(classifier);
            j.from(rootMain.getOutput());
            if (addJarToMaven) {
                addToPublication(target.getRootProject(), j);
            }
        });

        SourceSet ss = getSourceSet(target, "runTarget");
        ss.getJava().setSrcDirs(Collections.emptyList());
        ss.getResources().setSrcDirs(Collections.emptyList());
        var copyMod = target.getTasks().register("copyMod", Copy.class, t -> {
            t.from(main.getOutput());
            t.from(rootMain.getOutput());
            t.into(Objects.requireNonNull(ss.getOutput().getResourcesDir()));
        });
        target.getTasks().named(ss.getCompileJavaTaskName(), t -> t.dependsOn(copyMod));

        target.getConfigurations().named(ss.getRuntimeClasspathConfigurationName(), c -> c.extendsFrom(target.getConfigurations().getByName(main.getRuntimeClasspathConfigurationName())));

        applyMLPlugin(target, main, ss, rootMain);

        target.beforeEvaluate(p -> target.getExtensions().getByType(CommonMLExtension.class).runtimeSource = ss);
    }

    protected abstract void applyMLPlugin(Project target, SourceSet main, SourceSet run, SourceSet rootMain);

}
