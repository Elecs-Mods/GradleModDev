# Gradle Moddev

This plugin is a _settings plugin_, meaning it has to be applied in `settings.gradle`


## Main Settings
The main configuration for the settings is done in the `moddev` block, where modloader sub-projects can be enabled as shown below:
```
moddev {
    enableForge()
    enableNeoForge()
    enableFabric()
    enableQuilt()
}
```

If you wish to enable all supported modloaders, you can also simply call `enableAll()` instead of enabling them all manually

Some other setting can be defined here aswell (default values are shown):
```
moddev {
    ideaExtVersion = "1.1.7"
    quietJavaDoc = true
    rootIsCommon = false
    generateModInfo = true
    javaVersion = 17
}
```

-`ideaExtVersion` defines the version of the `idea-ext` plugin that is required for NeoGradle to function correctly in a multi-project setup\
-`quietJavaDoc` The linter for javadoc in newer java versions is very angry and verbose, this setting supresses the loggin by adding `"Xdoclint:none", "-quiet"` to the javadoc options\
-`rootIsCommon` If you use this plugin for a multi-loader project (more than 2 loaders enabled), this plugin will also add a common project with just vanilla as it's dependency. By default this project is located in the "common" subproject, with the root project being nothing but a shell for it's subprojects. Setting this to true will make the root project the common project.\
-`generateModInfo` This plugin has the ability to generate mod descriptors for all loader types. If you with to manually make those you can disable this.\
-`javaVersion` The java version used for _all_ subprojects

## Project Settings
Each modloader project (and the common project) can configure mod settings in the `modloader` block. Settings defined in the common project will be inherited by all modloader-based subprojects.
```
modloader {
    metadata {
        issues "https://github.com/your/project"
        sources "https://github.com/your/project"
        mod {
            homepage "homepage"
            logo("assets/yourmod/icon.png")
        }
        dependsOn("minecraft") {
            versionRange(">=1.20.4", "<1.21")
        }
    }

    mixin {
        mixinPackage = "your.common.mixin.root.package"
        clientMixins = ["YourCommonClientMixin"]
    }
}
```