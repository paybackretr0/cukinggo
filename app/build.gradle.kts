import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import java.util.Properties

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

/**
 * Kunci API Google Maps tidak pernah ditulis di sini. Sumbernya dua: environment
 * `MAPS_API_KEY` (jalur CI) atau `local.properties` di laptop. Nama paket dan
 * sidik SHA-1 yang membatasi kunci itu diatur di Google Cloud Console, bukan di
 * repo ini.
 *
 * Kalau kosong, build tetap jalan dan hanya petanya yang tampil abu-abu di HP,
 * dengan pesan galat dari Maps SDK di logcat. Rilis tanpa kunci berarti peta
 * rusak untuk semua pengguna, jadi nilainya wajib ada saat menyiapkan artifact.
 */
val localProperties = Properties().apply {
    rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.inputStream()
        ?.use { input -> load(input) }
}
val mapsApiKey: String = providers.environmentVariable("MAPS_API_KEY").orNull
    ?: localProperties.getProperty("MAPS_API_KEY")
    ?: ""

android {
    namespace = "com.khalied.cukinggo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.khalied.cukinggo"
        minSdk = 29
        targetSdk = 36
        // Nomor ini untuk build lokal dan artifact; saat publish, Gradle Play
        // Publisher menimpanya dengan versi tertinggi di Play + 1.
        versionCode = 8
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Dibaca manifest lewat ${MAPS_API_KEY}; placeholder ini yang menyambungkan
        // kuncinya tanpa menyentuh file manifest yang ikut git.
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    signingConfigs {
        if (uploadKeystoreFile != null) {
            create("upload") {
                storeFile = uploadKeystoreFile
                storePassword = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("KEY_ALIAS").orNull
                // Keystore PKCS12 tidak punya password kunci terpisah, dan
                // password kunci yang salah membuat bundletool gagal membaca
                // kuncinya. Jadi kalau tidak diisi, pakai password store.
                keyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
                    ?.takeIf { password -> password.isNotBlank() }
                    ?: providers.environmentVariable("KEYSTORE_PASSWORD").orNull
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

    // Lokasi + peta Google. Tile-nya dari Google dan butuh kunci API, tapi
    // pemuatan peta di app (Maps SDK untuk Android) tidak ditagih per muatan.
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)

    // Image loading from local file
    implementation(libs.coil.compose)

    // Stiker: memisahkan cuking dari latarnya, dikerjakan di HP
    implementation(libs.play.services.mlkit.subject.segmentation)
    // Frame bidik: model label ikut di APK, jadi deteksi tetap offline
    implementation(libs.mlkit.image.labeling)
    // Orientasi foto dibaca lewat API AndroidX, bukan android.media, supaya
    // hasilnya sama di semua versi Android
    implementation(libs.androidx.exifinterface)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
