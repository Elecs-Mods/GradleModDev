package nl.elec332.gradle.minecraft.moddev.projects.common;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.projects.ProjectPluginInitializer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Elec332 on 13-03-2024
 */
public class CommonProjectPluginInitializer implements ProjectPluginInitializer {

    @Override
    public void addPlugins(BiConsumer<String, String> nameVersionPluginRegistry, Function<String, String> propertyGetter) {
        nameVersionPluginRegistry.accept("org.spongepowered.gradle.vanilla", propertyGetter.apply(MLProperties.MIXIN_GRADLE_VANILLA));
    }

    @Override
    public void addProperties(Consumer<String> pluginProps) {
        pluginProps.accept(MLProperties.MIXIN_GRADLE_VANILLA);
    }

}
