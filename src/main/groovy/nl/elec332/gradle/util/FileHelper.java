package nl.elec332.gradle.util;

import java.io.File;

/**
 * Created by Elec332 on 6-4-2020
 */
public class FileHelper {

    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    public static void cleanFolder(File folder) {
        if (!folder.exists()) {
            folder.mkdirs();
        } else {
            if (!folder.isDirectory()) {
                folder.delete();
            } else {
                for (File file : folder.listFiles()) {
                    if (file.isDirectory()) {
                        for (File file2 : file.listFiles()) {
                            file2.delete();
                        }
                    }
                    file.delete();
                }
            }
        }
    }

}
