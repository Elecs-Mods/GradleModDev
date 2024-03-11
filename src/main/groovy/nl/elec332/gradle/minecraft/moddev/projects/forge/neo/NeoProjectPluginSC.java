package nl.elec332.gradle.minecraft.moddev.projects.forge.neo;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.ProjectHelper;
import nl.elec332.gradle.minecraft.moddev.SettingsPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPluginMLSC;
import nl.elec332.gradle.minecraft.moddev.projects.CommonExtension;
import nl.elec332.gradle.minecraft.moddev.projects.CommonMLExtension;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.tasks.Jar;

import java.util.Map;

/**
 * Created by Elec332 on 23-02-2024
 */
public class NeoProjectPluginSC extends AbstractPluginMLSC {

    @Override
    public void applyMLPlugin(Project target, SourceSet main, SourceSet run, SourceSet rootMain) {
        String mlVersion = ">=" + ProjectHelper.getProperty(target, MLProperties.ELECLOADER_VERSION);
        if (!ProjectHelper.hasProperty(target, MLProperties.NEO_LOADER_VERSION)) {
            ProjectHelper.setProperty(target, MLProperties.NEO_LOADER_VERSION, mlVersion);
        }
        target.getConfigurations().named(run.getImplementationConfigurationName(), c -> c.extendsFrom(target.getConfigurations().getByName(main.getImplementationConfigurationName())));
        target.beforeEvaluate(p -> {
            p.getExtensions().configure(CommonExtension.class, c -> {
                c.metadata(md -> md.loader("elecjava"));
                Project forge = SettingsPlugin.getDetails(target).getProject(ModLoader.FORGE);
                if (forge != null) {
                    ((CommonMLExtension) c).importMetadata(forge);
                }
            });
        });
        target.getRootProject().getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class, j -> j.getManifest().attributes(Map.of("FMLModType", "GAMELIBRARY")));
    }

}
