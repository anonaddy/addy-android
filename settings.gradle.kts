include(":app")
rootProject.name = "addy.io"
include(":app-wearos")
include(":anonaddy_shared")

pluginManagement {
    plugins {
        // [GitHub] https://github.com/google/ksp
        id("com.google.devtools.ksp") version "2.0.0-1.0.21"
    }
}