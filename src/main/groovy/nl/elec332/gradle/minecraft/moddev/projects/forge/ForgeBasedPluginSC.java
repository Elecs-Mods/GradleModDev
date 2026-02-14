package nl.elec332.gradle.minecraft.moddev.projects.forge;

import nl.elec332.gradle.minecraft.moddev.ProjectType;
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
 * Created by Elec332 on 13-02-2026
 */
public abstract class ForgeBasedPluginSC extends AbstractPluginMLSC {

    @Override
    protected final void applyMLPlugin(Project target, SourceSet main, SourceSet run, SourceSet commonMain) {
        SettingsPlugin.getDetails(target).getCommonProject().getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class, j -> j.getManifest().attributes(Map.of("FMLModType", "GAMELIBRARY")));
        applyForgeBasedMLPlugin(target, main, run, commonMain);
    }

    protected abstract void applyForgeBasedMLPlugin(Project target, SourceSet main, SourceSet run, SourceSet commonMain);

    protected void setupModMeta(Project target, ProjectType toImport) {
        target.beforeEvaluate(p -> {
            p.getExtensions().configure(CommonExtension.class, c -> {
                c.metadata(md -> md.loader("elecjava"));
                Project forge = SettingsPlugin.getDetails(target).getProject(toImport);
                if (forge != null) {
                    ((CommonMLExtension) c).importMetadata(forge);
                }
            });
        });
    }

}
