package nl.elec332.gradle.minecraft.moddev.stages;

import nl.elec332.gradle.minecraft.moddev.ModDevPlugin;
import nl.elec332.gradle.minecraft.moddev.util.ForgeHelper;
import nl.elec332.gradle.util.JavaPluginHelper;
import nl.elec332.gradle.util.ProjectHelper;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

/**
 * Created by Elec332 on 3-7-2020
 */
public class DuplicateForgeFixer {

    public static void fixDuplicateForgeVersions(Project project) {
        project.getGradle().projectsEvaluated(g -> {
            if (!ModDevPlugin.getExtension(project).fgTweaks) {
                System.out.println("Skipping FG tweaks");
                return;
            }

            System.out.println("Processing Forge versions");

            Function<Project, ArtifactRepository> repoGetter = p -> p.getRepositories().stream().filter(r -> r.getName().contains("BUNDELED")).reduce((a, b) -> null).orElseThrow(RuntimeException::new);

            Configuration mainMc = ForgeHelper.getMcConfiguration(project);
            Map<Project, Configuration> tbc = new HashMap<>();
            tbc.put(project, mainMc);
            List<Dependency> deps = new ArrayList<>(mainMc.getDependencies());
            mainMc.getDependencies().clear();
            String[] intVer = ForgeHelper.getMappings(deps);
            String version = intVer[0];
            String mappings = intVer.length == 1 ? null : intVer[1];
            ArtifactRepository repo = null;
            if (mappings != null) {
                repo = repoGetter.apply(project);
            }

            for (Dependency pd : project.getConfigurations().getByName("compileOnly").getAllDependencies()) {
                if (pd instanceof ProjectDependency) {
                    Project proj = ((ProjectDependency) pd).getDependencyProject();
                    Configuration cfg = ForgeHelper.getMcConfiguration(proj);
                    if (cfg != null) {
                        Configuration compile = ProjectHelper.getCompileConfiguration(proj);
                        Set<Configuration> cs = new HashSet<>(compile.getExtendsFrom());
                        cs.remove(cfg);
                        compile.setExtendsFrom(cs);

                        String[] intVerI = ForgeHelper.getMappings(deps);
                        String versionI = intVerI[0];
                        String mappingsI = intVerI.length == 1 ? null : intVerI[1];
                        if (!versionI.equals(version)) {
                            System.out.println("WARNING: mismatching mappings! Internal: " + version + " Dependency: " + versionI + " Cannot split...");
                            continue;
                        }
                        if (mappings == null) {
                            mappings = mappingsI;
                            deps.clear();
                            deps.addAll(cfg.getDependencies());
                            repo = repoGetter.apply(proj);
                            System.out.println("Using AT forge dependency from project: " + proj.getName());
                        } else if (!mappings.equals(mappingsI)) {
                            System.out.println("WARNING: Detected projects with different mappings. Cannot split...");
                            continue;
                        }

                        tbc.put(proj, compile);
                        JavaPluginHelper.getJavaCompileTask(proj).doFirst(task -> {
                            ((JavaCompile) task).setClasspath(((JavaCompile) task).getClasspath().plus(proj.files(cfg.resolve())));
                        });
                    }
                }
            }
            String realMappings = version.split("_mapped_")[1];
            System.out.println("Using mappings: " + realMappings);
            if (repo != null) {
                System.out.println("Re-ordering repositories");
                for (Project proj : tbc.keySet()) {
                    Collection<ArtifactRepository> reps = new ArrayList<>(proj.getRepositories());
                    ArtifactRepository nr = null;
                    if (proj == project) {
                        reps.removeIf(r -> r.getName().contains("BUNDELED"));
                        try {
                            File file = Paths.get(project.getGradle().getGradleUserHomeDir().getPath(), "caches", "forge_gradle", "deobf_dependencies").toFile();
                            URL url = file.toURI().toURL();
                            nr = proj.getRepositories().maven(r -> r.setUrl(url));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    proj.getRepositories().clear();
                    proj.getRepositories().add(repo);
                    proj.getRepositories().addAll(reps);
                    if (nr != null) {
                        proj.getRepositories().add(nr);
                    }
                    ForgeHelper.fixWailaRepo(proj);
                }
            }
            tbc.values().forEach(cfg -> cfg.getDependencies().addAll(deps));
        });
    }

}
