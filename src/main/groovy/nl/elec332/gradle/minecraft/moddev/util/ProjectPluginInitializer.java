package nl.elec332.gradle.minecraft.moddev.util;

import org.gradle.api.Project;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Elec332 on 12-03-2024
 */
public interface ProjectPluginInitializer {

    void addPlugins(BiConsumer<String, String> nameVersionPluginRegistry, Function<String, String> propertyGetter);

    void addProperties(Consumer<String> pluginProps);

    interface Listener {

        void afterRuntimePluginsAdded(Project project);

    }

}
