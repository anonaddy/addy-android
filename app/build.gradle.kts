plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 32
    //compileSdkPreview = "Tiramisu"
    defaultConfig {
        applicationId = "host.stjin.anonaddy"
        minSdk = 23
        targetSdk = 32
        //targetSdkPreview = "Tiramisu"
        versionCode = 40
        // The "v" is important, as the updater class compares with the RSS feed on gitlab
        versionName = "v4.0.0"
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
    implementation(project(mapOf("path" to ":anonaddy_shared")))
    wearApp(project(":app-wearos"))
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.20")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.navigation:navigation-fragment-ktx:2.4.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.4.2")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.4.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.4.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

}

//https://developer.android.com/studio/write/java8-support#library-desugaring
// For using java.time pre-oreo
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")
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
    implementation("com.github.yuriy-budiyev:code-scanner:v2.1.0")
}

// For updating widgets and caching data
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.7.1")
}

// For the donut in the aliasview
dependencies {
    implementation("app.futured.donut:donut:2.2.2")
}

// Loading spinners when execution actions from eg. bottomsheets
dependencies {
    implementation("com.github.Stjin:LoadingButtonAndroid:2.2.0")
}

// Backup manager
dependencies {
    implementation("org.ocpsoft.prettytime:prettytime:5.0.2.Final")
}

// Communication with Wear OS device
dependencies {
    implementation("com.google.android.gms:play-services-wearable:17.1.0")
}

// Backgroundworker
dependencies {
    implementation("com.google.code.gson:gson:2.9.0")
}

// Built-in updater
dependencies {
    implementation("com.github.einmalfel:Earl:1.2.0")
}
