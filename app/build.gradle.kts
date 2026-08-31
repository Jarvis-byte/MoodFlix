import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics.plugin)
}

// TMDB key lives in local.properties so it never lands in git.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Upload keystore credentials live in keystore.properties so they never land in git.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasSigningConfig = keystoreProps.getProperty("storeFile")?.let { File(it).exists() } == true

android {
    namespace = "com.arka.moodflix"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.arka.moodflix"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.0"

        buildConfigField(
            "String",
            "TMDB_API_KEY",
            "\"${localProps.getProperty("TMDB_API_KEY", "")}\""
        )

        // Web client ID of the OAuth client Firebase generates once Google is
        // enabled as a sign-in provider (Firebase console > Authentication >
        // Sign-in method > Google). Credential Manager needs this to know
        // which backend to issue the ID token for.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localProps.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\""
        )
    }

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.coil.compose)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    implementation(libs.androidx.browser)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    implementation(libs.firebase.auth)
    implementation(libs.firebase.remote.config)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.googleid)

    implementation(libs.play.services.ads)
}
