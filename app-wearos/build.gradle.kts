import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val compose_version = rootProject.extra["compose_version"]
val compose_material_version = rootProject.extra["compose_material_version"]
val wear_compose_version = rootProject.extra["wear_compose_version"]
val compose_activity_version = rootProject.extra["compose_activity_version"]
val wear_tiles_version = rootProject.extra["wear_tiles_version"]
val wear_protolayout_version = rootProject.extra["wear_protolayout_version"]

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" // this version matches your Kotlin version
}

configure<ApplicationExtension> {
    namespace = "host.stjin.anonaddy"
    compileSdk = 37

    defaultConfig {
        applicationId = namespace
        minSdk = 30
        targetSdk = 37
        /*
        Set the first two digits of the version code to the targetSdkVersion, such as 28.
        Set the next three digits to the product version, such as 152 for a product version of 1.5.2.
        Set the next two digits to build or release number, such as 01.
        Reserve the last two digits for a multi-APK variant, 00 for app, 01 for wearOS
         */

        // SDK 37 + v1.7.0 + release 01 + 01 (for wearos)
        versionCode = 371700101
        versionName = "1.7.0"
        vectorDrawables {
            useSupportLibrary = true
        }

    }

    buildTypes {
        getByName("release") {
            // Do not enable, Fuel will break
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":anonaddy_shared"))

    // Core library desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("androidx.compose.material3:material3:$compose_material_version")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.fragment:fragment-ktx:1.9.0")
    implementation("com.google.android.gms:play-services-wearable:20.0.1")

    // General compose dependencies
    implementation("androidx.activity:activity-compose:$compose_activity_version")
    implementation("androidx.compose.ui:ui-tooling-preview:$compose_version")
    implementation("androidx.wear:wear-tooling-preview:1.0.0")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    // Animated graphics (for the icon on setup)
    implementation("androidx.compose.animation:animation-graphics:$compose_version")

    // For Wear Material Design UX guidelines and specifications
    implementation("androidx.wear.compose:compose-foundation:$wear_compose_version")
    implementation("androidx.wear.compose:compose-material:$wear_compose_version")

    // WorkManager for updating widgets and caching data
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    // For the donut in the aliasview
    implementation("app.futured.donut:donut-compose:2.3.1")

    // Tiles & ProtoLayout
    implementation("androidx.wear.tiles:tiles:$wear_tiles_version")
    implementation("androidx.wear.tiles:tiles-material:$wear_tiles_version")
    implementation("androidx.wear.protolayout:protolayout:$wear_protolayout_version")
    implementation("androidx.wear.protolayout:protolayout-material:$wear_protolayout_version")
    implementation("androidx.wear.protolayout:protolayout-expression:$wear_protolayout_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.11.0")
    implementation("androidx.wear.tiles:tiles-proto:$wear_tiles_version")

    // For smooth scrolling
    implementation("com.google.android.horologist:horologist-compose-layout:0.7.15")

    // Splash screen
    implementation("androidx.core:core-splashscreen:1.2.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.wear.tiles:tiles-testing:$wear_tiles_version")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose_version")
    debugImplementation("androidx.compose.ui:ui-tooling:$compose_version")
    debugImplementation("androidx.wear.tiles:tiles-renderer:$wear_tiles_version")
}
