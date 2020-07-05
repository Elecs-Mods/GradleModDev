package nl.elec332.gradle.minecraft.moddev.util;

import nl.elec332.gradle.minecraft.moddev.ModDevExtension;

/**
 * Created by Elec332 on 2-7-2020
 */
public class ResourcePackHelper {

    public static String createPackMcMeta(ModDevExtension extension) {
        String modName = extension.modName;
        return "{\n" +
                "   \"pack\": {\n" +
                "      \"description\": \"" + modName + " resource pack\",\n" +
                "\t  \"pack_format\": 4\n" +
                "   }\n" +
                "}\n";
    }

}
