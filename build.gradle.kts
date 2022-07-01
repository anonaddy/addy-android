// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    // Define versions in a single place
    extra.apply {
        // https://developer.android.com/jetpack/androidx/releases/wear-compose
        set("wear_compose_version", "1.0.0-beta03")
        // https://developer.android.com/jetpack/androidx/releases/compose
        set("compose_version", "1.2.0-beta03")
        // https://developer.android.com/jetpack/androidx/releases/activity
        set("compose_activity_version", "1.4.0")
        // https://developer.android.com/jetpack/androidx/releases/compose-material3
        set("compose_material_version", "1.0.0-alpha13")
        // https://developer.android.com/jetpack/androidx/releases/wear-tiles
        set("wear_tiles_version", "1.1.0-alpha08")
    }
    repositories {
        google()
        maven { setUrl("https://jitpack.io") }
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.2.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        maven { setUrl("https://jitpack.io") }
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}