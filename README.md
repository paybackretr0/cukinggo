# CukingGo

App Android untuk mencatat dan menandai cuking yang kamu temui di jalan: foto, nama panggilan, catatan singkat, tanggal, dan titik lokasinya di peta. Semua data disimpan lokal di HP, tanpa akun, tanpa backend.

Spesifikasi produknya ada di [`spesifikasi-app-penanda-kucing.md`](spesifikasi-app-penanda-kucing.md), arah desain di [`DESIGN.md`](DESIGN.md), dan aturan agen di [`AGENTS.md`](AGENTS.md) + [`antislop.md`](antislop.md).

## Fitur

- **Peta cuking** (osmdroid / OpenStreetMap, tanpa API key)
  - Marker berupa **foto cuking** saat zoom dekat, otomatis berubah jadi **gelembung angka** saat zoom jauh supaya marker tidak saling menabrak. Tap gelembung untuk zoom ke area itu.
  - Animasi pop-in hanya untuk marker baru, bukan tiap kali peta digeser.
  - Titik **"kamu di sini"** muncul sendiri begitu izin lokasi aktif, dan tombol target untuk mengantar kamera ke posisimu.
  - Kabar kecil kalau tile peta gagal dimuat, dengan kalimat berbeda untuk offline vs server bermasalah, plus tombol "Coba lagi".
- **Tambah cuking**: kamera in-app (CameraX), frame ala polaroid, kolom **nama panggilan** opsional (dengan contoh seperti "si Kumis"), kolom catatan opsional, dan tombol "Simpan & Tandain!". Lokasi diambil otomatis saat menyimpan.
- **Detail cuking**: foto full-width, chip tanggal, chip koordinat, chip jarak dari lokasimu, nama, catatan, tombol bagikan, dan hapus dengan dialog konfirmasi.
- **Bagikan cuking**: kirim foto cuking (kalau filenya masih ada) plus template chat berisi nama, catatan, koordinat, dan **link Google Maps** ke titik ketemunya. Link-nya memakai format resmi Maps URLs, jadi terbuka di app Google Maps kalau ada, dan tidak butuh API key.
- **Kabar kalau dekat cuking** (opsional, mati secara bawaan): begitu kamu masuk radius cuking yang pernah ditandai, muncul notifikasi, dan tap-nya langsung membuka detail cuking itu. Kalau namanya sudah diisi, nama itu dipakai sebagai judul notifikasinya, jadi kabarnya bisa dikenali tanpa membuka app. Radiusnya bisa dipilih di dialognya: **100 m, 200 m (bawaan), atau 500 m**, masing-masing dengan penjelasan akibatnya. Dipasang sebagai geofence Play Services, maksimal cuking terbaru sesuai batas 100 geofence, dengan jeda 12 jam per cuking supaya tidak berisik. Menyalakannya butuh izin lokasi sepanjang waktu (Android 11 ke atas mengarahkannya ke Pengaturan) dan izin notifikasi.
- **Rentetan harian**: jumlah hari berturut-turut kamu menandai cuking, tampil sebagai chip api di header Home dan sebagai lencana kecil di sudut foto kedua widget. Angkanya dihitung dari waktu catatan yang ada, bukan dari angka yang disimpan, jadi tidak bisa jadi tidak sinkron. Chip dan lencana baru muncul kalau rentetannya jalan (hari ini atau kemarin ada catatan), dan lencananya hilang sama sekali saat angkanya nol.
- **Daftar terbaru** di Home dengan swipe untuk hapus.
- **Dialog Tampilan** di header Home, isinya dua hal: **tema** app (ikut sistem / terang / gelap) dan **bentuk widget** (Otomatis, Stiker, Balon bicara, Jendela bulat). Keduanya tersimpan permanen, dan menekan salah satunya langsung menggambar ulang widget tanpa menunggu.
- **State yang jujur**: loading, konten, kosong, dan gagal dipisah, jadi empty state tidak berkedip sebelum data datang.
- **Dua widget layar utama**, keduanya berisi satu foto dan catatannya (kalau catatannya kosong, dipakai satu baris lucu dari `widget_funny_lines`, dipilih dari id cuking sehingga tidak berubah-ubah sendiri). Tap membuka detail cuking itu.
  - **Ukuran**: bawaan 3x2 sel, dan bisa ditarik sendiri oleh pengguna antara 2x2 sampai 4x3. Fotonya memakai ruang sisa, jadi ikut membesar tanpa ada ukuran yang dipatok.
  - **Bingkainya berganti tiap hari** (bawaan Otomatis), bergilir antara tiga bentuk: **stiker** dengan telinga cuking yang naik 12dp di atas garis badan, **balon bicara** dengan ekor kecil, dan **jendela bulat** dengan kumis yang digambar di atas fotonya (jadi selalu menempel di tepi lingkarannya, bukan hiasan yang melayang di kartu). Bentuknya dipilih dari tanggal, jadi dua widget yang terpasang bersamaan menampilkan bentuk yang sama hari itu, dan gambar yang sama tidak berubah kalau widget digambar ulang.
  - **Cuking terakhir**: foto cuking yang paling baru kamu tandai.
  - **Cuking hari ini**: satu cuking dari seluruh koleksi, bergilir satu langkah per hari dan berganti sendiri tengah malam. Tidak ada izin alarm tambahan, dan tidak ada yang membangunkan HP tengah malam.
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

Kenapa osmdroid, bukan Google Maps SDK: tidak perlu API key, akun Google Cloud, maupun risiko billing saat dirilis ke Play Store. Tile peta tetap butuh internet, tapi data cukingmu (foto, catatan, koordinat) tidak pernah dikirim ke mana pun.

Widget layar utama memakai **RemoteViews bawaan Android, bukan Jetpack Glance**. Alasannya konkret: `FontFamily` di Glance hanya menerima nama font sistem, jadi Fredoka dan Nunito tidak bisa dipakai, padahal keduanya bagian dari identitas app ini. Memakai RemoteViews juga berarti tidak ada dependency baru sama sekali untuk widget.

Alarm tengah malam widget memakai `AlarmManager.setAndAllowWhileIdle` (alarm inexact, jenis RTC) yang dipasang ulang setiap kali widget digambar, jadi tidak perlu izin "Alarms & reminders" dan tidak membangunkan HP tengah malam. Konsekuensinya, pengirimannya boleh bergeser sampai sekitar satu jam menurut dokumentasi Android, dan lebih lama lagi kalau HP sedang hemat baterai. Alarm ini dipasang oleh **kedua** varian widget, bukan hanya "Cuking hari ini": lencana rentetan di sudut foto juga perlu dihitung ulang tiap hari, dan kalau bukan begitu lencana di varian "Cuking terakhir" bisa menampilkan angka lama saat rentetannya sudah putus.

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

## Aset untuk Play Store

Ikon daftar Play Store ada di `store/ic_launcher_512.png`. Membuat ulangnya:

```bash
bash tools/icon-export/export-icon.sh
```

Script itu menggambar PNG-nya langsung dari geometri ikon adaptive di app memakai `java.awt`, jadi tidak perlu Android Studio, tidak perlu aplikasi desain, dan tidak ada dependensi atau koneksi internet. Yang dihasilkan juga bisa diperiksa ulang angkanya: jejak kakinya diukur dan dicetak, termasuk apakah ia masih utuh di dalam area aman ikon adaptive (66 dari 108, yang tidak pernah dipotong masking launcher).

Ada **dua** file, dan kamu pilih satu untuk diunggah:

| File | Jejak kaki | Cocok kalau |
|---|---|---|
| `store/ic_launcher_512.png` | 40% lebar ikon, sama persis dengan ikon di layar utama | kamu mau ikon di daftar Play identik dengan yang dilihat pengguna di HP-nya |
| `store/ic_launcher_512_besar.png` | 55% lebar ikon, batas amannya 57% | kamu merasa yang pertama kekecilan saat ditampilkan di daftar Play, karena halaman spesifikasi ikon Google Play justru menyarankan memakai ruang aset sepenuhnya |

Syarat Play Console untuk ikon daftar dan hasil pengukuran file pertama:

| Syarat | Hasil |
|---|---|
| Ukuran 512 x 512 px | 512 x 512 |
| PNG 32-bit | ya, 8 bit per kanal (RGBA) |
| Ruang warna sRGB | ya, tiap file membawa keterangan sRGB sendiri |
| Maksimal 1024 KB | 8 KB (varian besar 10 KB) |
| Persegi penuh, tanpa sudut membulat dan tanpa bayangan | ya, dan 0 piksel tembus pandang |

Angka-angka di tabel itu bukan dari hitungan di memori: script membuka lagi file PNG hasilnya, lalu mengukur ulang dari isi file yang benar-benar akan diunggah (ukuran, warna sudut dan bantalan, jumlah piksel tembus pandang, lebar jejak kaki, dan batas area aman).

Ikon di dalam app tetap **adaptive icon vektor** (`mipmap-anydpi/ic_launcher.xml`), dan itu memang yang benar untuk Android 8 ke atas: Play Console tidak membaca aset itu, yang dimintanya file 512 terpisah di `store/`. Jadi tidak ada PNG yang perlu ditanam ke `res/mipmap-*`.

Yang **belum** ada untuk listing, dan belum bisa saya buat tanpa keputusanmu: feature graphic 1024 x 500, minimal 2 screenshot, dan deskripsi singkat/panjang. Satu kandidat bersih-bersih sebelum rilis: masih ada 10 file `ic_launcher.webp` + `ic_launcher_round.webp` bawaan template di `res/mipmap-hdpi` sampai `mipmap-xxxhdpi`. Semuanya tidak pernah tampil (semua kerapatan menunjuk ke `mipmap-anydpi`), tapi ikut terpaket di APK dan isinya masih ikon robot hijau bawaan template.

## Struktur

```
app/src/main/java/com/khalied/cukinggo/
├─ CukingGoApp.kt              # Application, init AppContainer + cache tile osmdroid
├─ MainActivity.kt             # host Compose, menerapkan tema pilihan pengguna
├─ di/AppContainer.kt          # service locator sederhana
├─ data/local/                 # CatEntity, CatDao, CatDatabase, DisplayPreferences
├─ data/repository/            # CatRepository
├─ domain/model/               # model Cat
├─ location/                   # LocationHelper + geofence kabar "dekat cuking"
├─ navigation/                 # CukingGoNavHost (home, add cat, detail)
├─ ui/home|addcat|detail/      # screen + ViewModel per layar
├─ ui/components/              # peta, marker, ilustrasi cuking, kartu list, dialog tampilan
├─ ui/theme/                   # palet, tipografi, shape, mode tema
├─ widget/                     # widget layar utama: provider, bentuk harian, foto, teks
└─ util/                       # format tanggal, jarak, rentetan harian, penyimpanan foto, izin, penyeragaman teks kosong
```

Alurnya satu arah: `Room` → `CatRepository` → `ViewModel` (StateFlow) → Compose. `AppContainer` menyediakan repository, helper lokasi, penyimpanan foto, dan preferensi tema.

## Data dan privasi

- Foto disimpan di `filesDir` sebagai `cat_<timestamp>.jpg`, yang masuk database hanya path-nya.
- Tidak ada permission storage, karena foto dan cache tile peta berada di storage internal app.
- Cache tile osmdroid juga di `cacheDir`, jadi tidak ada file yang bocor ke folder publik.
- Kolom nama ditambahkan lewat migrasi Room versi 1 ke 2 (`ALTER TABLE cats ADD COLUMN name TEXT`), jadi catatan lama tetap ada setelah update, hanya kolom barunya yang kosong.
- Database dan foto **dikecualikan dari cloud backup** (transfer antar-HP lewat `dataExtractionRules` tetap boleh), supaya klaim "100% lokal" tetap jujur.
- Widget layar utama memakai `FileProvider` untuk memberi launcher izin baca **satu URI** foto yang sedang tampil. Provider-nya `exported=false`, tidak ada file lain yang bisa diminta, dan pemeriksaan di `widget/WidgetPhoto.kt` hanya meloloskan file `cat_*.jpg` di `filesDir`. Foto tetap tidak keluar dari HP.
- Uninstall app = semua data hilang. Tidak ada fitur export/backup, sesuai cakupan MVP.
- Izin yang diminta: `CAMERA`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `INTERNET` + `ACCESS_NETWORK_STATE` (khusus tile peta), `POST_NOTIFICATIONS` (kabar dekat cuking), dan `ACCESS_BACKGROUND_LOCATION` (supaya kabar dekat cuking tetap jalan saat app tertutup, dan hanya dipakai kalau fiturnya kamu nyalakan).
- Fitur kabar dekat cuking **mati secara bawaan**. Kalau tidak dinyalakan, app tidak membaca lokasi di latar belakang sama sekali. Kalau dinyalakan, penilaian radius tetap dikerjakan Play Services di HP, tidak ada koordinatmu yang dikirim ke mana pun, dan waktu kabar terakhir per cuking hanya disimpan di HP.

## Pengujian

83 unit test di `app/src/test` menutup logika yang tidak butuh device:

| Test | Yang diuji |
|---|---|
| `util/DateFormattingTest` | format tanggal gaya Indonesia |
| `util/DistanceTest` | haversine + format jarak (m, km satu desimal, km bulat) |
| `ui/components/MapClusteringTest` | pengelompokan marker per grid, ambang zoom |
| `ui/theme/ThemeModeTest` | pemilihan tema terang/gelap/ikut sistem + baca-tulis preferensi |
| `data/local/CatMappingTest` | pemetaan entity ↔ model domain, termasuk kolom nama yang boleh kosong |
| `util/BlankTextTest` | penyeragaman isian kosong: spasi dipotong, teks berisi spasi saja jadi null |
| `widget/WidgetCaptionTest` | teks widget: catatan dipakai apa adanya, baris lucu kalau catatannya kosong, dan cuking yang sama selalu dapat baris yang sama |
| `widget/WidgetStyleTest` | tiap bentuk widget punya bingkai dan hiasannya sendiri, warna kumis bentuk jendela berbeda antara garis dan garis luarnya, dan aset serta warnanya benar-benar berganti antara mode terang dan gelap |
| `widget/WidgetSkinTest` | rotasi bentuk harian: tanggal yang sama memberi bentuk yang sama, besoknya berbeda, dan tiga hari menampilkan ketiga bentuk |
| `widget/WidgetSkinModeTest` | pilihan bentuk widget: Otomatis mengikuti rotasi harian, bentuk yang dipaksa tidak berubah bersama tanggal, dan key asing jatuh ke Otomatis |
| `widget/CatOfDayTest` | pemilihan cuking harian: rotasi satu langkah per hari, tidak ada cuking yang muncul dua hari berturut-turut, dan satu putaran menampilkan semua cuking |
| `util/CatShareTest` | bentuk link Google Maps pada pesan bagikan, termasuk encode koma dan jumlah angka desimalnya |
| `location/NearbyAlertTest` | daftar area pantauan (termasuk batas 100), masa jeda kabar, dan urutan langkah izin |
| `location/NearbyRadiusTest` | tiga pilihan radius (100/200/500 m), radius bawaan, dan jatuhnya angka asing ke bawaan |
| `util/CatStreakTest` | rentetan harian: hari berturut-turut, beberapa cuking di hari yang sama tetap satu hari, kemarin masih dianggap hidup, rentetan putus setelah kemarin lusa, catatan bertanggal masa depan tidak memperpanjang, dan batas harinya ikut zona waktu setempat |

Status terakhir: build sukses, 83 test hijau, `lintDebug` 0 error (9 warning). Satu aturan lint dimatikan dengan alasan tertulis di [`app/lint.xml`](app/lint.xml): `PluralsCandidate`, karena aplikasi ini hanya berbahasa Indonesia dan bahasa Indonesia tidak mengubah bentuk kata karena jumlah.

## Yang belum diverifikasi

Beberapa hal hanya bisa dibuktikan di perangkat, jadi belum saya klaim lulus:

- Click-through tiap elemen dan tampilan kedua tema di layar (kontras sudah dihitung manual WCAG AA, belum dilihat mata).
- Perilaku runtime kamera (CameraX), akurasi GPS asli, dan kerapatan marker di lokasi nyata.
- Munculnya kabar gagal-tile saat mode pesawat menyala.
- Semantik TalkBack (kartu daftar sudah digabung jadi satu simpul, tapi perlu TalkBack aktif untuk membuktikan).
- Tampilan widget di layar utama: apakah fotonya benar-benar muncul lewat jalur `FileProvider` + izin ke launcher, seperti apa ketiga bentuk barunya, dan apakah bentuknya ikut berganti saat tema app ditukar. Versi pertama dari bentuk-bentuk ini sudah dilihat di HP dan hasilnya salah: telinga stiker terbaca sebagai dua segitiga di dalam kartu, dan kumis bentuk jendela terbaca sebagai hiasan nyasar di kiri kanan karena melayang jauh dari lingkarannya. Keduanya sudah diubah (pita telinga 12dp jadi 18dp, kumis dipindah ke dalam bitmap foto), tapi perbaikannya belum dilihat lagi di perangkat. Yang masih perlu dibuktikan: apakah telinga sekarang benar-benar terbaca mencuat dan tidak menyatu dengan wallpaper sewarna, apakah kumisnya cukup terlihat di foto yang ramai, dan apakah foto bentuk jendela benar-benar bulat di semua ukuran sel, bukan jadi elips saat widget ditarik lebar. Yang bisa dibuktikan tanpa device: APK mendeklarasikan `provides-component:'app-widget'`, receiver, FileProvider, dan blok `<queries>` untuk mencari paket launcher sudah ada di merged manifest, dan empat unit test menutup logika teks, rotasi bentuk harian, serta pilihan aset kedua tema.
- Bentuk jendela memotong fotonya bulat lewat bitmap kecil (448 px) alih-alih mengirim URI resolusi penuh seperti dua bentuk lain. Yang belum diukur: apakah 448 px masih cukup tajam saat widget ditarik ke 4x3, dan apakah foto bulat itu masih nyaman dikirim bersama seluruh RemoteViews di perangkat dengan batas transaksi yang lebih ketat.
- Ukuran dan kemampuan resize widget belum pernah saya coba di launcher: yang terbukti tanpa device, `appwidget-provider` sudah meminta bawaan 3x2 dan rentang 2x2 sampai 4x3, serta layout widget memakai weight supaya ikut melar. Apakah grid launcher benar-benar memberi ukuran itu, dan apakah teks catatannya tetap muat saat dikecilkan, perlu dilihat di HP.
- Kalau paket launcher tidak ditemukan lewat query HOME, widget otomatis jatuh ke jalur bitmap 384 px, jadi fotonya tetap tampil walau lebih lembut. Jalur cadangan ini juga belum pernah saya lihat jalan di perangkat.
- Pratinjau di pemilih widget: `previewLayout` dipakai Android 12 ke atas. Di Android 10 dan 11 pemilih memakai ikon app sebagai gantinya. Dua varian widget sengaja memakai pratinjau yang sama, dan pratinjaunya menampilkan bentuk stiker pada 180 x 110dp sebagai contoh, karena cuma satu bentuk yang bisa dijanjikan sementara bentuk aslinya bergilir tiap hari.
- Pergantian harian widget "Cuking hari ini": saya tidak bisa menunggu lewat tengah malam di sini, jadi yang terbukti hanya dua: pemilihan cukingnya (7 unit test, termasuk rotasi harian dan tidak ada pengulangan berurutan) dan alarmnya benar-benar dipasang lewat jalur API yang tidak butuh izin khusus. Apakah alarmnya benar-benar berbunyi dan fotonya berganti besok butuh dicek di HP, dan itu baru terlihat setelah satu hari penuh.
- Kalau app di-force-stop (mis. dari Pengaturan), alarm dan pembaruan widget dihentikan sistem sampai app dibuka lagi. Jadi "cuking hari ini" bisa tertinggal di cuking kemarin pada kondisi itu.
- Kabar "dekat cuking" sama sekali belum saya lihat jalan: butuh berjalan membawa HP melewati radius cuking yang sudah ditandai. Yang terbukti tanpa device: aturan pemilihan area, batas 100 geofence, masa jeda 12 jam, urutan langkah izin (7 unit test), dan daftar geofence-nya diterima API `GeofencingClient` yang sudah ada di dependency project. Berapa lama kabarnya muncul setelah masuk radius (Play Services yang memutuskan, biasanya dalam hitungan menit) belum terukur.
- Alur izin "sepanjang waktu" belum pernah saya lewati: di Android 11 ke atas pengguna diantar ke Pengaturan app untuk memilih izin lokasi sepanjang waktu, dan tombolnya baru aktif setelah kembali ke app. Termasuk belum teruji apakah dialog panduannya terasa jelas.
- Penggantian radius belum pernah saya lakukan di HP: yang terbukti hanya bahwa radius pilihan dan radius terpasang disimpan terpisah lalu dibandingkan (jadi penggantiannya pasti memicu pemasangan ulang), dan bahwa `addGeofences` memang menimpa geofence dengan request ID yang sama menurut referensi API Play Services. Efek sampingnya yang perlu kamu tahu: setelah radius diganti, kalau kamu sedang berada di dalam radius baru, kabarnya bisa muncul sekali saat itu juga. Jeda 12 jam per cuking yang menahan supaya tidak jadi berisik.
- Lencana rentetan di widget belum pernah saya lihat berganti tengah malam: sama seperti pergantian harian, ini butuh menunggu satu hari penuh di HP. Yang terbukti tanpa device: fungsi perhitungan rentetannya sendiri (10 unit test) dan bahwa alarm tengah malam kini dipasang oleh kedua provider widget lalu memicu penggambaran ulang keduanya.
- Migrasi database versi 1 ke 2 belum pernah dijalankan di HP: `exportSchema = false`, jadi Room tidak bisa memeriksa SQL migrasinya saat build. Yang terbukti tanpa device: skema hasil kompilasi memang meminta kolom `name TEXT` yang boleh null (dibaca dari `CatDatabase_Impl` hasil KSP), dan SQL migrasinya sama dengan itu.
- Nama cuking di kartu Home, layar detail, dan judul notifikasi belum pernah saya lihat di layar. Yang belum terlihat: apakah nama panjang dipotong rapi di kartu, dan apakah baris catatan satu baris di bawahnya tetap enak dibaca.
- Dialog Tampilan belum pernah dibuka di perangkat: yang terbukti tanpa device cuma nilai bawaannya (Otomatis + ikut sistem), jatuhnya key asing ke bawaan, dan bahwa pilihan disimpan sebagai key di preferensi yang sama dengan tema. Yang belum terlihat: apakah tujuh baris pilihan terasa terlalu panjang dan perlu digulir, dan apakah widget benar-benar berganti bentuk seketika setelah pilihan disentuh.
- Tombol bagikan belum pernah saya tekan: yang terbukti hanya bentuk link Maps-nya (3 unit test) dan bahwa intent-nya memakai FileProvider yang sudah terpasang. Tampilan lembar bagikan di HP aslinya, dan perilaku app chat yang membuang teks saat ada gambar, perlu kamu lihat sendiri.
- Untuk rilis ke Play Store, `ACCESS_BACKGROUND_LOCATION` punya syarat tambahan dari Google: formulir deklarasi terpisah plus video yang menunjukkan fiturnya dipakai. Ini di luar cakupan kode, tapi harus disiapkan sebelum submit.

Daftar temuan audit UI lengkap dengan status per temuan ada di [`anti-slop/audit-001-2026-09-23.md`](anti-slop/audit-001-2026-09-23.md). Tiga temuan yang masih menunggu keputusan pemilik: **F-07** (variasi radius), **F-08** (motif emoji), **F-09** (angka ajaib padding daftar).

## Rencana lanjutan

Sesuai bagian 3 dan 11 spesifikasi, kandidat berikutnya: pencarian/filter cuking, edit catatan yang sudah ada, statistik sederhana, dan export/backup manual. Kalau mau dirilis ke Play Store, bagian 13 spesifikasi memuat daftar yang wajib disiapkan (privacy policy, Data safety form, app signing, screenshot listing, feature graphic).
