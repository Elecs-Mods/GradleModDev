package nl.elec332.gradle.minecraft.moddev.tasks;

import nl.elec332.gradle.minecraft.moddev.ModDevExtension;
import nl.elec332.gradle.minecraft.moddev.ModDevPlugin;
import nl.elec332.gradle.minecraft.moddev.util.ResourcePackHelper;
import org.gradle.api.tasks.TaskAction;

/**
 * Created by Elec332 on 2-7-2020
 */
public class GenerateResourcePackTask extends AbstractGenerateTask {

    public GenerateResourcePackTask() {
        super("pack.mcmeta");
    }

    @TaskAction
    public void generatePackMeta() {
        ModDevExtension extension = ModDevPlugin.getExtension(getProject());
        if (!extension.createPackMeta) {
            System.out.println(file + " generation disabled, skipping...");
            return;
        }
        writeFile(writer -> {
            String contents = ResourcePackHelper.createPackMcMeta(extension);
            writer.accept(contents);
        });
    }

}
