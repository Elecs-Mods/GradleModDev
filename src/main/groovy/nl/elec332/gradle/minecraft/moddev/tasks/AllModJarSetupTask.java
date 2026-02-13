package nl.elec332.gradle.minecraft.moddev.tasks;

import nl.elec332.gradle.minecraft.moddev.ModLoader;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Elec332 on 22-03-2024
 */
public abstract class AllModJarSetupTask extends DefaultTask {

    public static TaskProvider<AllJarTask> createTaskChain(Project target, String name) {
        String setupName = getSetupTaskName(name);
        var allMappings = target.getTasks().register(getMappingsTaskName(name), GenerateMappingsTask.class, t -> t.dependsOn(setupName));
        var allTask = target.getTasks().register(name, AllJarTask.class, j -> {
            j.dependsOn(setupName, allMappings);
            j.from(allMappings, c -> c.into("mappings"));
        });
        target.getTasks().register(setupName, AllModJarSetupTask.class, allTask, allMappings);
        return allTask;
    }

    public static String getMappingsTaskName(String name) {
        return name + "Mappings";
    }

    public static String getSetupTaskName(String name) {
        return name + "Setup";
    }

    @Inject
    public AllModJarSetupTask(TaskProvider<AllJarTask> combiner, TaskProvider<GenerateMappingsTask> mapper) {
        this.combiner = combiner;
        this.mapper = mapper;
        this.remappedJars = new HashSet<>();

        getMapping().convention(ModLoader.Mapping.NAMED.name());
    }

    private final TaskProvider<AllJarTask> combiner;
    private final TaskProvider<GenerateMappingsTask> mapper;
    private final Set<RemapJarTask> remappedJars;

    public void addJar(RemapJarTask jar) {
        dependsOn(jar);
        this.remappedJars.add(jar);
    }

    @Input
    public abstract Property<String> getMapping();

    @TaskAction
    public void setup() {
        AllJarTask c = combiner.get();
        c.getMapping().set(getMapping());
        GenerateMappingsTask m = mapper.get();
        remappedJars.forEach(j -> {
            c.dependsOn(j);
            m.dependsOn(j);
            if (c.addJar(j)) {
                m.getCommonJar().from(j);
            } else {
                m.getModdedJars().from(j);
            }
        });
    }

}
