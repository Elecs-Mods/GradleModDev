package nl.elec332.gradle.minecraft.moddev.stages;

import nl.elec332.gradle.minecraft.moddev.util.ForgeHelper;
import nl.elec332.gradle.minecraft.moddev.ModDevPlugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;

/**
 * Created by Elec332 on 4-7-2020
 */
public class DependencyHandler {

    public static void handleConfigurations(Project project) {
        Configuration confMod = project.getConfigurations().create(ModDevPlugin.MOD_CONFIG);
        Configuration confModCompile = project.getConfigurations().create(ModDevPlugin.MOD_CONFIG_COMPILE);
        Configuration confModRuntime = project.getConfigurations().create(ModDevPlugin.MOD_CONFIG_RUNTIME);

        project.afterEvaluate(p -> {
            DependencySet compileOnly = project.getConfigurations().getByName("compileOnly").getDependencies();
            DependencySet runtimeOnly = project.getConfigurations().getByName("runtimeOnly").getDependencies();

            for (Dependency dependency : confMod.getDependencies()) {
                if (!ForgeHelper.addProjectMod(project, dependency)) {
                    dependency = ForgeHelper.deobfDep(project, dependency);
                }
                compileOnly.add(dependency);
                runtimeOnly.add(dependency);
            }
            for (Dependency dependency : confModCompile.getDependencies()) {
                if (!ForgeHelper.addProjectMod(project, dependency)) {
                    dependency = ForgeHelper.deobfDep(project, dependency);
                }
                compileOnly.add(dependency);
            }
            for (Dependency dependency : confModRuntime.getDependencies()) {
                if (!ForgeHelper.addProjectMod(project, dependency)) {
                    dependency = ForgeHelper.deobfDep(project, dependency);
                }
                runtimeOnly.add(dependency);
            }
        });
    }

}
