package nl.elec332.gradle.minecraft.moddev.util;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.tasks.JvmConstants;
import org.gradle.util.internal.ConfigureUtil;

import javax.annotation.Nullable;

/**
 * Created by Elec332 on 14-03-2024
 * <p>
 * Central place for everything accessing Gradle internals
 */
public class GradleInternalHelper {

    public static final String JAVA_MAIN_COMPONENT_NAME = JvmConstants.JAVA_MAIN_COMPONENT_NAME;

    public static Settings getGradleSettings(Project project) {
        return ((ProjectInternal) project).getGradle().getSettings();
    }

    public static <T> Action<T> configureUsing(@Nullable final Closure<?> configureClosure) {
        return ConfigureUtil.configureUsing(configureClosure);
    }

}
