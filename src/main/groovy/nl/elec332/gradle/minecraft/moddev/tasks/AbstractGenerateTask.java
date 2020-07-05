package nl.elec332.gradle.minecraft.moddev.tasks;

import nl.elec332.gradle.minecraft.moddev.ModDevPlugin;
import nl.elec332.gradle.util.JavaPluginHelper;
import org.gradle.api.DefaultTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 2-7-2020
 */
public class AbstractGenerateTask extends DefaultTask {

    public AbstractGenerateTask(String file) {
        this.file = file;
    }

    protected final String file;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void writeFile(Consumer<Consumer<String>> writer) {
        File generated = ModDevPlugin.generatedResources(getProject());
        for (File f : JavaPluginHelper.getMainJavaSourceSet(getProject()).getResources().getSrcDirs()) {
            if (!f.equals(generated)) {
                File newPos = new File(f, file);
                if (newPos.exists()) {
                    System.out.println("Existing " + file + " found, skipping generation...");
                    break;
                }
            }
        }
        File newFile = new File(generated, file);
        if (newFile.exists()) {
            newFile.delete();
        }
        if (!newFile.getParentFile().exists()) {
            newFile.getParentFile().mkdirs();
        }

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(newFile));
            writer.accept(str -> {
                try {
                    bw.write(str);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            bw.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
