package nl.elec332.gradle.minecraft.moddev;

import nl.elec332.gradle.minecraft.moddev.projects.AbstractPlugin;
import nl.elec332.gradle.minecraft.moddev.util.GradleInternalHelper;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
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
        SettingsPlugin.ModDevConfig cfg = GradleInternalHelper.getGradleSettings(target).getExtensions().getByType(SettingsPlugin.ModDevConfig.class);

        target.getPluginManager().apply(JavaLibraryPlugin.class);
        looseConfigure(target, cfg);
        setProperties(target, null);

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

    public static void setProperties(Project target, ProjectType type) {
        ExtraPropertiesExtension ext = target.getExtensions().getExtraProperties();
        ext.set("modProject", type != null);
        ext.set("hasModLoader", type != null && type.getModLoader() != null);
        if (type != null) {
            ext.set("modProjectType", type.name());
            if (type.getModLoader() != null) {
                ext.set("modLoader", type.getModLoader().name());
            }
        }
    }

    public static void looseConfigure(Project target, SettingsPlugin.ModDevConfig cfg) {
        target.getPluginManager().apply(IdeaPlugin.class);
        target.getPluginManager().apply(EclipsePlugin.class);
        target.beforeEvaluate(p -> {
            p.getTasks().withType(JavaCompile.class).matching(AbstractPlugin.notNeoTask).configureEach(c -> {
                c.getOptions().setEncoding(UTF8);
                c.getOptions().getRelease().set(cfg.javaVersion);
            });
            p.getTasks().withType(Javadoc.class).matching(AbstractPlugin.notNeoTask).configureEach(javadoc -> {
                javadoc.getOptions().setEncoding(UTF8);
                ((StandardJavadocDocletOptions) javadoc.getOptions()).setCharSet(UTF8);
                if (cfg.quietJavaDoc) {
                    ((CoreJavadocOptions) javadoc.getOptions()).addStringOption("Xdoclint:none", "-quiet");
                }
            });
        });
        checkSubProperties(target);
    }

    public static Directory generatedResourceFolder(Project target) {
        return target.getLayout().getBuildDirectory().dir("generated/genResources").get();
    }

    private void configureSourceSet(Project project, SourceSet sourceSet) {
        sourceSet.getResources().srcDir(generatedResourceFolder(project).dir(sourceSet.getName()));
    }

    private static void checkSubProperties(Project target) {
        ExtraPropertiesExtension properties = target.getExtensions().getExtraProperties();
        String versionClassifier = "";
        if (properties.has(MLProperties.Sub.MOD_VERSION_CLASSIFIER)) {
            String str = (String) properties.get(MLProperties.Sub.MOD_VERSION_CLASSIFIER);
            if (str == null || str.isEmpty()) {
                versionClassifier = "";
            } else {
                versionClassifier = "-" + str;
            }
        }
        properties.set(MLProperties.Sub.MOD_VERSION_CLASSIFIER, versionClassifier);

        String build = System.getenv("BUILD_NUMBER");
        build = (build == null || build.isEmpty()) ? "9999-custom" : build;
        properties.set(MLProperties.Sub.MOD_BUILD_NUMBER, build);

        if (!ProjectHelper.hasProperty(target, MLProperties.MOD_VERSION)) {
            return;
        }

        String version = ProjectHelper.getStringProperty(target, MLProperties.MOD_VERSION);
        if (SettingsPlugin.getDetails(target).useBuildNumber()) {
            version += "." + ProjectHelper.getStringProperty(target, MLProperties.Sub.MOD_BUILD_NUMBER);
        }
        version += ProjectHelper.getStringProperty(target, MLProperties.Sub.MOD_VERSION_CLASSIFIER);
        properties.set(MLProperties.MOD_VERSION, version);
    }

}
