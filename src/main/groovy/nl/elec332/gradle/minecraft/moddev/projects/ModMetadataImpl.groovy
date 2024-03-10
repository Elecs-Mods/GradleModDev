package nl.elec332.gradle.minecraft.moddev.projects

import com.moandjiezana.toml.TomlWriter
import groovy.json.JsonBuilder
import nl.elec332.gradle.minecraft.moddev.MLProperties
import nl.elec332.gradle.minecraft.moddev.ModLoader
import nl.elec332.gradle.minecraft.moddev.ModLoaderType
import nl.elec332.gradle.minecraft.moddev.ProjectHelper
import org.gradle.api.Action
import org.gradle.api.Project

/**
 * Created by Elec332 on 07-09-2023
 */
class ModMetadataImpl implements ModMetadata, Serializable {

    static ModMetadata generate(Project project, Collection<Action<ModMetadata>> mods) {
        var mm = new ModMetadataImpl(project)
        mods.forEach {it.execute(mm)}
        return mm
    }

    private ModMetadataImpl(Project project) {
        this.loader = ProjectHelper.getPlugin(project).getModLoader()
        this.data = new HashMap()
        this.modId_INT = ProjectHelper.getStringProperty(project, MLProperties.MOD_ID)
        this.altLoaders = new HashSet<>();
    }

    private final ModLoader loader
    private final String modId_INT
    private final Map data
    private final Set<ModLoader> altLoaders;

    @Override
    void importFrom(ModMetadata other) {
        if (!(other instanceof ModMetadataImpl)) {
            throw new UnsupportedOperationException()
        }
        if (!other.altLoaders.empty) {
            if (other.altLoaders.size() == 1 && other.altLoaders.contains(loader) && altLoaders.empty) {
                data.clear()
                data.putAll(other.data)
                altLoaders.add(other.loader)
                return
            }
            throw new UnsupportedOperationException()
        }
        if (other.loader == loader) {
            data.putAll(other.data)
        } else if (loader.type == other.loader.type && loader.type == ModLoaderType.FORGE && modId_INT == other.modId_INT) {
            altLoaders.add(other.loader)
            Collection<Map> deps = data["dependencies"][modId_INT] as Collection<Map>
            Collection<Map> otherDeps = other.data["dependencies"][modId_INT] as Collection<Map>
            for (dep in (loader == ModLoader.FORGE ? deps : otherDeps)) {
                dep["type"] = "optional"
            }
            for (dep in (loader == ModLoader.NEO_FORGE ? deps : otherDeps)) {
                dep["mandatory"] = false
            }
            deps.addAll(otherDeps)
            if (other.data.containsKey(other.mixinEntry())) {
                merge(data, Map.of(other.mixinEntry(), other.data[other.mixinEntry()]))
            }
        } else {
            throw new UnsupportedOperationException()
        }
    }

    String mixinEntry() {
        return loader == ModLoader.FORGE ? "forgemixins" : "mixins"
    }

    @Override
    void modGroupId(String s) {
        if (this.loader == ModLoader.QUILT) {
            data["group"] = s
        }
    }

    @Override
    void modLicense(String s) {
        if (this.loader != ModLoader.QUILT) {
            data["license"] = s
        }
    }

    @Override
    void minecraftVersion(String s) {

    }

    @Override
    void loaderVersion(String s) {
        if (this.loader.getType() == ModLoaderType.FORGE) {
            this.data["loaderVersion"] = toForgeVersion(s)
        }
    }

    @Override
    String getLoaderVersion() {
        if (this.loader.getType() == ModLoaderType.FORGE) {
            return this.data["loaderVersion"]
        }
        return null
    }

    @Override
    void loader(String loader) {
        if (this.loader.getType() == ModLoaderType.FORGE) {
            this.data["modLoader"] = loader
        }
    }

    @Override
    void entryPoints(Map data) {
        if (this.loader.getType() == ModLoaderType.FABRIC) {
            this.data["entrypoints"] = data
        }
    }

    @Override
    void mixin(String s) {
        merge(data, Map.of(mixinEntry(), [s]))
    }

    @Override
    void issues(String s) {
        switch (this.loader) {
            case ModLoader.FABRIC:
                break
            case ModLoader.QUILT:
                merge(data, ["metadata": ["contact": ["issues": s]]])
                break
            case ModLoader.FORGE:
            case ModLoader.NEO_FORGE:
                data["issueTrackerURL"] = s
                break
            default:
                throw new UnsupportedOperationException()
        }
    }

    @Override
    void sources(String s) {
        switch (this.loader) {
            case ModLoader.FABRIC:
                merge(data, ["contact": ["sources": s]])
                break
            case ModLoader.QUILT:
                merge(data, ["metadata": ["contact": ["sources": s]]])
                break
            case ModLoader.FORGE:
            case ModLoader.NEO_FORGE:
                break
            default:
                throw new UnsupportedOperationException()
        }
    }

    @Override
    void mod(Action<ModInfo> mod) {
        this.mod(modId_INT, mod)
    }

    @Override
    void mod(String modId, Action<ModInfo> mod) {
        if (this.loader.getType() == ModLoaderType.FABRIC) {
            ModInfo mi = new ModInfo() {

                @Override
                void modVersion(String s) {
                    data["version"] = s
                }

                @Override
                void modName(String s) {
                    if (ModMetadataImpl.this.loader == ModLoader.QUILT) {
                        merge(data, ["metadata": ["name": s]])
                    } else {
                        data["name"] = s
                    }
                }

                @Override
                void modDescription(String s) {
                    if (ModMetadataImpl.this.loader == ModLoader.QUILT) {
                        merge(data, ["metadata": ["description": s]])
                    } else {
                        data["description"] = s
                    }
                }

                @Override
                void logo(String s) {
                    if (ModMetadataImpl.this.loader == ModLoader.QUILT) {
                        merge(data, ["metadata": ["icon": s]])
                    } else {
                        data["icon"] = s
                    }
                }

                @Override
                void setAuthors(Collection<String> s) {
                    if (ModMetadataImpl.this.loader == ModLoader.QUILT) {
                        merge(data, ["metadata": ["contributors": [(String.join(", ", s) + ""): ""]]])
                    } else {
                        data["authors"] = s
                    }
                }

                @Override
                void homepage(String s) {
                    if (ModMetadataImpl.this.loader == ModLoader.QUILT) {
                        merge(data, ["metadata": ["contact": ["homepage": s]]])
                    } else {
                        merge(data, ["contact": ["homepage": s]])
                    }
                }

            }
            mod.execute(mi)
            data["id"] = modId
        } else {
            Map m = null
            if (data.containsKey("mods")) {
                for (Map m2 : (Collection<Map>) data.get("mods")) {
                    if (m2.get("modId") == modId) {
                        m = m2
                        break
                    }
                }
            }
            if (m == null) {
                m = new HashMap()
                merge(data, ["mods": [m]])
            }
            m["modId"] = modId
            mod.execute(new ForgeModInfo(m))
        }
    }

    private Map getDependency(String modId, boolean create) {
        Map m = null
        Map iData = data
        String key = "depends";
        String id = "id"
        if (loader.getType() == ModLoaderType.FORGE) {
            key = modId_INT
            id = "modId"
            iData = data.computeIfAbsent("dependencies", k -> new HashMap<>()) as Map
        }

        if (iData.containsKey(key)) {
            for (Map m2 : (Collection<Map>) iData.get(key)) {
                if (m2[id] == modId) {
                    m = m2
                    break
                }
            }
        }
        if (!create || m != null) {
            return m
        }

        m = new HashMap()
        merge(iData, [(key): [m]])
        m[id] = modId
        return m
    }

    @Override
    boolean hasDependency(String modId) {
        return getDependency(modId, false) != null
    }

    @Override
    void dependsOn(String modId, Action<DependencyInfo> mod) {
        DependencyInfo di
        Map m = getDependency(modId, true)
        di = new DependencyInfo() {

            @Override
            void version(String s) {
                s = s.trim()
                boolean t = s.startsWith("=") || s.startsWith(">") || s.startsWith("<")
                switch (ModMetadataImpl.this.loader) {
                    case ModLoader.FABRIC:
                    case ModLoader.QUILT:
                        m["versions"] = (t ? "" : "=") + s
                        break
                    case ModLoader.FORGE:
                    case ModLoader.NEO_FORGE:
                        m["versionRange"] = toForgeVersion(s)
                        break
                    default:
                        throw new UnsupportedOperationException()
                }
            }

            @Override
            void versionRange(String from, String to) {
                if (to == null || to.isEmpty()) {
                    version(from)
                }
                if (from == null || from.isEmpty()) {
                    version(to)
                }
                if (from.startsWith("<") || to.startsWith(">")) {
                    throw new IllegalArgumentException("Invalid from/to range: " + from + " -> " + to)
                }
                switch (ModMetadataImpl.this.loader) {
                    case ModLoader.FABRIC:
                        m["versions"] = from + " " + to
                        break
                    case ModLoader.QUILT:
                        m["versions"] = ["all": [from, to]]
                        break
                    case ModLoader.FORGE:
                    case ModLoader.NEO_FORGE:
                        m["versionRange"] = toForge(from) + "," + toForge(to)
                        break
                    default:
                        throw new UnsupportedOperationException()
                }
            }

            @Override
            void mandatory(boolean b) {
                if (ModMetadataImpl.this.loader == ModLoader.FORGE) {
                    m["mandatory"] = b
                }
                if (ModMetadataImpl.this.loader == ModLoader.NEO_FORGE) {
                    m["type"] = b ? "required" : "optional"
                }
            }

            @Override
            void ordering(DependencyOrdering s) {
                if (ModMetadataImpl.this.loader.getType() == ModLoaderType.FORGE) {
                    m["ordering"] = s.toString()
                }
            }

            @Override
            void side(ModSide s) {
                if (ModMetadataImpl.this.loader.getType() == ModLoaderType.FORGE) {
                    m["side"] = s.toString()
                }
            }

        }
        if (m.keySet().size() <= 1 && loader.getType() == ModLoaderType.FORGE) {
            di.mandatory(true)
            di.ordering(DependencyOrdering.NONE)
            di.side(ModSide.BOTH)
        }
        mod.execute(di)
    }

    @Override
    String getFileLocation() {
        switch (this.loader) {
            case ModLoader.FABRIC:
                return "fabric.mod.json"
            case ModLoader.QUILT:
                return "quilt.mod.json"
            case ModLoader.FORGE:
            case ModLoader.NEO_FORGE:
                return "META-INF/mods.toml"
            default:
                throw new UnsupportedOperationException()
        }
    }

    @Override
    Set<String> getMixins() {
        Object data = this.data[mixinEntry()]
        if (data == null) {
            return null
        }
        return new HashSet<String>(data as Collection)
    }

    @Override
    String toString() {
        Map map = new HashMap(data)
        Map map2
        switch (this.loader) {
            case ModLoader.FABRIC:
                map["schemaVersion"] = 1
                if (map.containsKey("depends")) {
                    Collection deps = map.remove("depends") as Collection
                    map2 = new HashMap()
                    for (d in deps) {
                        map2[d["id"]] = d["versions"]
                    }
                    map["depends"] = map2
                }
                break
            case ModLoader.QUILT:
                map2 = new HashMap()
                map2["schema_version"] = 1
                map2["quilt_loader"] = map
                if (map.containsKey(mixinEntry())) {
                    Collection mixins = map.remove(mixinEntry()) as Collection
                    map2["mixin"] = mixins
                }
                map["intermediate_mappings"] = "net.fabricmc:intermediary"
                map = map2
                break
            case ModLoader.FORGE:
            case ModLoader.NEO_FORGE:
                if (!map.containsKey("modLoader")) {
                    map["modLoader"] = "javafml"
                }

                for (mixName in ["mixins", "forgemixins"]) {
                    Collection mixins = map.remove(mixName) as Collection
                    if (mixins != null && !mixins.empty) {
                        Set mm = new HashSet();
                        map.put(mixName, mm)
                        for (s in mixins) {
                            mm.add(["config": s])
                        }
                    }
                }
                break
            default:
                throw new UnsupportedOperationException()
        }
        if (this.loader.type == ModLoaderType.FORGE) {
            return new TomlWriter().write(map)
        }
        return new JsonBuilder(map).toPrettyString()
    }

    private static void merge(Map origin, Map extra) {
        if (extra === null){
            return
        }
        extra.forEach {key, value ->
            Object ok = origin[key]
            if (ok instanceof Map && value instanceof Map) {
                merge(ok as Map, value as Map)
                return
            }
            if (value instanceof Collection && ok != null) {
                if (!(ok instanceof Collection)) {
                    origin[key] = [ok]
                }
                (ok as Collection).addAll(value as Collection)
                return
            }
            if (ok instanceof Collection) {
                (ok as Collection).add(value)
                return
            }
            origin[key] = value
        }
    }

    //Credits, updateJson, side
    private static class ForgeModInfo implements ModInfo {

        ForgeModInfo(Map map) {
            this.map = map
        }

        private final Map map

        @Override
        void modVersion(String s) {
            map["version"] = s
        }

        @Override
        void modName(String s) {
            map["displayName"] = s
        }

        @Override
        void modDescription(String s) {
            map["description"] = s
        }

        @Override
        void logo(String s) {
            map["logoFile"] = s
        }

        @Override
        void setAuthors(Collection<String> s) {
            map["authors"] = String.join(", ", s)
        }

        @Override
        void homepage(String s) {
            map["displayURL"] = s
        }

    }

    static String toForgeVersion(String s) {
        if (s.contains("[") || s.contains("]") || s.contains(")") || s.contains("(")) {
            return s
        }
        if (s.startsWith(">")) {
            s = toForge(s) + ",)"
        } else if (s.startsWith("<")) {
            s = "(," + toForge(s)
        } else {
            s = "[" + toForge(s)
        }
        return s
    }

    private static String toForge(String s) {
        if (s.startsWith(">=")) {
            return s.replace(">=", "[")
        }
        if (s.startsWith(">")) {
            return s.replace(">", "(")
        }
        if (s.startsWith("<=")) {
            return s.replace("<=", "") + "]"
        }
        if (s.startsWith("<")) {
            return s.replace("<", "") + ")"
        }
        return s + "]"
    }

    @Override
    boolean equals(Object obj) {
        if (!(obj instanceof ModMetadataImpl)) {
            return false
        }
        return loader == obj.loader && data == obj.data
    }

    @Override
    int hashCode() {
        return Objects.hash(loader, data)
    }

}
