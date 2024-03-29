package nl.elec332.gradle.minecraft.moddev.projects.forge.forge

import nl.elec332.gradle.minecraft.moddev.MLProperties
import nl.elec332.gradle.minecraft.moddev.ProjectType
import nl.elec332.gradle.minecraft.moddev.SettingsPlugin
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper
import org.gradle.api.Project

/**
 * Created by Elec332 on 04-09-2023
 */
class ForgeGroovyHelper {

    static void setMinecraftSettings(Project project) {
        ProjectHelper.applyToProject(project, {
            minecraft {
                mappings channel: 'official', version: ProjectHelper.getStringProperty(project, MLProperties.MC_VERSION)
            }

            if (!SettingsPlugin.isSuperCommonMode(project)) {
                sourceSets.each {
                    def dir = layout.buildDirectory.dir("sourcesSets/$it.name")
                    it.output.resourcesDir = dir
                    it.java.destinationDirectory = dir
                }
            }
        })
    }

    static void addMixins(Project project, Collection<String> mixins) {
        ProjectHelper.applyToProject(project, {
            minecraft {
                runs {
                    configureEach {
                        for (m in mixins) {
                            args "-mixin.config=" + m
                        }
                    }
                }
            }
        })
    }

    static void setRunSettings(Project project, ForgeExtension extension) {
        ProjectHelper.applyToProject(project, {
            minecraft {
                if (extension.copyIdeResources) {
                    copyIdeResources = true
                }
                File f = file('src/main/resources/META-INF/accesstransformer.cfg')
                if (f.exists()) {
                    accessTransformer = f
                }
                runs {
                    configureEach {
                        source Objects.requireNonNull(extension.runtimeSource)
                        workingDirectory project.rootProject.file("run/" + ProjectType.getIdentifier(project) + "/" + it.name)
                        if (extension.loggingMarkers != null) {
                            property 'forge.logging.markers', extension.loggingMarkers
                        }
                        if (extension.consoleLevel != null) {
                            property 'forge.logging.console.level', extension.consoleLevel
                        }
                    }
                    if (extension.addDataGenerator) {
                        data {
                            args '--mod', ProjectHelper.getStringProperty(project, MLProperties.MOD_ID), '--all', '--output', file('src/generated/resources/'), '--existing', file('src/main/resources/')
                        }
                    }
                }
                runs.maybeCreate("client")
                runs.maybeCreate("server")
            }
        })
    }

    static void setMixinRunSettings(Project project) {
        ProjectHelper.applyToProject(project, {
            var output = createSrgToMcp.getOutput().get().getAsFile().path
            minecraft {
                runs {
                    configureEach {
                        property 'mixin.env.remapRefMap', 'true'
                        property 'mixin.env.refMapRemappingFile', output
                    }
                }
            }
        })
    }

}
