package nl.elec332.gradle.minecraft.moddev.projects.forge.forge;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ProjectType;
import nl.elec332.gradle.minecraft.moddev.SettingsPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPluginMLSC;
import nl.elec332.gradle.minecraft.moddev.projects.CommonExtension;
import nl.elec332.gradle.minecraft.moddev.projects.CommonMLExtension;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.tasks.Jar;

import java.util.Map;

/**
 * Created by Elec332 on 23-02-2024
 */
public class ForgeProjectPluginSC extends AbstractPluginMLSC {

    @Override
    public void applyMLPlugin(Project target, SourceSet main, SourceSet run, SourceSet commonMain) {
        String mlVersion = ">=" + ProjectHelper.getProperty(target, MLProperties.ELECLOADER_VERSION);
        if (!ProjectHelper.hasProperty(target, MLProperties.FORGE_LOADER_VERSION)) {
            ProjectHelper.setProperty(target, MLProperties.FORGE_LOADER_VERSION, mlVersion);
        }
        target.beforeEvaluate(p -> {
            p.getExtensions().configure(CommonExtension.class, c -> {
                c.metadata(md -> md.loader("elecjava"));
                Project neo = SettingsPlugin.getDetails(target).getProject(ProjectType.NEO_FORGE);
                if (neo != null) {
                    ((CommonMLExtension) c).importMetadata(neo);
                }
            });
        });
        SettingsPlugin.getDetails(target).getCommonProject().getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class, j -> j.getManifest().attributes(Map.of("FMLModType", "GAMELIBRARY")));
        var dest = target.getLayout().getBuildDirectory().dir("sourcesSets/" + run.getName());
        run.getOutput().setResourcesDir(dest);
        run.getJava().getDestinationDirectory().set(dest);
    }

}
