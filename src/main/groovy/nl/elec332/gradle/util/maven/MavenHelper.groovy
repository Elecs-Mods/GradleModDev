package nl.elec332.gradle.util.maven

import nl.elec332.gradle.util.GroovyHooks
import nl.elec332.gradle.util.Utils
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployer
import org.gradle.api.internal.artifacts.mvnsettings.LocalMavenRepositoryLocator

import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Created by Elec332 on 2-7-2020
 */
class MavenHelper {

    public static final String GITHUB_BASE_URL = "github.com"

    static List<String> getDevelopers(Project project) {
        List<String> ret = new ArrayList<>()
        GroovyHooks.configureMaven(project, { maven ->
            ret.addAll(maven.pom.mavenProject.getDevelopers().stream().map({d -> d.getName()}).collect(Collectors.toList()))
        })
        return ret
    }

    static String getLocalMavenUrl(LocalMavenRepositoryLocator mavenRepositoryLocator) {
        String ret = getLocalMavenProperty();
        if (!Utils.isNullOrEmpty(ret)) {
            ret = "file://" + ret;
        } else {
            ret = mavenRepositoryLocator.getLocalMavenRepository().getAbsolutePath();
        }
        if (!ret.endsWith(File.separator)) {
            ret += File.separator;
        }
        return ret;
    }

    static String getLocalMavenProperty() {
        return Stream.of(
                System.getProperty("local_maven"), System.getenv("local_maven"),
                System.getProperty("MAVEN_LOCAL"), System.getenv("MAVEN_LOCAL")
        ).filter({o -> o != null}).findFirst().orElse(null);
    }

    static void setMavenRepo(MavenDeployer deployer, String repo) {
        setMavenRepo(deployer, repo, false, null, null)
    }

    static void setMavenRepo(MavenDeployer deployer, String repo, String user, String password) {
        setMavenRepo(deployer, repo, true, user, password)
    }

    static void setMavenRepo(MavenDeployer deployer, String repo, boolean userPass, String user, String password) {
        deployer.repository(url: repo) {
            if (userPass) {
                authentication(userName: user, password: pass)
            }
        }
    }

    static void validateBaseExtension(Project project, BaseMavenExtension extension) {
        if (Utils.isNullOrEmpty(extension.name)) {
            extension.name = project.archivesBaseName
        }
        if (Utils.isNullOrEmpty(extension.artifactId)) {
            extension.artifactId = extension.name.toLowerCase(Locale.ROOT)
        }
        if (Utils.isNullOrEmpty(extension.packaging)) {
            throw new MissingPropertyException("Packaging type not defined!")
        }

        boolean hasGitHub = !Utils.isNullOrEmpty(extension.githubUrl)
        if (hasGitHub) {
            if (!extension.githubUrl.contains(GITHUB_BASE_URL)) {
                throw new IllegalArgumentException("Github URL does not contain " + GITHUB_BASE_URL)
            }
            String projectLink = extension.githubUrl.split(GITHUB_BASE_URL)[1]
            if (Utils.isNullOrEmpty(extension.url)) {
                extension.url = extension.githubUrl
            }
            if (Utils.isNullOrEmpty(extension.scmUrl)) {
                extension.scmUrl = extension.githubUrl
            }
            if (Utils.isNullOrEmpty(extension.scmConnection)) {
                extension.scmConnection = "scm:git:git://" + GITHUB_BASE_URL + projectLink + ".git"
            }
        }
        if (Utils.isNullOrEmpty(extension.url)) {
            throw new MissingPropertyException("No project URL defined!")
        }
        if (Utils.isNullOrEmpty(extension.scmUrl)) {
            throw new MissingPropertyException("No SCM property defined!")
        }
        if (Utils.isNullOrEmpty(extension.scmConnection)) {
            throw new MissingPropertyException("No SCM connection method defined!")
        }
        if (Utils.isNullOrEmpty(extension.scmDevConnection)) {
            if (extension.scmConnection.contains("git://")) {
                extension.scmDevConnection = extension.scmConnection.replace("git://", "ssh://git@")
            } else {
                throw new MissingPropertyException("No SCM developer connection method defined!")
            }
        }
    }

    static void configure(Project project, Closure closure, String name) {
        GroovyHooks.configureMaven(project, { maven ->
            maven.pom.project {
                "$name"(closure)
            }
        })
    }

    static void configureLicense(Project project, Closure closure) {
        configure(project, {
            license(closure)
        }, "licenses")
    }

    static void configureDeveloper(Project project, Closure closure) {
        configure(project, {
            developer(closure)
        }, "developers")
    }

    static void configureContributor(Project project, Closure closure) {
        configure(project, {
            contributor(closure)
        }, "contributors")
    }

    static <T extends BaseMavenExtension> void configureMaven(Project project, MavenDeployer deployer, T extension, Consumer<T> extraChecker, boolean noLicense) {
        validateBaseExtension(project, extension)
        if (extraChecker != null) {
            extraChecker.accept(extension)
        }

        deployer.pom.project {
            groupId = project.group
            version = project.version
            name extension.name
            artifactId extension.artifactId
            packaging extension.packaging
            url extension.url

            if (!Utils.isNullOrEmpty(extension.inceptionYear)) {
                inceptionYear extension.inceptionYear
            }

            if (!Utils.isNullOrEmpty(extension.description)) {
                description extension.description
            }

            scm {
                url extension.scmUrl
                connection extension.scmConnection
                developerConnection extension.scmDevConnection
            }

            if (!Utils.isNullOrEmpty(extension.githubUrl) && extension.useGithubIssues) {
                issueManagement {
                    system "Github"
                    url extension.githubUrl + "/issues"
                }
            }

        }

        List test = deployer.pom.getModel().getLicenses()
        if (!noLicense) {
            if (test.isEmpty()) {
                throw new MissingPropertyException("No license(s) defined!")
            }
        }
        test = deployer.pom.getModel().getDevelopers()
        if (test.isEmpty()) {
            throw new MissingPropertyException("No developer(s) defined!")
        }
    }

}
