plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 34
    namespace = "host.stjin.anonaddy"
    //compileSdkPreview = "Tiramisu"
    defaultConfig {
        applicationId = namespace
        minSdk = 23
        targetSdk = 34
        /*
        Set the first two digits of the version code to the targetSdkVersion, such as 28.
        Set the next three digits to the product version, such as 152 for a product version of 1.5.2.
        Set the next two digits to build or release number, such as 01.
        Reserve the last two digits for a multi-APK variant, 00 for app, 01 for wearOS
         */

        // SDK 33 + v4.7.0 + release 01 + 00 (for app)
        versionCode = 344700100 // https://developer.android.com/training/wearables/packaging
        // The "v" is important, as the updater class compares with the RSS feed on gitlab
        versionName = "v4.7.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        viewBinding = true
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

    flavorDimensions.add("type")
    productFlavors {
        create("gplay") {
            dimension = "type"
        }
        create("gplayless") {
            dimension = "type"
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
    lint {
        disable += setOf("WearableBindListener")
    }

}

dependencies {
    modules {
        module("org.jetbrains.kotlin:kotlin-stdlib-jdk7") {
            replacedBy("org.jetbrains.kotlin:kotlin-stdlib", "kotlin-stdlib-jdk7 is now part of kotlin-stdlib")
        }
        module("org.jetbrains.kotlin:kotlin-stdlib-jdk8") {
            replacedBy("org.jetbrains.kotlin:kotlin-stdlib", "kotlin-stdlib-jdk8 is now part of kotlin-stdlib")
        }
    }
}


dependencies {
    implementation(project(mapOf("path" to ":anonaddy_shared")))
    wearApp(project(":app-wearos"))
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.21")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.6.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.6.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

}

//https://developer.android.com/studio/write/java8-support#library-desugaring
// For using java.time pre-oreo
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
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

// Apache for extracting strings ManageAliasActivity
dependencies {
    // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
    implementation("org.apache.commons:commons-lang3:3.12.0")
}

// Scanning QR codes
dependencies {
    implementation("com.github.yuriy-budiyev:code-scanner:2.3.2")
}

// For updating widgets and caching data
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.8.1")
}

// For the donut in the aliasview
dependencies {
    implementation("app.futured.donut:donut:2.2.3")
}

// Loading spinners when execution actions from eg. bottomsheets
dependencies {
    implementation("com.github.Stjin:LoadingButtonAndroid:2.2.0")
}

// Backup manager
dependencies {
    implementation("org.ocpsoft.prettytime:prettytime:5.0.7.Final")
}

// Communication with Wear OS device
// Only implement GPlay in the gplay version

// Because the app has a gplayless flavor define a gplayImplementation
val gplayImplementation by configurations
dependencies {
    gplayImplementation("com.google.android.gms:play-services-wearable:18.0.0")
}

// Backgroundworker
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

// Built-in updater
dependencies {
    implementation("com.github.einmalfel:Earl:1.2.0")
}
