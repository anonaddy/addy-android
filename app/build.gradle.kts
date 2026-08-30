import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("com.android.application")
}

configure<ApplicationExtension> {
    compileSdk = 37
    namespace = "host.stjin.anonaddy"
    //compileSdkPreview = "Tiramisu"
    defaultConfig {
        applicationId = namespace
        minSdk = 23
        targetSdk = 37
        /*
        Set the first two digits of the version code to the targetSdkVersion, such as 28.
        Set the next three digits to the product version, such as 152 for a product version of 1.5.2.
        Set the next two digits to build or release number, such as 01.
        Reserve the last two digits for a multi-APK variant, 00 for app, 01 for wearOS
         */

        // SDK 37 + v6.5.0 + release 01 + 00 (for app)
        versionCode = 376500300 // https://developer.android.com/training/wearables/packaging //TODO set back to 01
        // The "v" is important, as the updater class compares with the RSS feed on GitHub
        versionName = "v6.5.0"
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
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

    lint {
        disable += setOf("WearableBindListener")
    }


}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":anonaddy_shared"))

    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.fragment:fragment-ktx:1.9.0")
    implementation("androidx.activity:activity-ktx:1.13.0")

    // Core library desugaring for pre-oreo java.time
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Shimmer
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.github.omtodkar:ShimmerRecyclerView:v0.4.1")

    // Securing app
    implementation("androidx.biometric:biometric:1.1.0")

    // Scanning QR codes
    implementation("com.github.yuriy-budiyev:code-scanner:2.3.2")

    // WorkManager for background worker
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    // Charts / Donut
    implementation("app.futured.donut:donut:2.3.0")

    // Loading button
    implementation("com.github.Stjin:LoadingButtonAndroid:3.0.2")

    // PrettyTime
    implementation("org.ocpsoft.prettytime:prettytime:5.0.7.Final")

    // Activity Embedding & App Startup
    implementation("androidx.window:window:1.5.1")
    implementation("androidx.startup:startup-runtime:1.2.0")

    // File support & UI Layouts
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    // Communication with Wear OS device / Google Play flavor
    "gplayImplementation"("com.google.android.gms:play-services-wearable:20.0.1")
    "gplayImplementation"("com.android.billingclient:billing-ktx:9.1.0")
    "gplayImplementation"("com.google.android.play:review-ktx:2.0.2")
}
