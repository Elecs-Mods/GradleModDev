package nl.elec332.gradle.minecraft.moddev.projects.forge.neo;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.util.ProjectPluginInitializer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Elec332 on 13-03-2024
 */
public class NeoProjectPluginInitializer implements ProjectPluginInitializer {

    @Override
    public void addPlugins(BiConsumer<String, String> nameVersionPluginRegistry, Function<String, String> propertyGetter) {
        nameVersionPluginRegistry.accept("net.neoforged.gradle.userdev", propertyGetter.apply(MLProperties.NEO_GRADLE_VERSION));
    }

    @Override
    public void addProperties(Consumer<String> pluginProps) {
        pluginProps.accept(MLProperties.NEO_GRADLE_VERSION);
    }

}
