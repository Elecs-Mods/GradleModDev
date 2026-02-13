package nl.elec332.gradle.minecraft.moddev.tasks;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.projects.CommonExtension;
import nl.elec332.gradle.minecraft.moddev.util.AbstractGroovyHelper;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

import java.util.*;

/**
 * Created by Elec332 on 12-03-2024
 */
public abstract class GenerateMixinJsonTask extends AbstractGenerateFilesTask {

    public GenerateMixinJsonTask() {
        super("generated/mixins");
        this.variants = new HashSet<>();
        this.variants.add(null);
    }

    private int counter = 1;
    private Map<String, CommonExtension.Mixin> mixins = null;
    private Set<String> metaMixinFiles = null;
    private final Set<String> variants;

    @Input
    public Map<String, CommonExtension.Mixin> getMixins() {
        return mixins == null ? Collections.emptyMap() : Collections.unmodifiableMap(mixins);
    }

    @Input
    public Set<String> getVariants() {
        return this.variants;
    }

    @Internal
    public Set<String> getMetaMixinFiles() {
        return metaMixinFiles == null ? Collections.emptySet() : metaMixinFiles;
    }

    public void addMixins(Set<CommonExtension.Mixin> nm) {
        if (nm == null || nm.isEmpty()) {
            return;
        }
        Set<String> pubs;
        if (mixins == null) {
            mixins = new TreeMap<>();
            pubs = new HashSet<>();
        } else {
            pubs = new HashSet<>(metaMixinFiles);
        }
        String fileNameBase = "." + ProjectHelper.getPlugin(getProject()).getProjectType().getName() + ".mixins.json";
        for (var m : nm) {
            String file;
            if (counter == 1) {
                file = ProjectHelper.getStringProperty(getProject(), MLProperties.MOD_ID) + fileNameBase;
            } else {
                file = ProjectHelper.getStringProperty(getProject(), MLProperties.MOD_ID) + counter + fileNameBase;
            }
            if (!m.generateOnly) {
                pubs.add(file);
            }
            mixins.put(file, m);
            counter++;
        }
        metaMixinFiles = Collections.unmodifiableSet(pubs);
    }

    @Override
    protected void generate() {
        if (mixins == null) {
            return;
        }
        for (var variant : this.variants) {
            for (var e : mixins.entrySet()) {
                AbstractGroovyHelper.writeFile(getOutputFile((variant == null ? "" : variant) + e.getKey()), Objects.requireNonNull(e.getValue().toJson(null, getProject(), variant)));
            }
        }
    }

}
