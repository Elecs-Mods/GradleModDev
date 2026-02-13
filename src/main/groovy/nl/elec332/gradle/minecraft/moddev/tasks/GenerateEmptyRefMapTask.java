package nl.elec332.gradle.minecraft.moddev.tasks;

import nl.elec332.gradle.minecraft.moddev.util.AbstractGroovyHelper;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;

import javax.inject.Inject;
import java.io.File;

/**
 * Created by Elec332 on 29-03-2024
 */
public abstract class GenerateEmptyRefMapTask extends AbstractGenerateFileTask {

    @Inject
    public GenerateEmptyRefMapTask(String variant) {
        super("generated/emptyRefMap", null);
        getFileName().convention((variant == null ? "" : variant) + ProjectHelper.getMixinRefMap(getProject()));
    }

    public static final String DEFAULT_TASK_NAME = "generateEmptyRefMap";

    @Override
    protected void generate(File file) {
        AbstractGroovyHelper.writeFile(file,
                "{\n" + "  \"mappings\": {},\n" + "  \"data\": {}\n" + "}\n"
        );
    }

}
