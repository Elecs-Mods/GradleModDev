package nl.elec332.gradle.minecraft.moddev;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class SettingsPlugin implements Plugin<Settings> {

    public static class ModDevConfig {

        public int javaVersion = 17;
        public boolean generateModInfo = true;

        public String ideaExtVersion = "1.1.7";

        public boolean quietJavaDoc = true;

        public boolean rootIsCommon = false;

        public boolean superCommonMode = false;

        public boolean multiProject = true;

        private final Set<ProjectType> loaders = new HashSet<>();

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

        @NotNull
        public Project getCommonProject() {
            if (singleProject()) {
                throw new UnsupportedOperationException();
            }
            return Objects.requireNonNull(projects.get(ProjectType.COMMON), "Common project hasn't been initialized yet!");
        }

        public final boolean singleProject() {
            return projects.size() == 1;
        }

        public boolean generateModInfo() {
            return generateModInfo;
        }

        public boolean isSuperCommonMode() {
            return superCommonMode;
        }

        @Nullable
        public Project getProject(@NotNull ProjectType ml) {
            return projects.get(Objects.requireNonNull(ml));
        }

    }

    public void apply(Settings settings) {
        boolean[] singleProject = {false};
        ModDevConfig cfg = settings.getExtensions().create("moddev", ModDevConfig.class);
        ModDevDetails mdd = settings.getExtensions().create("mdd", ModDevDetails.class);

        settings.pluginManagement(p -> {
            p.repositories(h -> {
                h.gradlePluginPortal();
                addRepositories(h);
            });
        });

        settings.getGradle().projectsLoaded(g -> {
            Project rootProject = g.getRootProject();
            if (singleProject[0]) {
                rootProject.getPluginManager().apply(AllProjectsPlugin.class);
                cfg.loaders.iterator().next().apply(rootProject, cfg.superCommonMode);
                return;
            }
            ProjectHelper.addPlugin(rootProject, "org.jetbrains.gradle.plugin.idea-ext", cfg.ideaExtVersion);
            if (cfg.rootIsCommon) {
                rootProject.getPluginManager().apply(AllProjectsPlugin.class);
                ProjectType.COMMON.apply(rootProject, cfg.superCommonMode);
                mdd.projects.put(ProjectType.COMMON, rootProject);
            } else {
                rootProject.getPluginManager().apply(JavaPlugin.class);
                AllProjectsPlugin.looseConfigure(rootProject, cfg);
            }
            rootProject.subprojects(sp -> {
                sp.getPluginManager().apply(AllProjectsPlugin.class);
                for (ProjectType l : ProjectType.values()) {
                    if (l.isType(sp)) {
                        mdd.projects.put(l, sp);
                        l.apply(sp, cfg.superCommonMode);
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
                }
                cfg.loaders.forEach(l -> {
                    String name = l.getName();
                    File f = new File(s.getRootDir(), name);
                    if (!f.exists() && cfg.superCommonMode) {
                        s.include(":build:" + name);
                    } else {
                        s.include(name);
                    }
                });
            } else {
                throw new UnsupportedOperationException("Please define a modloader type!");
            }
            mdd.generateModInfo = cfg.generateModInfo;
            mdd.superCommonMode = cfg.superCommonMode;
        });

        settings.getRootProject().setName((String) Objects.requireNonNull(settings.getExtensions().getExtraProperties().get(MLProperties.MOD_NAME)));
    }

    public static void addRepositories(RepositoryHandler h) {
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
    }

    public static ModDevDetails getDetails(Project project) {
        return ((ProjectInternal) project).getGradle().getSettings().getExtensions().getByType(ModDevDetails.class);
    }

    public static boolean isSuperCommonMode(Project project) {
        return getDetails(project).isSuperCommonMode();
    }

}
