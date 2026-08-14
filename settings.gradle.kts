include(":app")
rootProject.name = "addy.io"
include(":app-wearos")
include(":anonaddy_shared")

pluginManagement {
    plugins {
        // [GitHub] https://github.com/google/ksp
        id("com.google.devtools.ksp") version "2.4.10-1.0.0"
        id("org.jetbrains.kotlin.android") version "2.4.10"
        id("org.jetbrains.kotlin.jvm") version "2.4.10"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}