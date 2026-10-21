import com.github.triplet.gradle.androidpublisher.ResolutionStrategy

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.play.publisher)
}

/**
 * Keystore upload hanya ada saat CI menyediakannya lewat environment.
 * Di laptop, build rilis tetap keluar tanpa tanda tangan seperti sebelumnya.
 */
val uploadKeystoreFile = providers.environmentVariable("KEYSTORE_FILE").orNull?.let { path ->
    file(path)
}

android {
    namespace = "com.khalied.cukinggo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.khalied.cukinggo"
        minSdk = 29
        targetSdk = 36
        versionCode = 6
        versionName = "1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (uploadKeystoreFile != null) {
            create("upload") {
                storeFile = uploadKeystoreFile
                storePassword = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            // Cuma terisi kalau keystore-nya ada; tanpa itu artefaknya unsigned.
            signingConfig = signingConfigs.findByName("upload")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

/*
 * Publikasi rilis ke Play. Track `alpha` adalah closed testing bawaan.
 *
 * Kredensialnya dibaca dari environment ANDROID_PUBLISHER_CREDENTIALS (isi file
 * JSON service account), jadi tidak ada file kunci yang perlu ditulis ke disk
 * atau ikut ter-commit.
 */
play {
    track.set("alpha")
    defaultToAppBundles.set(true)
    // Nomor versi diambil dari Play (tertinggi + 1), jadi CI tidak perlu menaikkan
    // versionCode manual dan dua rilis tidak bisa bertabrakan. Hanya dinyalakan
    // saat kredensialnya ada: tanpa kredensial, GPP menolak menyentuh nomor versi
    // dan build rilis lokal jadi ikut gagal.
    if (providers.environmentVariable("ANDROID_PUBLISHER_CREDENTIALS").isPresent) {
        resolutionStrategy.set(ResolutionStrategy.AUTO)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Room (local database)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Paging: the full cat list is read one page at a time from Room
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // CameraX (in-app camera)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.video)

    // Location + map (OpenStreetMap, no API key needed)
    implementation(libs.play.services.location)
    implementation(libs.osmdroid.android)

    // Image loading from local file
    implementation(libs.coil.compose)

    // Frost UI: blur di belakang permukaan kaca, juga di bawah API 31
    implementation(libs.haze)
    implementation(libs.haze.materials)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
