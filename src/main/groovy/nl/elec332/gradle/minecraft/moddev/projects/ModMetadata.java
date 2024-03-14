package nl.elec332.gradle.minecraft.moddev.projects;

import groovy.lang.Closure;
import org.gradle.api.Action;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by Elec332 on 07-09-2023
 */
public interface ModMetadata extends Serializable {

    void importFrom(ModMetadata meta);

    void modGroupId(String s);

    void modLicense(String s);

    void minecraftVersion(String s);

    void entryPoints(Map<String, ?> data);

    void mod(Closure<?> c);

    void loaderVersion(String s);

    String getLoaderVersion();

    void loader(String loader);

    void mod(Action<ModInfo> mod);

    void mod(String modId, Action<ModInfo> mod);

    boolean hasDependency(String modId);

    void dependsOn(String modId, Closure<?> mod);

    void dependsOn(String modId, Action<DependencyInfo> mod);

    default void dependsOn(String modId, String version) {
        dependsOn(modId, i -> i.version(version));
    }

    void mixin(String s);

    void issues(String s);

    void sources(String s);

    interface DependencyInfo {

        void version(String s);

        void versionRange(String from, String to);

        void mandatory(boolean b);

        void ordering(DependencyOrdering o);

        void side(ModSide s);

    }

    interface ModInfo {

        void modVersion(String s);

        void modName(String s);

        void modDescription(String s);

        void logo(String s);

        void setAuthors(Collection<String> s);

        void homepage(String s);

    }

    enum DependencyOrdering {
        NONE, BEFORE, AFTER
    }

    enum ModSide {
        BOTH, CLIENT, SERVER
    }

    String getFileLocation();

    Set<String> getMixins();

}
