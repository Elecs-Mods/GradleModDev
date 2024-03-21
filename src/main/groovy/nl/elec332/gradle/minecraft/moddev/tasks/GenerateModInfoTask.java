package nl.elec332.gradle.minecraft.moddev.tasks;

import nl.elec332.gradle.minecraft.moddev.AllProjectsPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractGroovyHelper;
import nl.elec332.gradle.minecraft.moddev.projects.ModMetadata;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

/**
 * Created by Elec332 on 12-03-2024
 */
public abstract class GenerateModInfoTask extends DefaultTask {

    private String source = SourceSet.MAIN_SOURCE_SET_NAME;

    public void source(SourceSet ss) {
        this.source = ss.getName();
    }

    @Input
    public String getSource() {
        return source;
    }

    @Input
    public abstract Property<ModMetadata> getMetaData();

    @OutputFile
    public File getOutput() {
        return AllProjectsPlugin.generatedResourceFolder(getProject()).dir(source).file(getMetaData().get().getFileLocation()).getAsFile();
    }

    @TaskAction
    private void run() {
        File f = getOutput();
        if (f.exists()) {
            f.delete();
        }
        AbstractGroovyHelper.writeFile(f, getMetaData().get().toString());
    }

}
