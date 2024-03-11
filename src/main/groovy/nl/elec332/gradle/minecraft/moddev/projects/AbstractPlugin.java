package nl.elec332.gradle.minecraft.moddev.projects;

import nl.elec332.gradle.minecraft.moddev.MLProperties;
import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.ProjectHelper;
import nl.elec332.gradle.minecraft.moddev.SettingsPlugin;
import nl.elec332.gradle.minecraft.moddev.projects.common.CommonProjectPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.project.ProjectStateInternal;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.tasks.GenerateModuleMetadata;
import org.gradle.api.tasks.TaskInputs;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 02-09-2023
 */
public abstract class AbstractPlugin<E extends CommonExtension> implements Plugin<Project> {

    protected AbstractPlugin(ModLoader modLoader) {
        this.modLoader = modLoader;
        if (modLoader == null && getClass() != CommonProjectPlugin.class) {
            throw new UnsupportedOperationException();
        }
        if (modLoader != null && modLoader.getPluginClass() != getClass()) {
            throw new IllegalStateException();
        }
    }

    private final ModLoader modLoader;

    @Override
    public final void apply(@NotNull Project target) {
        Settings settings = ((ProjectInternal) target).getGradle().getSettings();
        SettingsPlugin.ModDevDetails cfg = SettingsPlugin.getDetails(target);

        Set<String> pluginProps = new HashSet<>();
        Set<String> projectProps = new HashSet<>(Set.of(MLProperties.MC_VERSION, MLProperties.MOD_VERSION, MLProperties.MOD_ID, MLProperties.MOD_GROUP_ID, MLProperties.MOD_NAME, MLProperties.MOD_AUTHORS, MLProperties.MOD_LICENSE, MLProperties.MOD_DESCRIPTION));
        addProperties(pluginProps::add, projectProps::add);
        ProjectHelper.checkProperties(target, pluginProps);
        E extension = target.getExtensions().create(extensionType(), "modloader", extensionType());
        preparePlugins(target, settings);
        boolean isCommon = getModLoader() == null;
        AbstractGroovyHelper.setProperties(target, true, getModLoader());
        target.beforeEvaluate(p -> {
            SettingsPlugin.addRepositories(p.getRepositories());
            ProjectHelper.checkProperties(p, projectProps);
            p.setVersion(ProjectHelper.getStringProperty(p, MLProperties.MOD_VERSION));
            p.setGroup(ProjectHelper.getStringProperty(p, MLProperties.MOD_GROUP_ID));
            p.getExtensions().configure(BasePluginExtension.class, e -> e.getArchivesName().set(ProjectHelper.getStringProperty(p, MLProperties.MOD_NAME)));
            p.getTasks().register(AbstractGroovyHelper.CHECK_CLASSES_TASK, CheckCompileTask.class);
            p.getTasks().register(AbstractGroovyHelper.GENERATE_METADATA).configure(gm -> {
                p.getTasks().withType(AbstractGroovyHelper.GenerateMixinJson.class).forEach(gm::dependsOn);
                p.getTasks().withType(AbstractGroovyHelper.GenerateModInfo.class).forEach(gm::dependsOn);
            });

            p.getTasks().withType(JavaCompile.class).configureEach(t -> t.dependsOn(AbstractGroovyHelper.GENERATE_METADATA));
            p.getTasks().withType(Jar.class).configureEach(t -> t.dependsOn(AbstractGroovyHelper.GENERATE_METADATA));
            p.getTasks().withType(ProcessResources.class).configureEach(t -> t.dependsOn(AbstractGroovyHelper.GENERATE_METADATA));

            if (!isCommon) {
                p.getTasks().create(AbstractGroovyHelper.GENERATE_MIXIN_TASK, AbstractGroovyHelper.GenerateMixinJson.class);
                p.getTasks().create(AbstractGroovyHelper.GENERATE_MODINFO_TASK, AbstractGroovyHelper.GenerateModInfo.class).onlyIf(t -> cfg.generateModInfo());
            } else {
                extension.metadata(md -> {
                    String id = ProjectHelper.getStringProperty(p, MLProperties.MOD_ID);
                    md.mod(id, mi -> {
                        mi.modVersion(ProjectHelper.getStringProperty(p, MLProperties.MOD_VERSION));
                        mi.modName(ProjectHelper.getStringProperty(p, MLProperties.MOD_NAME));
                        mi.setAuthors(List.of(ProjectHelper.getStringProperty(p, MLProperties.MOD_AUTHORS).split(",")));
                        mi.modDescription(ProjectHelper.getStringProperty(p, MLProperties.MOD_DESCRIPTION));
                    });
                    md.modGroupId(ProjectHelper.getStringProperty(p, MLProperties.MOD_GROUP_ID));
                    md.modLicense(ProjectHelper.getStringProperty(p, MLProperties.MOD_LICENSE));
                });
            }

            p.getTasks().withType(AbstractGroovyHelper.GenerateModInfo.class).configureEach(gm -> {
                p.getTasks().withType(AbstractGroovyHelper.GenerateMixinJson.class).forEach(gm::dependsOn);
            });

            p.getTasks().withType(Jar.class).configureEach(jar -> {
                jar.manifest(manifest -> manifest.attributes(Map.of(
                        "Specification-Title", ProjectHelper.getStringProperty(p, MLProperties.MOD_ID),
                        "Specification-Vendor", ProjectHelper.getStringProperty(p, MLProperties.MOD_AUTHORS),
                        "Specification-Version", '1',
                        "Implementation-Title", p.getName(),
                        "Implementation-Version", jar.getArchiveVersion(),
                        "Implementation-Vendor", ProjectHelper.getStringProperty(p, MLProperties.MOD_AUTHORS),
                        "Implementation-Timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date())
                ).entrySet().stream().filter(e -> !manifest.getAttributes().containsKey(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
            });

            p.getTasks().withType(ProcessResources.class).configureEach(pr -> {
                TaskInputs inputs = pr.getInputs();
                for (String s : MLProperties.ALL_PROPS) {
                    if (ProjectHelper.hasProperty(p, s)) {
                        inputs.property(s, ProjectHelper.getStringProperty(p, s));
                    }
                }
                inputs.property("version", p.getVersion());
                pr.filesMatching(List.of("/META-INF/mods.toml", "pack.mcmeta", "/fabric.mod.json", "/quilt.mod.json", "/*.txt"), f -> f.expand(inputs.getProperties(), d -> d.getEscapeBackslash().set(true)).filter(s -> s.replace("\\$", "$")));
            });
        });
        target.afterEvaluate(p -> {
            p.getTasks().named(JavaPlugin.CLASSES_TASK_NAME, it -> it.dependsOn(AbstractGroovyHelper.CHECK_CLASSES_TASK));

            if (extension.noModuleMetadata) {
                p.getTasks().withType(GenerateModuleMetadata.class).configureEach(meta -> meta.setEnabled(false));
            }

            p.getTasks().withType(AbstractGroovyHelper.GenerateMixinJson.class).forEach(j -> j.addMixins(extension.getMixins()));
            if (extension instanceof CommonMLExtension) {
                ModMetadata md;
                if (!cfg.singleProject()) {
                    Project common = cfg.getCommonProject();
                    CommonExtension commonExtension = Objects.requireNonNull(common.getExtensions().getByType(CommonExtension.class), "Failed to find extension in common project");
                    md = ModMetadataImpl.generate(p, commonExtension.getMetaModifiers());
                    extension.getMetaModifiers().forEach(a -> a.execute(md));
                    if (((CommonMLExtension) extension).addCommonDependency) {
                        if (cfg.isSuperCommonMode()) {
                            throw new UnsupportedOperationException("Cannot add CommonDependency to SuperCommon project");
                        }
                        AbstractGroovyHelper.importCommonProject(p, common, (CommonMLExtension) extension);
                    }
                    if (((CommonMLExtension) extension).addCommonMixins) {
                        p.getTasks().withType(AbstractGroovyHelper.GenerateMixinJson.class).forEach(j -> j.addMixins(commonExtension.getMixins()));
                    }
                } else {
                    md = ModMetadataImpl.generate(p, extension.getMetaModifiers());
                }
                p.getTasks().withType(AbstractGroovyHelper.GenerateMixinJson.class).forEach(j -> j.getMetaMixinFiles().forEach(md::mixin));
                if (md.getMixins() != null) {
                    addMixinDependencies(p);
                }
                checkModMetadata(p, md);
                p.getTasks().withType(AbstractGroovyHelper.GenerateModInfo.class).configureEach(pr -> pr.setMetadata(md));
                for (Project dp : ((CommonMLExtension) extension).getMetaImports()) {
                    if (dp == null || dp == p) {
                        continue;
                    }
                    if (((ProjectStateInternal) dp.getState()).isUnconfigured() || ((ProjectStateInternal) dp.getState()).isConfiguring()) {
                        dp.afterEvaluate(ddp -> importModMeta(ddp, p));
                    } else {
                        importModMeta(dp, p);
                    }
                }
            } else if (!extension.getMixins().isEmpty()) {
                addMixinDependencies(p);
            }
        });
        target.beforeEvaluate(this::beforeProject);
        target.afterEvaluate(this::afterProject);
    }

    private void importModMeta(Project from, Project to) {
        from.getTasks().withType(AbstractGroovyHelper.GenerateModInfo.class).configureEach(pr -> to.getTasks().withType(AbstractGroovyHelper.GenerateModInfo.class).configureEach(pr2 -> pr2.getMetadata().importFrom(pr.getMetadata())));
    }

    public final ModLoader getModLoader() {
        return modLoader;
    }

    protected final void addPlugin(Project project, String id, String versionProperty) {
        ProjectHelper.addPlugin(project, id, ProjectHelper.getStringProperty(project, versionProperty));
    }

    protected abstract void addMixinDependencies(Project project);

    protected abstract String getArchiveAppendix();

    protected abstract void preparePlugins(Project project, Settings settings);

    protected abstract void beforeProject(Project project);

    protected abstract void afterProject(Project project);

    protected abstract void addProperties(Consumer<String> pluginProps, Consumer<String> projectProps);

    protected abstract void checkModMetadata(Project project, ModMetadata metadata);

    protected final E getExtension(Project project) {
        return project.getExtensions().getByType(extensionType());
    }

    protected abstract Class<E> extensionType();

}
