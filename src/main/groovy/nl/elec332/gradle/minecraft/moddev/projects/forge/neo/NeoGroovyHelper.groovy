package nl.elec332.gradle.minecraft.moddev.projects.forge.neo

import nl.elec332.gradle.minecraft.moddev.MLProperties
import nl.elec332.gradle.minecraft.moddev.ProjectType
import nl.elec332.gradle.minecraft.moddev.projects.forge.ForgeBasedExtension
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper
import org.gradle.api.Project

/**
 * Created by Elec332 on 04-09-2023
 */
class NeoGroovyHelper {

    static void setMinecraftSettings(Project project) {
        ProjectHelper.applyToProject(project, {
            mappings {
                channel = official()
                version.put "minecraft", ProjectHelper.getStringProperty(project, MLProperties.MC_VERSION)
            }
        })
    }

    static void setRunSettings(Project project, ForgeBasedExtension extension) {
        ProjectHelper.applyToProject(project, {
            minecraft {
                File f = file('src/main/resources/META-INF/accesstransformer.cfg')
                if (f.exists()) {
                    accessTransformer = f
                }
            }
            runs {
                configureEach {
                    workingDirectory project.rootProject.file("run/" + ProjectType.getIdentifier(project) + "/" + it.name)
                    modSource Objects.requireNonNull(extension.runtimeSource)
                    if (extension.loggingMarkers != null) {
                        systemProperty 'forge.logging.markers', extension.loggingMarkers
                    }
                    if (extension.consoleLevel != null) {
                        systemProperty 'forge.logging.console.level', extension.consoleLevel
                    }
                }
                if (extension.addDataGenerator) {
                    data {
                        programArguments.addAll '--mod', ProjectHelper.getStringProperty(project, MLProperties.MOD_ID), '--all', '--output', file('src/generated/resources/').getAbsolutePath(), '--existing', file('src/main/resources/').getAbsolutePath()
                    }
                }
            }
            runs.maybeCreate("client")
            runs.maybeCreate("server")
        })
    }

}
