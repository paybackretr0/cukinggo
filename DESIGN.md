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

## Override yang perlu keputusan pemilik produk

- **R-11 (variasi border radius) vs spec 14.1:** spec minta semua elemen membulat tanpa sudut tajam, sedangkan R-11 melarang semua elemen berbentuk pill tanpa variasi radius.
  Status: **menunggu jawaban** (pertahankan sesuai spec, atau tambah variasi radius).
