package nl.elec332.gradle.minecraft.moddev.projects.forge;

import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPlugin;

/**
 * Created by Elec332 on 06-09-2023
 */
public abstract class ForgeBasedPlugin<E extends ForgeBasedExtension> extends AbstractPlugin<E> {

    public ForgeBasedPlugin(ModLoader modLoader) {
        super(modLoader);
    }

}
