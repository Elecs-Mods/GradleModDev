package nl.elec332.gradle.minecraft.moddev.projects.fabric.quilt;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.projects.ProjectPluginInitializer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Elec332 on 13-03-2024
 */
public class QuiltProjectPluginInitializer implements ProjectPluginInitializer {

    @Override
    public void addPlugins(BiConsumer<String, String> nameVersionPluginRegistry, Function<String, String> propertyGetter) {
        nameVersionPluginRegistry.accept("org.quiltmc.loom", propertyGetter.apply(MLProperties.QUILT_LOOM_VERSION));
    }

    @Override
    public void addProperties(Consumer<String> pluginProps) {
        pluginProps.accept(MLProperties.QUILT_LOOM_VERSION);
    }

}
