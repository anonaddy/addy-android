plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 31

    defaultConfig {
        minSdk = 23
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            // Do not enable, Fuel will break
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = rootProject.extra["jvm_target"] as String
    }
    composeOptions {
        kotlinCompilerExtensionVersion = rootProject.extra["compose_version"] as String
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.5.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}

dependencies {
    implementation("com.google.code.gson:gson:2.9.0")

    // Preferences for storing settings (and crypto settings)
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha03")
}

// Compose
dependencies {
    implementation("androidx.compose.ui:ui-text:1.1.1")
    implementation("androidx.compose.ui:ui-graphics:1.1.1")
    implementation("androidx.compose.material3:material3:1.0.0-alpha06")
    // Compose for Wear OS Dependencies
    implementation("androidx.wear.compose:compose-material:1.0.0-alpha17")
}

// Fuel, network requests
dependencies {
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
}



// Built-in updater
dependencies {
    implementation("com.github.einmalfel:Earl:1.2.0")
}
