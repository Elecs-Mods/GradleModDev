package nl.elec332.gradle.minecraft.moddev;

import nl.elec332.gradle.minecraft.moddev.projects.AbstractPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPluginSC;
import nl.elec332.gradle.minecraft.moddev.projects.ProjectPluginInitializer;
import nl.elec332.gradle.minecraft.moddev.projects.common.CommonProjectPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.common.CommonProjectPluginInitializer;
import nl.elec332.gradle.minecraft.moddev.projects.common.CommonProjectPluginSC;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.fabric.FabricProjectPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.fabric.FabricProjectPluginInitializer;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.fabric.FabricProjectPluginSC;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.quilt.QuiltProjectPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.quilt.QuiltProjectPluginInitializer;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.quilt.QuiltProjectPluginSC;
import nl.elec332.gradle.minecraft.moddev.projects.forge.forge.ForgeProjectPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.forge.forge.ForgeProjectPluginInitializer;
import nl.elec332.gradle.minecraft.moddev.projects.forge.forge.ForgeProjectPluginSC;
import nl.elec332.gradle.minecraft.moddev.projects.forge.neo.NeoProjectPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.forge.neo.NeoProjectPluginInitializer;
import nl.elec332.gradle.minecraft.moddev.projects.forge.neo.NeoProjectPluginSC;
import org.gradle.api.Project;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Elec332 on 12-03-2024
 */
public enum ProjectType implements ProjectPluginInitializer {

    COMMON("common", null, CommonProjectPlugin.class, CommonProjectPluginSC.class, CommonProjectPluginInitializer.class),
    FORGE(null, ModLoader.FORGE, ForgeProjectPlugin.class, ForgeProjectPluginSC.class, ForgeProjectPluginInitializer.class),
    NEO_FORGE(null, ModLoader.NEO_FORGE, NeoProjectPlugin.class, NeoProjectPluginSC.class, NeoProjectPluginInitializer.class),
    FABRIC(null, ModLoader.FABRIC, FabricProjectPlugin.class, FabricProjectPluginSC.class, FabricProjectPluginInitializer.class),
    QUILT(null, ModLoader.QUILT, QuiltProjectPlugin.class, QuiltProjectPluginSC.class, QuiltProjectPluginInitializer.class);


    ProjectType(String name, ModLoader loader, Class<? extends AbstractPlugin<?>> plugin, Class<? extends AbstractPluginSC> pluginSC, Class<? extends ProjectPluginInitializer> projectPluginInitializer) {
        if (name != null && loader != null) {
            throw new UnsupportedOperationException();
        }
        if (name == null) {
            this.name = loader.getName();
            this.loader = loader;
        } else {
            this.name = name;
            this.loader = null;
        }
        this.plugin = plugin;
        this.pluginSC = pluginSC;
        try {
            this.pluginInitializer = projectPluginInitializer.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final String name;
    private final ModLoader loader;
    private final Class<? extends AbstractPlugin<?>> plugin;
    private final Class<? extends AbstractPluginSC> pluginSC;
    private final ProjectPluginInitializer pluginInitializer;

    public Class<? extends AbstractPlugin<?>> getPluginClass() {
        return plugin;
    }

    @SuppressWarnings("unused")
    public String getProjectName() {
        return ":" + name;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public ModLoader getModLoader() {
        return loader;
    }

    public boolean isType(Project project) {
        return name.equals(getIdentifier(project));
    }

    public void apply(Project project, boolean sc) {
        if (plugin == null || (sc && pluginSC == null)) {
            throw new UnsupportedOperationException();
        }
        if (sc) {
            project.getPluginManager().apply(pluginSC);
        }
        project.getPluginManager().apply(plugin);
    }

    @Override
    public void addPlugins(BiConsumer<String, String> nameVersionPluginRegistry, Function<String, String> propertyGetter) {
        this.pluginInitializer.addPlugins(nameVersionPluginRegistry, propertyGetter);
    }

    @Override
    public void addProperties(Consumer<String> pluginProps) {
        this.pluginInitializer.addProperties(pluginProps);
    }

    public static String getIdentifier(Project project) {
        return project.getBuildFile().getParentFile().getName();
    }

    static {
        Set<String> checker = new HashSet<>();
        for (ProjectType type : values()) {
            if (!checker.add(type.getName())) {
                throw new IllegalStateException();
            }
        }
    }

}
