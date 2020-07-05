package nl.elec332.gradle.util

import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployer
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeLibrarySpec
import org.gradle.nativeplatform.platform.NativePlatform

import java.util.function.Consumer

/**
 * Created by Elec332 on 18-4-2020
 */
class GroovyHooks {

    static void addModelComponent(Project project, String name, Consumer<NativeLibrarySpec> lib, Consumer<NativeBinarySpec> bin) {
        Set<NativeBinarySpec> stuff = new HashSet<>();
        project.model {
            components {
                "$name"(NativeLibrarySpec) {
                    lib.accept(it)
                    binaries.all {
                        stuff.add(it)
                    }
                }
            }
        }
        ProjectHelper.afterNativeModelExamined(project, { ->
            for (NativeBinarySpec bs : stuff) {
                bin.accept(bs)
            }
        })
    }

    static void modifyPlatforms(Project project, Consumer<ExtensiblePolymorphicDomainObjectContainer<NativePlatform>> modifier) {
        project.model {
            platforms {
                modifier.accept(it)
                it.toArray() //Force it to process new entries
            }
        }
    }

    static void addArtifact(Project project, Object from) {
        project.artifacts {
            archives from
        }
    }

    static void configureMaven(Project project, Consumer<MavenDeployer> consumer) {
        project.uploadArchives {
            repositories {
                mavenDeployer {
                    consumer.accept(it)
                }
            }
        }
    }

}
