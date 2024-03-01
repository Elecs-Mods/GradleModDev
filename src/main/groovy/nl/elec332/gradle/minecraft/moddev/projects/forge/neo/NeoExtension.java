package nl.elec332.gradle.minecraft.moddev.projects.forge.neo;

import nl.elec332.gradle.minecraft.moddev.projects.forge.ForgeBasedExtension;

/**
 * Created by Elec332 on 05-09-2023
 */
public class NeoExtension extends ForgeBasedExtension {

    public NeoExtension() {
        loggingMarkers = "CORE,LOADING,SCAN,SPLASH";
    }

}
