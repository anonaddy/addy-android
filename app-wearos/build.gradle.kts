val compose_version = rootProject.extra["compose_version"]
val compose_material_version = rootProject.extra["compose_material_version"]
val compose_compiler_version = rootProject.extra["compose_compiler_version"]
val wear_compose_version = rootProject.extra["wear_compose_version"]
val compose_activity_version = rootProject.extra["compose_activity_version"]
val wear_tiles_version = rootProject.extra["wear_tiles_version"]

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" // this version matches your Kotlin version

}

android {
    compileSdk = 35
    namespace = "host.stjin.anonaddy"

    defaultConfig {
        applicationId = namespace
        minSdk = 30
        targetSdk = 34
        /*
        Set the first two digits of the version code to the targetSdkVersion, such as 28.
        Set the next three digits to the product version, such as 152 for a product version of 1.5.2.
        Set the next two digits to build or release number, such as 01.
        Reserve the last two digits for a multi-APK variant, 00 for app, 01 for wearOS
         */

        // SDK 34 + v1.5.3 + release 01 + 01 (for wearos)
        versionCode = 341530101
        versionName = "1.5.3"
        vectorDrawables {
            useSupportLibrary = true
        }

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }


    /**
     * COMPOSE
     */
    composeOptions {
        kotlinCompilerExtensionVersion = "$compose_compiler_version"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    /**
     * END COMPOSE
     */


    buildTypes {
        getByName("release") {
            // Do not enable, Fuel will break
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }
    /**
     * END FLAVORS
     */
}

dependencies {
    implementation(project(mapOf("path" to ":anonaddy_shared")))
}

dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material3:material3:$compose_material_version")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.wear:wear:1.3.0")

    compileOnly("com.google.android.wearable:wearable:2.9.0")
    implementation("com.google.android.support:wearable:2.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}


// Compose
dependencies {
    // General compose dependencies
    implementation("androidx.activity:activity-compose:$compose_activity_version")
    implementation("androidx.compose.ui:ui-tooling-preview:$compose_version")

    // Animated graphics (for the icon on setup)
    implementation("androidx.compose.animation:animation-graphics:$compose_version")
    //implementation("androidx.compose.animation:animation-graphics:$compose_version")

    implementation("androidx.wear.compose:compose-foundation:$wear_compose_version")

    // For Wear Material Design UX guidelines and specifications
    implementation("androidx.wear.compose:compose-material:$wear_compose_version")

    // NOTE: DO NOT INCLUDE a dependency on androidx.compose.material:material.
    // androidx.wear.compose:compose-material is designed as a replacement
    // not an addition to androidx.compose.material:material.
    // If there are features from that you feel are missing from
    // androidx.wear.compose:compose-material please raise a bug to let us know.

    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose_version")
    debugImplementation("androidx.compose.ui:ui-tooling:$compose_version")
}

// For updating widgets and caching data
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.10.1")
}

// For parsing wearOSSettings
dependencies {
    implementation("com.google.code.gson:gson:2.13.1")
}

// For the donut in the aliasview
dependencies {
    implementation("app.futured.donut:donut-compose:2.2.4")
}

// Tiles
dependencies {
    // Use to implement support for wear tiles
    implementation("androidx.wear.tiles:tiles:$wear_tiles_version")

    // Use to utilize components and layouts with Material design in your tiles
    implementation("androidx.wear.tiles:tiles-material:$wear_tiles_version")

    // Use to preview wear tiles in your own app
    debugImplementation("androidx.wear.tiles:tiles-renderer:$wear_tiles_version")

    // Use to fetch tiles from a tile provider in your tests
    testImplementation("androidx.wear.tiles:tiles-testing:$wear_tiles_version")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.10.2")
    implementation("androidx.wear.tiles:tiles-proto:$wear_tiles_version")

}


// For smooth scrolling
// https://github.com/google/horologist
dependencies {
    implementation("com.google.android.horologist:horologist-compose-layout:0.6.23")
}

// Splash screen
dependencies {
    implementation("androidx.core:core-splashscreen:1.1.0-rc01")
}