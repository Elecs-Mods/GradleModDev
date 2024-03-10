package nl.elec332.gradle.minecraft.moddev.projects.forge.forge


import nl.elec332.gradle.minecraft.moddev.MLProperties
import nl.elec332.gradle.minecraft.moddev.ModLoader
import nl.elec332.gradle.minecraft.moddev.ProjectHelper
import nl.elec332.gradle.minecraft.moddev.SettingsPlugin
import nl.elec332.gradle.minecraft.moddev.projects.forge.ForgeBasedExtension
import org.gradle.api.Project

/**
 * Created by Elec332 on 04-09-2023
 */
class ForgeGroovyHelper {

    static void addMixinAnnotationProcessor(Project project) {
        ProjectHelper.applyToProject(project, {
            dependencies {
                annotationProcessor "org.spongepowered:mixin:" + ProjectHelper.getProperty(project, MLProperties.MIXIN_VERSION) + ":processor"
            }
            afterEvaluate {
                sourceSets.each {
                    tasks.named(it.compileJavaTaskName).configure {
                        dependsOn("createMcpToSrg")
                        File destDir = it.getDestinationDirectory().getAsFile().get()
                        options.compilerArgs.addAll("-AreobfTsrgFile=" + project.layout.buildDir.file("createMcpToSrg/output.tsrg").get().asFile.path, "-AoutRefMapFile=" + new File(destDir, ProjectHelper.getMixinRefMap(project)).path, "-AmappingTypes=tsrg", "-AdefaultObfuscationEnv=searge")
                    }
                }
            }
        })
    }

    static void setMinecraftSettings(Project project, ForgeExtension extension) {
        ProjectHelper.applyToProject(project, {
            minecraft {
                if (extension.copyIdeResources) {
                    copyIdeResources = true
                }
                mappings channel: 'official', version: ProjectHelper.getStringProperty(project, MLProperties.MC_VERSION)
            }
        })
    }

    static void setDependencies(Project project) {
        ProjectHelper.applyToProject(project, {
            dependencies {
                minecraft "net.minecraftforge:forge:" + ProjectHelper.getStringProperty(project, MLProperties.MC_VERSION) + "-" + ProjectHelper.getStringProperty(project, MLProperties.FORGE_VERSION)
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

    static void setRunSettings(Project project, ForgeBasedExtension extension) {
        ProjectHelper.applyToProject(project, {
            minecraft {
                File f = file('src/main/resources/META-INF/accesstransformer.cfg')
                if (f.exists()) {
                    accessTransformer = f
                }
                runs {
                    configureEach {
                        if (extension.runtimeSource != null) {
                            source(extension.runtimeSource)
                        }
                        workingDirectory project.rootProject.file("run/" + ModLoader.getIdentifier(project) + "/" + it.name)
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
                runs.create("client")
                runs.create("server")
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

}