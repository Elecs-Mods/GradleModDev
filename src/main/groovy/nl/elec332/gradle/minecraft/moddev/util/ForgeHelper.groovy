package nl.elec332.gradle.minecraft.moddev.util

import nl.elec332.gradle.minecraft.moddev.ModDevPlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

import java.lang.reflect.Method

/**
 * Created by Elec332 on 4-7-2020
 */
class ForgeHelper {

    static Configuration getMcConfiguration(Project project) {
        return project.getConfigurations().findByName("minecraft")
    }

    static String[] getMappings(Collection<Dependency> deps) {
        String ret = null
        for (Dependency dep : deps) {
            if (isForge(dep)) {
                if (ret == null) {
                    ret = dep.getVersion()
                } else if (!ret.equals(dep.getVersion())) {
                    throw new RuntimeException()
                }
            }
        }
        if (ret == null) {
            throw new RuntimeException()
        }
        return ret.split("_at_")
    }

    static boolean isForge(Dependency dep) {
        return dep.getName().equals("forge") && dep.getGroup() != null && dep.getGroup().equals("net.minecraftforge")
    }

    static Dependency deobfDep(Project project, Dependency dependency) {
        if (dependency instanceof ExternalModuleDependency) {
            return project.fg.deobf(dependency)
//            dependency.transitive = true
//            Field f = Class.forName("net.minecraftforge.gradle.userdev.DependencyManagementExtension").getDeclaredField("remapper")
//            f.setAccessible(true)
//            project.getConfigurations().getByName("__obfuscated").getDependencies().add(dependency)
//            ExternalModuleDependency ret = f.get(project.fg).remapExternalModule(dependency)
//            return ret
        }
        println "Unable to remap dependency: " + dependency
        return dependency
    }

    static boolean addProjectMods(Project project, Configuration configuration) {
        boolean ret = false
        for (Dependency pd : configuration.getDependencies()) {
            ret |= addProjectMod(project, pd)
        }
        return ret
    }

    static boolean addProjectMod(Project project, Dependency pd) {
        if (pd instanceof ProjectDependency) {
            Project proj = ((ProjectDependency) pd).getDependencyProject()
            project.gradle.projectsEvaluated {
                Configuration cfg = getMcConfiguration(proj)
                if (cfg != null) {
                    String fullMap = getMappings(getMcConfiguration(project).dependencies)[0]
                    String cfgMap = getMappings(cfg.dependencies)[0]
                    if (!fullMap.split("_mapped_")[1].equals(cfgMap.split("_mapped_")[1])) {
                        pd.exclude(Collections.singletonMap("group", "*"))
                        println "Excluding all for dependency project " + proj.name + " because of mismatched mappings"
                    }
//                    for (Dependency dep : proj.getConfigurations().getByName("implementation").getAllDependencies()) {
//                        if (!ForgeHelper.isForge(dep)) {
//                            ProjectHelper.getCompileConfiguration(project).getDependencies().add(dep.copy())
//                        }
//                    }
                    String name = proj.getName()
                    project.minecraft {
                        runs {
                            client {
                                mods {
                                    "$name" {
                                        source(project.project(":$name").sourceSets.getByName("main"))
                                    }
                                }
                            }
                            server {
                                mods {
                                    "$name" {
                                        source(project.project(":$name").sourceSets.getByName("main"))
                                    }
                                }
                            }
                            data {
                                mods {
                                    "$name" {
                                        source(project.project(":$name").sourceSets.getByName("main"))
                                    }
                                }
                            }

                        }
                    }
                }
            }

            proj.afterEvaluate {
                proj.getPlugins().withType(ModDevPlugin.class, { p ->
                    ModDevPlugin.getExtension(proj).fgTweaks = false
                })
            }

            return true
        }
        return false;
    }

    static void addForgeGradle(Project project) {
        project.apply plugin: 'net.minecraftforge.gradle'
        project.sourceCompatibility = project.targetCompatibility = project.compileJava.sourceCompatibility = project.compileJava.targetCompatibility = '1.8'
    }

    //Force-load FG to the classpath, remove dependency results and apply later
    static void addForgeGradleClasspath(Project project) {
        //Don't add to classpath twice
        if (project.rootProject != project) {
            if (project.rootProject.plugins.hasPlugin("net.minecraftforge.gradle")) {
                println "Skip apply 1"
                return
            }
            Project check = project
            while (check.parent != null) {
                check = check.parent
                if (check.plugins.hasPlugin("net.minecraftforge.gradle")) {
                    println "Skip apply 2"
                    return
                }
            }
        }

        String cfg = "fgGradlePlugin"
        Configuration tezt = project.getConfigurations().create(cfg)
        MavenArtifactRepository repo1 = project.repositories.maven {
            url = "http://files.minecraftforge.net/maven"
        }
        MavenArtifactRepository repo2 = project.repositories.mavenCentral()
        project.dependencies {
            "$cfg" group: 'net.minecraftforge.gradle', name: 'ForgeGradle', version: '3.+', changing: true
        }
        for (File f : tezt.resolve()) {
            addURLs(project.getBuildscript().getClassLoader(), f.toURI().toURL());
        }
        //Remove all traces
        project.configurations.remove(tezt)
        project.repositories.remove(repo1)
        project.repositories.remove(repo2)
    }

    private static void addURLs(ClassLoader cl, URL url) {
        if (cl instanceof URLClassLoader) {
            URLClassLoader ul = (URLClassLoader) cl;
            try {
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(ul, url);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException();
        }
    }

}
