package nl.elec332.gradle.minecraft.moddev;

import nl.elec332.gradle.minecraft.moddev.stages.DependencyHandler;
import nl.elec332.gradle.minecraft.moddev.stages.DuplicateForgeFixer;
import nl.elec332.gradle.minecraft.moddev.stages.GradleExpander;
import nl.elec332.gradle.minecraft.moddev.tasks.GenerateResourcePackTask;
import nl.elec332.gradle.minecraft.moddev.tasks.GenerateTomlTask;
import nl.elec332.gradle.minecraft.moddev.util.ForgeHelper;
import nl.elec332.gradle.util.PluginHelper;
import nl.elec332.gradle.util.ProjectHelper;
import nl.elec332.gradle.util.Utils;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.internal.classloader.VisitableURLClassLoader;

import java.io.File;
import java.lang.reflect.Method;
import java.net.*;

/**
 * Created by Elec332 on 1-7-2020
 */
@NonNullApi
public class ModDevPlugin implements Plugin<Project> {

    public static final String TOML_TASK_NAME = "generateToml";
    public static final String MOD_CONFIG = "mod";
    public static final String MOD_CONFIG_RUNTIME = "modRuntime";
    public static final String MOD_CONFIG_COMPILE = "modCompile";

    @Override
    public void apply(Project project) {
        PluginHelper.checkMinimumGradleVersion("4.9");

        ForgeHelper.addForgeGradleClasspath(project);

        project.getPluginManager().apply(JavaPlugin.class);

        ModDevExtension settings = project.getExtensions().create("modSettings", ModDevExtension.class);

        GenerateResourcePackTask resourcePackTask = project.getTasks().create("generateResourcePackInfo", GenerateResourcePackTask.class);
        project.getTasks().getByName("processResources").dependsOn(resourcePackTask);
        GenerateTomlTask tomlTask = project.getTasks().create(TOML_TASK_NAME, GenerateTomlTask.class, settings);
        project.getTasks().getByName("processResources").dependsOn(tomlTask);

        GradleExpander.addConfig(project, settings);

        project.afterEvaluate(p -> { //Must run BEFORE ForgeGradle!
            GradleExpander.addMaven(project, settings);
            validateExtension(settings);
            GradleExpander.configureMinecraft(project, settings);
            GradleExpander.configureResources(project, settings);
            GradleExpander.addDeobf(project, settings);


            if (settings.fgTweaks) {
                ForgeHelper.addProjectMods(project, ProjectHelper.getCompileConfiguration(project));
            }

        });

        DependencyHandler.handleConfigurations(project); //Must run BEFORE ForgeGradle!

        ForgeHelper.addForgeGradle(project);

        DuplicateForgeFixer.fixDuplicateForgeVersions(project);
    }

    public static ModDevExtension getExtension(Project project) {
        return (ModDevExtension) project.getExtensions().getByName("modSettings");
    }

    public static File generatedResources(Project project) {
        return project.file("src/generated/resources");
    }

    private void validateExtension(ModDevExtension extension) {
        if (Utils.isNullOrEmpty(extension.modName)) {
            throw new IllegalArgumentException("No mod name entered!");
        }
        if (Utils.isNullOrEmpty(extension.modId)) {
            throw new IllegalArgumentException("No mod id entered!");
        }
        if (Utils.isNullOrEmpty(extension.basePackage)) {
            throw new IllegalArgumentException("No base package (for manifest version info) entered!");
        }
        if (Utils.isNullOrEmpty(extension.modVersion)) {
            throw new IllegalArgumentException("No mod version entered!");
        }
        if (Utils.isNullOrEmpty(extension.minecraftVersion)) {
            throw new IllegalArgumentException("No minecraft version entered!");
        }
        if (Utils.isNullOrEmpty(extension.forgeVersion)) {
            throw new IllegalArgumentException("No forge version entered!");
        }
        if (Utils.isNullOrEmpty(extension.snapshotMappings)) {
            System.out.println("No mappings entered, assuming user entered it manually in the minecraft{} block...");
        }
    }

}
