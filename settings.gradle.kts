include(":app")
rootProject.name = "addy.io"
include(":app-wearos")
include(":anonaddy_shared")

pluginManagement {
    plugins {
        // [GitHub] https://github.com/google/ksp
        id("com.google.devtools.ksp") version "2.3.2"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}