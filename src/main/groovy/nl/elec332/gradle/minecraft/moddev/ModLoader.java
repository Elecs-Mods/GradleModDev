package nl.elec332.gradle.minecraft.moddev;

import nl.elec332.gradle.minecraft.moddev.projects.AbstractPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPluginSC;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.fabric.FabricProjectPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.fabric.FabricProjectPluginSC;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.quilt.QuiltProjectPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.quilt.QuiltProjectPluginSC;
import nl.elec332.gradle.minecraft.moddev.projects.forge.forge.ForgeProjectPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.forge.forge.ForgeProjectPluginSC;
import nl.elec332.gradle.minecraft.moddev.projects.forge.neo.NeoProjectPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.forge.neo.NeoProjectPluginSC;
import org.gradle.api.Project;

/**
 * Created by Elec332 on 02-09-2023
 */
public enum ModLoader {

    FORGE("forge", ModLoaderType.FORGE, Mapping.FORGE_SRG, ForgeProjectPlugin.class, ForgeProjectPluginSC.class), NEO_FORGE("neo", ModLoaderType.FORGE, Mapping.NAMED, NeoProjectPlugin.class, NeoProjectPluginSC.class), FABRIC("fabric", ModLoaderType.FABRIC, Mapping.FABRIC_INTERMEDIARY, FabricProjectPlugin.class, FabricProjectPluginSC.class), QUILT("quilt", ModLoaderType.FABRIC, Mapping.FABRIC_INTERMEDIARY, QuiltProjectPlugin.class, QuiltProjectPluginSC.class);

    ModLoader(String name, ModLoaderType type, Mapping mapping, Class<? extends AbstractPlugin<?>> plugin, Class<? extends AbstractPluginSC> pluginSC) {
        this.name = name;
        this.type = type;
        this.mapping = mapping;
        this.plugin = plugin;
        this.pluginSC = pluginSC;
    }

    private final String name;
    private final ModLoaderType type;
    private final Mapping mapping;
    private final Class<? extends AbstractPlugin<?>> plugin;
    private final Class<? extends AbstractPluginSC> pluginSC;

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

    public ModLoaderType getType() {
        return type;
    }

    public Mapping getMapping() {
        return mapping;
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

    public static String getIdentifier(Project project) {
        return project.getBuildFile().getParentFile().getName();
    }

    public static final String COMMON_PROJECT_NAME = "common";

    public enum Mapping {

        NAMED, FORGE_SRG, FABRIC_INTERMEDIARY

    }

}
