// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    // Define versions in a single place
    extra.apply {
        // https://developer.android.com/jetpack/androidx/releases/wear-compose
        set("wear_compose_version", "1.5.6")
        // https://developer.android.com/jetpack/androidx/releases/compose
        set("compose_version", "1.10.4")
        set("compose_compiler_version", "1.5.15")
        // https://developer.android.com/jetpack/androidx/releases/activity
        set("compose_activity_version", "1.12.4")
        // https://developer.android.com/jetpack/androidx/releases/compose-material3
        set("compose_material_version", "1.4.0")
        // https://developer.android.com/jetpack/androidx/releases/wear-tiles
        set("wear_tiles_version", "1.5.0")
    }
    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("https://jitpack.io")
        }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.1.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10")

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