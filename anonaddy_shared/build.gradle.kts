import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
}

configure<LibraryExtension> {
    namespace = "host.stjin.anonaddy_shared"
    compileSdk = 37
    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            // Do not enable, Fuel will break
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")

    // Core library desugaring for pre-oreo java.time
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Preferences for storing settings (and crypto settings)
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.security:security-crypto-ktx:1.1.0")

    // Fuel, network requests
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    api("com.google.code.gson:gson:2.14.0")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    // Built-in updater
    api("com.github.einmalfel:Earl:1.2.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
