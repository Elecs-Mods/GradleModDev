package nl.elec332.gradle.minecraft.moddev.tasks;

import groovy.lang.Closure;
import nl.elec332.gradle.minecraft.moddev.ModDevExtension;
import nl.elec332.gradle.minecraft.moddev.ModDevPlugin;
import nl.elec332.gradle.minecraft.moddev.util.TomlExtensions;
import nl.elec332.gradle.util.Utils;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.tasks.TaskAction;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * Created by Elec332 on 2-7-2020
 */
public class GenerateTomlTask extends AbstractGenerateTask {

    public static final String DESCRIPTION = "Generates TOML files for Forge";

    @Inject
    public GenerateTomlTask(final ModDevExtension settings) {
        super("META-INF/mods.toml");
        mods = getProject().container(Mod.class, Mod::new);
        getProject().afterEvaluate(p -> {
            GenerateTomlTask.Mod mod = mods.findByName(settings.modId);
            if (mod == null) {
                mod = mods.create(settings.modId);
            }
            if (!Utils.isNullOrEmpty(settings.modName)) {
                mod.displayName = settings.modName;
            }
            if (Utils.isNullOrEmpty(modDescription) && !DESCRIPTION.equals(getDescription())) {
                this.modDescription = getDescription();
            }
            if (Utils.isNullOrEmpty(mod.description) && !Utils.isNullOrEmpty(modDescription)) {
                mod.description = modDescription;
            }
            boolean mc = false, forge = false;
            for (GenerateTomlTask.Dependency dep : mod.dependencies) {
                if (dep.modId.equals("minecraft")) {
                    mc = true;
                }
                if (dep.modId.equals("forge")) {
                    forge = true;
                }
            }
            if (!mc) {
                mod.dependencies.create("minecraft", dep -> dep.versionRange = "[${" + TomlExtensions.MC_VERSION + "},)");
            }
            if (!forge) {
                mod.dependencies.create("forge", dep -> dep.versionRange = "[${" + TomlExtensions.FORGE_VERSION + "},)");
            }
            this.hasEvaluated = true;
        });
    }

    private boolean hasEvaluated = false;

    public boolean hasEvaluated() {
        return hasEvaluated;
    }

    public final NamedDomainObjectContainer<Mod> mods;

    public void mods(Closure<?> closure) {
        mods.configure(closure);
    }

    public void description(String desc) {
        this.modDescription = desc;
    }

    public String modDescription = null;

    public String githubUrl = null;

    public String issueTrackerURL = null;

    public String displayURL = null;

    public String logoFile = null;

    public String credits = null;

    public String authors = null;

    @TaskAction
    public void generateToml() {
        ModDevExtension extension = ModDevPlugin.getExtension(getProject());
        if (!extension.createModToml) {
            System.out.println(file + " generation disabled, skipping...");
            return;
        }
        writeFile(writer -> {
            boolean hasGithub = !Utils.isNullOrEmpty(githubUrl);
            if (hasGithub && githubUrl.endsWith("/")) {
                githubUrl = githubUrl.substring(0, githubUrl.length() - 1);
            }
            if (Utils.isNullOrEmpty(issueTrackerURL) && hasGithub) {
                issueTrackerURL = githubUrl + "/issues";
            }
            if (Utils.isNullOrEmpty(displayURL) && hasGithub) {
                displayURL = githubUrl;
            }

            StringBuilder preFile = new StringBuilder();
            preFile.append("modLoader=\"javafml\"\n");
            preFile.append("loaderVersion=\"[${").append(TomlExtensions.LOADER_VERSION).append("},)\"\n");
            if (!Utils.isNullOrEmpty(issueTrackerURL)) {
                preFile.append("issueTrackerURL=\"").append(issueTrackerURL).append("\"\n");
            }
            if (!Utils.isNullOrEmpty(displayURL)) {
                preFile.append("displayURL=\"").append(displayURL).append("\"\n");
            }
            if (!Utils.isNullOrEmpty(logoFile)) {
                preFile.append("logoFile=\"").append(logoFile).append("\"\n");
            }
            if (!Utils.isNullOrEmpty(credits)) {
                preFile.append("credits=\"").append(credits).append("\"\n");
            }
            if (!Utils.isNullOrEmpty(authors)) {
                preFile.append("authors=\"").append(authors).append("\"\n");
            }

            for (Mod mod : mods) {
                mod.appendFile(preFile);
            }

            String contents = preFile.toString();
            writer.accept(contents);
        });
    }

    public class Mod implements Named {

        Mod(String s) {
            this.modId = s;
            this.dependencies = getProject().container(Dependency.class, Dependency::new);
        }

        private final String modId;

        private final NamedDomainObjectContainer<Dependency> dependencies;

        public void dependencies(Closure<?> closure) {
            dependencies.configure(closure);
        }

        public String version = null;

        public String displayName = null;

        public String description = null;

        void appendFile(StringBuilder file) {
            file.append("\n");
            file.append("[[mods]]\n");
            file.append("modId=\"").append(modId).append("\"\n");
            file.append("version=\"").append(version == null ? "${" + TomlExtensions.MOD_VERSION + "}" : version).append("\"\n");
            file.append("displayName=\"").append(displayName).append("\"\n");
            if (Utils.isNullOrEmpty(description)) {
                throw new IllegalArgumentException("No description provided for mod: " + modId);
            }
            file.append("description=\"").append(description).append("\"\n");
            for (Dependency dep : dependencies) {
                dep.appendFile(file, modId);
            }
        }

        @Nonnull
        @Override
        public String getName() {
            return modId;
        }

    }

    public static class Dependency implements Named {

        Dependency(String s) {
            this.modId = s;
        }

        public final String modId;

        public boolean mandatory = true;

        public String versionRange = null;

        public String ordering = "NONE";

        public String side = "BOTH";

        void appendFile(StringBuilder file, String modId) {
            if (Utils.isNullOrEmpty(versionRange)) {
                throw new IllegalArgumentException("No version range entered for dependency: " + modId);
            }
            file.append("\n");
            file.append("[[dependencies.").append(modId).append("]]\n");
            file.append("    modId=\"").append(this.modId).append("\"\n");
            file.append("    mandatory=").append(mandatory).append("\n");
            file.append("    versionRange=\"").append(versionRange).append("\"\n");
            file.append("    ordering=\"").append(ordering).append("\"\n");
            file.append("    side=\"").append(side).append("\"\n");
        }

        @Nonnull
        @Override
        public String getName() {
            return modId;
        }

    }

}
