package nl.elec332.gradle.minecraft.moddev;

import nl.elec332.gradle.minecraft.moddev.projects.CommonMLExtension;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.FabricBasedExtension;
import nl.elec332.gradle.minecraft.moddev.projects.forge.ForgeBasedExtension;

/**
 * Created by Elec332 on 06-09-2023
 */
public enum ModLoaderType {

    FORGE(ForgeBasedExtension.class, false), FABRIC(FabricBasedExtension.class, false);

    ModLoaderType(Class<? extends CommonMLExtension> extension, boolean hasSharedCode) {
        this.extension = extension;
        this.hasSharedCode = hasSharedCode;
    }

    private final Class<? extends CommonMLExtension> extension;
    private final boolean hasSharedCode;

    public Class<? extends CommonMLExtension> getExtensionType() {
        return extension;
    }

    public boolean hasSharedCode() {
        return hasSharedCode;
    }

}
