package nl.elec332.gradle.minecraft.modmaven;

import nl.elec332.gradle.util.maven.BaseMavenExtension;
import org.gradle.api.Project;

import javax.inject.Inject;

/**
 * Created by Elec332 on 2-7-2020
 */
public class ModMavenExtension extends BaseMavenExtension {

    public ModMavenExtension(Project project) {
        super(project);
    }

    @Inject
    public boolean forceLicense = true;

}
