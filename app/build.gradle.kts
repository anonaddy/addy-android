plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 31
    defaultConfig {
        applicationId = "host.stjin.anonaddy"
        minSdk = 23
        targetSdk = 31
        versionCode = 30
        // The "v" is important, as the updater class compares with the RSS feed on gitlab
        versionName = "v3.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        viewBinding = true
    }

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


    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.31")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("com.google.android.material:material:1.5.0-beta01")
    implementation("androidx.constraintlayout:constraintlayout:2.1.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

    // Preferences for storing settings (and crypto settings)
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha03")

}

//https://developer.android.com/studio/write/java8-support#library-desugaring
// For using java.time pre-oreo
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")
}

// Fuel, network requests
dependencies {
    implementation("com.github.kittinunf.fuel:fuel:2.2.3")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.2.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
}

// Shimmer
dependencies {
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.github.omtodkar:ShimmerRecyclerView:v0.4.1")
}

// Securing app
dependencies {
    implementation("androidx.biometric:biometric:1.1.0")
}

// Animations
dependencies {
    implementation("com.airbnb.android:lottie:3.4.1")
}

// Apache for extracting strings ManageAliasActivity
dependencies {
    // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
    implementation("org.apache.commons:commons-lang3:3.12.0")
}

// Scanning QR codes
dependencies {
    implementation("com.github.yuriy-budiyev:code-scanner:v2.1.0")
}

// For updating widgets and caching data
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.7.1")
}

// For the donut in the aliasview
dependencies {
    implementation("app.futured.donut:donut:2.1.0")
}

// Loading spinners when execution actions from eg. bottomsheets
dependencies {
    implementation("com.github.leandroBorgesFerreira:LoadingButtonAndroid:2.2.0")
}

// Built-in updater
dependencies {
    implementation("com.github.einmalfel:Earl:1.2.0")
}

// Backup manager
dependencies {
    implementation("org.ocpsoft.prettytime:prettytime:5.0.2.Final")
}
