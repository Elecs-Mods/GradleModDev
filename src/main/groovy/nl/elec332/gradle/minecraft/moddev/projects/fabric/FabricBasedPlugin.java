package nl.elec332.gradle.minecraft.moddev.projects.fabric;

import nl.elec332.gradle.minecraft.moddev.ProjectType;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPlugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven;
import org.gradle.jvm.tasks.Jar;

/**
 * Created by Elec332 on 06-09-2023
 */
public abstract class FabricBasedPlugin<E extends FabricBasedExtension> extends AbstractPlugin<E> {

    public FabricBasedPlugin(ProjectType projectType) {
        super(projectType);
    }

    public static final String REMAP_JAR_TASK = "remapJar";

    @Override
    protected void beforeProject(Project project) {
        project.getTasks().withType(AbstractPublishToMaven.class, m -> m.dependsOn(REMAP_JAR_TASK));
    }

    @Override
    public void afterRuntimePluginsAdded(Project project) {
        FabricBasedGroovyHelper.setRunDirs(project, getExtension(project));
        FabricBasedGroovyHelper.setRefMapName(project);
        project.getTasks().named(REMAP_JAR_TASK, Jar.class, j -> {
            Jar jt = (Jar) project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME);
            j.getArchiveBaseName().convention(jt.getArchiveBaseName());
            j.getArchiveAppendix().convention(jt.getArchiveAppendix());
            j.getArchiveVersion().convention(jt.getArchiveVersion());
            j.getArchiveExtension().convention(jt.getArchiveExtension());
            j.getArchiveClassifier().convention(jt.getArchiveClassifier());
        });
    }

    @Override
    protected void addMixinDependencies(Project project) {
    }

    protected static String getApiVersion(String s) {
        return s.split("[+]|-")[0];
    }

}
