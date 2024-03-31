package nl.elec332.gradle.minecraft.moddev.tasks;

import nl.elec332.gradle.minecraft.moddev.projects.ModMetadata;
import nl.elec332.gradle.minecraft.moddev.util.AbstractGroovyHelper;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

/**
 * Created by Elec332 on 12-03-2024
 */
public abstract class GenerateModInfoTask extends AbstractGenerateFilesTask {

    public GenerateModInfoTask() {
        super("generated/modinfo");
    }

    @Input
    public abstract Property<ModMetadata> getMetaData();

    protected void generate() {
        AbstractGroovyHelper.writeFile(getOutputFile(getMetaData().get().getFileLocation()), getMetaData().get().toString());
    }

}
