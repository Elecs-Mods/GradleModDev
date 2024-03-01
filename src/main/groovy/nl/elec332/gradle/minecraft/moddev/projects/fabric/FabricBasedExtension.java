package nl.elec332.gradle.minecraft.moddev.projects.fabric;

import nl.elec332.gradle.minecraft.moddev.SettingsPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.CommonMLExtension;

/**
 * Created by Elec332 on 05-09-2023
 */
public abstract class FabricBasedExtension extends CommonMLExtension {

    public boolean addApiDependency = !SettingsPlugin.isSuperCommonMode(getProject());

}
