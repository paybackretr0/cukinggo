# DESIGN.md: CukingGo

> **Sumber arah:** `spesifikasi-app-penanda-kucing.md` bagian 14, ditulis oleh pemilik produk.
> Isi di bawah ini transkripsi dari spec itu, bukan arah baru karangan agent.
> Mode antislop yang dipilih: **DURING** (diterapkan sejak sesi ini).

## Design Read

Reading this as: app catatan kucing untuk pemakaian harian pecinta kucing (Android, offline-first),
dalam bahasa visual playful/hand-drawn ala jurnal stiker, dial **ENERGY 2 / RHYTHM 2 / MOTION 2**.

## Identitas (spec 14.1)

- Vibe: playful, hangat, sedikit hand-drawn/doodle seperti stiker atau ilustrasi anak kucing di jurnal.
- Bentuk: lengkungan besar (rounded corner, blob shape), tanpa sudut tajam.
- Maskot: satu ikon kucing sederhana dipakai berulang (FAB, empty state, marker peta).

## Palet (spec 14.2)

| Token | Hex | Peran |
|---|---|---|
| CreamBg | #FFF8EE | Background utama, hangat seperti susu |
| PawBrown | #8B5E3C | Warna teks utama / outline |
| PeachAccent | #FFB27A | Aksen utama (tombol, FAB, marker) |
| MintPop | #8FD3C0 | Aksen sekunder (badge, highlight, titik lokasi user) |
| BlushPink | #F6A6B2 | Detail kecil (hati, notif, dekorasi) |
| InkSoft | #3A2E26 | Teks gelap, bukan hitam pekat |

## Tipografi (spec 14.3)

- Display/heading: **Fredoka** (rounded & tebal).
- Body: **Nunito** (rounded, tetap enak dibaca).
- Dihindari: font tegas/kaku (Roboto default) karena terasa seperti app biasa.

## Layout per layar (spec 14.4)

- Home: heading "Kucing yang kutemui" lalu peta bersudut membulat, marker bentuk wajah kucing, counter kecil, FAB besar warna PeachAccent.
- Add Cat: foto dalam frame membulat ala polaroid, placeholder mengobrol ("Kucingnya lagi ngapain nih? (opsional)"), tombol "Simpan & Tandain!".
- Detail: foto full-width sudut besar, info lokasi & tanggal sebagai chip membulat, bukan teks polos.

## Motion (spec 14.5)

- **Satu momen jadi bintang:** marker kucing pop-in di peta (scale bounce dengan overshoot).
- Sisanya transisi halus standar: FAB squish saat ditekan, kucing mondar-mandir saat cari GPS, ekor kucing empty state bergoyang pelan, swipe untuk hapus.
- Spec melarang animasi di semua tempat sekaligus: pilih 1-2 momen, sisanya halus.

## Keputusan teknis dan alasan (R-31, satu baris per keputusan)

- **Marker foto saat zoom dekat, angka saat zoom jauh (ambang zoom 14):** wajah kucing terlihat saat dekat, dan marker tidak saling menabrak saat jauh.
- **Marker digambar di kode (Canvas), bukan file aset:** tidak butuh aset eksternal, tetap jalan offline, dan warna mengikuti palet.
- **Diameter marker 52dp:** cukup besar untuk mengenali wajah kucing di peta tanpa menutupi jalan.
- **Tombol "ke lokasi kamu" pakai FAB ukuran standar 56dp:** ini satu-satunya kontrol di peta, harus nyaman dijangkau jempol satu tangan.
- **Font dibundel sebagai .ttf di `res/font`:** 100% offline, tidak bergantung Play Services atau unduhan runtime.
- **Tanpa dynamic color:** identitas warna app tetap sama di setiap HP, sesuai palet yang sudah dipilih.
- **Foto kucing di internal storage (`filesDir`) + database Room:** privasi, tidak ada upload, otomatis terhapus saat uninstall.
- **Data kucing dikecualikan dari cloud backup:** klaim "100% lokal" di privacy policy tetap jujur.
- **Tema mengikuti pilihan pengguna di app (ikut sistem/terang/gelap):** palet malam memakai keluarga warna yang sama, bukan tema gelap alasan "terlihat tech".

## Widget layar utama (permintaan pemilik, sesi 2026-09-23)

Reading this as: polaroid kecil yang menempel di layar utama, satu foto per kartu,
dial **ENERGY 2 / RHYTHM 1 / MOTION 0**. Focal point tunggal: fotonya. Tidak ada
animasi, karena layar utama bukan panggung app ini.

Dua varian, bentuknya sama persis, yang berbeda hanya kucing mana yang dipilih:

- **Kucing terakhir**: foto kucing yang paling baru ditandai.
- **Kucing hari ini**: satu kucing dari seluruh koleksi, bergilir satu langkah per hari.

Isi keduanya: satu foto, lalu satu baris teks di bawahnya (catatan pengguna apa
adanya; kalau belum ada catatan, satu baris lucu dari `widget_funny_lines`).
Tap membuka langsung detail kucing itu. Ukuran 2x2, mengikuti konsep "polaroid
kucing terakhir" yang pemilik pilih.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Satu polaroid, bukan daftar atau grid:** di layar utama orang tidak menggulir; satu foto harus terbaca dalam sekali lihat.
- **Bentuk polaroid (foto bersudut tegak di dalam bingkai membulat):** RemoteViews tidak bisa memotong gambar mengikuti radius, jadi foto bulat seperti di dalam app tidak mungkin; bingkai dengan margin membuat sudut tegak itu terbaca sebagai "foto ditempel", bukan sebagai sudut yang lupa dibulatkan.
- **Foto dikirim sebagai URI `content://` + izin ke paket host, bukan bitmap:** foto resolusi penuh tanpa risiko melewati batas transaksi Binder 1 MB, karena bitmap ikut dikirim bersama seluruh RemoteViews. Bitmap kecil tetap disiapkan sebagai cadangan kalau paket host tidak ditemukan.
- **Layout XML klasik, bukan Jetpack Glance:** Glance hanya bisa font sistem (`FontFamily` cuma menerima nama string), sedangkan identitas app ini ada di Fredoka dan Nunito; memakai Glance juga menambah empat dependency baru untuk satu widget.
- **Radius bingkai 18dp:** widget hanya selebar 2 sel, jadi radius besar (28-36dp) akan memakan bidang foto.
- **Garis tepi tipis, tanpa shadow:** widget tidak sedang melayang di atas permukaan app, dan shadow di atas wallpaper justru terbaca sebagai bayangan palsu. Alasan yang sama dipakai di `ui/theme/CardStyle.kt`.
- **Baris lucu dipilih dari id kucing, bukan acak:** teks di layar utama tidak boleh berubah setiap kali widget di-refresh.
- **Warna mengikuti pilihan tema di dalam app, bukan cuma setelan HP:** supaya widget tidak jadi satu-satunya tempat yang memakai tema berbeda dari app.
- **Tinggi foto memakai weight, bukan ukuran tetap:** host boleh memberi sel yang lebih tinggi; foto tetap proporsional tanpa dipatok angka.
- **`updatePeriodMillis` 0, jadi tidak ada alarm berkala dari sistem:** isi widget hanya berubah saat app menambah atau menghapus kucing, dan saat itu widget di-refresh dari repository. Satu-satunya alarm yang dipasang app ini untuk widget adalah alarm tengah malam milik rentetan harian (lihat bagian berikutnya).
- **Tidak ada aset baru:** bingkai, jejak kaki, dan pratinjau digambar sebagai vektor di paket app, bukan file bitmap.
- **Varian harian bergilir satu langkah per hari, bukan acak:** setiap kucing kebagian tampil dalam satu putaran, dan tidak ada kucing yang muncul dua hari berturut-turut. Kalau acak, kucing yang sama bisa muncul tiga hari beruntun dan widget terasa tidak pernah berganti.
- **Pergantian harian memakai alarm inexact jenis RTC, bukan alarm exact:** alarm exact butuh izin "Alarms & reminders" yang harus diminta ke pengguna, padahal gunanya cuma mengganti foto di layar utama. RTC (tanpa wakeup) juga tidak membangunkan HP tengah malam, jadi kucingnya berganti saat HP memang sedang dipakai. Ongkosnya: pengiriman boleh bergeser sampai sekitar satu jam, dan lebih lama lagi kalau HP sedang hemat baterai.
- **Alarm tengah malam dipakai bersama kedua varian, bukan milik varian harian saja:** sejak ada lencana rentetan, angka di sudut foto juga perlu dihitung ulang tiap hari. Kalau alarmnya cuma dipasang oleh varian harian, lencana di varian "kucing terakhir" bisa tetap menampilkan angka lama saat rentetannya sudah putus. Karena itu alarmnya dikelola di satu tempat (`widget/CatWidgets.kt`) dan dipasang ulang oleh kedua provider.
- **Dua varian memakai satu layout dan satu pratinjau pemilih:** yang membedakan keduanya hanya kucing mana yang tampil, jadi menggambar pratinjau kedua berarti punya dua versi desain yang bisa saling menyimpang. Di layar pemilih, bedanya dikenali dari label dan deskripsi.
- **RemoteViews disusun per instance, dan request code tap memakai id widget:** dua widget bisa menampilkan kucing yang berbeda, sedangkan PendingIntent dibedakan tanpa melihat isi extra. Kalau keduanya berbagi satu RemoteViews, tap-nya akan mendarat di kucing yang salah.

## Rentetan harian / streak (permintaan pemilik, sesi 2026-09-23)

Reading this as: satu angka kecil yang bikin orang mau kembali besok, ditampilkan
di dua tempat dengan angka yang sama. Dial **ENERGY 1 / RHYTHM 0 / MOTION 0**: satu
elemen, satu angka, tanpa animasi dan tanpa nyala-nyala.

- **Chip di header Home**, warnanya PeachAccent dengan ikon api: "5 hari beruntun".
- **Lencana di sudut foto widget**: bingkai kecil berisi ikon api dan angkanya.
  Kalau rentetannya 0, lencananya hilang sama sekali.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Dihitung dari waktu catatan yang sudah ada, bukan dari angka yang disimpan lalu ditambah satu tiap hari:** kalau catatan kucing dihapus, atau jam HP bergeser, angkanya langsung ikut benar tanpa ada yang perlu memperbaikinya. Tidak ada keadaan yang bisa jadi tidak sinkron, karena tidak ada angka tersimpan yang bisa salah.
- **Kemarin masih dianggap rentetan yang hidup:** rentetan tidak boleh hilang cuma karena orang belum sempat keluar pagi-pagi. Rentetan baru dianggap putus kalau hari terakhir ada catatannya lebih lama dari kemarin.
- **Dua catatan di hari yang sama tetap dihitung satu hari:** yang dihitung hari, bukan jumlah kucing. Menandai tiga kucing dalam sehari tidak melompatkan angkanya.
- **Batas harinya ikut zona waktu setempat, bukan UTC:** pukul 23.30 di Jakarta sudah hari berikutnya menurut UTC; memakai UTC akan menghitungnya sebagai hari yang sama dan angkanya jadi salah untuk pengguna di Indonesia.
- **Catatan bertanggal masa depan diabaikan:** jam HP yang bergeser tidak boleh dipakai untuk memperpanjang rentetan, karena itu bukan hari yang benar-benar terjadi.
- **Tidak ada hukuman dan tidak ada notifikasi "rentetanmu mau putus":** ini alat menyenangkan, bukan alat menagih. Tidak ada suara, tidak ada badge merah, tidak ada kata-kata menyalahkan.
- **Kedua tempat memakai fungsi yang sama:** chip Home dihitung dari daftar kucing yang sudah ada di layar (tanpa query tambahan), lencana widget dihitung sekali per gambar lalu dipakai kedua varian widget. Jadi tidak mungkin keduanya menampilkan angka yang berbeda.
- **Lencana disembunyikan saat 0, bukan ditampilkan "0 hari":** angka nol di sudut foto terbaca sebagai kegagalan, padahal artinya cuma "belum mulai".
- **Home menghitung ulang saat `ON_RESUME`, bukan hanya saat data berubah:** kalau app dibiarkan terbuka lewat tengah malam, angka kemarin tidak boleh menempel di layar.
- **TalkBack membaca lencananya sebagai satu kalimat ("Rentetan 5 hari"), bukan angka sendirian:** angka di sudut foto tidak berarti apa-apa tanpa konteks.

## Bagikan kucing (permintaan pemilik, sesi 2026-09-23)

Satu tombol di halaman detail, warnanya PeachAccent seperti tombol utama app.
Isinya: foto kucing plus template chat berisi catatan, koordinat, dan link Google Maps.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Fotonya ikut dikirim, bukan cuma teks:** inti "share cuking" itu fotonya, dan foto dikirim lewat FileProvider yang sama dengan widget, jadi tidak ada permission storage dan tidak ada file yang disalin ke folder publik.
- **Link memakai format resmi Maps URLs (`/maps/search/?api=1&query=lat,lng`):** tidak butuh API key, dan terbuka di app Google Maps kalau ada atau di browser kalau tidak ada. Komanya di-encode jadi `%2C` sesuai permintaan dokumentasi, dan ada unit test yang menjaga bentuk ini.
- **Teksnya dari string resource, bukan ditempel di kode:** kalimatnya gampang diganti tanpa menyentuh logika intent-nya.
- **Foto hilang bukan alasan gagal:** kalau filenya sudah tidak ada, yang dibagikan tinggal teksnya, bukan pesan error.

Batas yang diketahui: sebagian app chat membuang teks saat ada gambar terlampir, jadi link Maps-nya bisa hilang di app seperti itu. WhatsApp, Telegram, SMS, dan Gmail tetap menampilkan teksnya sebagai caption.

## Kabar kalau dekat kucing (permintaan pemilik, sesi 2026-09-23)

Fitur opsional yang memakai geofence Play Services: kalau kamu masuk radius 200 m
dari kucing yang pernah ditandai, muncul notifikasi. Ada satu pill lonceng di
header Home untuk menyalakan dan mematikannya.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Radius bisa dipilih di dialog (100 m / 200 m / 500 m), bawaannya 200 m:** yang paling ketat berhenti di 100 m karena dokumentasi Android menyarankan minimal segitu supaya akurasi lokasi lewat Wi-Fi tidak menghasilkan kabar palsu, dan tiga pilihan ini cukup untuk membedakan "harus lewat persis di depannya" sampai "sekitaran sini".
- **Satu baris alasan ditulis di bawah pemilih radiusnya:** pilihan radius itu tukar-menukar antara salah kabar dan sering keinget, jadi tiap pilihan menjelaskan akibatnya sendiri, bukan cuma angkanya.
- **Radius pilihan dan radius yang terpasang disimpan terpisah:** radius adalah bagian dari geofence-nya, bukan nilai yang bisa diubah di tempat, jadi penggantian radius harus terlihat sebagai "perlu dipasang ulang". Dua nilai itu yang dibandingkan, bukan ditebak.
- **Opt-in dan mati secara bawaan (pilihan pemilik):** fiturnya butuh izin lokasi latar belakang, dan itu izin yang pemakaiannya harus benar-benar kamu minta. Kalau tidak dinyalakan, app tidak membaca lokasi di latar belakang sama sekali.
- **Geofence dipakai, bukan polling lokasi sendiri:** pemantauan radius diserahkan ke Play Services, jadi app tidak perlu service yang jalan terus dan tidak ada baterai yang dipakai untuk melacak posisi.
- **Maksimal 100 kucing, diambil yang terbaru:** Play Services membatasi 100 geofence per app, dan aturan pengambilan ini ditulis sebagai fungsi murni supaya bisa diuji.
- **Jeda 12 jam per kucing:** tanpa jeda, kucing di dekat rumah akan mengabarkan setiap kali kamu masuk radius, dan itu jadi berisik. Dua belas jam berarti paling banyak dua kabar per hari untuk kucing yang sama.
- **Satu kabar per kejadian:** kalau beberapa radius terpicu bersamaan, yang dikabarkan cukup satu yang lolos masa jeda.
- **Pill lonceng berbentuk ikon saja:** supaya judul di sebelahnya tidak makin sempit dibanding waktu hanya ada pill Tema, dengan tinggi tetap 44dp supaya lolos tap target.
- **Nama opsi pengaturan diambil dari sistem (`getBackgroundPermissionOptionLabel()`):** sejak Android 11 izin "sepanjang waktu" hanya bisa diberikan lewat Pengaturan, jadi kalimatnya harus memakai nama opsi yang benar-benar tertulis di HP pengguna, bukan teks terjemahan sendiri.
- **Urutan izin dipaksa oleh kode (`nearbyAlertState`):** notifikasi, lalu lokasi biasa, baru lokasi latar belakang. Tombolnya selalu menunjuk langkah yang paling awal belum terpenuhi, jadi pengguna tidak perlu tahu urutannya, dan ada satu tombol untuk menolak seluruh fiturnya.

## Override yang perlu keputusan pemilik produk

- **R-11 (variasi border radius) vs spec 14.1:** spec minta semua elemen membulat tanpa sudut tajam, sedangkan R-11 melarang semua elemen berbentuk pill tanpa variasi radius.
  Status: **menunggu jawaban** (pertahankan sesuai spec, atau tambah variasi radius).
