package nl.elec332.gradle.minecraft.moddev.projects.fabric


import nl.elec332.gradle.minecraft.moddev.ModLoader
import nl.elec332.gradle.minecraft.moddev.ProjectHelper
import nl.elec332.gradle.minecraft.moddev.projects.CommonMLExtension
import org.gradle.api.Project

/**
 * Created by Elec332 on 05-09-2023
 */
class FabricBasedGroovyHelper {

    static void setRunDirs(Project project, CommonMLExtension extension) {
        ProjectHelper.applyToProject(project, {
            loom {
                runs {
                    configureEach {
                        runDir project.relativePath(project.rootProject.file("run/" + ModLoader.getIdentifier(project) + "/" + it.name))
                        if (extension.runtimeSource != null) {
                            setSource extension.runtimeSource
                        }
                    }
                }
            }
        })
    }

    static void setRefMapName(Project project) {
        ProjectHelper.applyToProject(project, {
            loom {
                mixin {
                    defaultRefmapName = ProjectHelper.getMixinRefMap(project)
                }
            }
        })
    }

}
