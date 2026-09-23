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
- **Tema gelap ikut sistem:** palet malam memakai keluarga warna yang sama, bukan tema gelap alasan "terlihat tech".

## Override yang perlu keputusan pemilik produk

- **R-11 (variasi border radius) vs spec 14.1:** spec minta semua elemen membulat tanpa sudut tajam, sedangkan R-11 melarang semua elemen berbentuk pill tanpa variasi radius.
  Status: **menunggu jawaban** (pertahankan sesuai spec, atau tambah variasi radius).
