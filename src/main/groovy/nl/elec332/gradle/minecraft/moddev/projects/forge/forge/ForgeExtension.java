package nl.elec332.gradle.minecraft.moddev.projects.forge.forge;

import nl.elec332.gradle.minecraft.moddev.SettingsPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.forge.ForgeBasedExtension;

/**
 * Created by Elec332 on 05-09-2023
 */
public class ForgeExtension extends ForgeBasedExtension {

    public ForgeExtension() {
        loggingMarkers = "REGISTRIES";
    }

    public boolean copyIdeResources = true;

    public boolean addMixinsToManifest = !SettingsPlugin.isSuperCommonMode(getProject());

}
