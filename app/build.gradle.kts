import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Backend settings resolve from local.properties (untracked) first, then gradle.properties.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

fun backendSetting(key: String): String =
    localProperties.getProperty(key) ?: providers.gradleProperty(key).get()

// The placeholders in gradle.properties only let tests and lint run without a backend. An APK built
// with them talks to a project that does not exist: every call fails, and the 401s on the token
// refresh wipe the pairing on the next launch. So no APK may be built or installed with them.
val backendIsPlaceholder = backendSetting("supabase.url").contains("your-project") ||
    backendSetting("supabase.anonKey") == "your-anon-key"

val apkTask = Regex("(assemble|install|bundle)(Debug|Release)?")
tasks.configureEach {
    if (apkTask.matches(name)) {
        val placeholder = backendIsPlaceholder // a plain value: the configuration cache cannot hold the script
        doFirst {
            check(!placeholder) {
                "supabase.url / supabase.anonKey siguen con los valores de ejemplo: ponlos en local.properties."
            }
        }
    }
}

android {
    namespace = "com.ecoruta.dispositivo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ecoruta.dispositivo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "SUPABASE_URL", "\"${backendSetting("supabase.url")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${backendSetting("supabase.anonKey")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        // AAPT2 does not register the unversioned mipmap-anydpi folder in this setup, so the
        // adaptive icon stays in mipmap-anydpi-v26 even though minSdk is 26.
        disable += "ObsoleteSdkInt"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
        // Apply constructor-property annotations (e.g. @StringRes) to both param and field.
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    constraints {
        implementation(libs.androidx.fragment) {
            because("play-services-basement pulls fragment 1.1.0, which breaks the Activity Result APIs")
        }
    }

    // AndroidX core + lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // UI (Jetpack Compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Incident types are recognised by icon at a glance; R8 strips every icon the app does not use.
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Persistence
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // Background work
    implementation(libs.androidx.work.runtime.ktx)

    // Location
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
