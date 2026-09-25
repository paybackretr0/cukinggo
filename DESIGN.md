# DESIGN.md: CukingGo

> **Sumber arah:** `spesifikasi-app-penanda-kucing.md` bagian 14, ditulis oleh pemilik produk.
> Isi di bawah ini transkripsi dari spec itu, bukan arah baru karangan agent.
> Mode antislop yang dipilih: **DURING** (diterapkan sejak sesi ini).

## Design Read

Reading this as: app catatan cuking untuk pemakaian harian pecinta cuking (Android, offline-first),
dalam bahasa visual playful/hand-drawn ala jurnal stiker, dial **ENERGY 2 / RHYTHM 2 / MOTION 2**.

## Identitas (spec 14.1)

- Vibe: playful, hangat, sedikit hand-drawn/doodle seperti stiker atau ilustrasi anak cuking di jurnal.
- Bentuk: lengkungan besar (rounded corner, blob shape), tanpa sudut tajam.
- Maskot: satu ikon cuking sederhana dipakai berulang (FAB, empty state, marker peta).

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

- Home: heading "Cuking yang kutemui" lalu peta bersudut membulat, marker bentuk wajah cuking, counter kecil, FAB besar warna PeachAccent.
- Add Cat: foto dalam frame membulat ala polaroid, placeholder mengobrol ("Cukingnya lagi ngapain nih? (opsional)"), tombol "Simpan & Tandain!".
- Detail: foto full-width sudut besar, info lokasi & tanggal sebagai chip membulat, bukan teks polos.

## Motion (spec 14.5)

- **Satu momen jadi bintang:** marker cuking pop-in di peta (scale bounce dengan overshoot).
- Sisanya transisi halus standar: FAB squish saat ditekan, cuking mondar-mandir saat cari GPS, ekor cuking empty state bergoyang pelan, swipe untuk hapus.
- Spec melarang animasi di semua tempat sekaligus: pilih 1-2 momen, sisanya halus.

## Keputusan teknis dan alasan (R-31, satu baris per keputusan)

- **Marker foto saat zoom dekat, angka saat zoom jauh (ambang zoom 14):** wajah cuking terlihat saat dekat, dan marker tidak saling menabrak saat jauh.
- **Marker digambar di kode (Canvas), bukan file aset:** tidak butuh aset eksternal, tetap jalan offline, dan warna mengikuti palet.
- **Diameter marker 52dp:** cukup besar untuk mengenali wajah cuking di peta tanpa menutupi jalan.
- **Tombol "ke lokasi kamu" pakai FAB ukuran standar 56dp:** ini satu-satunya kontrol di peta, harus nyaman dijangkau jempol satu tangan.
- **Font dibundel sebagai .ttf di `res/font`:** 100% offline, tidak bergantung Play Services atau unduhan runtime.
- **Tanpa dynamic color:** identitas warna app tetap sama di setiap HP, sesuai palet yang sudah dipilih.
- **Foto cuking di internal storage (`filesDir`) + database Room:** privasi, tidak ada upload, otomatis terhapus saat uninstall.
- **Data cuking dikecualikan dari cloud backup:** klaim "100% lokal" di privacy policy tetap jujur.
- **Tema mengikuti pilihan pengguna di app (ikut sistem/terang/gelap):** palet malam memakai keluarga warna yang sama, bukan tema gelap alasan "terlihat tech".

## Widget layar utama (permintaan pemilik, sesi 2026-09-23)

Reading this as: kartu kecil yang menempel di layar utama, satu foto per kartu,
dengan bingkai lucu yang bergilir tiap hari, dial **ENERGY 2 / RHYTHM 2 / MOTION 0**.
Focal point tunggal tetap fotonya. Tidak ada animasi, karena layar utama bukan
panggung app ini, dan RemoteViews memang tidak punya animasi.

Dua varian, bentuknya sama persis, yang berbeda hanya cuking mana yang dipilih:

- **Cuking terakhir**: foto cuking yang paling baru ditandai.
- **Cuking hari ini**: satu cuking dari seluruh koleksi, bergilir satu langkah per hari.

Isi keduanya: satu foto, lalu satu baris teks di bawahnya (catatan pengguna apa
adanya; kalau belum ada catatan, satu baris lucu dari `widget_funny_lines`).
Tap membuka langsung detail cuking itu. Ukuran bawaan 3x2 dan boleh ditarik
sendiri antara 2x2 sampai 4x3, dan bingkainya bergilir tiap hari antar tiga bentuk
(lihat bagian "Bingkai widget bergilir tiap hari").

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Satu foto per kartu, bukan daftar atau grid:** di layar utama orang tidak menggulir; satu foto harus terbaca dalam sekali lihat.
- **Bingkai dengan margin, bukan foto di tepi kartu:** foto bersudut tegak di dalam bingkai membuatnya terbaca sebagai "foto ditempel", bukan sebagai sudut yang lupa dibulatkan. Untuk bentuk yang memang butuh foto berpotong bulat, pemotongannya dikerjakan di bitmap sebelum dikirim (lihat bagian bentuk hari) dan bukan oleh RemoteViews.
- **Foto dikirim sebagai URI `content://` + izin ke paket host, bukan bitmap:** foto resolusi penuh tanpa risiko melewati batas transaksi Binder 1 MB, karena bitmap ikut dikirim bersama seluruh RemoteViews. Bitmap kecil tetap disiapkan sebagai cadangan kalau paket host tidak ditemukan.
- **Layout XML klasik, bukan Jetpack Glance:** Glance hanya bisa font sistem (`FontFamily` cuma menerima nama string), sedangkan identitas app ini ada di Fredoka dan Nunito; memakai Glance juga menambah empat dependency baru untuk satu widget.
- **Radius bingkai berbeda per bentuk (20dp, 26dp, dan 28dp):** radius jadi salah satu penanda bentuk, jadi bentuk dengan hiasan paling sedikit justru memakai radius paling besar, supaya ketiganya tidak terasa kembar.
- **Garis tepi tebal/tipis/ tanpa garis, sesuai bentuknya, dan tidak pernah ada shadow:** widget tidak sedang melayang di atas permukaan app, dan shadow di atas wallpaper justru terbaca sebagai bayangan palsu. Alasan yang sama dipakai di `ui/theme/CardStyle.kt`.
- **Baris lucu dipilih dari id cuking, bukan acak:** teks di layar utama tidak boleh berubah setiap kali widget di-refresh.
- **Warna mengikuti pilihan tema di dalam app, bukan cuma setelan HP:** supaya widget tidak jadi satu-satunya tempat yang memakai tema berbeda dari app.
- **Tinggi foto memakai weight, bukan ukuran tetap:** host boleh memberi sel yang lebih tinggi; foto tetap proporsional tanpa dipatok angka.
- **`updatePeriodMillis` 0, jadi tidak ada alarm berkala dari sistem:** isi widget hanya berubah saat app menambah atau menghapus cuking, dan saat itu widget di-refresh dari repository. Satu-satunya alarm yang dipasang app ini untuk widget adalah alarm tengah malam milik rentetan harian (lihat bagian berikutnya).
- **Tidak ada file bitmap:** bingkai digambar sebagai shape dan layer-list, hiasannya sebagai vektor, dan pratinjaunya sebagai layout; yang dikirim ke launcher cuma bitmap foto.
- **Varian harian bergilir satu langkah per hari, bukan acak:** setiap cuking kebagian tampil dalam satu putaran, dan tidak ada cuking yang muncul dua hari berturut-turut. Kalau acak, cuking yang sama bisa muncul tiga hari beruntun dan widget terasa tidak pernah berganti.
- **Pergantian harian memakai alarm inexact jenis RTC, bukan alarm exact:** alarm exact butuh izin "Alarms & reminders" yang harus diminta ke pengguna, padahal gunanya cuma mengganti foto di layar utama. RTC (tanpa wakeup) juga tidak membangunkan HP tengah malam, jadi cukingnya berganti saat HP memang sedang dipakai. Ongkosnya: pengiriman boleh bergeser sampai sekitar satu jam, dan lebih lama lagi kalau HP sedang hemat baterai.
- **Alarm tengah malam dipakai bersama kedua varian, bukan milik varian harian saja:** sejak ada lencana rentetan, angka di sudut foto juga perlu dihitung ulang tiap hari. Kalau alarmnya cuma dipasang oleh varian harian, lencana di varian "cuking terakhir" bisa tetap menampilkan angka lama saat rentetannya sudah putus. Karena itu alarmnya dikelola di satu tempat (`widget/CatWidgets.kt`) dan dipasang ulang oleh kedua provider.
- **Dua varian memakai satu layout dan satu pratinjau pemilih:** yang membedakan keduanya hanya cuking mana yang tampil, jadi menggambar pratinjau kedua berarti punya dua versi desain yang bisa saling menyimpang. Di layar pemilih, bedanya dikenali dari label dan deskripsi.
- **RemoteViews disusun per instance, dan request code tap memakai id widget:** dua widget bisa menampilkan cuking yang berbeda, sedangkan PendingIntent dibedakan tanpa melihat isi extra. Kalau keduanya berbagi satu RemoteViews, tap-nya akan mendarat di cuking yang salah.

## Bingkai widget bergilir tiap hari (permintaan pemilik, sesi 2026-09-24)

Widgetnya diperbesar, dibuat bisa ditarik sendiri, dan bingkainya tidak lagi satu
bentuk: ada tiga bentuk lucu yang bergilir tiap hari. Konsep "polaroid cuking
terakhir" dari spec digantikan atas permintaan pemilik, jadi bentuk polaroid
tunggal yang lama sudah tidak dipakai.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Tiga bentuk, bukan satu:** permintaan pemiliknya memang widget yang tidak monoton, dan bentuk yang berganti adalah cara paling langsung untuk itu tanpa menambah varian widget baru di pemilih widget.
- **Bentuk dipilih dari tanggal, bukan acak, dan bukan dari id cuking:** kalau dari id, "cuking terakhir" bisa menampilkan bentuk yang sama berminggu-minggu, sedangkan kalau acak, widget berubah tiap kali digambar ulang dan dua widget yang terpasang bersamaan bisa berbeda tanpa alasan. Rotasi harian ini yang jadi bawaan, dan pemilik bisa memaksakan satu bentuk kalau mau (lihat bagian "Pilihan bentuk widget").
- **Tiga bentuknya: stiker, balon bicara, dan jendela:** ketiganya sudah dipakai di dunia nyata untuk "menempelkan" sesuatu (stiker tempel, balon ucapan, jendela), jadi hiasannya tidak perlu dijelaskan lagi, dan masing-masing punya penanda yang jelas: telinga, ekor, kumis.
- **Stiker pakai telinga yang mencuat di luar badan:** badannya dimasukkan 18dp dari tepi atas dan telinganya 18dp tinggi, jadi 12dp telinganya naik di atas garis badan sementara pangkalnya masih masuk 6dp ke dalam badan. Versi pertamanya cuma menyisakan pita 12dp dengan telinga 15dp, dan hasilnya terbaca sebagai dua segitiga yang digambar di dalam kartu, bukan telinga yang mencuat. Angka pitanya yang sekarang dipilih justru dari kekurangan itu.
- **Telinga dan ekor digambar sebagai jalur terbuka:** sisi bawah telinga dan sisi atas ekor sengaja tidak digariskan, jadi hiasannya menyatu dengan badan di bawahnya dan garis badannya hanya terputus selebar hiasan itu.
- **Foto bentuk jendela dipotong bulat lewat bitmap, bukan lewat URI:** RemoteViews tidak bisa memotong gambar, jadi foto itu dikirim sebagai bitmap kecil yang sudah dipotong bulat dan sudut luarnya diisi warna badan widget. Isi sudut dari kode itu yang membuat bitmap-nya tidak butuh saluran alpha, sehingga ukurannya separuh dan tetap aman dikirim bersama seluruh RemoteViews.
- **Kumis bentuk jendela digambar ke dalam bitmap itu, bukan jadi hiasan di layout:** hiasan layout punya jarak tetap dari tepi kartu, sedangkan lingkaran fotonya menyusut di tengah kartu dan ukurannya ikut berubah saat widget ditarik. Versi pertama memakai hiasan layout, dan di widget lebar kumisnya memang melayang jauh dari lingkarannya. Digambar dari sisi lingkaran, kumis selalu menempel di ukuran apa pun, dan ujungnya ikut terpotong lingkaran seperti kumis yang menempel di tepi kaca.
- **Tiap kumis digambar dua kali, garis tebal gelap lalu garis tipis terang:** isi fotonya tidak bisa diketahui saat menggambar, jadi satu warna kumis pasti hilang di salah satu foto (gelap atau terang); dua lapis itu membuatnya tetap terbaca tanpa membaca fotonya dulu.
- **Batas 448 px untuk foto bulat:** cukup tajam saat widget ditarik besar, dan bitmap RGB-nya sekitar 400 KB, jauh di bawah batas transaksi Binder, sedangkan bentuk lain tetap memakai URI supaya fotonya resolusi penuh.
- **Satu layout untuk ketiga bentuk, hiasannya tinggal disembunyikan:** tiga file layout yang isinya hampir sama akan cepat saling menyimpang, sedangkan yang berbeda antar bentuk cuma aset dan beberapa jarak.
- **Dua ImageView foto di dalam satu layout:** bentuk jendela butuh scaleType `fitCenter` supaya lingkarannya tidak jadi elips, sedangkan dua bentuk lain butuh `centerCrop`; scaleType hanya bisa dibaca saat inflate, jadi dua view itu solusi yang tidak butuh trik refleksi RemoteViews.
- **Ukuran naik ke 3x2 dan boleh ditarik 2x2 sampai 4x3:** 2x2 terlalu kecil untuk hiasan seperti telinga dan kumis, dan dengan weight foto tetap proporsional di ukuran berapa pun. Angka dp-nya mengikuti rumus resmi Android per sel (2 sel 110dp, 3 sel 180dp, 4 sel 250dp).
- **Pratinjau pemilih menampilkan bentuk stiker pada 180 x 110dp:** cuma satu bentuk yang bisa dijanjikan oleh pratinjau, sedangkan bentuk aslinya bergilir, jadi yang ditampilkan bentuk yang paling berbeda dari widget biasa.
- **Caption naik dari 11sp ke 12sp:** widgetnya kini lebih lebar, dan catatan pengguna adalah satu-satunya teks di sana, jadi ia boleh sedikit lebih besar.

Batas yang diketahui: bentuk-bentuknya belum pernah dilihat di launcher. Dua hal
paling berisiko adalah apakah telinga bentuk stiker sekarang benar-benar terbaca
mencuat (dan tidak menyatu dengan wallpaper yang sewarna), dan apakah kumis di
dalam foto bulat cukup terlihat di foto yang ramai. Kalau salah satu tidak seperti yang diharapkan, yang
perlu diubah cuma daftar bentuk di `widget/WidgetStyle.kt` beserta asetnya.

## Pilihan bentuk widget (permintaan pemilik, sesi 2026-09-24)

Rotasi bentuk harian bikin dua dari tiga bentuk tidak bisa dilihat tanpa menunggu,
jadi pemilik meminta pilihan bentuk di dalam app: **Otomatis**, **Stiker**,
**Balon bicara**, atau **Jendela bulat**.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Pilihannya digabung ke dialog tampilan yang sudah ada, bukan pill ketiga di header:** header Home sengaja hanya punya dua pill supaya judul di sebelahnya tidak makin sempit, dan pill ketiga akan memakan ruang yang sama tanpa menambah kemampuan baru.
- **Nama pill dan dialognya jadi "Tampilan", bukan "Tema":** isinya sekarang dua hal, tema app dan bentuk widget, jadi label lamanya akan menjanjikan sesuatu yang tidak lagi lengkap.
- **Otomatis tetap jadi bawaan:** rotasi harian itu yang bikin widget tidak monoton, sedangkan pilihan bebas dipakai kalau pemilik mau memeriksa atau menyukai satu bentuk tertentu; jadi keduanya tersedia tanpa mengubah perilaku pengguna lain.
- **Pilihan disimpan sebagai key mentah, dan key asing jatuh ke otomatis:** pola yang sama dengan pilihan tema, supaya layer penyimpanan tidak tahu enum widget dan nilai lama yang tidak dikenal tidak pernah bikin widget gagal digambar.
- **Kedua pilihan digabung jadi satu kelas preferensi (`DisplayPreferences`):** keduanya preferensi tampilan yang dibaca app dan widget, jadi tidak perlu dua file preferensi berbagi satu file SharedPreferences yang sama.
- **Widget digambar ulang begitu pilihan disimpan:** widget membaca preferensi saat digambar, jadi tanpa langkah ini pilihan baru terlihat hanya setelah ada hal lain yang memicu pembaruan, dan itu terasa seperti pilihannya tidak jalan. Kesempatan yang sama dipakai untuk tema: dulu tema baru ikut ke widget setelah pembaruan berikutnya, sekarang langsung.
- **Isi dialognya bisa digulir:** tujuh baris pilihan lebih tinggi dari dialog biasa, dan pilihan terakhir tidak boleh terpotong di layar pendek.

Batas yang diketahui: dialognya belum pernah dibuka di perangkat. Yang belum
dibuktikan: apakah tujuh baris terasa terlalu panjang, dan apakah widget benar-benar
ganti bentuk sesaat setelah pilihan disentuh.

## Rentetan harian / streak (permintaan pemilik, sesi 2026-09-23)

Reading this as: satu angka kecil yang bikin orang mau kembali besok, ditampilkan
di dua tempat dengan angka yang sama. Dial **ENERGY 1 / RHYTHM 0 / MOTION 0**: satu
elemen, satu angka, tanpa animasi dan tanpa nyala-nyala.

- **Chip di header Home**, warnanya PeachAccent dengan ikon api: "5 hari beruntun".
- **Lencana di sudut foto widget**: bingkai kecil berisi ikon api dan angkanya.
  Kalau rentetannya 0, lencananya hilang sama sekali.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Dihitung dari waktu catatan yang sudah ada, bukan dari angka yang disimpan lalu ditambah satu tiap hari:** kalau catatan cuking dihapus, atau jam HP bergeser, angkanya langsung ikut benar tanpa ada yang perlu memperbaikinya. Tidak ada keadaan yang bisa jadi tidak sinkron, karena tidak ada angka tersimpan yang bisa salah.
- **Kemarin masih dianggap rentetan yang hidup:** rentetan tidak boleh hilang cuma karena orang belum sempat keluar pagi-pagi. Rentetan baru dianggap putus kalau hari terakhir ada catatannya lebih lama dari kemarin.
- **Dua catatan di hari yang sama tetap dihitung satu hari:** yang dihitung hari, bukan jumlah cuking. Menandai tiga cuking dalam sehari tidak melompatkan angkanya.
- **Batas harinya ikut zona waktu setempat, bukan UTC:** pukul 23.30 di Jakarta sudah hari berikutnya menurut UTC; memakai UTC akan menghitungnya sebagai hari yang sama dan angkanya jadi salah untuk pengguna di Indonesia.
- **Catatan bertanggal masa depan diabaikan:** jam HP yang bergeser tidak boleh dipakai untuk memperpanjang rentetan, karena itu bukan hari yang benar-benar terjadi.
- **Tidak ada hukuman dan tidak ada notifikasi "rentetanmu mau putus":** ini alat menyenangkan, bukan alat menagih. Tidak ada suara, tidak ada badge merah, tidak ada kata-kata menyalahkan.
- **Kedua tempat memakai fungsi yang sama:** chip Home dihitung dari daftar cuking yang sudah ada di layar (tanpa query tambahan), lencana widget dihitung sekali per gambar lalu dipakai kedua varian widget. Jadi tidak mungkin keduanya menampilkan angka yang berbeda.
- **Lencana disembunyikan saat 0, bukan ditampilkan "0 hari":** angka nol di sudut foto terbaca sebagai kegagalan, padahal artinya cuma "belum mulai".
- **Home menghitung ulang saat `ON_RESUME`, bukan hanya saat data berubah:** kalau app dibiarkan terbuka lewat tengah malam, angka kemarin tidak boleh menempel di layar.
- **TalkBack membaca lencananya sebagai satu kalimat ("Rentetan 5 hari"), bukan angka sendirian:** angka di sudut foto tidak berarti apa-apa tanpa konteks.

## Bagikan cuking (permintaan pemilik, sesi 2026-09-23)

Satu tombol di halaman detail, warnanya PeachAccent seperti tombol utama app.
Isinya: foto cuking plus template chat berisi catatan, koordinat, dan link Google Maps.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Fotonya ikut dikirim, bukan cuma teks:** inti "share cuking" itu fotonya, dan foto dikirim lewat FileProvider yang sama dengan widget, jadi tidak ada permission storage dan tidak ada file yang disalin ke folder publik.
- **Link memakai format resmi Maps URLs (`/maps/search/?api=1&query=lat,lng`):** tidak butuh API key, dan terbuka di app Google Maps kalau ada atau di browser kalau tidak ada. Komanya di-encode jadi `%2C` sesuai permintaan dokumentasi, dan ada unit test yang menjaga bentuk ini.
- **Teksnya dari string resource, bukan ditempel di kode:** kalimatnya gampang diganti tanpa menyentuh logika intent-nya.
- **Kalimat template dan label tombolnya memakai kata "cuking":** pesannya dibaca orang di luar app, jadi bahasanya dibuat seperti orang bercerita ("Dia nongkrong di:"), bukan seperti label di layar.
- **Foto hilang bukan alasan gagal:** kalau filenya sudah tidak ada, yang dibagikan tinggal teksnya, bukan pesan error.

Batas yang diketahui: sebagian app chat membuang teks saat ada gambar terlampir, jadi link Maps-nya bisa hilang di app seperti itu. WhatsApp, Telegram, SMS, dan Gmail tetap menampilkan teksnya sebagai caption.

## Sebutan "cuking" di seluruh teks (permintaan pemilik, sesi 2026-09-24)

Semua teks yang dibaca pengguna, plus README dan DESIGN, memakai kata "cuking",
bukan "kucing".

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Yang diganti hanya teks yang dibaca pengguna, README, dan DESIGN:** komentar kode, nama unit test, dan dokumen sumber pemilik dibiarkan memakai "kucing", supaya perubahan ini tidak menyentuh apa yang tidak terlihat di layar app atau di dokumen ini.
- **Id channel notifikasi tetap `kucing_dekat`:** itu id internal, bukan teks yang dibaca pengguna, dan menggantinya membuat Android menganggapnya channel baru sehingga setelan notifikasi yang sudah dipilih pengguna (diamkan, ubah suara) ikut hilang.

Batas yang diketahui: nama berkas `spesifikasi-app-penanda-kucing.md` dan komentar di `app/lint.xml` yang mengutip teks lama tidak ikut diubah, jadi keduanya masih menyebut "kucing".

## Kabar kalau dekat cuking (permintaan pemilik, sesi 2026-09-23)

Fitur opsional yang memakai geofence Play Services: kalau kamu masuk radius 200 m
dari cuking yang pernah ditandai, muncul notifikasi. Ada satu pill lonceng di
header Home untuk menyalakan dan mematikannya.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Radius bisa dipilih di dialog (100 m / 200 m / 500 m), bawaannya 200 m:** yang paling ketat berhenti di 100 m karena dokumentasi Android menyarankan minimal segitu supaya akurasi lokasi lewat Wi-Fi tidak menghasilkan kabar palsu, dan tiga pilihan ini cukup untuk membedakan "harus lewat persis di depannya" sampai "sekitaran sini".
- **Satu baris alasan ditulis di bawah pemilih radiusnya:** pilihan radius itu tukar-menukar antara salah kabar dan sering keinget, jadi tiap pilihan menjelaskan akibatnya sendiri, bukan cuma angkanya.
- **Radius pilihan dan radius yang terpasang disimpan terpisah:** radius adalah bagian dari geofence-nya, bukan nilai yang bisa diubah di tempat, jadi penggantian radius harus terlihat sebagai "perlu dipasang ulang". Dua nilai itu yang dibandingkan, bukan ditebak.
- **Opt-in dan mati secara bawaan (pilihan pemilik):** fiturnya butuh izin lokasi latar belakang, dan itu izin yang pemakaiannya harus benar-benar kamu minta. Kalau tidak dinyalakan, app tidak membaca lokasi di latar belakang sama sekali.
- **Geofence dipakai, bukan polling lokasi sendiri:** pemantauan radius diserahkan ke Play Services, jadi app tidak perlu service yang jalan terus dan tidak ada baterai yang dipakai untuk melacak posisi.
- **Maksimal 100 cuking, diambil yang terbaru:** Play Services membatasi 100 geofence per app, dan aturan pengambilan ini ditulis sebagai fungsi murni supaya bisa diuji.
- **Jeda 12 jam per cuking:** tanpa jeda, cuking di dekat rumah akan mengabarkan setiap kali kamu masuk radius, dan itu jadi berisik. Dua belas jam berarti paling banyak dua kabar per hari untuk cuking yang sama.
- **Satu kabar per kejadian:** kalau beberapa radius terpicu bersamaan, yang dikabarkan cukup satu yang lolos masa jeda.
- **Pill lonceng berbentuk ikon saja:** supaya judul di sebelahnya tidak makin sempit dibanding waktu hanya ada pill Tampilan, dengan tinggi tetap 44dp supaya lolos tap target.
- **Nama opsi pengaturan diambil dari sistem (`getBackgroundPermissionOptionLabel()`):** sejak Android 11 izin "sepanjang waktu" hanya bisa diberikan lewat Pengaturan, jadi kalimatnya harus memakai nama opsi yang benar-benar tertulis di HP pengguna, bukan teks terjemahan sendiri.
- **Urutan izin dipaksa oleh kode (`nearbyAlertState`):** notifikasi, lalu lokasi biasa, baru lokasi latar belakang. Tombolnya selalu menunjuk langkah yang paling awal belum terpenuhi, jadi pengguna tidak perlu tahu urutannya, dan ada satu tombol untuk menolak seluruh fiturnya.

## Nama panggilan cuking (permintaan pemilik, sesi 2026-09-24)

Kolom nama ditambahkan di layar review foto, tepat di atas kolom catatan. Isinya
bebas dan tidak harus nama asli: pengguna diarahkan memakai ciri khasnya, misalnya
"si Kumis" atau "si Belang". Nama itu lalu dipakai di kartu daftar Home, layar
detail, template share, dan judul notifikasi kabar dekat. Ini tambahan di luar
bagian 14.4 spec, yang cuma menyebut kolom catatan.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Nama opsional, bukan wajib:** menandai cuking harus tetap bisa selesai dalam satu jepret, jadi kolom kosong tidak menahan tombol Simpan, dan semua tempat jatuh ke tampilan lama kalau namanya tidak ada.
- **Ajakan "nggak harus nama asli" jadi teks bantu di bawah kolom, bukan dialog:** pengguna perlu contoh sebelum bisa membayangkan isian yang benar, dan teks bantu selalu terlihat tanpa menambah ketukan.
- **Nama jadi judul di kartu Home, catatannya turun jadi satu baris kecil:** nama itu yang dipakai pengguna untuk mengingat cukingnya, jadi ia yang layak dibaca duluan, sedangkan catatannya tetap terlihat tanpa membuat kartu jadi gemuk.
- **Di layar detail nama ditaruh di dalam kartu catatan, bukan di top bar:** top bar memakai judul layar yang sama untuk semua catatan, sedangkan nama ini milik satu cuking.
- **Template share jadi empat varian (ada nama atau tidak, ada catatan atau tidak), bukan dirangkai di kode:** aturan di bagian bagikan sudah menetapkan kalimatnya tinggal di string resource, jadi percabangannya cuma memilih resource, bukan menempel potongan teks.
- **Notifikasi memakai nama sebagai judul, catatan tetap jadi isinya:** judul adalah baris pertama yang terbaca di bilah notifikasi, dan dengan nama di situ kabarnya bisa dikenali tanpa membuka app.
- **Widget tidak ikut memakai nama:** widget cuma punya satu baris caption di bawah foto, dan di sana catatan pengguna lebih berguna daripada mengulang nama, jadi perilaku widget dibiarkan seperti sebelumnya.
- **Kolom ditambah lewat migrasi Room 1 ke 2, bukan dengan menghapus database:** `fallbackToDestructiveMigration` akan membuang seluruh catatan pengguna saat update, sedangkan satu kolom null baru tidak butuh itu.
- **Kosong hanya punya satu arti, lewat `blankToNull`:** nama dan catatan yang isinya cuma spasi dianggap belum diisi, jadi UI cukup memeriksa null dan tidak perlu kasus "string kosong" yang berbeda di tiap layar.

Batas yang diketahui: `exportSchema = false`, jadi Room tidak memverifikasi SQL
migrasinya saat build, dan migrasinya belum pernah dijalankan di HP. Tampilan nama
di kartu, detail, dan notifikasi juga belum dilihat di perangkat.

## Ikon daftar Play Store (permintaan pemilik, sesi 2026-09-23)

Play Console tidak membaca adaptive icon milik app; yang diminta satu file PNG 512 x 512
persegi penuh, karena masking sudut dan bayangannya dipasang oleh Play sendiri. Untuk
itu ada `store/ic_launcher_512.png`, dan pembuatnya `tools/icon-export/`.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **PNG-nya digambar dari geometri drawable yang sama, bukan digambar ulang di aplikasi desain:** kalau nanti bentuk jejak kakinya diubah di `ic_launcher_foreground.xml`, ikon daftar Play tidak diam-diam jadi versi lama yang berbeda.
- **Digambar lewat kode `java.awt`, bukan diekspor manual:** hasilnya bisa dibuat ulang kapan saja tanpa Android Studio, dan angkanya bisa diperiksa: ukuran, jumlah piksel tembus pandang, lebar jejak kaki, dan apakah ia masih di dalam area aman.
- **Latar peach tetap penuh sampai tepi, tanpa sudut membulat dan tanpa bayangan:** halaman spesifikasi ikon Google Play meminta keduanya kosong karena Play yang memasangnya; menggambarnya sendiri di file justru menghasilkan sudut bergerigi ganda.
- **Bawaan memakai ukuran jejak kaki yang sama dengan ikon launcher (40% lebar ikon):** inilah bentuk yang sudah pemilik setujui, dan di daftar Play ia terlihat sama seperti di layar utama.
- **Varian kedua memperbesar jejak kaki sampai 55% (batas amannya 57%), bukan sebesar mungkin:** logo kecil di tengah bidang besar terasa hilang saat ditampilkan di daftar, tapi melewati area aman berarti sudutnya berisiko terpotong di perangkat yang masking-nya bulat. Batas itu dihitung dari bentuknya, bukan angka yang dipatok.
- **Tidak ada PNG yang ditanam ke `res/mipmap-*`:** `minSdk` 29, jadi semua perangkat yang disasar sudah mendukung adaptive icon. Menambah PNG di sana hanya menambah bobot APK tanpa pernah dipakai.

## Animasi setelah cuking tersimpan (permintaan pemilik, sesi 2026-09-24)

Layar "Tandai cuking baru" tidak langsung menutup begitu catatannya masuk. Ada
satu perayaan singkat: cukingnya melompat di tempat sambil hati kecil berhamburan
di sekitarnya, di dalam kartu yang menutupi form. Dial **ENERGY 2 / RHYTHM 2 /
MOTION 2**, sama dengan dial app.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Perayaannya di layar Add, bukan overlay di Home:** layar inilah yang punya keadaan "berhasil", jadi tidak perlu ada sinyal hasil simpan yang baru di antara dua layar, dan kalau pengguna ingin melihat lagi form yang baru diisi, layarnya masih ada di belakang.
- **Grafikanya digambar di kode (Canvas), dengan kosakata bentuk yang sama seperti cuking empty state:** konsisten dengan keputusan marker peta, tidak butuh aset baru, dan warnanya ikut palet. Bedanya cuma matanya melek, karena mata terpejam sudah dipakai untuk keadaan kosong.
- **Hati memakai BlushPink saja:** DESIGN.md memang menyediakan warna itu untuk hati dan dekorasi, jadi satu animasi pendek tidak perlu warna kedua.
- **Gerakannya cuma satu objek dan satu lingkaran hati:** spec melarang animasi di semua tempat sekaligus, dan dengan ini momen "jadi bintang" di app menjadi dua (marker peta dan perayaan ini), masih di dalam batas 1-2 momen. Tidak ada gerakan lain yang ditambahkan ke layar ini.
- **Layar ditahan 1,5 detik lalu lanjut sendiri, dan satu ketukan melewatinya:** perayaan yang wajib ditunggu mengubah momen menyenangkan jadi waktu tunggu, sedangkan perayaan yang wajib diketuk menambah satu ketukan ke alur satu jepret. Keduanya dihindari: bawaannya jalan sendiri, angkanya cuma batas paling lama.
- **Satu penanda `handedOff` dipakai bersama oleh ketukan dan batas waktu:** keduanya bisa selesai di saat yang hampir sama, dan dua kali `onSaved()` akan memundurkan dua layar sekaligus.
- **Layarnya diredupkan, bukan dikartukan penuh:** form yang baru diisi masih terlihat samar di belakang, jadi terasa seperti lapisan yang diletakkan di atas pekerjaan yang baru selesai, bukan layar baru.
- **Ada baris "Ketuk buat lanjut" di bawah pesannya:** ketukannya nyata (melewati perayaan), jadi ia harus terlihat dan bukan sesuatu yang kebetulan ditemukan.

Batas yang diketahui: perayaan ini belum pernah dilihat di perangkat. Yang belum
dibuktikan: apakah 1,5 detik terasa pas saat pengguna buru-buru menandai cuking
berikutnya, dan apakah hati kecilnya masih terbaca di layar kecil.

## Perayaan rentetan bertambah dan lambaian saat cuking dihapus (permintaan pemilik, sesi 2026-09-24)

Dua momen lain ikut dapat animasi, dengan dial yang sama (ENERGY 2 / RHYTHM 2 /
MOTION 2) dan kosakata gerak yang sudah ada: cuking yang sama, digambar dari
bentuk yang sama seperti perayaan simpan.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Rentetan dihitung sebelum dan sesudah menyimpan, bukan disimpan sebagai angka:** selisih keduanya yang menjawab "apakah rentetannya baru saja memanjang", dan aturannya tetap di satu tempat (`catStreak`) yang sudah dipakai chip Home dan lencana widget, jadi tidak ada angka tersimpan yang bisa berbeda dari yang tampil di dua tempat itu.
- **Keadaannya tetap satu (`Saved(streakDays)`), bukan dua keadaan "berhasil":** yang berbeda cuma pesan dan satu lencana, sedangkan gambar, gerakan, dan cara menutupnya sama persis. Dua keadaan akan jadi dua jalur kode yang bisa saling menyimpang.
- **Batas bawahnya dua hari (`streakToCelebrate`):** naik dari 0 ke 1 berarti rentetannya baru mulai, dan angka "1 hari beruntun" di layar justru terbaca seperti mengulang dari nol, bukan seperti rentetan yang bertambah.
- **Lencana api memakai `InfoChip` dan string `streak_chip` yang sama dengan chip di Home:** angkanya harus terbaca sebagai angka yang sama dengan yang nanti ditemukan di Home, jadi bentuk dan kalimatnya dipinjam, bukan ditulis ulang.
- **Lencananya melompat sekali dengan pegas, tidak berdenyut terus:** angkanya memang berita di versi ini, tapi lencana yang berdenyut terbaca seperti tombol yang minta ditekan. Lompatan kecil sekali itu juga yang membedakan versi ini dari perayaan biasa tanpa menambah kosakata gerak baru.
- **Lambaian saat menghapus nadanya lebih rendah, bukan perayaan hati:** yang baru terjadi bukan sesuatu untuk dirayakan, jadi tidak ada lompatan dan tidak ada hati; yang bergerak cuma satu kaki depan yang diputar dari bahunya, plus ekor yang bergoyang pelan.
- **Lambaiannya di layar Detail, dan isi layarnya dilepas selama melambai:** catatannya sudah terhapus saat itu, jadi kalau isinya dibiarkan, yang ada di belakang peredup justru keterangan "catatan sudah tidak ada" tepat saat cukingnya sedang melambai.
- **Penghapusannya jalan lebih dulu, layarnya baru menunggu pamitan:** menaruh animasi sebelum penghapusan berarti pengguna yang keluar dari app di tengah lambaian meninggalkan data yang sudah dijanjikan terhapus.
- **Kembalinya ke Home dipicu laporan lapisan data (`onDeleted`), bukan hitungan waktu:** layarnya tidak boleh menutup sebelum catatannya benar-benar hilang.
- **Kerangka kartunya (peredup, kartu, baris "Ketuk buat lanjut") dipakai bersama perayaan simpan:** cara menutup kedua momen ini harus sama, dan satu kerangka berarti perbaikan berikutnya cuma perlu sekali.
- **Semua adegan memakai satu fungsi gambar (`drawCat`):** tiga adegan (melompat, melambai, tidur di empty state) yang digambar sendiri-sendiri akan cepat saling menyimpang, padahal maksudnya cuking yang sama.

Batas yang diketahui: ketiga adegan ini belum pernah dilihat di perangkat, dan
lambaiannya cuma diperiksa dari angkanya. Kalau terlihat aneh, yang perlu diubah
cuma dua angka di `WavingCatScene` (sudut dan tempo lambaiannya).

## Halaman "Semua cuking" dengan paging, dan Home dibatasi lima (permintaan pemilik, sesi 2026-09-24)

Home sekarang menampilkan lima cuking terbaru saja, dan seluruh koleksinya pindah
ke halaman sendiri yang dibuka dari baris "Lihat semua cuking" di antara peta dan
daftar. Halaman itu membaca catatannya sehalaman demi sehalaman, bukan sekaligus.

Reading this as: daftar panjang koleksi pribadi untuk pemakaian harian, dalam
bahasa visual yang sama dengan Home, dial **ENERGY 2 / RHYTHM 2 / MOTION 1**.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Paging dari Room, bukan membaca semua lalu memotong di layar:** memotong daftar di Compose tetap membaca seluruh catatan ke memori lebih dulu, jadi ia tidak menyelesaikan hal yang diminta. Dengan PagingSource, halaman pertama yang dibaca cuma dua puluh baris.
- **`room-paging`, bukan PagingSource tulis sendiri:** PagingSource milik Room menandai dirinya basi saat isi tabelnya berubah, jadi catatan yang baru ditandai atau dihapus ikut hilang dari daftar tanpa ada yang perlu memberi tahu dari luar.
- **Dua puluh catatan per halaman, dan `initialLoadSize` disamakan dengannya:** cukup untuk mengisi layar tanpa membuat pemuatan pertamanya berat, dan sisanya menyusul sambil digulir. Angka bawaannya tiga halaman sekaligus, dan itu melebihi satu layar penuh untuk daftar yang isinya foto.
- **Tanpa placeholder (`enablePlaceholders = false`):** baris kosong sementara di daftar yang isinya foto terbaca seperti catatan yang gagal dimuat.
- **Daftarnya di-cache di scope ViewModel (`cachedIn`):** tanpa itu, menggulir jauh lalu memutar layar akan memuat ulang dari halaman pertama dan posisi bacaannya hilang.
- **Yang dibatasi di Home cuma daftar kartunya:** peta tetap menampilkan semua marker, dan chip rentetan serta kartu cuking terdekat tetap dihitung dari seluruh catatan. Membatasi seluruh halaman akan menyisakan lima titik di peta, padahal peta itulah isi utama layar itu.
- **Chip jumlah di Home menampilkan jumlah seluruh koleksi, bukan jumlah kartu di bawahnya:** lima dari dua puluh tiga yang ditulis "5 cuking ditemukan" justru angka yang salah.
- **Baris "Lihat semua" cuma muncul kalau memang masih ada sisa (lebih dari lima):** kalau koleksinya belum lebih dari lima, halaman daftarnya isinya sama persis dengan yang sudah ada di Home, jadi baris itu cuma menambah satu ketukan tanpa menambah apa pun.
- **Barisnya duduk di antara peta dan daftar, berwarna `surfaceVariant`:** di posisi itu ia terbaca sebagai pintu keluar dari lima kartu yang dipotong, dan warnanya sengaja lebih tenang daripada FAB supaya aksi utama layar ini tetap "Tandai cuking".
- **Angka di baris itu tidak diulang:** chip jumlah di bawahnya sudah menyebutkan angka yang sama, dan dua angka yang sama di satu layar cuma menambah bacaan.
- **Kartu di halaman daftar memakai `CatListCard` yang sama, termasuk swipe untuk menghapus:** kartu yang bisa di-swipe di Home tapi diam saja di halaman daftar akan terbaca sebagai kontrol yang rusak. Penghapusannya tidak diberi lambaian pamit, karena yang dipamitkan di layar Detail adalah sebuah layar yang menutup, sedangkan di sini kartunya memang sedang disapu keluar dari daftar.
- **Keadaan kosong dan keadaan gagal memakai kalimat Home yang sama:** yang terjadi sama persis, yaitu belum ada catatan atau catatannya belum kebaca, dan satu keadaan tidak perlu punya dua versi kalimat.
- **Penanda ujung daftar ("Udah semua, nih") muncul saat Paging bilang tidak ada halaman lagi:** tanpa itu, daftar yang berhenti karena sudah habis terlihat sama seperti daftar yang berhenti karena macet.
- **Chip jumlah disembunyikan selama angkanya belum kebaca:** angka 0 yang muncul sekejap terbaca sebagai "koleksimu kosong", padahal catatannya cuma belum selesai dihitung.
- **Gagal memuat halaman berikutnya tidak mengosongkan layar:** kartu yang sudah termuat tetap ada, dan yang muncul cuma satu baris dengan tombol coba lagi.

Batas yang diketahui: halaman ini belum pernah dibuka di perangkat, jadi yang belum
terbukti adalah apakah gulirannya terasa mulus saat halaman kedua menyusul, dan
apakah dua puluh kartu per halaman terasa pas saat koleksinya sudah banyak.
Dependency Paging 3 dan `room-paging` baru masuk di sesi ini, jadi ukuran APK-nya
sedikit bertambah.

## Transisi antar layar (permintaan pemilik, sesi 2026-09-24)

Tiap alur dapat gerak yang berbeda sesuai hubungan antar layarnya, dan fotonya
menyambung dari kartu daftar ke fotonya di layar detail.

Reading this as: perpindahan antar halaman di app catatan harian, dial
**ENERGY 2 / RHYTHM 2 / MOTION 2**.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Alur telusur (Home ke Detail, Home ke daftar lengkap, daftar ke Detail) pakai geser mendatar:** ketiganya berarti "masuk lebih dalam", dan geseran mendatar adalah cara Android menunjukkan arah itu. Karena kembalinya dibalik, tombol kembali dan gerakan kembali mengucapkan hal yang sama.
- **Layar kamera naik dari bawah, bukan digeser dari kanan:** menandai cuking bukan tempat yang ditelusuri, tapi satu tugas yang dikerjakan lalu ditinggalkan, dan gerakan dari bawah sudah dipakai untuk hal seperti itu.
- **Menuju kamera, Home tidak ikut bergeser, cuma memudar dan menyusut sedikit:** kameranya masuk dari bawah, jadi kalau Home ikut bergeser ke kiri, satu layar bergerak ke dua arah sekaligus dan terbaca seperti dua kejadian yang tidak berhubungan.
- **Durasi fade lebih pendek dari durasi geser (220 ms dan 300 ms):** fade yang sama panjang membuat layar sempat terlihat kosong di tengah jalan, sedangkan yang lebih pendek cuma memuluskan tepinya.
- **Layar kamera dikasih waktu lebih longgar (380 ms):** jarak yang ditempuhnya satu layar penuh, jadi tempo yang sama dengan geseran mendatar akan terasa menyentak.
- **Foto kartu menyambung jadi foto di layar detail:** yang berpindah cuma satu benda, jadi mata tidak perlu mencari lagi cuking mana yang barusan dibuka. Ini satu-satunya gerakan menyambung di app ini, supaya tidak jadi hiasan di mana-mana.
- **Cakupan animasi bersama disediakan lewat composition local, bukan parameter tiap layar:** yang dipakai layar cuma satu modifier, sedangkan meneruskan dua cakupan animasi berarti dua janji tambahan di setiap tanda tangan fungsi layar. Kalau cakupannya tidak ada, modifier itu diam saja.
- **Potongan membulat foto dipasang lewat helper yang sama dengan elemen bersamanya:** potongan yang diletakkan di luar elemen bersama tidak berlaku di perjalanannya, dan yang terlihat justru foto bersudut tegak saat terbang.
- **Karena kuncinya sama, foto yang sama juga menyambung antara daftar di Home dan halaman "Semua cuking":** keduanya menampilkan catatan yang sama, jadi kartunya memang lanjutan dari kartu yang sama, bukan dua benda berbeda yang kebetulan mirip.
- **Tap dari notifikasi atau widget tetap dianimasikan biasa:** saat itu layar Detail tidak punya pasangan foto di layar mana pun, dan elemen bersamanya cuma tidak menemukan pasangan, bukan gagal.
- **Peta di Home ikut dianimasikan, tanpa pengecualian:** peta memang isi utama layar itu, jadi mengeluarkannya dari animasi akan membuat peta terlihat "diam di tempat" sementara layarnya bergerak.

Batas yang diketahui: semua ini belum pernah dilihat di perangkat. Tiga yang paling
berisiko: peta di Home adalah AndroidView yang berat, jadi geserannya di HP lambat
bisa tersendat; radius foto berpindah dari 18dp ke 36dp saat serah terima di tengah
penerbangan (bentuknya berganti, bukan ikut dianimasikan); dan foto di layar detail
baru digambar setelah Coil selesai membacanya, jadi kalau cache fotonya kosong,
fotonya bisa menyusul beberapa bingkai setelah penerbangannya mulai.

## Gesture kembali ikut memakai transisi pop (permintaan pemilik, sesi 2026-09-24)

`android:enableOnBackInvokedCallback="true"` dipasang di `<application>`, jadi
gesture kembali sistem memakai transisi pop yang sama dengan tombol kembali, dan
layar sebelumnya terlihat menyusul di belakang jari.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Nilainya ditulis eksplisit walau di Android 16 sudah jadi bawaan:** untuk app yang menargetkan API 36, predictive back memang sudah aktif di HP Android 16, tapi di Android 13 sampai 15 atribut ini masih opt-in. Ditulis di manifest supaya perilakunya sama di ketiga versi itu, bukan cuma di HP terbaru.
- **Dipasang di `<application>`, bukan per activity:** app ini cuma punya satu activity, dan menaruhnya per activity berarti aturan yang sama ditulis di tempat yang harus dicari lebih dulu.
- **Tidak ada `BackHandler` atau `PredictiveBackHandler` yang ditambahkan:** NavHost sudah menangani gesture-nya sejak Navigation Compose 2.8, dan transisi pop yang sudah dipasang itulah yang dipakai untuk menyusuri gerak jarinya. Menambahkan handler sendiri justru akan menutupi transisi itu.
- **Dipasang setelah memastikan tidak ada `onBackPressed` atau `KeyEvent.KEYCODE_BACK` di kode:** keduanya tidak lagi dipanggil di Android 16 saat app menargetkan API 36, jadi memakainya akan langsung jadi bug. Pemeriksaan itu yang membuat atribut ini aman dinyalakan sekarang.
- **Dialog hapus tidak ikut diurus:** `AlertDialog` memakai `OnBackPressedDispatcher`, dan itu tetap dipanggil apa pun nilai atribut ini, jadi menutup dialog dengan back tetap jalan.

Batas yang diketahui: belum dicoba di perangkat. Gesture kembali cuma ada di mode
navigasi gesture; di mode tombol tiga, tombol kembali tetap memakai transisi pop
yang sama. Yang belum terbukti: apakah geseran mendatar di layar Home terasa mulus
saat jarinya menyusuri peta yang berat, dan apakah gesture kembali dari layar kamera
(transisi tegak) ikut terasa menyambung.

## Jejak perjalanan cuking di peta (permintaan pemilik, sesi 2026-09-25)

Peta sekarang menyambung catatan-catatan satu cuking jadi satu garis, jadi
terlihat kalau si cuking pindah dari tempat A ke tempat B. Dial app tetap dipakai
(**ENERGY 2 / RHYTHM 2 / MOTION 2**), dan tidak ada kosakata gerak baru.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Garisnya lurus antar catatan, bukan mengikuti jalan:** app ini offline-first (foto lokal, Room, osmdroid), jadi rute yang mengikuti jalan menuntut layanan routing pihak ketiga plus internet; selain itu yang tercatat memang cuma titik penemuannya, bukan rute perjalanannya, jadi garis lurus juga yang paling jujur. Dikonfirmasi pemilik lewat pilihan "garis lurus antar titik".
- **Dua konstanta `false` di `Polyline(mapView, false, false)` diperiksa langsung di bytecode AAR osmdroid 6.1.20, bukan ditebak:** yang kedua berarti tidak geodesik, dan itulah yang membuat ruasnya lurus; kalau salah, garisnya melengkung mengikuti permukaan bumi tanpa terlihat jelas salahnya di layar kecil.
- **Jejak hanya digambar di zoom dekat, sama seperti marker foto:** di zoom jauh penemuan sudah diganti gelembung angka, dan garis yang menghubungkan titik-titik yang tidak lagi terlihat justru menyesatkan.
- **Arah jejak dibaca dari kekuatan warna ruasnya, bukan dari panah atau nomor urut:** ruas tertua paling pudar dan ruas terbaru paling pekat, jadi tidak perlu marker tambahan yang menutupi foto cukingnya, dan tidak ada panah dekoratif yang harus dipertanggungjawabkan (R-08).
- **Warna jejak diambil dari id cuking dan tetap:** kalau warnanya diambil dari urutan di daftar, daftar yang berubah setiap ada catatan baru akan membuat jejak yang sama berpindah warna dan terbaca seperti jejak cuking lain.
- **Empat warna jejak dipekatkan dari palet dan disimpan sebagai angka di `MapTrails.kt`, bukan ditambahkan ke `ui/theme`:** tile peta selalu terang (tidak ikut mode gelap app), jadi pastel mentahnya seperti PeachAccent dan MintPop terlalu pudar di sana; karena warna di situ penanda data, ia tidak dipakai di permukaan UI mana pun dan tidak dihitung sebagai palet tema (R-29).
- **Catatan yang ketemu di tempat yang sama persis tidak menambah titik, dengan toleransi lima angka di belakang koma (sekitar satu meter):** dua pembacaan GPS di titik yang sama tidak pernah persis sama angkanya, dan tanpa toleransi itu jejaknya akan terbaca seperti cuking yang mondar-mandir di satu halaman.
- **Kembali ke tempat lama tetap dihitung sebagai gerakan:** yang dibuang hanya pengulangan yang berurutan, karena perjalanan A ke B lalu kembali ke A memang perjalanan.
- **Kartu petanya selalu ada di layar Detail, juga saat cukingnya baru ketemu di satu tempat (dikoreksi pemilik; awalnya kartunya cuma muncul kalau ada jejak):** batasan awal itu keliru karena menyamakan "ada jejak" dengan "ada yang berguna di peta". Koordinat di chip atas tidak menjelaskan dia di mana, sedangkan peta menjelaskannya sekilas, termasuk saat dibandingkan dengan posisi pengguna.
- **Judul dan keterangannya menyesuaikan: "Jejak di peta" saat ada garis, "Lokasinya di peta" saat tempatnya cuma satu, dan keterangan tentang garis yang makin terang hanya muncul kalau garisnya memang ada:** menyebut satu titik sebagai "jejak" menjanjikan sesuatu yang tidak ada, dan keterangan yang isinya menjelaskan ketiadaan lebih buruk daripada tidak ada keterangan.
- **Kartunya dipaskan ke bentang jejaknya (`fitToContent`), bukan berpusat di tempat terakhir ketemu:** justru jejak panjang yang perlu dilihat utuh, dan di kartu 220dp titik terakhir saja akan memotong sisa perjalanannya. Zoomnya dihitung dari bentang dengan rumus tile peta (`trailFocus`), lalu dibatasi 12 sampai 16 supaya jejak beberapa meter tidak di-zoom sampai petanya tidak terbaca lagi.
- **Pembingkaian pertama dipasang langsung (`setCenter`), bukan dianimasikan:** peta dibuat di titik bawaannya (Jakarta), jadi menganimasikannya berarti peta terbang dulu dari Jakarta ke lokasi cukingnya setiap kali layar dibuka, atau dibuka lagi setelah kembali dari layar lain. Animasi tetap dipakai untuk perubahan berikutnya, yaitu saat sorotan di Home berganti selagi petanya sudah terlihat, karena lompatan mendadak di situ terbaca seperti peta yang digambar ulang.
- **Tinggi kartunya 220dp:** cukup untuk membaca arah garisnya tanpa menggeser fokus halaman, yang tetap fotonya.
- **Shadow 6dp di kartunya sama dengan kartu peta di Home:** keduanya permukaan yang sama-sama "duduk di atas" halaman, jadi tingginya satu bahasa (R-12).
- **Tombol "ke lokasi kamu" disembunyikan di kartu ini (`showLocateButton`):** di kartu sempit ia memakan ruang dan menyesatkan fokus, karena yang dilihat jejak cukingnya, bukan posisi pengguna.
- **Menyentuh satu titik di kartu memilih catatan itu di riwayat, bukan membuka layar baru:** halaman ini memang sudah halaman cukingnya, jadi petanya berfungsi sebagai pemilih riwayat, sama seperti baris riwayat di bawahnya.
- **Tidak ada animasi baru:** MOTION 2 dipakai apa adanya lewat animasi pop-in marker yang sudah ada, karena garis jejak memang bukan sesuatu yang perlu masuk sendiri.

Batas yang diketahui: belum pernah dilihat di perangkat. Empat yang paling berisiko:
apakah empat warna jejak cukup terbedakan di atas gambar peta yang sibuk (warnanya
dipilih untuk kontras terhadap tile yang terang, tapi rasionya tidak diukur terhadap
piksel tile yang sesungguhnya, karena tile-nya gambar); apakah toleransi satu meter
terasa pas di lapangan, atau perlu dinaikkan kalau GPS-nya lebih berisik; apakah peta
di dalam `LazyColumn` layar Detail tidak berkedip saat digulir, karena item yang keluar
layar akan melepas lalu membuat ulang `MapView`-nya; dan apakah pembingkaian pertama
yang tidak lagi dianimasikan terasa lebih baik daripada terbangnya peta dari Jakarta
sebelumnya. Perubahan itu ikut menghapus animasi pembingkaian pertama di peta Home,
jadi kalau ternyata justru terasa kaku, yang perlu dikembalikan cuma satu cabang di
`CatMapView`: animasi khusus untuk peta yang dibingkai pertama kali.

## Pemilih cuking di peta Home (permintaan pemilik, sesi 2026-09-25)

Jejak satu cuking sekarang bisa disorot dari Home, tanpa membuka halaman
detailnya dulu. Dial tetap **ENERGY 2 / RHYTHM 2 / MOTION 2**, dan tidak ada
animasi baru.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Baris chip di atas peta, bukan dialog atau bottom sheet:** pilihannya sedikit dan hasilnya harus langsung terlihat di peta, sedangkan dialog justru menutupi peta yang jadi tempat melihat akibat pilihannya.
- **Yang bisa dipilih cuma cuking yang punya jejak (minimal dua tempat berbeda):** cuking dengan satu tempat tidak punya garis untuk disorot, dan kalau ia ikut masuk ke baris ini, memilihnya akan terbaca seperti tombol yang tidak bekerja (R-26).
- **Barisnya hilang sendiri saat yang bisa dipilih kurang dari dua:** dengan nol atau satu pilihan, baris itu tidak menambah kemampuan apa pun, jadi lebih jujur tidak ada daripada selalu memakan tinggi layar.
- **Menekan chip yang sedang menyala melepas sorotannya, di samping chip "Semua jejak":** melepas sorotan jadi satu ketukan dari keadaan terpilih, dan chip "Semua jejak" tetap tersedia sebagai penanda keadaan "tidak ada yang disorot".
- **Chipnya membawa foto cuking 26dp:** cuking sering belum dinamai, dan fotonya satu-satunya pembeda; karena itu chip berfoto dan chip "Semua jejak" (tanpa foto) tetap satu komponen yang sama supaya tingginya sejajar.
- **Labelnya nama panggilan, lalu catatan, lalu "tanpa nama":** memakai urutan yang sama dengan kartu di daftar, jadi cuking yang sudah dikenal di daftar terbaca dengan sebutan yang sama di sini.
- **Label dibatasi 160dp:** catatan adalah teks bebas, dan satu chip yang memanjang hampir selebar layar membuat sisa barisnya tidak terbaca, padahal gunanya membandingkan beberapa cuking sekaligus.
- **Urutan chip mengikuti urutan jejak, yaitu paling baru dilihat lebih dulu:** urutan itu sudah dipakai daftar di bawah peta, jadi tidak ada urutan kedua yang harus dipahami pengguna.
- **Warna "terpilih" meminjam `secondaryContainer`, sama dengan baris riwayat di layar Detail:** satu bahasa untuk "yang ini sedang dipamerkan", dan pasangan warnanya sudah dipakai di app ini sehingga kontrasnya bukan angka baru.
- **`selectable`, bukan `clickable`:** chip ini memilih, jadi TalkBack perlu membacakan mana yang sedang terpilih, sama seperti baris riwayat.
- **Tinggi chip minimal 44dp, dan barisnya digulir mendatar:** tap target (R-03), dan gulir mendatar berarti barisnya tidak bisa meluberkan lebar layar berapa pun jumlah cukingnya.
- **Jejak cuking lain diredupkan (alpha 0x33), bukan disembunyikan:** yang dilihat pengguna jadi "yang ini di antara yang lain", bukan satu jejak yang berdiri sendiri tanpa konteks.
- **Jejak terpilih dibuat 2dp lebih tebal, bukan dua kali lipat:** bedanya harus terbaca tanpa membuat lebar garis di layar terasa berganti bahasa.
- **Kamera diantar ke jejak yang dipilih (`trailFocus`):** menyorot jejak yang ada di luar layar tidak ada gunanya, dan ini memakai fungsi yang sama dengan pemaskaan di layar Detail.
- **Melepas sorotan tidak menggeser peta:** yang dilihat setelah itu adalah semua jejak di area yang sedang dilihat pengguna, sedangkan mengembalikan kamera ke tempat terakhir cuma membuat peta melompat tanpa dia minta.
- **Cuking yang dihapus otomatis melepas sorotannya:** kalau tidak, chip-nya hilang sementara peta tetap membingkai jejak yang sudah tidak ada.

Batas yang diketahui: belum pernah dilihat di perangkat, dan tidak ada unit test baru
untuk bagian ini karena seluruhnya hidup di dalam `AndroidView` (peta) dan di pohon
Compose; yang bisa dibuktikan tanpa perangkat cuma aturan "cuking mana yang punya
jejak", dan itu sudah diuji di `MapTrailsTest`. Tiga yang paling berisiko: apakah
empat warna jejak cukup terbedakan saat satu di antaranya disorot (yang lain
diredupkan tetapi tetap terlihat); apakah baris chip tetap nyaman saat cuking yang punya
jejak sudah belasan (belum ada batas jumlah, dan kalau ternyata terlalu panjang,
yang paling murah adalah membatasinya ke beberapa yang paling baru dilihat); dan
apakah foto di marker cuking lain juga perlu diredupkan saat sorotannya menyala,
karena untuk sekarang hanya garis jejaknya yang berubah.

## Jarak di kartu ulasan foto (laporan pemilik, sesi 2026-09-25)

Di halaman "Tandai ketemu lagi", chip "Masuk ke catatan cuking ..." dan field
kegiatan saling menempel. Dial tetap **ENERGY 2 / RHYTHM 2 / MOTION 2**.

Keputusan dan alasan (R-31, satu baris per keputusan):

- **Jarak diatur lewat `verticalArrangement = Arrangement.spacedBy(12.dp)` di kolomnya, bukan `Spacer` satu-satu:** isi kartu ini bercabang (nama untuk cuking baru, chip untuk penemuan baru), jadi jarak yang dihitung per elemen selalu kelewat satu setiap kali ada isi baru; ini persis yang terjadi pada chip dan field kegiatan.
- **`Spacer` setelah foto dihapus, bukan dibiarkan:** kalau dibiarkan, jarak setelah foto jadi 24dp sementara jarak lain 12dp, yaitu masalah baru yang kelihatan.
- **Angkanya 12dp, sama dengan padding kartunya:** jarak antarisi jadi sebentuk dengan jarak isi ke tepi kartu, jadi tidak ada dua irama jarak di satu kartu.

Batas yang diketahui: belum pernah dilihat di perangkat, dan tidak ada unit test
untuk jarak Compose. Yang membuktikan tanpa perangkat cuma perubahan satu tempat
(lebar kolom tidak berubah, jadi tidak ada risiko layout baru).

## Override yang perlu keputusan pemilik produk

- **R-11 (variasi border radius) vs spec 14.1:** spec minta semua elemen membulat tanpa sudut tajam, sedangkan R-11 melarang semua elemen berbentuk pill tanpa variasi radius.
  Status: **menunggu jawaban** (pertahankan sesuai spec, atau tambah variasi radius).
