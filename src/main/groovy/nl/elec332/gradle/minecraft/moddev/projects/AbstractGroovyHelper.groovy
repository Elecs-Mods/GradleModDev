package nl.elec332.gradle.minecraft.moddev.projects

/**
 * Created by Elec332 on 07-09-2023
 */
class AbstractGroovyHelper {

    static void writeFile(File file, String text) {
        file.text = text
    }

}
