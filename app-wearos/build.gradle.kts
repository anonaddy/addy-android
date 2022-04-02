plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 32

    defaultConfig {
        applicationId = "host.stjin.anonaddy"
        minSdk = 28
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"
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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }


    /**
     * COMPOSE
     */
    composeOptions {
        kotlinCompilerExtensionVersion = "1.2.0-alpha06"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    /**
     * END COMPOSE
     */

    /**
     * FLAVORS
     */
    flavorDimensions.add("flavor")
    productFlavors {
        create("main") {
        }
    }

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
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.compose.material3:material3:1.0.0-alpha08")

    implementation("androidx.core:core-ktx:1.7.0")
    implementation("com.google.android.gms:play-services-wearable:17.1.0")
    implementation("androidx.percentlayout:percentlayout:1.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.wear:wear:1.2.0")
    implementation(project(mapOf("path" to ":anonaddy_shared")))

    compileOnly("com.google.android.wearable:wearable:2.9.0")
    implementation("com.google.android.support:wearable:2.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}


// Compose
dependencies {
    // General compose dependencies
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.2.0-alpha06")

    // Animated graphics (for the icon on setup)
    implementation("androidx.compose.animation:animation-graphics:1.2.0-alpha06")

    implementation("androidx.wear.compose:compose-foundation:1.0.0-alpha19")

    // For Wear Material Design UX guidelines and specifications
    implementation("androidx.wear.compose:compose-material:1.0.0-alpha19")

    // NOTE: DO NOT INCLUDE a dependency on androidx.compose.material:material.
    // androidx.wear.compose:compose-material is designed as a replacement
    // not an addition to androidx.compose.material:material.
    // If there are features from that you feel are missing from
    // androidx.wear.compose:compose-material please raise a bug to let us know.

    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.2.0-alpha06")
    debugImplementation("androidx.compose.ui:ui-tooling:1.2.0-alpha06")
}

// For updating widgets and caching data
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.7.1")
}

// For parsing wearOSSettings
dependencies {
    implementation("com.google.code.gson:gson:2.9.0")
}

// For the donut in the aliasview
dependencies {
    implementation("app.futured.donut:donut-compose:2.2.1")
}

// Tiles
dependencies {
    // For watchface
    implementation("com.google.android.gms:play-services-base:18.0.1")

    // Use to implement support for wear tiles
    implementation("androidx.wear.tiles:tiles:1.1.0-alpha04")

    // Use to utilize components and layouts with Material design in your tiles
    implementation("androidx.wear.tiles:tiles-material:1.1.0-alpha04")

    // Use to preview wear tiles in your own app
    debugImplementation("androidx.wear.tiles:tiles-renderer:1.1.0-alpha04")

    // Use to fetch tiles from a tile provider in your tests
    testImplementation("androidx.wear.tiles:tiles-testing:1.1.0-alpha04")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.6.0")
    implementation("androidx.wear.tiles:tiles-proto:1.1.0-alpha04")

}