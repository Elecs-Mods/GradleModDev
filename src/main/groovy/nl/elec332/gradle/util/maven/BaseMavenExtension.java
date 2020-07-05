package nl.elec332.gradle.util.maven;

import groovy.lang.Closure;
import org.gradle.api.Project;

/**
 * Created by Elec332 on 2-7-2020
 */
public class BaseMavenExtension {

    public BaseMavenExtension(Project project) {
        this.project = project;
    }

    private final Project project;

    public String name;

    public String artifactId;

    public String packaging = "jar";

    public String description;

    public String inceptionYear;

    public String url;

    public String githubUrl;

    public boolean useGithubIssues = true;

    public String scmUrl;

    public String scmConnection;

    public String scmDevConnection;

    public void license(Closure<?> closure) {
        MavenHelper.configureLicense(project, closure);
    }

    public void developer(Closure<?> closure) {
        MavenHelper.configureDeveloper(project, closure);
    }

    public void contributor(Closure<?> closure) {
        MavenHelper.configureContributor(project, closure);
    }

    public void licenses(Closure<?> closure) {
        MavenHelper.configure(project, closure, "licenses");
    }

    public void developers(Closure<?> closure) {
        MavenHelper.configure(project, closure, "developers");
    }

    public void contributors(Closure<?> closure) {
        MavenHelper.configure(project, closure, "contributors");
    }

    public void issueManagement(Closure<?> closure) {
        MavenHelper.configure(project, closure, "issueManagement");
    }

}
