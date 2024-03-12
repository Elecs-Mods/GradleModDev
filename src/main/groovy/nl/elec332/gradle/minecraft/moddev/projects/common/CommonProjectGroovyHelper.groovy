package nl.elec332.gradle.minecraft.moddev.projects.common


import nl.elec332.gradle.minecraft.moddev.MLProperties
import nl.elec332.gradle.minecraft.moddev.ProjectHelper
import org.gradle.api.Project

/**
 * Created by Elec332 on 04-09-2023
 */
class CommonProjectGroovyHelper {

    static void setMinecraft(Project project) {
        ProjectHelper.applyToProject(project, {
            minecraft {
                version(ProjectHelper.getStringProperty(project, MLProperties.MC_VERSION))
            }
        })
    }

}
