package nl.elec332.gradle.minecraft.moddev.projects.fabric;

import nl.elec332.gradle.minecraft.moddev.ProjectType;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPlugin;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.io.File;
import java.util.List;

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
        FabricBasedGroovyHelper.setRefMapName(project);
        project.getTasks().named(REMAP_JAR_TASK, Jar.class, j -> {
            Jar jt = (Jar) project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME);
            ProjectHelper.copyNameTo(jt, j);
        });
        File rootDir = project.getLayout().getBuildDirectory().dir("generated/refmaps").get().getAsFile();
        project.afterEvaluate(p -> ProjectHelper.getSourceSets(p).forEach(ss -> {
            File ssRefMap = new File(rootDir,  ss.getName() + "/" + FabricBasedGroovyHelper.getRefMapName(p));
            TaskProvider<JavaCompile> compileTask = p.getTasks().named(ss.getCompileJavaTaskName(), JavaCompile.class, c -> {
                List<String> args = c.getOptions().getCompilerArgs();
                args.removeIf(s -> s.contains("-AoutRefMapFile="));
                args.add("-AoutRefMapFile=" + ssRefMap.getPath());
            });
            p.getTasks().named(ss.getProcessResourcesTaskName(), ProcessResources.class, r -> {
                r.from(ssRefMap);
                r.dependsOn(compileTask);
            });
        }));
    }

    @Override
    protected void afterProject(Project project) {
        FabricBasedGroovyHelper.setRunSettings(project, getExtension(project));
    }

    @Override
    protected void addMixinDependencies(Project project) {
    }

    @Override
    protected TaskProvider<? extends AbstractArchiveTask> setupRemapTask(Project project, Task task, TaskProvider<Jar> jarTask) {
        return project.getTasks().named(FabricBasedPlugin.REMAP_JAR_TASK, Jar.class);
    }

    protected static String getApiVersion(String s) {
        return s.split("[+]|-")[0];
    }

}
