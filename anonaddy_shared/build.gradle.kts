val compose_version = rootProject.extra["compose_version"]
val compose_compiler_version = rootProject.extra["compose_compiler_version"]
val compose_material_version = rootProject.extra["compose_material_version"]
val wear_compose_version = rootProject.extra["wear_compose_version"]


plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" // this version matches your Kotlin version

}

android {
    namespace = "host.stjin.anonaddy_shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            // Do not enable, Fuel will break
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "$compose_compiler_version"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

dependencies {
    implementation("com.google.code.gson:gson:2.12.1")

    // Preferences for storing settings (and crypto settings)
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha07")
}

// Compose
dependencies {
    implementation("androidx.compose.ui:ui-text:$compose_version")
    implementation("androidx.compose.ui:ui-graphics:$compose_version")
    implementation("androidx.compose.material3:material3:$compose_material_version")
    // Compose for Wear OS Dependencies
    implementation("androidx.wear.compose:compose-material:$wear_compose_version")
}

// Fuel, network requests
dependencies {
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.google.code.gson:gson:2.12.1")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}


// Built-in updater
dependencies {
    implementation("com.github.einmalfel:Earl:1.2.0")
}
