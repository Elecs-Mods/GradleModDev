package nl.elec332.gradle.minecraft.moddev.projects;

import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.ProjectHelper;
import org.gradle.api.Project;
import org.gradle.api.Task;
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

        addToPublication(target.getRootProject(), target.getTasks().register(DEV_JAR_TASK_NAME, Jar.class, j -> {
            j.getArchiveClassifier().set(classifier + "-dev");
            j.from(main.getOutput());
            j.getManifest().attributes(Map.of(MAPPINGS, ModLoader.Mapping.NAMED));
        }));

        var jarTask = target.getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class, j -> {
            j.getArchiveClassifier().set(classifier);
            j.from(rootMain.getOutput());
            j.getManifest().attributes(Map.of(MAPPINGS, Objects.requireNonNull(ProjectHelper.getPlugin(target).getModLoader().getMapping())));
        });

        var remapTask = target.getTasks().register(REMAPPED_JAR_TASK_NAME, t -> {
            var ret = setupRemapTask(target, t);
            if (ret == null) {
                //Either there is no remapping, or the remap task doesn't have outputs setup correctly (ForgeGradle)
                ret = jarTask;
            }
            if (!ret.isPresent()) {
                throw new IllegalStateException();
            }
            t.dependsOn(ret);
            t.getInputs().files(ret.get().getOutputs().getFiles());
            t.getOutputs().files(ret.get().getOutputs().getFiles());
        });

        target.afterEvaluate(p -> p.afterEvaluate(p2 -> addToPublication(p.getRootProject(), remapTask.get().getOutputs().getFiles().getSingleFile(), a -> {
            a.setClassifier(jarTask.get().getArchiveClassifier().get());
            a.builtBy(remapTask);
        })));

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

    protected TaskProvider<? extends Task> setupRemapTask(Project project, Task task) {
        return null;
    }

}
