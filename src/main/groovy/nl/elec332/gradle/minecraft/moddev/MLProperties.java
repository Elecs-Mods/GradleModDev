package nl.elec332.gradle.minecraft.moddev;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Elec332 on 05-09-2023
 */
public class MLProperties {

    public static final String MC_VERSION = "minecraft_version";
    public static final String MIXIN_VERSION = "mixin_version";


    public static final String MOD_VERSION = "mod_version";
    public static final String MOD_ID = "mod_id";
    public static final String MOD_GROUP_ID = "mod_group_id";
    public static final String MOD_NAME = "mod_name";
    public static final String MOD_AUTHORS = "mod_authors";
    public static final String MOD_DESCRIPTION = "mod_description";
    public static final String MOD_LICENSE = "mod_license";
    public static final String MIXIN_GRADLE_VANILLA = "mixin_gradle_vanilla_version";

    public static final String FORGE_VERSION = "forge_version";
    public static final String FORGE_VERSION_DEP = "forge_version_dependency";
    public static final String FORGE_LOADER_VERSION = "forge_loader_version";
    public static final String FORGE_GRADLE_VERSION = "forge_gradle_version";

    public static final String NEO_VERSION = "neo_version";
    public static final String NEO_VERSION_DEP = "neo_version_dependency";
    public static final String NEO_LOADER_VERSION = "neo_loader_version";
    public static final String NEO_GRADLE_VERSION = "neo_gradle_version";

    public static final String FABRIC_LOADER_VERSION = "fabric_loader_version";
    public static final String FABRIC_LOOM_VERSION = "fabric_loom_version";
    public static final String FABRIC_VERSION = "fabric_version";
    public static final String FABRIC_VERSION_DEP = "fabric_version_dependency";

    public static final String QUILT_LOADER_VERSION = "quilt_loader_version";
    public static final String QUILT_LOOM_VERSION = "quilt_loom_version";
    public static final String QUILT_VERSION = "quilt_version";
    public static final String QUILT_VERSION_DEP = "quilt_version_dependency";

    public static final String ELECLOADER_VERSION = "elecloader_version";
    public static final String ELECLOADER_VERSION_DEP = "elecloader_version_dependency";

    public static final Set<String> ALL_PROPS;

    public static class Sub {

        public static final String MOD_VERSION_CLASSIFIER = "mod_version_classifier";
        public static final String MOD_BUILD_NUMBER = "mod_build_number";

    }

    static {
        Set<String> props = new HashSet<>();
        try {
            for (Field f : MLProperties.class.getDeclaredFields()) {
                if (f.getType() == String.class) {
                    props.add((String) f.get(null));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ALL_PROPS = Collections.unmodifiableSet(props);
    }

}
