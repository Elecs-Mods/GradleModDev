package nl.elec332.gradle.minecraft.moddev;

/**
 * Created by Elec332 on 02-09-2023
 */
public enum ModLoader {

    FORGE("forge", ModLoaderType.FORGE, Mapping.FORGE_SRG),
    NEO_FORGE("neo", ModLoaderType.FORGE, Mapping.NAMED),
    FABRIC("fabric", ModLoaderType.FABRIC, Mapping.FABRIC_INTERMEDIARY),
    QUILT("quilt", ModLoaderType.FABRIC, Mapping.FABRIC_INTERMEDIARY);

    ModLoader(String name, ModLoaderType type, Mapping mapping) {
        this.name = name;
        this.type = type;
        this.mapping = mapping;
    }

    private final String name;
    private final ModLoaderType type;
    private final Mapping mapping;

    public String getName() {
        return name;
    }

    public ModLoaderType getType() {
        return type;
    }

    public Mapping getMapping() {
        return mapping;
    }

    public enum Mapping {

        NAMED, FORGE_SRG, FABRIC_INTERMEDIARY

    }

}
