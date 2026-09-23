# Spesifikasi Aplikasi: Penanda Kucing (Cat Spotter)

## 1. Ringkasan

Aplikasi Android lokal (offline-first, tanpa backend) untuk mencatat kucing yang ditemukan user. User memotret kucing, opsional menambahkan deskripsi, lalu aplikasi otomatis mengambil lokasi GPS saat itu dan menandainya di peta. Semua data (foto, deskripsi, koordinat) disimpan **sepenuhnya lokal** di perangkat — tidak ada sinkronisasi ke server.

- **Platform:** Android
- **Bahasa:** Kotlin
- **UI:** Jetpack Compose
- **Penyimpanan:** Lokal saja (Room DB + internal storage untuk foto)

---

## 2. Alur Pengguna (User Flow)

1. User membuka app → langsung ke **Home Screen** (peta + daftar kucing).
2. User menekan tombol **"+ Tambah Kucing"** (FAB).
3. App membuka kamera (native camera intent atau CameraX in-app).
4. User memotret kucing.
5. Muncul form kecil: kolom deskripsi (opsional) + tombol simpan.
6. Saat disimpan:
   - App mengambil lokasi GPS saat ini.
   - Foto disimpan ke internal storage.
   - Data (path foto, deskripsi, lat/lng, timestamp) disimpan ke database lokal.
7. Kembali ke Home Screen → marker baru otomatis muncul di peta pada lokasi tersebut.
8. User bisa tap marker untuk lihat detail (foto, deskripsi, tanggal, lokasi).

---

## 3. Fitur Utama (MVP)

| Fitur | Deskripsi |
|---|---|
| Ambil foto kucing | Menggunakan kamera perangkat |
| Deskripsi opsional | Text field, boleh dikosongkan |
| Auto-tagging lokasi | Ambil GPS otomatis saat foto disimpan, tanpa input manual |
| Peta di Home | Menampilkan semua kucing sebagai marker di peta |
| Detail kucing | Tap marker → lihat foto, deskripsi, waktu, lokasi |
| Riwayat/list | (Opsional) daftar kucing dalam bentuk list di bawah/samping peta |
| Hapus data | User bisa hapus entri kucing tertentu |
| 100% offline | Tidak ada request ke server, semua CRUD lokal |

### Fitur lanjutan (opsional, next iteration)
- Edit deskripsi setelah disimpan
- Filter kucing berdasarkan tanggal
- Export data (backup ke file JSON/zip)
- Kategori/tag kucing (misal: warna, kondisi)
- Dark mode

---

## 4. Arsitektur

Disarankan pakai **MVVM** + **Clean-ish layering** sederhana:

```
UI (Compose)  →  ViewModel  →  Repository  →  Room DB + File Storage
                                   ↓
                          Location Provider (FusedLocationProviderClient)
```

### Struktur folder yang disarankan

```
app/
 └─ src/main/java/com/example/catspotter/
     ├─ data/
     │   ├─ local/
     │   │   ├─ CatEntity.kt
     │   │   ├─ CatDao.kt
     │   │   └─ CatDatabase.kt
     │   └─ repository/
     │       └─ CatRepository.kt
     ├─ domain/
     │   └─ model/
     │       └─ Cat.kt
     ├─ location/
     │   └─ LocationHelper.kt
     ├─ ui/
     │   ├─ home/
     │   │   ├─ HomeScreen.kt
     │   │   └─ HomeViewModel.kt
     │   ├─ addcat/
     │   │   ├─ AddCatScreen.kt
     │   │   └─ AddCatViewModel.kt
     │   ├─ detail/
     │   │   ├─ CatDetailScreen.kt
     │   │   └─ CatDetailViewModel.kt
     │   └─ components/
     │       └─ MapView.kt
     ├─ util/
     │   └─ ImageStorageHelper.kt
     └─ MainActivity.kt
```

---

## 5. Model Data

### Entity: `CatEntity` (Room)

```kotlin
@Entity(tableName = "cats")
data class CatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val photoPath: String,      // path ke file foto di internal storage
    val description: String?,   // nullable, opsional
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long         // System.currentTimeMillis()
)
```

### DAO

```kotlin
@Dao
interface CatDao {
    @Insert
    suspend fun insertCat(cat: CatEntity): Long

    @Query("SELECT * FROM cats ORDER BY timestamp DESC")
    fun getAllCats(): Flow<List<CatEntity>>

    @Query("SELECT * FROM cats WHERE id = :id")
    suspend fun getCatById(id: Long): CatEntity?

    @Delete
    suspend fun deleteCat(cat: CatEntity)
}
```

---

## 6. Teknologi / Library yang Dibutuhkan

| Kebutuhan | Library |
|---|---|
| UI | Jetpack Compose (`androidx.compose`) |
| Database lokal | Room (`androidx.room`) |
| Lokasi | `com.google.android.gms:play-services-location` (FusedLocationProviderClient) |
| Peta | **osmdroid** (OpenStreetMap) — `org.osmdroid:osmdroid-android` |
| Kamera | CameraX, atau lebih simpel: `ActivityResultContracts.TakePicture()` dengan `FileProvider` |
| Navigasi | Navigation Compose (`androidx.navigation:navigation-compose`) |
| Async | Kotlin Coroutines + Flow |
| Image loading | Coil (`io.coil-kt:coil-compose`) untuk load foto dari file lokal |

> **Kenapa osmdroid, bukan Google Maps SDK?** Karena rencananya app ini akan dirilis ke Play Store, osmdroid dipilih supaya **tidak ada API key, tidak ada akun Google Cloud, dan tidak ada resiko billing** berapa pun jumlah user-nya nanti. Google Maps SDK for Android sebenarnya gratis untuk pemakaian dasar (nampilkan peta), tapi tetap butuh setup API key + billing account di Google Cloud, dan ada resiko kalau produk lain di project kepakai tanpa sengaja. Data tile peta osmdroid tetap butuh koneksi internet untuk di-load (itu wajar, semua provider peta begitu), tapi **data kucing kamu (foto, deskripsi, koordinat) tetap 100% lokal**, tidak dikirim ke server manapun.

---

## 7. Permission yang Dibutuhkan

Di `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

Runtime permission request untuk **Camera** dan **Location** wajib di-handle (pakai `rememberLauncherForActivityResult` atau library seperti Accompanist Permissions).

---

## 8. Penyimpanan Foto

- Simpan foto di `context.filesDir` (internal storage app) → otomatis terhapus kalau app di-uninstall, aman & privat.
- Nama file disarankan: `cat_<timestamp>.jpg`
- Simpan **path**-nya saja di database, bukan bytes foto.

```kotlin
fun saveImageToInternalStorage(context: Context, bitmap: Bitmap): String {
    val filename = "cat_${System.currentTimeMillis()}.jpg"
    val file = File(context.filesDir, filename)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return file.absolutePath
}
```

---

## 9. Home Screen — Komponen

```
HomeScreen
 ├─ GoogleMap / OSMDroidMapView
 │    └─ Marker untuk setiap CatEntity (posisi = lat/lng)
 │        onClick → navigate ke DetailScreen(catId)
 ├─ FAB "+ Tambah Kucing" → navigate ke AddCatScreen
 └─ (opsional) Bottom sheet / list kecil kucing terbaru
```

State di `HomeViewModel`:
```kotlin
val cats: StateFlow<List<Cat>> = repository.getAllCats()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

---

## 10. Add Cat Screen — Alur Teknis

1. Minta permission kamera & lokasi (kalau belum ada).
2. Buka kamera → dapat `Bitmap` / `Uri` hasil foto.
3. Tampilkan preview foto + `OutlinedTextField` untuk deskripsi (opsional).
4. Tombol "Simpan":
   - Ambil lokasi lewat `FusedLocationProviderClient.getCurrentLocation()`.
   - Simpan foto ke internal storage → dapat `photoPath`.
   - Insert `CatEntity` baru ke Room.
   - Navigate back ke Home.

---

## 11. Roadmap Pengerjaan (Saran Urutan)

1. Setup project + dependencies (Room, Maps/osmdroid, Location, Coil, Navigation).
2. Buat data layer (Entity, DAO, Database, Repository).
3. Buat `LocationHelper` untuk ambil GPS.
4. Buat `AddCatScreen` (kamera + form + simpan).
5. Buat `HomeScreen` dengan peta + marker dari data Room.
6. Buat `CatDetailScreen`.
7. Tambah fitur hapus.
8. Polish UI (empty state, loading, permission denied state).
9. (Opsional) Fitur lanjutan dari bagian 3.

---

## 12. Catatan Privasi

Karena semua data lokal:
- Tidak perlu backend/API.
- Tidak perlu akun/login.
- Data hilang kalau app di-uninstall atau data app dihapus (kecuali dibuat fitur backup/export manual).
- Cocok untuk use case personal/hobi tanpa perlu mikirin server cost atau privasi data ke pihak ketiga.

---

## 13. Persiapan Rilis ke Play Store

Karena target akhirnya rilis publik, ada beberapa hal tambahan yang wajib disiapkan (di luar coding fitur):

| Kebutuhan | Keterangan |
|---|---|
| **Google Play Developer Account** | Biaya pendaftaran sekali bayar (bukan biaya rutin). Perlu identitas & pembayaran (kartu kredit/PayPal tergantung wilayah). |
| **Privacy Policy** | **Wajib** karena app minta izin Kamera dan Lokasi. Harus ada halaman/URL privacy policy yang jelas menyatakan bahwa data (foto, lokasi) disimpan lokal di perangkat dan tidak dikirim ke server manapun. Bisa dibuat gratis pakai generator privacy policy, atau ditulis manual & di-hosting di GitHub Pages/Google Sites. |
| **Data Safety form (Play Console)** | Play Store mewajibkan pengisian form "Data safety" yang menjelaskan data apa yang dikumpulkan app. Karena data 100% lokal dan tidak pernah keluar perangkat, ini justru jadi nilai plus — isi form dengan jujur bahwa data lokasi & foto **diproses di perangkat, tidak dikumpulkan/dibagikan ke pihak lain**. |
| **App Signing** | Play Store mewajibkan app di-sign. Gunakan Play App Signing (opsi default & direkomendasikan Google saat upload). |
| **Target SDK / API level** | Pastikan `targetSdkVersion` mengikuti minimum requirement Play Store terbaru saat rilis (selalu cek halaman requirement resmi Play Console sebelum submit, karena angkanya naik tiap tahun). |
| **Runtime permission rationale** | Sejak Android versi baru, kalau app minta izin Kamera/Lokasi, sebaiknya tampilkan penjelasan singkat kenapa izin itu dibutuhkan sebelum system dialog muncul (good practice, juga membantu approval review). |
| **Ikon, screenshot, deskripsi store listing** | Siapkan app icon (adaptive icon), minimal 2 screenshot, judul & deskripsi singkat untuk listing Play Store. |
| **Testing track** | Disarankan rilis dulu ke **Internal Testing** atau **Closed Testing** track sebelum ke Production, untuk pastikan tidak ada crash di device lain. |
| **Kebijakan lokasi Play Store** | Karena app pakai lokasi, Google kadang minta justifikasi tambahan (declaration form) khusus untuk permission lokasi saat submit — isi sesuai fungsi sebenarnya (menandai lokasi kucing di peta lokal). |

> Karena osmdroid dipakai (bukan Google Maps SDK), kamu **tidak perlu** mengurus Google Maps API key/billing sama sekali saat proses rilis — mengurangi satu langkah setup yang cukup ribet.

---

## 14. Arah Desain UI/UX — "Lucu & Playful"

Karena ini app kucing untuk hobi, desainnya sebaiknya jangan terasa seperti app enterprise/dashboard. Berikut arah konkret:

### 14.1 Konsep Visual

- **Vibe:** playful, hangat, sedikit "hand-drawn"/doodle — seperti stiker atau ilustrasi anak kucing di jurnal.
- **Bentuk:** banyak lengkungan (rounded corner besar, blob shape), hindari sudut tajam.
- **Karakter maskot:** satu ikon kucing sederhana (siluet/line-art) dipakai berulang sebagai elemen berulang — misal jadi cursor loading, jadi empty-state, jadi ikon FAB.

### 14.2 Palet Warna (contoh, bisa disesuaikan selera)

| Token | Hex | Peran |
|---|---|---|
| `CreamBg` | `#FFF8EE` | Background utama, hangat seperti susu |
| `PawBrown` | `#8B5E3C` | Warna teks utama / outline |
| `PeachAccent` | `#FFB27A` | Aksen utama (tombol, FAB, marker) |
| `MintPop` | `#8FD3C0` | Aksen sekunder (badge, highlight) |
| `BlushPink` | `#F6A6B2` | Detail kecil (hati, notif, dekorasi) |
| `InkSoft` | `#3A2E26` | Teks gelap (bukan hitam pekat, biar lembut) |

### 14.3 Tipografi

- **Display/heading:** font rounded & tebal, contoh **Fredoka**, **Baloo 2**, atau **Quicksand Bold** (semua tersedia gratis di Google Fonts, bisa dipasang sebagai font Compose).
- **Body text:** font rounded yang lebih netral seperti **Nunito** atau **Quicksand Regular**, biar tetap kebaca meski akun playful.
- Hindari font tegas/kaku (Roboto default, sans-serif kaku) karena akan terasa "app biasa".

### 14.4 Layout per Layar

**Home Screen**
```
┌─────────────────────────┐
│  "Kucing yang kutemui 🐾"│ ← heading playful, bukan "Home"
│ ┌──────────────────────┐ │
│ │                      │ │
│ │   PETA (osmdroid)    │ │ ← marker custom bentuk paw/cat face
│ │   rounded corner      │ │    besar, bukan pin default merah
│ │                      │ │
│ └──────────────────────┘ │
│  🐱 12 kucing ditemukan   │ ← counter kecil, playful microcopy
│                     ⊕    │ ← FAB besar bentuk paw/cat, warna PeachAccent
└─────────────────────────┘
```

**Add Cat Screen**
- Foto ditampilkan dalam frame membulat/polaroid-style (bukan kotak biasa).
- Text field deskripsi diberi placeholder yang ngobrol santai, misal: *"Kucingnya lagi ngapain nih? (opsional)"* — bukan "Enter description".
- Tombol simpan: *"Simpan & Tandain! 🐾"* — bukan "Submit"/"Save".

**Detail Screen**
- Foto full-width dengan corner radius besar.
- Info lokasi & tanggal ditampilkan sebagai "chip" kecil membulat, bukan teks polos.

### 14.5 Animasi & Micro-interaction (bisa native Compose, tanpa library tambahan)

Semua ini bisa dibuat pakai `androidx.compose.animation` bawaan Compose — tidak butuh Lottie kalau mau tetap ringan:

| Momen | Animasi | API Compose |
|---|---|---|
| Marker baru muncul di peta | Pop-in / scale bounce dari 0 → 1 dengan sedikit overshoot | `Animatable` + `spring(dampingRatio = Spring.DampingRatioMediumBouncy)` |
| Tombol FAB ditekan | Sedikit squish (scale down lalu balik) | `animateFloatAsState` di `onPress` |
| Transisi Add → Home setelah simpan | Foto "terbang" kecil ke arah lokasi di peta (opsional, agak advance) | `AnimatedVisibility` + shared element / custom offset animation |
| Loading saat ambil GPS | Kucing kecil "mondar-mandir" / ekor bergoyang | `rememberInfiniteTransition` untuk gerakan looping halus |
| Empty state (belum ada kucing) | Ilustrasi kucing tidur + teks *"Belum ada kucing yang ditandain, yuk jalan-jalan!"* dengan ekor yang goyang pelan | `rememberInfiniteTransition` (rotasi kecil ±5°) |
| Kartu/list item muncul | Fade + slide up ringan saat list pertama kali render | `AnimatedVisibility(enter = fadeIn() + slideInVertically())` |
| Swipe untuk hapus | Card "meleot" lalu menghilang, ikon paw kecil sebagai indikator delete | `SwipeToDismissBox` (Compose Material3) |

> **Catatan penting soal animasi (dari prinsip desain):** jangan taruh animasi di semua tempat sekaligus — pilih **1–2 momen paling berkesan** untuk dibuat benar-benar niat (misal: marker pop-in di peta, dan kucing loading saat ambil GPS), sisanya cukup transisi halus standar. Animasi yang berlebihan di semua elemen justru bikin app terasa berat & norak, bukan lucu.

### 14.6 Opsi Kalau Mau Lebih Jauh Lagi (opsional)

- **Lottie animation** (`com.airbnb.android:lottie-compose`): kalau mau animasi kucing yang lebih detail/ilustratif (misal kucing yang benar-benar "berjalan" dengan banyak frame), bisa pakai file Lottie JSON gratis dari [LottieFiles](https://lottiefiles.com) (cari kategori "cat"), lalu load di Compose. Ini nambah 1 dependency tapi hasilnya jauh lebih hidup.
- **Custom marker icon**: bikin beberapa varian ikon marker (misal beda warna berdasarkan waktu ditemukan: pagi/siang/malam) biar peta makin hidup.
- **Sound effect kecil** (opsional, easy to overdo): bunyi "meong" singkat saat berhasil simpan kucing baru — pakai `SoundPool`, volume kecil, dan tetap bisa dimatikan di setting kalau mau lebih matang.

### 14.7 Prinsip Desain Ringkas untuk App Ini

1. **Satu momen jadi bintang** — pilih 1 animasi paling niat (disarankan: marker pop-in di peta), yang lain cukup sederhana.
2. **Microcopy berkarakter** — semua teks di app (tombol, placeholder, empty state) ditulis santai & personal, bukan bahasa teknis default.
3. **Konsisten pada satu bahasa bentuk** — kalau pilih rounded/blob, pakai itu di semua tempat (card, button, image frame), jangan campur dengan kotak tajam.
4. **Warna terbatas** — cukup 4–6 warna inti di atas, jangan nambah warna baru tiap layar biar tetap kohesif.
