package nl.elec332.gradle.minecraft.moddev.projects;

import com.moandjiezana.toml.TomlWriter;
import groovy.json.JsonBuilder;
import groovy.lang.Closure;
import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.ModLoaderType;
import nl.elec332.gradle.minecraft.moddev.util.GradleInternalHelper;
import nl.elec332.gradle.minecraft.moddev.util.MappedData;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Action;
import org.gradle.api.Project;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Elec332 on 07-09-2023
 * <p>
 * Don't look, your eyes won't survive it
 */
public class ModMetadataImpl implements ModMetadata {

    public static ModMetadata generate(Project project, Collection<Action<ModMetadata>> mods) {
        final ModMetadata mm = new ModMetadataImpl(project);
        mods.forEach(a -> a.execute(mm));
        return mm;
    }

    private ModMetadataImpl(Project project) {
        this.loader = Objects.requireNonNull(ProjectHelper.getPlugin(project).getProjectType().getModLoader());
        this.data = new MappedData();
        this.modId_INT = ProjectHelper.getStringProperty(project, MLProperties.MOD_ID);
        this.altLoaders = new HashSet<>();
    }

    private final ModLoader loader;
    private final String modId_INT;
    private final MappedData data;
    private final Set<ModLoader> altLoaders;

    @Override
    public void importFrom(ModMetadata other_) {
        if (!(other_ instanceof ModMetadataImpl)) {
            throw new UnsupportedOperationException();
        }
        ModMetadataImpl other = (ModMetadataImpl) other_;
        if (!other.altLoaders.isEmpty()) {
            if (other.altLoaders.size() == 1 && other.altLoaders.contains(this.loader) && this.altLoaders.isEmpty()) {
                this.data.clear();
                this.data.mergeWith(other.data);
                this.altLoaders.add(other.loader);
                return;
            }
            throw new UnsupportedOperationException();
        }
        if (other.loader == this.loader) {
            this.data.mergeWith(other.data);
        } else if (this.loader.getType() == other.loader.getType() && this.loader.getType() == ModLoaderType.FORGE && Objects.equals(modId_INT, other.modId_INT)) {
            this.altLoaders.add(other.loader);
            MappedData deps = this.data.getMap("dependencies");
            MappedData otherDeps = other.data.getMap("dependencies");
            (this.loader == ModLoader.FORGE ? deps : otherDeps).getMapCollection(this.modId_INT).forEach(d -> d.putString("type", "optional"));
            (this.loader == ModLoader.NEO_FORGE ? deps : otherDeps).getMapCollection(this.modId_INT).forEach(d -> d.put("mandatory", false));
            deps.mergeWith(otherDeps);
            if (other.data.containsKey(other.mixinEntry())) {
                this.data.mergeWith(MappedData.ofStringCollection(other.mixinEntry(), other.data.getStringCollection(other.mixinEntry())));
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private String mixinEntry() {
        return this.loader == ModLoader.FORGE ? "forgemixins" : "mixins";
    }

    @Override
    public void modGroupId(String s) {
        if (this.loader == ModLoader.QUILT) {
            this.data.putString("group", s);
        }
    }

    @Override
    public void modLicense(String s) {
        if (this.loader != ModLoader.QUILT) {
            this.data.putString("license", s);
        }
    }

    @Override
    public void minecraftVersion(String s) {

    }

    @Override
    public void mod(Closure<?> c) {
        mod(GradleInternalHelper.configureUsing(c));
    }

    @Override
    public void loaderVersion(String s) {
        if (this.loader.getType() == ModLoaderType.FORGE) {
            this.data.putString("loaderVersion", toForgeVersion(s));
        }
    }

    @Override
    public String getLoaderVersion() {
        if (this.loader.getType() == ModLoaderType.FORGE) {
            return this.data.getString("loaderVersion");
        }
        return null;
    }

    @Override
    public void loader(String loader) {
        if (this.loader.getType() == ModLoaderType.FORGE) {
            this.data.putString("modLoader", loader);
        }
    }

    @Override
    public void entryPoints(Map<String, ?> entryPoints) {
        if (this.loader.getType() == ModLoaderType.FABRIC) {
            this.data.mergeWith(MappedData.of(b -> b.putMap("entrypoints", entryPoints)));
        }
    }

    @Override
    public void mixin(String s) {
        this.data.mergeWith(MappedData.ofStringCollection(mixinEntry(), List.of(s)));
    }

    @Override
    public void issues(String s) {
        switch (this.loader) {
            case FABRIC:
                break;
            case QUILT:
                this.data.mergeWith(MappedData.of("metadata", MappedData.of("contact", MappedData.of("issues", s))));
                break;
            case FORGE:
            case NEO_FORGE:
                this.data.putString("issueTrackerURL", s);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public void sources(String s) {
        switch (this.loader) {
            case FABRIC:
                this.data.mergeWith(MappedData.of("contact", MappedData.of("sources", s)));
                break;
            case QUILT:
                this.data.mergeWith(MappedData.of("metadata", MappedData.of("contact", MappedData.of("sources", s))));
                break;
            case FORGE:
            case NEO_FORGE:
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public void mod(Action<ModInfo> mod) {
        this.mod(this.modId_INT, mod);
    }

    @Override
    public void mod(String modId, Action<ModInfo> mod) {
        if (this.loader.getType() == ModLoaderType.FABRIC) {
            ModInfo mi = new ModInfo() {

                @Override
                public void modVersion(String s) {
                    ModMetadataImpl.this.data.putString("version", s);
                }

                @Override
                public void modName(String s) {
                    if (ModMetadataImpl.this.loader == ModLoader.QUILT) {
                        ModMetadataImpl.this.data.mergeWith(MappedData.of("metadata", MappedData.of("name", s)));
                    } else {
                        ModMetadataImpl.this.data.putString("name", s);
                    }
                }

                @Override
                public void modDescription(String s) {
                    if (ModMetadataImpl.this.loader == ModLoader.QUILT) {
                        ModMetadataImpl.this.data.mergeWith(MappedData.of("metadata", MappedData.of("description", s)));
                    } else {
                        ModMetadataImpl.this.data.putString("description", s);
                    }
                }

                @Override
                public void logo(String s) {
                    if (ModMetadataImpl.this.loader == ModLoader.QUILT) {
                        ModMetadataImpl.this.data.mergeWith(MappedData.of("metadata", MappedData.of("icon", s)));
                    } else {
                        ModMetadataImpl.this.data.putString("icon", s);
                    }
                }

                @Override
                public void setAuthors(Collection<String> s) {
                    if (ModMetadataImpl.this.loader == ModLoader.QUILT) {
                        ModMetadataImpl.this.data.mergeWith(MappedData.of("metadata", MappedData.of(b -> b.putMap("contributors", s.stream().collect(Collectors.toMap(t -> t, t -> "Author"))))));
                    } else {
                        ModMetadataImpl.this.data.mergeWith(MappedData.ofStringCollection("authors", s));
                    }
                }

                @Override
                public void homepage(String s) {
                    if (ModMetadataImpl.this.loader == ModLoader.QUILT) {
                        ModMetadataImpl.this.data.mergeWith(MappedData.of("metadata", MappedData.of("contact", MappedData.of("homepage", s))));
                    } else {
                        ModMetadataImpl.this.data.mergeWith(MappedData.of("contact", MappedData.of("homepage", s)));
                    }
                }

            };
            mod.execute(mi);
            ModMetadataImpl.this.data.putString("id", modId);
        } else {
            MappedData m = null;
            if (this.data.containsKey("mods")) {
                m = this.data.getMapCollection("mods")
                        .filter(d -> Objects.equals(d.getString("modId"), modId))
                        .findFirst()
                        .orElse(null);
            }
            if (m == null) {
                m = new MappedData();
                m.putString("modId", modId);
                this.data.mergeWith(MappedData.ofMapCollection("mods", List.of(m)));
            }
            mod.execute(new ForgeModInfo(m));
        }
    }

    private MappedData getDependency(String modId, boolean create) {
        MappedData m = null;
        MappedData iData = this.data;
        String key = "depends";
        String id = "id";
        if (this.loader.getType() == ModLoaderType.FORGE) {
            key = this.modId_INT;
            id = "modId";
            iData = this.data.getOrCreateMap("dependencies");
        }

        if (iData.containsKey(key)) {
            for (MappedData m2 : iData.getMapCollection(key).toList()) {
                if (modId.equals(m2.getString(id))) {
                    m = m2;
                    break;
                }
            }
        }
        if (!create || m != null) {
            return m;
        }

        m = new MappedData();
        iData.mergeWith(MappedData.ofMapCollection(key, List.of(m)));
        m.putString(id, modId);
        return m;
    }

    @Override
    public boolean hasDependency(String modId) {
        return getDependency(modId, false) != null;
    }

    @Override
    public void dependsOn(String modId, Closure<?> mod) {
        dependsOn(modId, GradleInternalHelper.configureUsing(mod));
    }

    @Override
    public void dependsOn(String modId, Action<DependencyInfo> mod) {
        DependencyInfo di;
        MappedData m = getDependency(modId, true);
        di = new DependencyInfo() {

            @Override
            public void version(String s) {
                s = s.trim();
                boolean t = s.startsWith("=") || s.startsWith(">") || s.startsWith("<");
                switch (ModMetadataImpl.this.loader) {
                    case FABRIC:
                    case QUILT:
                        m.putString("versions", (t ? "" : "=") + s);
                        break;
                    case FORGE:
                    case NEO_FORGE:
                        m.putString("versionRange", toForgeVersion(s));
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }

            @Override
            public void versionRange(String from, String to) {
                if (to == null || to.isEmpty()) {
                    version(from);
                    return;
                }
                if (from == null || from.isEmpty()) {
                    version(to);
                    return;
                }
                if (from.startsWith("<") || to.startsWith(">")) {
                    throw new IllegalArgumentException("Invalid from/to range: " + from + " -> " + to);
                }
                switch (ModMetadataImpl.this.loader) {
                    case FABRIC:
                        m.putString("versions", from + " " + to);
                        break;
                    case QUILT:
                        m.mergeWith(MappedData.of("versions", MappedData.ofStringCollection("all", List.of(from, to))));
                        break;
                    case FORGE:
                    case NEO_FORGE:
                        m.putString("versionRange", toForge(from) + "," + toForge(to));
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }

            @Override
            public void mandatory(boolean b) {
                if (ModMetadataImpl.this.loader == ModLoader.FORGE) {
                    m.put("mandatory", b);
                }
                if (ModMetadataImpl.this.loader == ModLoader.NEO_FORGE) {
                    m.putString("type", b ? "required" : "optional");
                }
            }

            @Override
            public void ordering(DependencyOrdering s) {
                if (ModMetadataImpl.this.loader.getType() == ModLoaderType.FORGE) {
                    m.putString("ordering", s.toString());
                }
            }

            @Override
            public void side(ModSide s) {
                if (ModMetadataImpl.this.loader.getType() == ModLoaderType.FORGE) {
                    m.putString("side", s.toString());
                }
            }

        };
        if (m.keySet().size() <= 1 && this.loader.getType() == ModLoaderType.FORGE) {
            di.mandatory(true);
            di.ordering(DependencyOrdering.NONE);
            di.side(ModSide.BOTH);
        }
        mod.execute(di);
    }

    @Override
    @SuppressWarnings("UnnecessaryDefault")
    public String getFileLocation() {
        return switch (this.loader) {
            case FABRIC -> "fabric.mod.json";
            case QUILT -> "quilt.mod.json";
            case FORGE, NEO_FORGE -> "META-INF/mods.toml";
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public Set<String> getMixins() {
        Collection<String> data = this.data.getStringCollection(mixinEntry());
        if (data == null) {
            return null;
        }
        return new HashSet<>(data);
    }

    @Override
    public String toString() {
        final MappedData map = this.data.copy();
        MappedData ret = map;
        switch (this.loader) {
            case FABRIC:
                map.put("schemaVersion", 1);
                if (map.containsKey("depends")) {
                    MappedData map2 = new MappedData();
                    map.removeMapCollection("depends").forEach(d -> map2.putString(d.getString("id"), d.getString("versions")));
                    map.mergeWith(MappedData.of("depends", map2));
                }
                break;
            case QUILT:
                MappedData map2 = MappedData.of(b -> {
                    b.put("schema_version", 1);
                    b.putMap("quilt_loader", map);
                    if (map.containsKey(mixinEntry())) {
                        b.putStringCollection("mixin", map.removeStringCollection(mixinEntry()));
                    }
                });
                map.putString("intermediate_mappings", "net.fabricmc:intermediary");
                ret = map2;
                break;
            case FORGE:
            case NEO_FORGE:
                if (!map.containsKey("modLoader")) {
                    map.putString("modLoader", "javafml");
                }

                Stream.of("mixins", "forgemixins").forEach(mixName -> {
                    if (map.containsKey(mixName)) {
                        map.mergeWith(MappedData.ofMapCollection(mixName, map.removeStringCollection(mixName).stream().map(s -> MappedData.of("config", s)).toList()));
                    }
                });
                break;
            default:
                throw new UnsupportedOperationException();
        }
        if (this.loader.getType() == ModLoaderType.FORGE) {
            return new TomlWriter().write(ret.getbackedMap());
        }
        return new JsonBuilder(ret.getbackedMap()).toPrettyString();
    }

    //Credits, updateJson, side
    @SuppressWarnings("ClassCanBeRecord") // No it can't
    private static class ForgeModInfo implements ModInfo {

        private ForgeModInfo(MappedData map) {
            this.map = map;
        }

        private final MappedData map;

        @Override
        public void modVersion(String s) {
            this.map.putString("version", s);
        }

        @Override
        public void modName(String s) {
            this.map.putString("displayName", s);
        }

        @Override
        public void modDescription(String s) {
            this.map.putString("description", s);
        }

        @Override
        public void logo(String s) {
            this.map.putString("logoFile", s);
        }

        @Override
        public void setAuthors(Collection<String> s) {
            this.map.putString("authors", String.join(", ", s));
        }

        @Override
        public void homepage(String s) {
            this.map.putString("displayURL", s);
        }

    }

    static String toForgeVersion(String s) {
        if (s.contains("[") || s.contains("]") || s.contains(")") || s.contains("(")) {
            return s;
        }
        if (s.startsWith(">")) {
            s = toForge(s) + ",)";
        } else if (s.startsWith("<")) {
            s = "(," + toForge(s);
        } else {
            s = "[" + toForge(s);
        }
        return s;
    }

    private static String toForge(String s) {
        if (s.startsWith(">=")) {
            return s.replace(">=", "[");
        }
        if (s.startsWith(">")) {
            return s.replace(">", "(");
        }
        if (s.startsWith("<=")) {
            return s.replace("<=", "") + "]";
        }
        if (s.startsWith("<")) {
            return s.replace("<", "") + ")";
        }
        return s + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ModMetadataImpl)) {
            return false;
        }
        return this.loader == ((ModMetadataImpl) obj).loader && this.data.equals(((ModMetadataImpl) obj).data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.loader, this.data);
    }

}

