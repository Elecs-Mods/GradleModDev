package nl.elec332.gradle.minecraft.moddev.projects.fabric.fabric;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.util.ProjectPluginInitializer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Elec332 on 13-03-2024
 */
public class FabricProjectPluginInitializer implements ProjectPluginInitializer {

    @Override
    public void addPlugins(BiConsumer<String, String> nameVersionPluginRegistry, Function<String, String> propertyGetter) {
        nameVersionPluginRegistry.accept("fabric-loom", propertyGetter.apply(MLProperties.FABRIC_LOOM_VERSION));
    }

    @Override
    public void addProperties(Consumer<String> pluginProps) {
        pluginProps.accept(MLProperties.FABRIC_LOOM_VERSION);
    }

}
