package nl.elec332.gradle.minecraft.moddev.projects.fabric.quilt


import nl.elec332.gradle.minecraft.moddev.MLProperties
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper
import org.gradle.api.Project

/**
 * Created by Elec332 on 04-09-2023
 */
class QuiltGroovyHelper {

    static void setDependencies(Project project, boolean api) {
        ProjectHelper.applyToProject(project, {
            dependencies {
                minecraft "com.mojang:minecraft:" + ProjectHelper.getStringProperty(project, MLProperties.MC_VERSION)
                mappings loom.officialMojangMappings()
                modImplementation "org.quiltmc:quilt-loader:" + ProjectHelper.getStringProperty(project, MLProperties.QUILT_LOADER_VERSION)
                if (api) {
                    modImplementation "org.quiltmc.quilted-fabric-api:quilted-fabric-api:" + ProjectHelper.getStringProperty(project, MLProperties.QUILT_VERSION)
                }
            }
        })
    }

    static void disableTracking(Project project) { //Fuck you Quilt (again)
        ProjectHelper.applyToProject(project, {
            loom {
                for(r in runs) {
                    r.property("loader.disable_beacon", "true")
                }
            }
        })
    }

}