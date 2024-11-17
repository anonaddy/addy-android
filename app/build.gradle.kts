plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 35
    namespace = "host.stjin.anonaddy"
    //compileSdkPreview = "Tiramisu"
    defaultConfig {
        applicationId = namespace
        minSdk = 23
        targetSdk = 35
        /*
        Set the first two digits of the version code to the targetSdkVersion, such as 28.
        Set the next three digits to the product version, such as 152 for a product version of 1.5.2.
        Set the next two digits to build or release number, such as 01.
        Reserve the last two digits for a multi-APK variant, 00 for app, 01 for wearOS
         */

        // SDK 35 + v5.4.1 + release 01 + 00 (for app)
        versionCode = 355420100 // https://developer.android.com/training/wearables/packaging
        // The "v" is important, as the updater class compares with the RSS feed on Github
        versionName = "v5.4.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
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

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
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
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

}

//https://developer.android.com/studio/write/java8-support#library-desugaring
// For using java.time pre-oreo
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
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
    implementation("org.apache.commons:commons-lang3:3.17.0")
}

// Scanning QR codes
dependencies {
    implementation("com.github.yuriy-budiyev:code-scanner:2.3.2")
}

// For updating widgets and caching data
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.10.0")
}

// For the donut in the aliasview
dependencies {
    implementation("app.futured.donut:donut:2.2.3") // FIXME: https://github.com/futuredapp/donut/pull/96
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
    gplayImplementation("com.google.android.gms:play-services-wearable:18.2.0")
    gplayImplementation("com.android.billingclient:billing-ktx:7.1.1")
    gplayImplementation("com.google.android.play:review-ktx:2.0.2")
}

// Backgroundworker
dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
}

// Built-in updater
dependencies {
    implementation("com.github.einmalfel:Earl:1.2.0")
}

// Activity Embedding
dependencies {
    implementation("androidx.window:window:1.3.0")
    implementation("androidx.startup:startup-runtime:1.2.0")
}

// Edge-To-Edge
dependencies {
    implementation("androidx.activity:activity-ktx:1.9.3")
}