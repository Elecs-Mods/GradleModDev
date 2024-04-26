package nl.elec332.gradle.minecraft.moddev.projects.common;

import nl.elec332.gradle.minecraft.moddev.SettingsPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.CommonExtension;

/**
 * Created by Elec332 on 06-09-2023
 */
public class CommonProjectExtension extends CommonExtension {

    public CommonProjectExtension() {
        noModuleMetadata = !SettingsPlugin.isSuperCommonMode(getProject());
    }

}
