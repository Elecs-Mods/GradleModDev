package nl.elec332.gradle.minecraft.moddev.projects;

import groovy.json.JsonBuilder;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.*;

/**
 * Created by Elec332 on 08-09-2023
 */
public class CommonExtension {

    @Inject
    protected Project getProject() {
        throw new UnsupportedOperationException();
    }

    public boolean noModuleMetadata = true;

    private final Set<Action<ModMetadata>> metaModifiers = new HashSet<>();

    public final void metadata(Action<ModMetadata> mod) {
        this.metaModifiers.add(mod);
    }

    public final Set<Action<ModMetadata>> getMetaModifiers() {
        return Collections.unmodifiableSet(metaModifiers);
    }

    private final Set<Mixin> mixins = new HashSet<>();

    public final Set<Mixin> getMixins() {
        return mixins;
    }

    public final void mixin(Action<Mixin> modifier) {
        Mixin r = new Mixin();
        modifier.execute(r);
        mixins.add(r);
    }

    public final boolean hasMixins() {
        return !mixins.isEmpty();
    }

    public static final class Mixin implements Serializable {

        public boolean generateOnly = false;
        public boolean required = true;
        public String mixinPackage = null;
        public JavaVersion compatibilityLevel = null;
        public String plugin = null;
        public String refMap = null;
        public List<String> mixins = null;
        public List<String> clientMixins = null;
        public List<String> serverMixins = null;

        public String toJson(Action<Mixin> modifier, Project project) {
            boolean hasMixins = false;
            hasMixins |= mixins != null && !mixins.isEmpty();
            hasMixins |= clientMixins != null && !clientMixins.isEmpty();
            hasMixins |= serverMixins != null && !serverMixins.isEmpty();
            if (!hasMixins && plugin == null) {
                throw new IllegalArgumentException("Attempting to add mixin config without mixins or plugin!");
            }
            if (modifier != null) {
                modifier.execute(this);
            }
            JavaVersion compatibilityLevel = this.compatibilityLevel;
            if (compatibilityLevel == null) {
                compatibilityLevel = project.getExtensions().getByType(JavaPluginExtension.class).getTargetCompatibility();
            }
            JsonBuilder b = new JsonBuilder();
            Map<String, Object> m = new HashMap<>();
            m.put("required", required);
            if (!(plugin != null && mixinPackage == null && !hasMixins)) {
                m.put("package", Objects.requireNonNull(mixinPackage));
            }
            m.put("compatibilityLevel", "JAVA_" + Objects.requireNonNull(compatibilityLevel).getMajorVersion());
            if (hasMixins) {
                String refMap = this.refMap;
                if (refMap == null || refMap.isEmpty()) {
                    refMap = ProjectHelper.getMixinRefMap(project);
                }
                m.put("refmap", refMap);
            }
            if (plugin != null) {
                m.put("plugin", plugin);
            }
            if (mixins != null) {
                m.put("mixins", mixins);
            }
            if (clientMixins != null) {
                m.put("client", clientMixins);
            }
            if (serverMixins != null) {
                m.put("server", serverMixins);
            }
            m.put("injectors", Map.of("defaultRequire", 1));
            m.put("minVersion", "0.8");
            b.call(m);
            return b.toPrettyString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Mixin mixin = (Mixin) o;
            return generateOnly == mixin.generateOnly && required == mixin.required && Objects.equals(mixinPackage, mixin.mixinPackage) && compatibilityLevel == mixin.compatibilityLevel && Objects.equals(plugin, mixin.plugin) && Objects.equals(refMap, mixin.refMap) && Objects.equals(mixins, mixin.mixins) && Objects.equals(clientMixins, mixin.clientMixins);
        }

        @Override
        public int hashCode() {
            return Objects.hash(generateOnly, required, mixinPackage, compatibilityLevel, plugin, refMap, mixins, clientMixins);
        }

        @Override
        public String toString() {
            return "Mixin{" +
                    "generateOnly=" + generateOnly +
                    ", required=" + required +
                    ", mixinPackage='" + mixinPackage + '\'' +
                    ", compatibilityLevel=" + compatibilityLevel +
                    ", plugin='" + plugin + '\'' +
                    ", refMap='" + refMap + '\'' +
                    ", mixins=" + mixins +
                    ", clientMixins=" + clientMixins +
                    '}';
        }

    }

}
