package nl.elec332.gradle.minecraft.moddev.projects.fabric;

import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPlugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven;
import org.gradle.jvm.tasks.Jar;

/**
 * Created by Elec332 on 06-09-2023
 */
public abstract class FabricBasedPlugin<E extends FabricBasedExtension> extends AbstractPlugin<E> {

    public FabricBasedPlugin(ModLoader modLoader) {
        super(modLoader);
    }

    public static final String REMAP_JAR_TASK = "remapJar";

    @Override
    protected void afterProject(Project project) {
        FabricBasedGroovyHelper.setRunDirs(project, getExtension(project));
        FabricBasedGroovyHelper.setRefMapName(project);
        var td = project.getTasks().named(REMAP_JAR_TASK, Jar.class, j -> {
            Jar jt = (Jar) project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME);
            j.getArchiveBaseName().set(jt.getArchiveBaseName().getOrNull());
            j.getArchiveAppendix().set(jt.getArchiveAppendix().getOrNull());
            j.getArchiveVersion().set(jt.getArchiveVersion().getOrNull());
            j.getArchiveExtension().set(jt.getArchiveExtension().getOrNull());
            j.getArchiveClassifier().set(jt.getArchiveClassifier().getOrNull());
        });
        project.getTasks().withType(AbstractPublishToMaven.class, m -> m.dependsOn(td));
    }

    @Override
    protected void addMixinDependencies(Project project) {
    }

    protected static String getApiVersion(String s) {
        return s.split("[+]|-")[0];
    }

}
