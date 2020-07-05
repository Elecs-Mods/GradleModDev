package nl.elec332.gradle.minecraft.moddev;

/**
 * Created by Elec332 on 1-7-2020
 */
public class ModDevExtension {

    public String modName = null;

    public String modId = null;

    public String basePackage = null;

    public boolean fgTweaks = false;

    public boolean addConfigFile = true;

    public boolean localBuildIdentifier = true;

    public boolean createDeobf = false;

    public boolean createModToml = true;

    public boolean createPackMeta = true;

    public boolean jenkinsBuildNumber = true;

    public boolean addModMaven = true;

    public boolean addWailaMaven = false;

    public String modVersion = null;

    public String modClassifier = null;

    public String minecraftVersion = null;

    public String forgeVersion = null;

    public String snapshotMappings = null;

}
