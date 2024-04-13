package nl.elec332.gradle.minecraft.moddev.tasks;

import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPluginSC;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.jvm.tasks.Jar;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Elec332 on 21-03-2024
 */
public abstract class AllJarTask extends Jar {

    public AllJarTask() {
        this.deps = new HashSet<>();
        getMapping().convention(ModLoader.Mapping.NAMED.name());
        setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
    }

    private Set<AbstractArchiveTask> deps;

    @Input
    public abstract Property<String> getMapping();

    @InputFiles
    abstract ConfigurableFileCollection getInputTracker();

    public boolean addJar(RemapJarTask jar) {
        if (!getMapping().isPresent()) {
            throw new UnsupportedOperationException();
        }
        boolean ret = false;
        if (deps != null) {
            if (jar.getMapping().get() == ModLoader.Mapping.valueOf(getMapping().get())) {
                getManifest().attributes(Collections.singletonMap(AbstractPluginSC.MAPPINGS, "MIXED-" + getMapping().get()));
                ret = true;
            } else {
                deps.add(jar);
                return ret;
            }
        }
        process(jar);
        if (deps != null) {
            deps.forEach(this::process);
            deps = null;
        }
        return ret;
    }

    private void process(AbstractArchiveTask jar) {
        Provider<RegularFile> file = jar.getArchiveFile();
        from(getProject().zipTree(file));
        getInputTracker().from(file);
        dependsOn(jar);
    }

}
