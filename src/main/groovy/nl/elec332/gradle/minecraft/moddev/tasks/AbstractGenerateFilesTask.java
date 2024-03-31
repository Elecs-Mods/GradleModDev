package nl.elec332.gradle.minecraft.moddev.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

/**
 * Created by Elec332 on 29-03-2024
 */
public abstract class AbstractGenerateFilesTask extends DefaultTask {

    public AbstractGenerateFilesTask(String defaultDir) {
        if (defaultDir != null) {
            getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir(defaultDir));
        }
    }

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    protected File getOutputFile(String target) {
        return getOutputDirectory().file(target).get().getAsFile();
    }

    @TaskAction
    private void generateAction() {
        getProject().delete(getOutputDirectory());
        generate();
    }

    protected abstract void generate();

}
