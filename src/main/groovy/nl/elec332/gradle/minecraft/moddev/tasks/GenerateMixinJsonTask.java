package nl.elec332.gradle.minecraft.moddev.tasks;

import nl.elec332.gradle.minecraft.moddev.AllProjectsPlugin;
import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractGroovyHelper;
import nl.elec332.gradle.minecraft.moddev.projects.CommonExtension;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.*;

/**
 * Created by Elec332 on 12-03-2024
 */
public abstract class GenerateMixinJsonTask extends DefaultTask {

    private int counter = 1;
    private String source = SourceSet.MAIN_SOURCE_SET_NAME;
    private Map<String, CommonExtension.Mixin> mixins = null;
    private Set<String> metaMixinFiles = null;

    public void source(SourceSet ss) {
        this.source = ss.getName();
    }

    @Input
    public String getSource() {
        return source;
    }

    @Input
    public Map<String, CommonExtension.Mixin> getMixins() {
        return mixins == null ? Collections.emptyMap() : Collections.unmodifiableMap(mixins);
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
        Directory rd = AllProjectsPlugin.generatedResourceFolder(getProject()).dir(source);
        for (CommonExtension.Mixin m : nm) {
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
            getOutputs().file(rd.file(file));
            counter++;
        }
        metaMixinFiles = Collections.unmodifiableSet(pubs);
    }

    @TaskAction
    private void run() {
        File rootDir = AllProjectsPlugin.generatedResourceFolder(getProject()).dir(source).getAsFile();
        if (rootDir.exists()) {
            for (File f : Objects.requireNonNull(rootDir.listFiles())) {
                if (f.isFile() && f.getName().endsWith(".mixins.json")) {
                    f.delete();
                }
            }
        }
        if (mixins == null) {
            return;
        }
        for (Map.Entry<String, CommonExtension.Mixin> e : mixins.entrySet()) {
            AbstractGroovyHelper.writeFile(new File(rootDir, e.getKey()), Objects.requireNonNull(e.getValue().toJson(null, getProject())));
        }
    }

}
