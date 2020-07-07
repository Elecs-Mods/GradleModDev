package nl.elec332.gradle.minecraft.moddev.stages

import nl.elec332.gradle.minecraft.moddev.ModDevExtension
import nl.elec332.gradle.minecraft.moddev.ModDevPlugin
import nl.elec332.gradle.minecraft.moddev.util.ForgeHelper
import nl.elec332.gradle.minecraft.moddev.util.TomlExtensions
import nl.elec332.gradle.util.Utils
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/**
 * Created by Elec332 on 4-7-2020
 */
class GradleExpander {

    static void addConfig(Project project, ModDevExtension extension) {
        File configFile = project.file "build.properties"
        if (configFile.exists()) {
            configFile.withReader {
                def prop = new Properties()
                prop.load(it)
                project.ext.config = new ConfigSlurper().parse prop

                //Do check, in case the plugin is applied late or in a sub-project
                if (Utils.isNullOrEmpty(extension.modVersion)) {
                    extension.modVersion = project.config.mod_version
                }
                if (Utils.isNullOrEmpty(extension.modClassifier)) {
                    extension.modClassifier = project.config.mod_classifier
                }
                if (Utils.isNullOrEmpty(extension.minecraftVersion)) {
                    extension.minecraftVersion = project.config.minecraft_version
                }
                if (Utils.isNullOrEmpty(extension.forgeVersion)) {
                    extension.forgeVersion = project.config.forge_version
                }
                if (Utils.isNullOrEmpty(extension.snapshotMappings)) {
                    extension.snapshotMappings = project.config.mappings
                }
                println "Loaded configuration file"
            }
        }
    }

    static void addMaven(Project project, ModDevExtension extension) {
        project.repositories {
            if (extension.addModMaven) {
                maven { //Everything except WAILA
                    name "ModMaven"
                    url "https://modmaven.k-4u.nl"
                }
                println "Added ModMaven repo"
            }
            if (extension.addWailaMaven) {
                maven { //WAILA, notoriously unreliable (Can cause other deps to fail due to timeouts)
                    name "Waila"
                    url "http://maven.tehnut.info"
                }
                project.afterEvaluate {
                    ForgeHelper.fixWailaRepo(project)
                }
                println "Added Waila repo"
            }
        }
    }

    static void configureMinecraft(Project project, ModDevExtension extension) {
        String modName = extension.modName
        project.minecraft {
            accessTransformer = project.file 'src/main/resources/META-INF/accesstransformer.cfg'

            if (!Utils.isNullOrEmpty(extension.snapshotMappings)) {
                mappings channel: 'snapshot', version: extension.snapshotMappings
            }

            runs {
                client {
                    workingDirectory project.file('run')
                    mods {
                        "$modName" {
                            source project.sourceSets.main
                        }
                    }
                }

                server {
                    workingDirectory project.file('run')
                    mods {
                        "$modName" {
                            source project.sourceSets.main
                        }
                    }
                }

                data {
                    workingDirectory project.file('run')
                    args '--mod', extension.modId, '--all', '--output', ModDevPlugin.generatedResources(project), '--existing', project.sourceSets.main.resources.srcDirs[0]
                    mods {
                        "$modName" {
                            source project.sourceSets.main
                        }
                    }
                }

            }
        }
        project.sourceSets.main.resources {
            srcDir ModDevPlugin.generatedResources(project)
        }
        project.dependencies {
            minecraft 'net.minecraftforge:forge:' + extension.minecraftVersion + "-" + extension.forgeVersion
        }
        println "Comfigured minecraft{} block"
    }

    static void configureResources(Project project, ModDevExtension extension) {
        String build = System.getenv("BUILD_NUMBER")
        boolean localBuild = Utils.isNullOrEmpty(build) && extension.localBuildIdentifier
        if (!extension.jenkinsBuildNumber || build == null) {
            build = ""
        }
        if (!Utils.isNullOrEmpty(build)) {
            build = "." + build
        }

        String temp = extension.modVersion
        String classifier = extension.modClassifier
        classifier = Utils.isNullOrEmpty(classifier) ? "" : "-" + classifier

        project.version = temp + (localBuild ? ".9999.custom" : build)
        extension.modVersion = extension.minecraftVersion + "-" + temp + (localBuild ? ".localBuild" : build) + classifier

        project.archivesBaseName = extension.modName

        project.processResources {
            // this will ensure that this task is redone every time.
            inputs.property "timeStamp", new Date().format("yyyy-MM-dd'T'HH:mm:ssZ")

            // replace stuff in mcmod.info, nothing else
            from(project.sourceSets.main.resources.srcDirs) {
                include 'META-INF/mods.toml'
                expand([
                        (TomlExtensions.MOD_VERSION)   : extension.modVersion,
                        (TomlExtensions.MC_VERSION)    : extension.minecraftVersion,
                        (TomlExtensions.LOADER_VERSION): extension.forgeVersion.split('\\.')[0],
                        (TomlExtensions.FORGE_VERSION) : extension.forgeVersion
                ])
            }

            // copy everything else, thats not the mcmod.info
            from(project.sourceSets.main.resources.srcDirs) {
                exclude 'META-INF/mods.toml'
            }

        }

        println "Added Toml Resource processing block"

        project.jar {
            manifest {

                attributes([
                        'Specification-Title'   : extension.modName,
                        'Specification-Version' : extension.modVersion,
                        'Implementation-Title'  : extension.modName,
                        'Implementation-Version': extension.modVersion
                ] as LinkedHashMap, extension.basePackage)

            }
        }

        println "Added Manifest info"
    }

    static void addDeobf(Project project, ModDevExtension extension) {
        if (extension.createDeobf) {
            project.task deobfJar(type: Jar) { // Generate deobfuscated
                from sourceSets.main.output
                classifier = 'deobf'
            }
            project.tasks.build.dependsOn('deobfJar')
            println "Added Deobf task"
        }
    }

}
