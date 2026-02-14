package nl.elec332.gradle.minecraft.moddev.projects.fabric.fabric;

import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.SettingsPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPluginMLSC;
import nl.elec332.gradle.minecraft.moddev.projects.CommonExtension;
import nl.elec332.gradle.minecraft.moddev.tasks.AllModJarSetupTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

/**
 * Created by Elec332 on 23-02-2024
 */
public class FabricProjectPluginSC extends AbstractPluginMLSC {

    @Override
    public void applyMLPlugin(Project target, SourceSet main, SourceSet run, SourceSet commonMain) {
        target.beforeEvaluate(p -> {
            p.getExtensions().getByType(CommonExtension.class).metadata(md -> md.dependsOn("elecloader", getElecLoaderDependency(target)));
            SettingsPlugin.getDetails(p).getCommonProject().getTasks()
                    .named(AllModJarSetupTask.getSetupTaskName(ALL_JAR_TASK_NAME), AllModJarSetupTask.class, s -> s.getMapping().convention(ModLoader.FABRIC.getMapping().name()));
        });
    }

}
