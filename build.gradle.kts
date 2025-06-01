// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    // Define versions in a single place
    extra.apply {
        // https://developer.android.com/jetpack/androidx/releases/wear-compose
        set("wear_compose_version", "1.4.1")
        // https://developer.android.com/jetpack/androidx/releases/compose
        set("compose_version", "1.8.2")
        set("compose_compiler_version", "1.5.15")
        // https://developer.android.com/jetpack/androidx/releases/activity
        set("compose_activity_version", "1.10.1")
        // https://developer.android.com/jetpack/androidx/releases/compose-material3
        set("compose_material_version", "1.3.2")
        // https://developer.android.com/jetpack/androidx/releases/wear-tiles
        set("wear_tiles_version", "1.4.1")
    }
    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("https://jitpack.io")
        }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.10.1")
        classpath(kotlin("gradle-plugin", version = "2.0.0"))

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}