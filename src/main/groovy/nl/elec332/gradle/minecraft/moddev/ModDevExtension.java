package nl.elec332.gradle.minecraft.moddev;

/**
 * Created by Elec332 on 1-7-2020
 */
public class ModDevExtension {

    // Required mod info

    public String modName = null;

    public String modId = null;

    public String basePackage = null;

    //Options

    public boolean fgTweaks = false;

    public boolean includeMinecraftVersion = true;

    public boolean localBuildIdentifier = true;

    public boolean jenkinsBuildNumber = true;

    public boolean createDeobf = false;

    public boolean createModToml = true;

    public boolean createPackMeta = true;

    public boolean addModMaven = true;

    public boolean addWailaMaven = false;

    //Config options

    public String modVersion = null;

    public String modClassifier = null;

    public String minecraftVersion = null;

    public String forgeVersion = null;

    public String snapshotMappings = null;

}
