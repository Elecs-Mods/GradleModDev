package nl.elec332.gradle.minecraft.moddev.util;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.SettingsInternal;
import org.gradle.plugin.management.PluginRequest;
import org.gradle.plugin.management.internal.PluginCoordinates;
import org.gradle.plugin.management.internal.PluginRequestInternal;
import org.gradle.plugin.management.internal.PluginRequests;
import org.gradle.plugin.management.internal.autoapply.AutoAppliedPluginHandler;
import org.gradle.plugin.management.internal.autoapply.AutoAppliedPluginRegistry;
import org.gradle.plugin.management.internal.autoapply.CompositeAutoAppliedPluginRegistry;
import org.gradle.plugin.use.PluginId;
import org.gradle.plugin.use.internal.DefaultPluginId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 14-03-2024
 * <p>
 * Heavily dependent on Gradle internals, but I have yet to find a better way...
 */
public final class RuntimeProjectPluginRequests implements AutoAppliedPluginRegistry {

    public static final String PROP_NAME = "autoPluginFunction";

    public static void inject(Settings settings) {
        AutoAppliedPluginHandler autoAppliedPluginHandler = ((SettingsInternal) settings).getServices().get(AutoAppliedPluginHandler.class);
        Class<?> targetClass;
        try {
            targetClass = Class.forName("org.gradle.plugin.management.internal.autoapply.DefaultAutoAppliedPluginHandler");
        } catch (ClassNotFoundException e) {
            try {
                targetClass = Class.forName("org.gradle.plugin.management.internal.DefaultPluginHandler");
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
        try {
            Field f = targetClass.getDeclaredField("registry");
            f.setAccessible(true);
            f.set(autoAppliedPluginHandler, new CompositeAutoAppliedPluginRegistry(List.of((AutoAppliedPluginRegistry) f.get(autoAppliedPluginHandler), new RuntimeProjectPluginRequests())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public PluginRequests getAutoAppliedPlugins(Project target) {
        if (target.hasProperty(PROP_NAME) && target.property(PROP_NAME) != null) {
            Consumer<BiConsumer<String, String>> reg = Objects.requireNonNull((Consumer<BiConsumer<String, String>>) target.property(PROP_NAME));
            List<PluginRequestInternal> plugins = new ArrayList<>();
            reg.accept((id, version) -> plugins.add(new Request(id, version)));
            return PluginRequests.of(plugins);
        }
        return PluginRequests.EMPTY;
    }

    @Override
    public PluginRequests getAutoAppliedPlugins(Settings target) {
        return PluginRequests.EMPTY;
    }

    //Gradle has a default implementation, but the constructor signature changed from 8.6 -> 8.7
    private static class Request implements PluginRequestInternal {

        private Request(String id, String version) {
            this.pluginId = new DefaultPluginId(id);
            this.version = version;
        }

        private final PluginId pluginId;
        private final String version;

        @Override
        public boolean isApply() {
            return true;
        }

        @Nullable
        @Override
        public Integer getLineNumber() {
            return null;
        }

        @Nullable
        @Override
        public String getScriptDisplayName() {
            return "RuntimePluginRequest";
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return "[id: '" + getId() + "', version: '" + version + "']";
        }

        @Nullable
        @Override
        public PluginRequest getOriginalRequest() {
            return null;
        }

        @NotNull
        @Override
        public Origin getOrigin() {
            return Origin.OTHER;
        }

        @NotNull
        @Override
        public Optional<PluginCoordinates> getAlternativeCoordinates() {
            return Optional.empty();
        }

        @NotNull
        @Override
        public PluginId getId() {
            return this.pluginId;
        }

        @Nullable
        @Override
        public String getVersion() {
            return this.version;
        }

        @Nullable
        @Override
        public ModuleVersionSelector getModule() {
            return null;
        }

    }

}
