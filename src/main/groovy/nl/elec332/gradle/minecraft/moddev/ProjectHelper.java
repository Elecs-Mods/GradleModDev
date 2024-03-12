package nl.elec332.gradle.minecraft.moddev;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPlugin;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.resources.MissingResourceException;
import org.gradle.api.resources.ResourceException;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.groovy.scripts.ScriptSource;
import org.gradle.groovy.scripts.TextResourceScriptSource;
import org.gradle.internal.DisplayName;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.hash.Hashing;
import org.gradle.internal.hash.PrimitiveHasher;
import org.gradle.internal.resource.ResourceLocation;
import org.gradle.internal.resource.TextResource;
import org.gradle.util.internal.ConfigureUtil;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 03-09-2023
 */
public class ProjectHelper {

    public static SourceSetContainer getSourceSets(Project root) {
        return Objects.requireNonNull(root.getExtensions().getByType(JavaPluginExtension.class).getSourceSets());
    }

    public static void afterEvaluateSafe(Project target, Action<Project> runnable) {
        if (hasEvaluated(target)) {
            runnable.execute(target);
        } else {
            target.afterEvaluate(runnable);
        }
    }

    public static boolean hasEvaluated(Project target) {
        return target.getState().getExecuted();
//        return ((ProjectStateInternal) target.getState()).isUnconfigured() || ((ProjectStateInternal) target.getState()).isConfiguring();
    }

    public static void checkProperties(Project project, Set<String> props) {
        Set<String> fail = new HashSet<>();
        for (String s : props) {
            if (!ProjectHelper.hasProperty(project, s)) {
                fail.add(s);
                continue;
            }
            Object o = ProjectHelper.getProperty(project, s);
            if (o == null || (o instanceof String && ((String) o).isEmpty())) {
                fail.add(s);
            }
        }
        if (!fail.isEmpty()) {
            throw new RuntimeException("Missing the following properties: " + fail);
        }
    }

    public static String getMixinRefMap(Project project) {
        String fileNameBase = ProjectHelper.getPlugin(project).getProjectType().getName() + ".refmap.json";
        return ProjectHelper.getProperty(project, MLProperties.MOD_ID) + fileNameBase;
    }

    public static AbstractPlugin<?> getPlugin(Project project) {
        return project.getPlugins().withType(AbstractPlugin.class).iterator().next();
    }

    public static boolean hasProperty(Project project, String name) {
        return project.hasProperty(name);
//        return project.getExtensions().getExtraProperties().hasProperty(name);
    }

    public static String getStringProperty(Project project, String name) {
        return (String) getProperty(project, name);
    }

    public static Object getProperty(Project project, String name) {
//        return project.getExtensions().getExtraProperties().get(name);
        return project.property(name);
    }

    public static void setProperty(Project project, String name, Object value) {
        project.getExtensions().getExtraProperties().set(name, value);
    }

    public static void applyToProject(Project project, @DelegatesTo(Project.class) Closure<?> closure) {
        ConfigureUtil.configureSelf(closure, project);
    }

    public static void addPlugin(Project project, String id, String version) {
        if (!(project instanceof ProjectInternal)) {
            throw new UnsupportedOperationException();
        }
        ScriptSource ss = ((ProjectInternal) project).getBuildScriptSource();
        if (ss.getResource() instanceof WithPluginsTextResource) {
            ((WithPluginsTextResource) ss.getResource()).addPlugin(id, version);
            return;
        }
        if (!(ss instanceof TextResourceScriptSource)) {
            throw new UnsupportedOperationException();
        }
        try {
            Field f = TextResourceScriptSource.class.getDeclaredField("resource");
            f.setAccessible(true);
            TextResource text = (TextResource) f.get(ss);
            f.set(ss, new WithPluginsTextResource(text));
            addPlugin(project, id, version);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class WithPluginsTextResource implements TextResource {

        public WithPluginsTextResource(TextResource resource) {
            this.resource = resource;
            this.plugins = new LinkedHashSet<>();
        }

        private final TextResource resource;
        private String content;
        private HashCode contentHash;

        private final Set<String> plugins;

        public void addPlugin(String id, String version) {
            String root = "id '" + id + "' ";
            if (version != null && !version.isEmpty()) {
                root += "version '" + version + "' ";
            }
            plugins.add(root);
        }

        private String modify(String original) {
            String allPlugins = plugins.stream().map(s -> "plugins{" + s + "}").collect(Collectors.joining("\n"));
            //Temporary fix to allow for buildscript{} blocks
            if (original.contains("plugins {")) {
                return original.replaceFirst("plugins \\{", allPlugins + "\n plugins {");
            }
            if (original.contains("plugins{")) {
                return original.replaceFirst("plugins\\{", allPlugins + "\n plugins {");
            }
            return allPlugins + "\n" + original;
        }

        public String getDisplayName() {
            return this.resource.getDisplayName();
        }

        public DisplayName getLongDisplayName() {
            return this.resource.getLongDisplayName();
        }

        public DisplayName getShortDisplayName() {
            return this.resource.getShortDisplayName();
        }

        public ResourceLocation getLocation() {
            return this.resource.getLocation();
        }

        public File getFile() {
            return this.resource.getFile();
        }

        public Charset getCharset() {
            return this.resource.getCharset();
        }

        public boolean isContentCached() {
            return true;
        }

        public boolean getExists() {
            try {
                this.maybeFetch();
                return true;
            } catch (MissingResourceException var2) {
                return false;
            }
        }

        public boolean getHasEmptyContent() {
            this.maybeFetch();
            return this.content.isEmpty();
        }

        public String getText() {
            this.maybeFetch();
            return this.content;
        }

        public HashCode getContentHash() throws ResourceException {
            this.maybeFetch();
            return this.contentHash;
        }

        public Reader getAsReader() {
            this.maybeFetch();
            return new StringReader(this.content);
        }

        private void maybeFetch() {
            if (this.content == null) {
                this.content = this.modify(this.resource.getText());
                PrimitiveHasher hasher = Hashing.newPrimitiveHasher();
                hasher.putHash(Hashing.signature(this.getClass()));
                hasher.putString(this.content);
                this.contentHash = hasher.hash();
            }
        }

    }

}
