package nl.elec332.gradle.minecraft.moddev.projects;

import nl.elec332.gradle.minecraft.moddev.SettingsPlugin;
import nl.elec332.gradle.minecraft.moddev.util.ProjectHelper;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Elec332 on 05-09-2023
 */
public class CommonMLExtension extends CommonExtension {

    public SourceSet runtimeSource = ProjectHelper.getSourceSets(getProject()).getByName(SourceSet.MAIN_SOURCE_SET_NAME);

    public SourceSet mainModSource = ProjectHelper.getSourceSets(getProject()).getByName(SourceSet.MAIN_SOURCE_SET_NAME);

    public boolean addCommonDependency = !SettingsPlugin.isSuperCommonMode(getProject());

    public boolean addCommonMixins = true;

    public boolean addCommonSourceToAll = false;

    private final Set<Project> metaImports = new HashSet<>();

    public void importMetadata(Project project) {
        this.metaImports.add(project);
    }

    public Set<Project> getMetaImports() {
        return Collections.unmodifiableSet(metaImports);
    }

}
