package nl.elec332.gradle.minecraft.moddev;

import nl.elec332.gradle.minecraft.moddev.projects.AbstractPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPluginSC;
import nl.elec332.gradle.minecraft.moddev.projects.common.CommonProjectPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.common.CommonProjectPluginSC;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.fabric.FabricProjectPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.fabric.FabricProjectPluginSC;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.quilt.QuiltProjectPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.fabric.quilt.QuiltProjectPluginSC;
import nl.elec332.gradle.minecraft.moddev.projects.forge.forge.ForgeProjectPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.forge.forge.ForgeProjectPluginSC;
import nl.elec332.gradle.minecraft.moddev.projects.forge.neo.NeoProjectPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.forge.neo.NeoProjectPluginSC;
import org.gradle.api.Project;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Elec332 on 12-03-2024
 */
public enum ProjectType {

    COMMON("common", null, CommonProjectPlugin.class, CommonProjectPluginSC.class),
    FORGE(null, ModLoader.FORGE, ForgeProjectPlugin.class, ForgeProjectPluginSC.class),
    NEO_FORGE(null, ModLoader.NEO_FORGE, NeoProjectPlugin.class, NeoProjectPluginSC.class),
    FABRIC(null, ModLoader.FABRIC, FabricProjectPlugin.class, FabricProjectPluginSC.class),
    QUILT(null, ModLoader.QUILT, QuiltProjectPlugin.class, QuiltProjectPluginSC.class);


    ProjectType(String name, ModLoader loader, Class<? extends AbstractPlugin<?>> plugin, Class<? extends AbstractPluginSC> pluginSC) {
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
    }

    private final String name;
    private final ModLoader loader;
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
