package nl.elec332.gradle.minecraft.moddev.projects.fabric.fabric


import nl.elec332.gradle.minecraft.moddev.MLProperties
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper
import org.gradle.api.Project

/**
 * Created by Elec332 on 04-09-2023
 */
class FabricGroovyHelper {

    static void setDependencies(Project project, boolean api) {
        ProjectHelper.applyToProject(project, {
            dependencies {
                minecraft "com.mojang:minecraft:" + ProjectHelper.getStringProperty(project, MLProperties.MC_VERSION)
                mappings loom.officialMojangMappings()
                modImplementation "net.fabricmc:fabric-loader:" + ProjectHelper.getStringProperty(project, MLProperties.FABRIC_LOADER_VERSION)
                if (api) {
                    modImplementation "net.fabricmc.fabric-api:fabric-api:" + ProjectHelper.getStringProperty(project, MLProperties.FABRIC_VERSION)
                }
            }
        })
    }

}
