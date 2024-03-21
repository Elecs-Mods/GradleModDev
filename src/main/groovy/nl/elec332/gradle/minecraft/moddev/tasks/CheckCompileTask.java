package nl.elec332.gradle.minecraft.moddev.tasks;

import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;

/**
 * Created by Elec332 on 08-02-2024
 */
public abstract class CheckCompileTask extends JavaCompile {

    public CheckCompileTask() {
        JavaCompile main = (JavaCompile) getProject().getTasks().getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
        dependsOn(main);
        setEnabled(false);
        setSource(main.getSource());
        ProjectHelper.afterEvaluateSafe(getProject(), p -> {
            setClasspath(main.getClasspath());
            getDestinationDirectory().fileValue(new File(main.getDestinationDirectory().getAsFile().get().getParentFile(), "allClasses"));

            getOptions().setAnnotationProcessorPath(main.getOptions().getAnnotationProcessorPath());
            setClasspath(main.getClasspath());
            getOptions().setCompilerArgs(main.getOptions().getCompilerArgs());
        });
    }

    public void checkSource(SourceSet sourceSet) {
        source(sourceSet.getAllSource());
        setEnabled(true);
    }

}
