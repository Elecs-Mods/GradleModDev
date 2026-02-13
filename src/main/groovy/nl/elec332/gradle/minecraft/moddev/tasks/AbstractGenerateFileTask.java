package nl.elec332.gradle.minecraft.moddev.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

/**
 * Created by Elec332 on 30-03-2024
 */
public abstract class AbstractGenerateFileTask extends DefaultTask {

    public AbstractGenerateFileTask(String defaultDir, String defaultFile) {
        if (defaultDir != null) {
            getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir(defaultDir));
        }
        if (defaultFile != null) {
            getFileName().convention(defaultFile);
        }
        getOutputFile().set(getOutputDirectory().file(getFileName()));
    }

    @Input
    public abstract Property<String> getFileName();

    @Internal
    public abstract DirectoryProperty getOutputDirectory();

    @OutputFile
    protected abstract RegularFileProperty getOutputFile();

    @TaskAction
    public void generateAction() {
        File output = getOutputFile().getAsFile().get();
        getProject().delete(output);
        generate(output);
    }

    protected abstract void generate(File file);

}