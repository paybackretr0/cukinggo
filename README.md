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
- **Detail kucing**: foto full-width, chip tanggal, chip koordinat, chip jarak dari lokasimu, catatan, tombol bagikan, dan hapus dengan dialog konfirmasi.
- **Bagikan kucing**: kirim foto kucing (kalau filenya masih ada) plus template chat berisi catatan, koordinat, dan **link Google Maps** ke titik ketemunya. Link-nya memakai format resmi Maps URLs, jadi terbuka di app Google Maps kalau ada, dan tidak butuh API key.
- **Kabar kalau dekat kucing** (opsional, mati secara bawaan): begitu kamu masuk radius kucing yang pernah ditandai, muncul notifikasi, dan tap-nya langsung membuka detail kucing itu. Radiusnya bisa dipilih di dialognya: **100 m, 200 m (bawaan), atau 500 m**, masing-masing dengan penjelasan akibatnya. Dipasang sebagai geofence Play Services, maksimal kucing terbaru sesuai batas 100 geofence, dengan jeda 12 jam per kucing supaya tidak berisik. Menyalakannya butuh izin lokasi sepanjang waktu (Android 11 ke atas mengarahkannya ke Pengaturan) dan izin notifikasi.
- **Rentetan harian**: jumlah hari berturut-turut kamu menandai kucing, tampil sebagai chip api di header Home dan sebagai lencana kecil di sudut foto kedua widget. Angkanya dihitung dari waktu catatan yang ada, bukan dari angka yang disimpan, jadi tidak bisa jadi tidak sinkron. Chip dan lencana baru muncul kalau rentetannya jalan (hari ini atau kemarin ada catatan), dan lencananya hilang sama sekali saat angkanya nol.
- **Daftar terbaru** di Home dengan swipe untuk hapus.
- **Toggle tema** di dalam app: ikut sistem / terang / gelap, tersimpan permanen.
- **State yang jujur**: loading, konten, kosong, dan gagal dipisah, jadi empty state tidak berkedip sebelum data datang.
- **Dua widget layar utama**, keduanya polaroid 2x2 berisi satu foto dan catatannya (kalau catatannya kosong, dipakai satu baris lucu dari `widget_funny_lines`, dipilih dari id kucing sehingga tidak berubah-ubah sendiri). Tap membuka detail kucing itu.
  - **Kucing terakhir**: foto kucing yang paling baru kamu tandai.
  - **Kucing hari ini**: satu kucing dari seluruh koleksi, bergilir satu langkah per hari dan berganti sendiri tengah malam. Tidak ada izin alarm tambahan, dan tidak ada yang membangunkan HP tengah malam.
  - Keduanya menampilkan lencana rentetan di sudut fotonya, dan lencananya ikut dihitung ulang saat tengah malam.

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

Widget layar utama memakai **RemoteViews bawaan Android, bukan Jetpack Glance**. Alasannya konkret: `FontFamily` di Glance hanya menerima nama font sistem, jadi Fredoka dan Nunito tidak bisa dipakai, padahal keduanya bagian dari identitas app ini. Memakai RemoteViews juga berarti tidak ada dependency baru sama sekali untuk widget.

Alarm tengah malam widget memakai `AlarmManager.setAndAllowWhileIdle` (alarm inexact, jenis RTC) yang dipasang ulang setiap kali widget digambar, jadi tidak perlu izin "Alarms & reminders" dan tidak membangunkan HP tengah malam. Konsekuensinya, pengirimannya boleh bergeser sampai sekitar satu jam menurut dokumentasi Android, dan lebih lama lagi kalau HP sedang hemat baterai. Alarm ini dipasang oleh **kedua** varian widget, bukan hanya "Kucing hari ini": lencana rentetan di sudut foto juga perlu dihitung ulang tiap hari, dan kalau bukan begitu lencana di varian "Kucing terakhir" bisa menampilkan angka lama saat rentetannya sudah putus.

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
├─ location/                   # LocationHelper + geofence kabar "dekat kucing"
├─ navigation/                 # CukingGoNavHost (home, add cat, detail)
├─ ui/home|addcat|detail/      # screen + ViewModel per layar
├─ ui/components/              # peta, marker, ilustrasi kucing, kartu list
├─ ui/theme/                   # palet, tipografi, shape, mode tema
├─ widget/                     # widget layar utama: provider, palet, foto, teks
└─ util/                       # format tanggal, jarak, rentetan harian, penyimpanan foto, izin
```

Alurnya satu arah: `Room` → `CatRepository` → `ViewModel` (StateFlow) → Compose. `AppContainer` menyediakan repository, helper lokasi, penyimpanan foto, dan preferensi tema.

## Data dan privasi

- Foto disimpan di `filesDir` sebagai `cat_<timestamp>.jpg`, yang masuk database hanya path-nya.
- Tidak ada permission storage, karena foto dan cache tile peta berada di storage internal app.
- Cache tile osmdroid juga di `cacheDir`, jadi tidak ada file yang bocor ke folder publik.
- Database dan foto **dikecualikan dari cloud backup** (transfer antar-HP lewat `dataExtractionRules` tetap boleh), supaya klaim "100% lokal" tetap jujur.
- Widget layar utama memakai `FileProvider` untuk memberi launcher izin baca **satu URI** foto yang sedang tampil. Provider-nya `exported=false`, tidak ada file lain yang bisa diminta, dan pemeriksaan di `widget/WidgetPhoto.kt` hanya meloloskan file `cat_*.jpg` di `filesDir`. Foto tetap tidak keluar dari HP.
- Uninstall app = semua data hilang. Tidak ada fitur export/backup, sesuai cakupan MVP.
- Izin yang diminta: `CAMERA`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `INTERNET` + `ACCESS_NETWORK_STATE` (khusus tile peta), `POST_NOTIFICATIONS` (kabar dekat kucing), dan `ACCESS_BACKGROUND_LOCATION` (supaya kabar dekat kucing tetap jalan saat app tertutup, dan hanya dipakai kalau fiturnya kamu nyalakan).
- Fitur kabar dekat kucing **mati secara bawaan**. Kalau tidak dinyalakan, app tidak membaca lokasi di latar belakang sama sekali. Kalau dinyalakan, penilaian radius tetap dikerjakan Play Services di HP, tidak ada koordinatmu yang dikirim ke mana pun, dan waktu kabar terakhir per kucing hanya disimpan di HP.

## Pengujian

63 unit test di `app/src/test` menutup logika yang tidak butuh device:

| Test | Yang diuji |
|---|---|
| `util/DateFormattingTest` | format tanggal gaya Indonesia |
| `util/DistanceTest` | haversine + format jarak (m, km satu desimal, km bulat) |
| `ui/components/MapClusteringTest` | pengelompokan marker per grid, ambang zoom |
| `ui/theme/ThemeModeTest` | pemilihan tema terang/gelap/ikut sistem + baca-tulis preferensi |
| `data/local/CatMappingTest` | pemetaan entity ↔ model domain |
| `widget/WidgetCaptionTest` | teks widget: catatan dipakai apa adanya, baris lucu kalau catatannya kosong, dan kucing yang sama selalu dapat baris yang sama |
| `widget/WidgetPaletteTest` | aset dan warna widget benar-benar berganti antara mode terang dan gelap |
| `widget/CatOfDayTest` | pemilihan kucing harian: rotasi satu langkah per hari, tidak ada kucing yang muncul dua hari berturut-turut, dan satu putaran menampilkan semua kucing |
| `util/CatShareTest` | bentuk link Google Maps pada pesan bagikan, termasuk encode koma dan jumlah angka desimalnya |
| `location/NearbyAlertTest` | daftar area pantauan (termasuk batas 100), masa jeda kabar, dan urutan langkah izin |
| `location/NearbyRadiusTest` | tiga pilihan radius (100/200/500 m), radius bawaan, dan jatuhnya angka asing ke bawaan |
| `util/CatStreakTest` | rentetan harian: hari berturut-turut, beberapa kucing di hari yang sama tetap satu hari, kemarin masih dianggap hidup, rentetan putus setelah kemarin lusa, catatan bertanggal masa depan tidak memperpanjang, dan batas harinya ikut zona waktu setempat |

Status terakhir: build sukses, 63 test hijau, `lintDebug` 0 error (9 warning). Satu aturan lint dimatikan dengan alasan tertulis di [`app/lint.xml`](app/lint.xml): `PluralsCandidate`, karena aplikasi ini hanya berbahasa Indonesia dan bahasa Indonesia tidak mengubah bentuk kata karena jumlah.

## Yang belum diverifikasi

Beberapa hal hanya bisa dibuktikan di perangkat, jadi belum saya klaim lulus:

- Click-through tiap elemen dan tampilan kedua tema di layar (kontras sudah dihitung manual WCAG AA, belum dilihat mata).
- Perilaku runtime kamera (CameraX), akurasi GPS asli, dan kerapatan marker di lokasi nyata.
- Munculnya kabar gagal-tile saat mode pesawat menyala.
- Semantik TalkBack (kartu daftar sudah digabung jadi satu simpul, tapi perlu TalkBack aktif untuk membuktikan).
- Tampilan widget di layar utama: apakah fotonya benar-benar muncul lewat jalur `FileProvider` + izin ke launcher, seperti apa bentuk polaroidnya, dan apakah ia ikut berganti saat tema app ditukar. Yang bisa dibuktikan tanpa device: APK mendeklarasikan `provides-component:'app-widget'`, receiver, FileProvider, dan blok `<queries>` untuk mencari paket launcher sudah ada di merged manifest, dan dua unit test menutup logika teks serta pilihan aset tema gelap.
- Kalau paket launcher tidak ditemukan lewat query HOME, widget otomatis jatuh ke jalur bitmap 384 px, jadi fotonya tetap tampil walau lebih lembut. Jalur cadangan ini juga belum pernah saya lihat jalan di perangkat.
- Pratinjau di pemilih widget: `previewLayout` dipakai Android 12 ke atas. Di Android 10 dan 11 pemilih memakai ikon app sebagai gantinya. Dua varian widget sengaja memakai pratinjau yang sama, jadi di layar pemilih keduanya hanya dibedakan oleh label dan deskripsi.
- Pergantian harian widget "Kucing hari ini": saya tidak bisa menunggu lewat tengah malam di sini, jadi yang terbukti hanya dua: pemilihan kucingnya (7 unit test, termasuk rotasi harian dan tidak ada pengulangan berurutan) dan alarmnya benar-benar dipasang lewat jalur API yang tidak butuh izin khusus. Apakah alarmnya benar-benar berbunyi dan fotonya berganti besok butuh dicek di HP, dan itu baru terlihat setelah satu hari penuh.
- Kalau app di-force-stop (mis. dari Pengaturan), alarm dan pembaruan widget dihentikan sistem sampai app dibuka lagi. Jadi "kucing hari ini" bisa tertinggal di kucing kemarin pada kondisi itu.
- Kabar "dekat kucing" sama sekali belum saya lihat jalan: butuh berjalan membawa HP melewati radius kucing yang sudah ditandai. Yang terbukti tanpa device: aturan pemilihan area, batas 100 geofence, masa jeda 12 jam, urutan langkah izin (7 unit test), dan daftar geofence-nya diterima API `GeofencingClient` yang sudah ada di dependency project. Berapa lama kabarnya muncul setelah masuk radius (Play Services yang memutuskan, biasanya dalam hitungan menit) belum terukur.
- Alur izin "sepanjang waktu" belum pernah saya lewati: di Android 11 ke atas pengguna diantar ke Pengaturan app untuk memilih izin lokasi sepanjang waktu, dan tombolnya baru aktif setelah kembali ke app. Termasuk belum teruji apakah dialog panduannya terasa jelas.
- Penggantian radius belum pernah saya lakukan di HP: yang terbukti hanya bahwa radius pilihan dan radius terpasang disimpan terpisah lalu dibandingkan (jadi penggantiannya pasti memicu pemasangan ulang), dan bahwa `addGeofences` memang menimpa geofence dengan request ID yang sama menurut referensi API Play Services. Efek sampingnya yang perlu kamu tahu: setelah radius diganti, kalau kamu sedang berada di dalam radius baru, kabarnya bisa muncul sekali saat itu juga. Jeda 12 jam per kucing yang menahan supaya tidak jadi berisik.
- Lencana rentetan di widget belum pernah saya lihat berganti tengah malam: sama seperti pergantian harian, ini butuh menunggu satu hari penuh di HP. Yang terbukti tanpa device: fungsi perhitungan rentetannya sendiri (10 unit test) dan bahwa alarm tengah malam kini dipasang oleh kedua provider widget lalu memicu penggambaran ulang keduanya.
- Tombol bagikan belum pernah saya tekan: yang terbukti hanya bentuk link Maps-nya (3 unit test) dan bahwa intent-nya memakai FileProvider yang sudah terpasang. Tampilan lembar bagikan di HP aslinya, dan perilaku app chat yang membuang teks saat ada gambar, perlu kamu lihat sendiri.
- Untuk rilis ke Play Store, `ACCESS_BACKGROUND_LOCATION` punya syarat tambahan dari Google: formulir deklarasi terpisah plus video yang menunjukkan fiturnya dipakai. Ini di luar cakupan kode, tapi harus disiapkan sebelum submit.

Daftar temuan audit UI lengkap dengan status per temuan ada di [`anti-slop/audit-001-2026-09-23.md`](anti-slop/audit-001-2026-09-23.md). Tiga temuan yang masih menunggu keputusan pemilik: **F-07** (variasi radius), **F-08** (motif emoji), **F-09** (angka ajaib padding daftar).

## Rencana lanjutan

Sesuai bagian 3 dan 11 spesifikasi, kandidat berikutnya: pencarian/filter kucing, edit catatan yang sudah ada, statistik sederhana, dan export/backup manual. Kalau mau dirilis ke Play Store, bagian 13 spesifikasi memuat daftar yang wajib disiapkan (privacy policy, Data safety form, app signing, screenshot listing).
