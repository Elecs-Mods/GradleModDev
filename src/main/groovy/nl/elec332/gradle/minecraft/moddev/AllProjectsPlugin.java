package nl.elec332.gradle.minecraft.moddev;

import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.CoreJavadocOptions;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Elec332 on 08-02-2024
 */
public class AllProjectsPlugin implements Plugin<Project> {

    private static final String UTF8 = "UTF-8";
    public static String GENERATED_RESOURCES = "src/generated/resources";

    @Override
    public void apply(@NotNull Project target) {
        Settings settings = ((ProjectInternal) target).getGradle().getSettings();
        SettingsPlugin.ModDevConfig cfg = settings.getExtensions().getByType(SettingsPlugin.ModDevConfig.class);

        target.getPluginManager().apply(JavaLibraryPlugin.class);
        looseConfigure(target, cfg);
        setProperties(target, false, null);

        target.getExtensions().configure(JavaPluginExtension.class, e -> {
            e.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(cfg.javaVersion));
            e.setSourceCompatibility(JavaVersion.toVersion(cfg.javaVersion));
            e.setTargetCompatibility(JavaVersion.toVersion(cfg.javaVersion));
            e.getSourceSets().whenObjectAdded(ss -> configureSourceSet(target, ss));
            e.getSourceSets().forEach(ss -> configureSourceSet(target, ss));
            e.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME, ss -> ss.getResources().srcDir(GENERATED_RESOURCES));
            e.withSourcesJar();
            e.withJavadocJar();
        });
    }

    public static void setProperties(Project target, boolean hasMod, ModLoader ml) {
        ExtraPropertiesExtension ext = target.getExtensions().getExtraProperties();
        ext.set("modProject", hasMod);
        ext.set("hasModLoader", hasMod && ml != null);
        if (hasMod && ml != null) {
            ext.set("modLoader", ml.name());
        }
    }

    public static void looseConfigure(Project target, SettingsPlugin.ModDevConfig cfg) {
        target.getPluginManager().apply(IdeaPlugin.class);
        target.getPluginManager().apply(EclipsePlugin.class);
        target.beforeEvaluate(p -> {
            p.getTasks().withType(JavaCompile.class).configureEach(c -> {
                c.getOptions().setEncoding(UTF8);
                c.getOptions().getRelease().set(cfg.javaVersion);
            });
            p.getTasks().withType(Javadoc.class).configureEach(javadoc -> {
                javadoc.getOptions().setEncoding(UTF8);
                ((StandardJavadocDocletOptions) javadoc.getOptions()).setCharSet(UTF8);
                if (cfg.quietJavaDoc) {
                    ((CoreJavadocOptions) javadoc.getOptions()).addStringOption("Xdoclint:none", "-quiet");
                }
            });
        });
    }

    public static Directory generatedResourceFolder(Project target) {
        return target.getLayout().getBuildDirectory().dir("generated/genResources").get();
    }

    private void configureSourceSet(Project project, SourceSet sourceSet) {
        sourceSet.getResources().srcDir(generatedResourceFolder(project).dir(sourceSet.getName()));
    }

}
