package nl.elec332.gradle.minecraft.moddev.tasks;

import nl.elec332.gradle.minecraft.moddev.util.AbstractGroovyHelper;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;

import java.io.File;

/**
 * Created by Elec332 on 29-03-2024
 */
public abstract class GenerateEmptyRefMapTask extends AbstractGenerateFileTask {

    public GenerateEmptyRefMapTask() {
        super("generated/emptyRefMap", null);
        getFileName().convention(ProjectHelper.getMixinRefMap(getProject()));
    }

    @Override
    protected void generate(File file) {
        AbstractGroovyHelper.writeFile(file,
                "{\n" + "  \"mappings\": {},\n" + "  \"data\": {}\n" + "}\n"
        );
    }

}
