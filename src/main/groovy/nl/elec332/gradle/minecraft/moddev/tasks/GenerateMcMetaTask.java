package nl.elec332.gradle.minecraft.moddev.tasks;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.util.AbstractGroovyHelper;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

/**
 * Created by Elec332 on 28-03-2024
 */
public abstract class GenerateMcMetaTask extends AbstractGenerateFileTask {

    public GenerateMcMetaTask() {
        super("generated/mcmeta");
        getPackDescription().convention(ProjectHelper.getStringProperty(getProject(), MLProperties.MOD_NAME) + " resources");
        getPackFormat().convention(1);
    }

    @Input
    public abstract Property<String> getPackDescription();

    @Input
    public abstract Property<Integer> getPackFormat();

    protected void generate() {
        AbstractGroovyHelper.writeFile(getOutputFile("pack.mcmeta"),
                "{\n" +
                    "  \"pack\": {\n" +
                    "    \"description\": \"" + getPackDescription().get() + "\",\n" +
                    "    \"pack_format\": " + getPackFormat().get() + "\n" +
                    "  }\n" +
                    "}\n"
        );
    }

}
