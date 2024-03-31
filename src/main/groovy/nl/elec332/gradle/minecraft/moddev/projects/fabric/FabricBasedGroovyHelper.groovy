package nl.elec332.gradle.minecraft.moddev.projects.fabric

import nl.elec332.gradle.minecraft.moddev.ProjectType
import nl.elec332.gradle.minecraft.moddev.projects.CommonMLExtension
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper
import org.gradle.api.Project

/**
 * Created by Elec332 on 05-09-2023
 */
class FabricBasedGroovyHelper {

    static void setRunSettings(Project project, CommonMLExtension extension) {
        ProjectHelper.applyToProject(project, {
            loom {
                runs {
                    configureEach {
                        runDir project.relativePath(project.rootProject.file("run/" + ProjectType.getIdentifier(project) + "/" + it.name))
                        setSource Objects.requireNonNull(extension.runtimeSource)
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

    static String getRefMapName(Project project) {
        return project.loom.mixin.defaultRefmapName.get()
    }

}
