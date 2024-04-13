package nl.elec332.gradle.minecraft.moddev;

import nl.elec332.gradle.minecraft.moddev.util.GradleInternalHelper;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import nl.elec332.gradle.minecraft.moddev.util.ProjectPluginInitializer;
import nl.elec332.gradle.minecraft.moddev.util.RuntimeProjectPluginRequests;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.initialization.Settings;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class SettingsPlugin implements Plugin<Settings> {

    public static class ModDevConfig {

        public int javaVersion = 17;
        public boolean generateModInfo = true;

        public String ideaExtVersion = "1.1.7";

        public boolean quietJavaDoc = true;

        public boolean rootIsCommon = false;

        public boolean superCommonMode = false;

        public boolean multiProject = true;

        public boolean useBuildNumber = false;

        public boolean addModMaven = true;

        private final Set<ProjectType> loaders = new HashSet<>();

        public void enableVanilla() {
            loaders.add(ProjectType.COMMON);
        }

        public void enableForge() {
            loaders.add(ProjectType.FORGE);
        }

        public void enableNeoForge() {
            loaders.add(ProjectType.NEO_FORGE);
        }

        public void enableFabric() {
            loaders.add(ProjectType.FABRIC);
        }

        public void enableQuilt() {
            loaders.add(ProjectType.QUILT);
        }

        public void enableAll() {
            enableForge();
            enableNeoForge();
            enableFabric();
            enableQuilt();
        }

    }

    public static class ModDevDetails {

        private final Map<ProjectType, Project> projects = new HashMap<>();
        private boolean generateModInfo = true;
        private boolean superCommonMode = false;
        private boolean useBuildNumber = false;
        private boolean singleProject = false;
        private boolean modMaven = false;

        @NotNull
        public Project getCommonProject() {
            if (singleProject()) {
                throw new UnsupportedOperationException();
            }
            return Objects.requireNonNull(projects.get(ProjectType.COMMON), "Common project hasn't been initialized yet!");
        }

        public final boolean singleProject() {
            return singleProject;
        }

        public boolean generateModInfo() {
            return generateModInfo;
        }

        public boolean isSuperCommonMode() {
            return superCommonMode;
        }

        public boolean useBuildNumber() {
            return useBuildNumber;
        }

        public boolean useModMaven() {
            return modMaven;
        }

        @Nullable
        public Project getProject(@NotNull ProjectType ml) {
            return projects.get(Objects.requireNonNull(ml));
        }

    }

    public void apply(@NotNull Settings settings) {
        RuntimeProjectPluginRequests.inject(settings);
        boolean[] singleProject = {false};
        ModDevConfig cfg = settings.getExtensions().create("moddev", ModDevConfig.class);
        ModDevDetails mdd = settings.getExtensions().create("mdd", ModDevDetails.class);

        settings.pluginManagement(p -> {
            p.repositories(h -> {
                h.gradlePluginPortal();
                addRepositories(h, false);
            });
        });

        settings.getGradle().projectsLoaded(g -> {
            Project rootProject = g.getRootProject();
            String id = "org.jetbrains.gradle.plugin.idea-ext";
            String version = cfg.ideaExtVersion;
            if (GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("8.7")) < 0) {
                rootProject.getBuildscript().getDependencies().add(ScriptHandler.CLASSPATH_CONFIGURATION, id + ":" + id + ".gradle.plugin:" + version);
                rootProject.afterEvaluate(p -> p.getPluginManager().apply(id));
            }
            Consumer<BiConsumer<String, String>> ideaExt = reg -> reg.accept(id, version);
            if (singleProject[0]) {
                apply(rootProject, cfg.loaders.iterator().next(), mdd, null);
                return;
            }
            if (cfg.rootIsCommon) {
                apply(rootProject, ProjectType.COMMON, mdd, ideaExt);
            } else {
                rootProject.getPluginManager().apply(JavaPlugin.class);
                AllProjectsPlugin.looseConfigure(rootProject, cfg);
                applyPlugins(rootProject, ideaExt);
            }
            rootProject.subprojects(sp -> {
                sp.getExtensions().getExtraProperties().set(RuntimeProjectPluginRequests.PROP_NAME, null);
                sp.getPluginManager().apply(AllProjectsPlugin.class);
                for (ProjectType l : ProjectType.values()) {
                    if (l.isType(sp)) {
                        apply(sp, l , mdd, null);
                    }
                }
            });
        });


        settings.getGradle().settingsEvaluated(s -> {
            System.out.println("Java: "  + System.getProperty("java.version") + ", JVM: "  + System.getProperty("java.vm.version") + " ("  + System.getProperty("java.vendor") + "), Arch: " + System.getProperty("os.arch"));
            if (cfg.superCommonMode) {
                cfg.rootIsCommon = true;
            }
            if (cfg.loaders.size() == 1 && !cfg.multiProject) {
                singleProject[0] = true;
            } else if (!cfg.loaders.isEmpty()) {
                if (!cfg.rootIsCommon) {
                    //Common project must go first, so add it manually here
                    s.include(ProjectType.COMMON.getName());
                    cfg.loaders.add(ProjectType.COMMON);
                }
                cfg.loaders.forEach(l -> {
                    if (l == ProjectType.COMMON) {
                        return;
                    }
                    String name = l.getName();
                    File f = new File(s.getRootDir(), name);
                    if (!f.exists() && cfg.superCommonMode) {
                        s.include(name);
                        s.project(":" + name).setProjectDir(new File(s.getRootDir(), "build/" + name));
                    } else {
                        s.include(name);
                    }
                });
            } else {
                throw new UnsupportedOperationException("Please define a modloader type!");
            }
            mdd.singleProject = singleProject[0];
            mdd.generateModInfo = cfg.generateModInfo;
            mdd.superCommonMode = cfg.superCommonMode;
            mdd.useBuildNumber = cfg.useBuildNumber;
            mdd.modMaven = cfg.addModMaven;
        });

        settings.getRootProject().setName((String) Objects.requireNonNull(settings.getExtensions().getExtraProperties().get(MLProperties.MOD_NAME)));
    }

    private void apply(Project project, ProjectType type, ModDevDetails mdd, Consumer<BiConsumer<String, String>> subReg) {
        Set<String> properties = new HashSet<>();
        Function<String, String> propGetter = prop -> {
            if (!properties.contains(prop)) {
                throw new UnsupportedOperationException();
            }
            return ProjectHelper.getStringProperty(project, prop);
        };

        project.getPluginManager().apply(AllProjectsPlugin.class);
        type.addProperties(properties::add);
        ProjectHelper.checkProperties(project, properties);
        mdd.projects.put(type, project);

        applyPlugins(project, reg -> {
            if (subReg != null) {
                subReg.accept(reg);
            }
            type.addPlugins(reg, propGetter);
        });
        final Set<String> pluginCounter = new HashSet<>();
        type.addPlugins((id, version) -> {
            pluginCounter.add(id);
            project.getPluginManager().withPlugin(id, p -> {
                pluginCounter.remove(id);
                if (pluginCounter.isEmpty()) {
                    project.getPlugins().forEach(plugin -> {
                        if (plugin instanceof ProjectPluginInitializer.Listener) {
                            ((ProjectPluginInitializer.Listener) plugin).afterRuntimePluginsAdded(project);
                        }
                    });
                }
            });
        }, propGetter);
        type.apply(project, mdd.superCommonMode);
    }

    private void applyPlugins(Project project, Consumer<BiConsumer<String, String>> reg) {
        project.getExtensions().getExtraProperties().set(RuntimeProjectPluginRequests.PROP_NAME, reg);
    }

    public static void addRepositories(RepositoryHandler h, boolean modMaven) {
        h.mavenCentral();
        h.maven(m -> {
            m.setName("Forge");
            m.setUrl("https://maven.minecraftforge.net/");
        });
        h.maven(m -> {
            m.setName("NeoForged");
            m.setUrl("https://maven.neoforged.net/releases");
        });
        h.maven(m -> {
            m.setName("Fabric");
            m.setUrl("https://maven.fabricmc.net/");
        });
        h.maven(m -> {
            m.setName("Quilt");
            m.setUrl("https://maven.quiltmc.org/repository/release");
        });
        h.maven(m -> {
            m.setName("Sponge Snapshots");
            m.setUrl("https://repo.spongepowered.org/repository/maven-public/");
        });
        if (modMaven) {
            h.maven(m -> {
                m.setName("ModMaven");
                m.setUrl("https://modmaven.dev");
            });
        }
    }

    public static ModDevDetails getDetails(Project project) {
        return GradleInternalHelper.getGradleSettings(project).getExtensions().getByType(ModDevDetails.class);
    }

    public static boolean isSuperCommonMode(Project project) {
        return getDetails(project).isSuperCommonMode();
    }

}
