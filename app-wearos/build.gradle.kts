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

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
}

dependencies {
    implementation("com.google.android.material:material:1.5.0")

    implementation("androidx.core:core-ktx:1.7.0")
    implementation("com.google.android.gms:play-services-wearable:17.1.0")
    implementation("androidx.percentlayout:percentlayout:1.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.wear:wear:1.2.0")
    implementation(project(mapOf("path" to ":anonaddy_shared")))

    compileOnly("com.google.android.wearable:wearable:2.8.1")
    implementation("com.google.android.support:wearable:2.8.1")
}