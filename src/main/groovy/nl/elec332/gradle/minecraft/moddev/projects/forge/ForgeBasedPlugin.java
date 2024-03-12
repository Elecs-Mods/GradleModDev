package nl.elec332.gradle.minecraft.moddev.projects.forge;

import nl.elec332.gradle.minecraft.moddev.ProjectType;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPlugin;

/**
 * Created by Elec332 on 06-09-2023
 */
public abstract class ForgeBasedPlugin<E extends ForgeBasedExtension> extends AbstractPlugin<E> {

    public ForgeBasedPlugin(ProjectType projectType) {
        super(projectType);
    }

}
