# CukingGo

App Android untuk mencatat dan menandai kucing yang kamu temui di jalan: foto, catatan singkat, tanggal, dan titik lokasinya di peta. Semua data disimpan lokal di HP, tanpa akun, tanpa backend.

Spesifikasi produknya ada di [`spesifikasi-app-penanda-kucing.md`](spesifikasi-app-penanda-kucing.md), arah desain di [`DESIGN.md`](DESIGN.md), dan aturan agen di [`AGENTS.md`](AGENTS.md) + [`antislop.md`](antislop.md).

## Fitur

- **Peta kucing** (osmdroid / OpenStreetMap, tanpa API key)
  - Marker berupa **foto kucing** saat zoom dekat, otomatis berubah jadi **gelembung angka** saat zoom jauh supaya marker tidak saling menabrak. Tap gelembung untuk zoom ke area itu.
  - Animasi pop-in hanya untuk marker baru, bukan tiap kali peta digeser.
  - Titik **"kamu di sini"** muncul sendiri begitu izin lokasi aktif, dan tombol target untuk mengantar kamera ke posisimu.
  - Kabar kecil kalau tile peta gagal dimuat, dengan kalimat berbeda untuk offline vs server bermasalah, plus tombol "Coba lagi".
- **Tambah kucing**: kamera in-app (CameraX), frame ala polaroid, kolom catatan opsional, tombol "Simpan & Tandain!". Lokasi diambil otomatis saat menyimpan.
- **Detail kucing**: foto full-width, chip tanggal, chip koordinat, chip jarak dari lokasimu, catatan, dan hapus dengan dialog konfirmasi.
- **Daftar terbaru** di Home dengan swipe untuk hapus.
- **Toggle tema** di dalam app: ikut sistem / terang / gelap, tersimpan permanen.
- **State yang jujur**: loading, konten, kosong, dan gagal dipisah, jadi empty state tidak berkedip sebelum data datang.

## Teknologi

| Kebutuhan | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Database | Room (KSP) |
| Peta | osmdroid (OpenStreetMap) |
| Lokasi | `play-services-location` (FusedLocationProviderClient) |
| Kamera | CameraX (`LifecycleCameraController` + `PreviewView`) |
| Navigasi | Navigation Compose |
| Async | Coroutines + Flow |
| Foto | Coil |

Kenapa osmdroid, bukan Google Maps SDK: tidak perlu API key, akun Google Cloud, maupun risiko billing saat dirilis ke Play Store. Tile peta tetap butuh internet, tapi data kucingmu (foto, catatan, koordinat) tidak pernah dikirim ke mana pun.

## Menjalankan

Prasyarat:

- **JDK 21** (project memakai Gradle daemon JVM toolchain 21)
- **Android SDK Platform 37** (`compileSdk = 37`), Build Tools, dan `local.properties` yang menunjuk ke SDK
- `minSdk = 29` (Android 10), `targetSdk = 36`

```bash
# build APK debug
./gradlew :app:assembleDebug

# unit test
./gradlew :app:testDebugUnitTest

# lint
./gradlew :app:lintDebug
```

Hasil APK ada di `app/build/outputs/apk/debug/app-debug.apk`. Untuk memasang: `./gradlew :app:installDebug`, atau lewat Android Studio.

Catatan toolchain: template awal project tidak langsung bisa build, jadi ada tiga penyesuaian versi. `compileSdk` 37 (dituntut core-ktx 1.19.0 dan lifecycle 2.11.0), plugin Compose Kotlin 2.4.10 (compiler built-in AGP 9 tidak bisa membaca metadata stdlib 2.4.x), dan KSP 2.3.12 + Room 2.8.5 (KSP di bawah 2.3.11 bentrok dengan "built-in Kotlin" AGP 9).

## Struktur

```
app/src/main/java/com/khalied/cukinggo/
├─ CukingGoApp.kt              # Application, init AppContainer + cache tile osmdroid
├─ MainActivity.kt             # host Compose, menerapkan tema pilihan pengguna
├─ di/AppContainer.kt          # service locator sederhana
├─ data/local/                 # CatEntity, CatDao, CatDatabase, ThemePreferences
├─ data/repository/            # CatRepository
├─ domain/model/               # model Cat
├─ location/                   # LocationHelper (FusedLocationProviderClient)
├─ navigation/                 # CukingGoNavHost (home, add cat, detail)
├─ ui/home|addcat|detail/      # screen + ViewModel per layar
├─ ui/components/              # peta, marker, ilustrasi kucing, kartu list
├─ ui/theme/                   # palet, tipografi, shape, mode tema
└─ util/                       # format tanggal, jarak, penyimpanan foto, izin
```

Alurnya satu arah: `Room` → `CatRepository` → `ViewModel` (StateFlow) → Compose. `AppContainer` menyediakan repository, helper lokasi, penyimpanan foto, dan preferensi tema.

## Data dan privasi

- Foto disimpan di `filesDir` sebagai `cat_<timestamp>.jpg`, yang masuk database hanya path-nya.
- Tidak ada permission storage, karena foto dan cache tile peta berada di storage internal app.
- Cache tile osmdroid juga di `cacheDir`, jadi tidak ada file yang bocor ke folder publik.
- Database dan foto **dikecualikan dari cloud backup** (transfer antar-HP lewat `dataExtractionRules` tetap boleh), supaya klaim "100% lokal" tetap jujur.
- Uninstall app = semua data hilang. Tidak ada fitur export/backup, sesuai cakupan MVP.
- Izin yang diminta: `CAMERA`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `INTERNET` + `ACCESS_NETWORK_STATE` (khusus tile peta).

## Pengujian

24 unit test di `app/src/test` menutup logika yang tidak butuh device:

| Test | Yang diuji |
|---|---|
| `util/DateFormattingTest` | format tanggal gaya Indonesia |
| `util/DistanceTest` | haversine + format jarak (m, km satu desimal, km bulat) |
| `ui/components/MapClusteringTest` | pengelompokan marker per grid, ambang zoom |
| `ui/theme/ThemeModeTest` | pemilihan tema terang/gelap/ikut sistem + baca-tulis preferensi |
| `data/local/CatMappingTest` | pemetaan entity ↔ model domain |

Status terakhir: build sukses, 24 test hijau, `lintDebug` 0 error.

## Yang belum diverifikasi

Beberapa hal hanya bisa dibuktikan di perangkat, jadi belum saya klaim lulus:

- Click-through tiap elemen dan tampilan kedua tema di layar (kontras sudah dihitung manual WCAG AA, belum dilihat mata).
- Perilaku runtime kamera (CameraX), akurasi GPS asli, dan kerapatan marker di lokasi nyata.
- Munculnya kabar gagal-tile saat mode pesawat menyala.
- Semantik TalkBack (kartu daftar sudah digabung jadi satu simpul, tapi perlu TalkBack aktif untuk membuktikan).

Daftar temuan audit UI lengkap dengan status per temuan ada di [`anti-slop/audit-001-2026-09-23.md`](anti-slop/audit-001-2026-09-23.md). Tiga temuan yang masih menunggu keputusan pemilik: **F-07** (variasi radius), **F-08** (motif emoji), **F-09** (angka ajaib padding daftar).

## Rencana lanjutan

Sesuai bagian 3 dan 11 spesifikasi, kandidat berikutnya: pencarian/filter kucing, edit catatan yang sudah ada, statistik sederhana, dan export/backup manual. Kalau mau dirilis ke Play Store, bagian 13 spesifikasi memuat daftar yang wajib disiapkan (privacy policy, Data safety form, app signing, screenshot listing).
