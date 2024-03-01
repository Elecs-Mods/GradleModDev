package nl.elec332.gradle.minecraft.moddev.projects.forge;

import nl.elec332.gradle.minecraft.moddev.projects.CommonMLExtension;

/**
 * Created by Elec332 on 05-09-2023
 */
public abstract class ForgeBasedExtension extends CommonMLExtension {

    public String loggingMarkers;

    public String consoleLevel = "debug";

    public boolean addDataGenerator = true;

}
