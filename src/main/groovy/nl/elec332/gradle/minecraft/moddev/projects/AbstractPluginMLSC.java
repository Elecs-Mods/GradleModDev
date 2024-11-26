package nl.elec332.gradle.minecraft.moddev.projects;

import nl.elec332.gradle.minecraft.moddev.SettingsPlugin;
import nl.elec332.gradle.minecraft.moddev.tasks.CheckCompileTask;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.jvm.tasks.Jar;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Elec332 on 26-02-2024
 */
public abstract class AbstractPluginMLSC extends AbstractPluginSC {

    @Override
    protected final void applyPlugin(Project target, SourceSet main) {
        Project commonProject = SettingsPlugin.getDetails(target).getCommonProject();
        target.getDependencies().add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, target.getDependencies().project(Map.of("path", commonProject.getPath(), "configuration", COMMON_CONFIG_NAME)));
        target.getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, target.getDependencies().project(Map.of("path", commonProject.getPath(), "configuration", COMMON_JAR_CONFIG_NAME)));
        SourceSet commonMain = ProjectHelper.getSourceSets(commonProject).maybeCreate(SourceSet.MAIN_SOURCE_SET_NAME);
        target.beforeEvaluate(trgt -> trgt.getTasks().named(AbstractPlugin.CHECK_CLASSES_TASK, CheckCompileTask.class, t -> t.checkSource(commonMain)));

        commonProject.getTasks().named(JavaPlugin.JAVADOC_TASK_NAME, Javadoc.class, t -> t.source(main.getJava()));
        commonProject.getTasks().named("sourcesJar", Jar.class, t -> {
            Copy pr = (Copy) target.getTasks().getByName(main.getProcessResourcesTaskName());
            t.from(pr.getDestinationDir());
            t.from(main.getJava());
            t.dependsOn(pr);
        });
        commonProject.getTasks().named(DEV_ALL_JAR_TASK_NAME, Jar.class, t -> t.from(main.getOutput()));

        target.getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class, j -> {
            j.from(commonMain.getOutput());
            j.getManifest().attributes(Map.of(MAPPINGS, Objects.requireNonNull(Objects.requireNonNull(ProjectHelper.getPlugin(target).getProjectType().getModLoader()).getMapping())));
        });

        //SourceSet cannot be named 'runTarget', because NeoGradle will then attempt to make actual run configurations from it...
        SourceSet ss = ProjectHelper.getSourceSets(target).maybeCreate("modRunTarget");
        ss.getJava().setSrcDirs(Collections.emptyList());
        ss.getResources().setSrcDirs(Collections.emptyList());
        var copyMod = target.getTasks().register("copyMod", Copy.class, t -> {
            t.from(main.getOutput());
            t.from(commonMain.getOutput());
            t.into(Objects.requireNonNull(ss.getOutput().getResourcesDir()));
        });
        target.getTasks().named(ss.getCompileJavaTaskName(), t -> t.dependsOn(copyMod));

        target.getConfigurations().named(ss.getRuntimeClasspathConfigurationName(), c -> c.extendsFrom(target.getConfigurations().getByName(main.getRuntimeClasspathConfigurationName())));

        applyMLPlugin(target, main, ss, commonMain);

        target.beforeEvaluate(p -> target.getExtensions().getByType(CommonMLExtension.class).runtimeSource = ss);
        target.afterEvaluate(p -> {
            SourceSet mss = target.getExtensions().getByType(CommonMLExtension.class).mainModSource;
            if (mss != null && mss != main) {
                throw new UnsupportedOperationException();
            }
        });
    }

    protected abstract void applyMLPlugin(Project target, SourceSet main, SourceSet run, SourceSet commonMain);

}
