package nl.elec332.gradle.minecraft.moddev.util;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPlugin;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Created by Elec332 on 03-09-2023
 */
public class ProjectHelper {

    public static SourceSetContainer getSourceSets(Project root) {
        return Objects.requireNonNull(root.getExtensions().getByType(JavaPluginExtension.class).getSourceSets());
    }

    public static void afterEvaluateSafe(Project target, Action<Project> runnable) {
        if (hasEvaluated(target)) {
            runnable.execute(target);
        } else {
            target.afterEvaluate(runnable);
        }
    }

    public static boolean hasEvaluated(Project target) {
        return target.getState().getExecuted();
    }

    public static void checkProperties(Project project, Set<String> props) {
        Set<String> fail = new HashSet<>();
        for (String s : props) {
            if (!ProjectHelper.hasProperty(project, s)) {
                fail.add(s);
                continue;
            }
            Object o = ProjectHelper.getProperty(project, s);
            if (o == null || (o instanceof String && ((String) o).isEmpty())) {
                fail.add(s);
            }
        }
        if (!fail.isEmpty()) {
            throw new RuntimeException("Missing the following properties: " + fail);
        }
    }

    public static String getMixinRefMap(Project project) {
        String fileNameBase = ProjectHelper.getPlugin(project).getProjectType().getName() + ".refmap.json";
        return ProjectHelper.getProperty(project, MLProperties.MOD_ID) + fileNameBase;
    }

    public static AbstractPlugin<?> getPlugin(Project project) {
        return project.getPlugins().withType(AbstractPlugin.class).iterator().next();
    }

    public static boolean hasProperty(Project project, String name) {
        return project.hasProperty(name);
    }

    public static String getStringProperty(Project project, String name) {
        return (String) getProperty(project, name);
    }

    public static Object getProperty(Project project, String name) {
        return project.property(name);
    }

    public static void setProperty(Project project, String name, Object value) {
        project.getExtensions().getExtraProperties().set(name, value);
    }

    public static void applyToProject(Project project, @DelegatesTo(Project.class) Closure<?> closure) {
        GradleInternalHelper.configureUsing(closure).execute(project);
    }

}
