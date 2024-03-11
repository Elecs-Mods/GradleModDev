package nl.elec332.gradle.minecraft.moddev.projects.fabric.fabric;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ProjectHelper;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPluginMLSC;
import nl.elec332.gradle.minecraft.moddev.projects.CommonExtension;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.FabricBasedPlugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

/**
 * Created by Elec332 on 23-02-2024
 */
public class FabricProjectPluginSC extends AbstractPluginMLSC {

    @Override
    public void applyMLPlugin(Project target, SourceSet main, SourceSet run, SourceSet rootMain) {
        String mlVersion = ">=" + ProjectHelper.getProperty(target, MLProperties.ELECLOADER_VERSION);
        target.beforeEvaluate(p -> p.getExtensions().getByType(CommonExtension.class).metadata(md -> md.dependsOn("elecloader", mlVersion)));
    }

    @Override
    protected TaskProvider<? extends Task> setupRemapTask(Project project, Task task) {
        return project.getTasks().named(FabricBasedPlugin.REMAP_JAR_TASK);
    }

}
