package nl.elec332.gradle.minecraft.moddev.projects;

import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.SettingsPlugin;
import nl.elec332.gradle.minecraft.moddev.tasks.CheckCompileTask;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
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
        commonProject.afterEvaluate(p -> p.getConfigurations().getByName(COMMON_CONFIG_NAME).getDependencies().forEach(dep -> target.getDependencies().add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, dep)));
        SourceSet commonMain = ProjectHelper.getSourceSets(commonProject).maybeCreate(SourceSet.MAIN_SOURCE_SET_NAME);
        target.beforeEvaluate(trgt -> trgt.getTasks().named(AbstractPlugin.CHECK_CLASSES_TASK, CheckCompileTask.class, t -> t.checkSource(commonMain)));
        target.getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, commonProject);
        String classifier = target.getName();

        commonProject.getTasks().named(JavaPlugin.JAVADOC_TASK_NAME, Javadoc.class, t -> t.source(main.getJava()));
        commonProject.getTasks().named("sourcesJar", Jar.class, t -> {
            Copy pr = (Copy) target.getTasks().getByName(main.getProcessResourcesTaskName());
            t.from(pr.getDestinationDir());
            t.from(main.getJava());
            t.dependsOn(pr);
        });
        commonProject.getTasks().named(DEV_ALL_JAR_TASK_NAME, Jar.class, t -> t.from(main.getOutput()));

        var devTask = target.getTasks().register(DEV_JAR_TASK_NAME, Jar.class, j -> {
            j.getArchiveClassifier().set(classifier + "-dev");
            j.from(main.getOutput());
            j.getManifest().attributes(Map.of(MAPPINGS, ModLoader.Mapping.NAMED));
        });
        addToPublication(commonProject, devTask);
        target.getTasks().named(BasePlugin.ASSEMBLE_TASK_NAME, t -> t.dependsOn(devTask));

        var jarTask = target.getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class, j -> {
            j.from(commonMain.getOutput());
            j.getManifest().attributes(Map.of(MAPPINGS, Objects.requireNonNull(Objects.requireNonNull(ProjectHelper.getPlugin(target).getProjectType().getModLoader()).getMapping())));
        });

        var remapTask = target.getTasks().register(REMAPPED_JAR_TASK_NAME, t -> {
            var ret = setupRemapTask(target, t);
            if (ret != null && !ret.isPresent()) {
                throw new IllegalStateException();
            }
            Task dep;
            if (ret == null) {
                //Either there is no remapping, or the remap task doesn't have outputs setup correctly (ForgeGradle)
                dep = jarTask.get();
            } else {
                //In case the remapping task doesn't have its inputs setup correctly
                dep = ret.get();
                dep.dependsOn(jarTask);
            }
            t.dependsOn(dep);
            t.getInputs().files(dep.getOutputs().getFiles());
            t.getOutputs().files(dep.getOutputs().getFiles());
        });
        target.getTasks().named(BasePlugin.ASSEMBLE_TASK_NAME, t -> t.dependsOn(remapTask));
        target.afterEvaluate(p -> addToPublication(commonProject, remapTask.get().getOutputs().getFiles().getSingleFile(), a -> {
            a.setClassifier(jarTask.get().getArchiveClassifier().get());
            a.builtBy(remapTask);
        }));

        SourceSet ss = ProjectHelper.getSourceSets(target).maybeCreate("runTarget");
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
    }

    protected abstract void applyMLPlugin(Project target, SourceSet main, SourceSet run, SourceSet commonMain);

    protected TaskProvider<? extends Task> setupRemapTask(Project project, Task task) {
        return null;
    }

}
