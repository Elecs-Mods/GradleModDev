package nl.elec332.gradle.minecraft.moddev.projects.forge.neo;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ProjectType;
import nl.elec332.gradle.minecraft.moddev.projects.forge.ForgeBasedPluginSC;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

/**
 * Created by Elec332 on 23-02-2024
 */
public class NeoProjectPluginSC extends ForgeBasedPluginSC {

    @Override
    public void applyForgeBasedMLPlugin(Project target, SourceSet main, SourceSet run, SourceSet commonMain) {
        if (!ProjectHelper.hasProperty(target, MLProperties.NEO_LOADER_VERSION)) {
            ProjectHelper.setProperty(target, MLProperties.NEO_LOADER_VERSION, getElecLoaderDependency(target));
        }
        setupModMeta(target, ProjectType.FORGE);

        target.getConfigurations().named(run.getImplementationConfigurationName(), c -> c.extendsFrom(target.getConfigurations().getByName(main.getImplementationConfigurationName())));
    }

}
