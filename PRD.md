# PRD — Sistem Kesiapsiagaan & Navigasi Evakuasi Tsunami Kota Padang

**Status:** Draft v0.6
**Terakhir diperbarui:** 5 Agustus 2026 (Bagian 7A ditambahkan)
**Konteks:** GEMASTIK XIX 2026 — Divisi VIII (Pengembangan Perangkat Lunak)
**Tim:** Sheva (Backend) · Mikail (Frontend) · Habib (AI/Spatial)

> Dokumen ini adalah sumber kebenaran tunggal tentang **apa yang kita bangun dan apa yang tidak**. Setiap usulan fitur baru harus diuji terhadap dokumen ini. Jika bertentangan, dokumen ini yang menang — kecuali kita sepakat mengubahnya secara eksplisit.

---

## 1. Ringkasan Eksekutif

Sistem navigasi evakuasi tsunami berbasis posisi yang **berfungsi penuh tanpa koneksi internet**, ditujukan untuk warga dan pendatang di zona merah tsunami Kota Padang, dilengkapi modul simulasi pergerakan kolektif untuk BPBD.

**Satu kalimat pembeda:**
Penelitian dan aplikasi terdahulu mencari *rute terpendek untuk satu orang dengan asumsi ada internet*. Kami membangun sistem yang *bekerja tanpa internet* dan *memperhitungkan apa yang terjadi ketika ratusan ribu orang bergerak serentak*.

---

## 2. Latar Belakang & Urgensi

### 2.1 Data pendukung (terverifikasi)

| Fakta | Angka | Sumber |
|---|---|---|
| Golden time evakuasi Kota Padang | **20–30 menit** setelah gempa | ✅ **BPBD Kota Padang** (materi sosialisasi resmi Padang Sigap) |
| Golden time Kepulauan Mentawai | ~10 menit | BMKG Stasiun Geofisika Padang Panjang |
| Populasi zona merah Kota Padang | ±200.000 jiwa | Pemkot Padang |
| Cakupan wilayah rawan | 8 kecamatan, 55 kelurahan | Pemkot Padang |
| Blue Line Tsunami Safe Zone | 22 titik | BPBD Padang |
| Kriteria evakuasi vertikal mandiri | Bangunan **di atas 4 lantai** | ✅ BPBD Kota Padang |
| Waktu tunggu arahan di TES/TEA | 30 menit (dari RT/RW setempat) | ✅ BPBD Kota Padang |

### Prosedur Resmi BPBD yang Mengikat Desain Sistem

Materi sosialisasi resmi BPBD Kota Padang memuat instruksi yang **wajib dipatuhi** oleh sistem kami:

| Instruksi resmi | Konsekuensi terhadap desain |
|---|---|
| **"Jangan berlari melakukan evakuasi"** | Perhitungan waktu tempuh menggunakan **kecepatan jalan cepat**, bukan berlari. Seluruh teks antarmuka menggunakan istilah "jalan cepat" — **kata "lari" dilarang muncul** |
| **"Jangan melakukan evakuasi dengan kendaraan"** | Membenarkan penggunaan jaringan jalan pejalan kaki (`network_type="walk"`); data lalu lintas kendaraan tidak relevan |
| **"Jangan mendekati pantai"** | Perhitungan rute wajib menghindari arah laut |
| Tujuan evakuasi: **TES / TEA / Tsunami Safe Zone** | **Gunakan terminologi resmi ini**, bukan "shelter" |
| Bangunan >4 lantai → evakuasi di gedung sendiri | Parameter penyaringan kandidat TES dari data OSM: `building:levels >= 4` |
| Setelah tiba, tunggu arahan RT/RW selama 30 menit | Layar "Anda di zona aman" menampilkan instruksi menunggu, bukan menyarankan pulang |

> **Terminologi wajib:**
> **TES** = Tempat Evakuasi Sementara · **TEA** = Tempat Evakuasi Akhir · **Tsunami Safe Zone** = kawasan perkiraan aman, ditandai Blue Line

**Wilayah percontohan MVP:** Kecamatan **Padang Barat** — dipilih karena kepadatan tinggi, terletak di pesisir, dan merupakan pusat aktivitas kota sehingga banyak orang berada di luar kelurahan tempat tinggalnya (kasus penggunaan utama). Cakupan wilayah yang kecil juga menjaga ukuran paket data offline tetap ringan.

> **TODO:** verifikasi ulang seluruh angka di atas langsung ke BPBD saat audiensi. Jangan gunakan angka yang tidak bisa kita tunjukkan sumbernya.

### 2.2 Kondisi saat ini

Pemkot Padang telah menerbitkan peta rencana evakuasi per kelurahan (dapat diakses di `padang.go.id/padangsigap`) dalam **format gambar raster**, disosialisasikan melalui kelurahan dan drill tahunan.

### 2.3 Keterbatasan sistem peringatan berbasis ponsel

Data ini memperkuat argumen offline-first dan **dapat disitasi** (sumber: Google Research):

- Android Earthquake Alerts aktif di **98 negara** (akhir 2023), telah mendeteksi **>18.000 gempa** (M1.9–M7.8) dan mengirim **790 juta peringatan**
- Jangkauan sistem peringatan dini gempa global naik ~10x — dari ~250 juta orang (2019) menjadi **2,5 miliar orang**
- **Syarat teknis:** pengguna wajib memiliki koneksi Wi-Fi dan/atau data seluler, serta mengaktifkan pengaturan Earthquake Alerts dan lokasi

**Implikasi untuk kami:** sistem peringatan sebaik apa pun tetap **gagal saat koneksi lumpuh** — kondisi yang paling mungkin terjadi justru pada gempa besar. Selain itu, peringatan dini menjawab *"apakah akan terjadi guncangan"*, sementara pertanyaan yang menentukan keselamatan adalah *"dari posisi saya sekarang, ke mana saya bergerak, dan apakah masih sempat"*.

> **Catatan penulisan:** argumen ini sengaja disusun agar **tetap sahih walaupun Google mengaktifkan layanan di Indonesia besok**. Hindari menjadikan "Indonesia belum didukung" sebagai fondasi utama — status itu bisa berubah kapan saja. Untuk klaim ketersediaan di perangkat, gunakan bukti empiris tim sendiri (asumsi A-8), lengkap dengan tanggal pemeriksaan.

### 2.4 Celah yang kami serang
Peta statis per kelurahan **tidak dapat menjawab pertanyaan operasional saat darurat**:

1. **Masalah posisi** — peta disusun per kelurahan, sementara orang sering tidak berada di kelurahan tempat tinggalnya (di pasar, kampus, tempat kerja, jalan).
2. **Masalah orientasi** — membaca peta cetak dan mengorientasikan diri terhadapnya membutuhkan waktu dan ketenangan yang tidak tersedia saat panik.
3. **Masalah pendatang** — mahasiswa perantau dan wisatawan tidak mengikuti sosialisasi dan tidak tahu apakah lokasinya termasuk zona merah.
4. **Masalah kolektif** — rute terpendek individual tidak mempertimbangkan bahwa semua orang akan mengambil rute yang sama, menciptakan titik sumbat.

---

## 3. Analisis Karya Terdahulu

**Wajib dicantumkan di proposal.** Juri akan menemukan karya-karya ini dengan pencarian singkat; mengklaim kebaruan yang tidak ada akan merusak kredibilitas.

| Karya | Fokus | Keterbatasan yang kami isi |
|---|---|---|
| InaRISK Personal (BNPB) | Peta risiko & potensi bahaya berbasis GPS | Bukan navigasi rute personal; sangat bergantung koneksi (keluhan *connection timed out* di Play Store) |
| InaTEWS / InaEEWS (BMKG) | Peringatan dini gempa & tsunami | Kami **tidak menduplikasi ini** — kami mengonsumsi outputnya |
| Penelitian Dijkstra evakuasi Padang | Rute terpendek individual | Berbasis peta online; tidak menangani pergerakan kolektif |
| **SIG Evakuasi Padang Barat (Android)** | Pemetaan jalur & edukasi bencana | ⚠️ **Wilayah yang sama dengan MVP kami.** Wajib dibahas eksplisit di proposal. Pembeda kami: offline-first + simulasi pergerakan kolektif |
| Android Earthquake Alerts (Google) | Deteksi gempa via accelerometer ponsel, aktif di 98 negara | **Mensyaratkan koneksi internet aktif**; menjawab "apakah terjadi gempa", bukan "ke mana saya lari" |
| Aplikasi Info BMKG | Notifikasi gempa resmi | Informasi kejadian, bukan navigasi evakuasi |
| Analisis Network Analyst Padang | Peta jalur terdekat per kelurahan | Output statis (peta), bukan sistem interaktif |
| Aplikasi evakuasi BPBD Denpasar | Edukasi + pemetaan jalur (Flutter) | Wilayah berbeda; tidak menangani pergerakan kolektif |

**Posisi kami:** melengkapi ekosistem yang ada, bukan menggantikannya. Jalur yang kami tampilkan harus **konsisten dengan rencana evakuasi resmi Pemkot** agar tidak bertentangan dengan yang sudah dilatih warga saat drill.

---

## 4. Pengguna

### 4.1 Pengguna primer — Warga zona merah
- Tinggal atau beraktivitas di zona merah tsunami — MVP difokuskan pada Kecamatan Padang Barat
- Sudah pernah terpapar sosialisasi, tapi tidak selalu berada di rumah
- Kondisi pemakaian: panik, tergesa, mungkin gelap, mungkin tanpa kuota

### 4.2 Pengguna sekunder — Pendatang & mahasiswa perantau
- Tidak mengikuti drill, tidak tahu Blue Line
- Kebutuhan pertama bukan rute, tapi: *"apakah saya sedang di zona bahaya?"*

### 4.3 Pengguna institusional — BPBD Kota Padang
- Butuh alat perencanaan, bukan alat darurat
- Kebutuhan: identifikasi titik sumbat, evaluasi distribusi beban shelter, pengukuran drill

---

## 5. Prinsip Desain (tidak bisa ditawar)

1. **Offline-first, bukan offline-fallback.** Semua perhitungan inti dilakukan dari data lokal. Jaringan hanya meningkatkan kualitas, tidak pernah menjadi syarat berfungsi.
2. **Sistem harus jujur soal apa yang tidak diketahuinya.** Jika status tsunami belum terkonfirmasi, katakan demikian — jangan diam seolah normal.
3. **Tidak menciptakan perilaku berbahaya.** Setiap fitur diuji terhadap pertanyaan: *apakah ini bisa membuat orang menunda evakuasi atau berbalik arah?*
4. **Satu instruksi pada satu waktu.** Saat darurat, layar tidak boleh menyajikan pilihan.
5. **Tidak menduplikasi infrastruktur negara.** Kami mengonsumsi output BMKG/BNPB, tidak membangun sistem peringatan dini sendiri.

---

## 6. Ruang Lingkup

### 6.1 Termasuk (MVP GEMASTIK)

**Fitur inti — berfungsi 100% offline:**

| ID | Fitur | Deskripsi |
|---|---|---|
| F-01 | **Widget layar utama** | Menampilkan status zona lokasi saat ini, TES terdekat + estimasi waktu tempuh, dan tombol besar menuju panduan evakuasi. Berfungsi sebagai pintu akses cepat tanpa perlu mencari ikon aplikasi |
| F-02 | Penentuan status zona | Menentukan apakah posisi pengguna berada di zona merah |
| F-03 | **Penyajian rute evakuasi** | Mengambil rute prakomputasi dari basis data lokal berdasarkan persimpangan terdekat; 2–3 alternatif tersedia per titik asal (lihat Bagian 7A) |
| F-04 | Navigasi arah berbasis kompas | Panah besar mengikuti arah hadap pengguna — bukan peta yang harus diorientasikan sendiri |
| F-05 | Hitung mundur golden time | Estimasi sisa waktu vs estimasi waktu tempuh |
| F-06 | Deteksi tiba di zona aman | Berhenti memberi instruksi, beralih ke layar aman |
| F-07 | Rencana evakuasi keluarga | Disusun di fase tenang: tiap anggota tahu tujuan masing-masing dari lokasi rutinnya |

**Pemicu sistem — susunan berlapis:**

| Lapisan | Pemicu | Butuh jaringan | Peran |
|---|---|---|---|
| **Utama** | Pengguna membuka widget/aplikasi setelah merasakan guncangan | ❌ Tidak | Selalu tersedia, selaras dengan prosedur resmi BPBD |
| Pelengkap | Notifikasi status BMKG (F-08) | ✅ Ya | Mempercepat respons dan memberi konteks |

> Prosedur resmi BPBD menyatakan guncangan kuat di zona merah berarti evakuasi mandiri tanpa menunggu konfirmasi. Dengan demikian, **pemicu utama sistem adalah guncangan yang dirasakan pengguna sendiri** — bukan sensor maupun layanan eksternal. Sistem tidak bergantung pada notifikasi untuk dapat berfungsi.

**Fitur pelengkap — membutuhkan jaringan:**

| ID | Fitur | Deskripsi |
|---|---|---|
| F-08 | Konfirmasi status BMKG | Menarik status resmi gempa & potensi tsunami dari BMKG. **Perilaku mengikuti status resmi BMKG, bukan ambang magnitudo yang ditetapkan sendiri** (lihat tabel di bawah) |
| F-09 | Check-in aman | Menandai "saya selamat di shelter X" setelah tiba |
| F-10 | Status okupansi shelter | Dilaporkan petugas/pengguna yang sudah tiba |
| F-11 | Laporan jalur terhalang | Reruntuhan/banjir → jalur dihindari untuk pengguna lain |
| F-12 | Sinkronisasi data | Pembaruan shelter, jalur, peta rendaman |

**Perilaku sistem berdasarkan status BMKG (F-08):**

| Status dari BMKG | Perilaku aplikasi |
|---|---|
| Ada peringatan tsunami | Layar penuh, arahan evakuasi langsung |
| Gempa dirasakan, **tidak** berpotensi tsunami | Notifikasi informatif: *"Gempa M[x]. BMKG menyatakan tidak berpotensi tsunami."* → mencegah evakuasi yang tidak perlu |
| Tidak ada data (tanpa jaringan) | Widget tetap menampilkan status zona; pengguna membuka sendiri bila merasakan guncangan |

> **Alasan tidak menetapkan ambang magnitudo sendiri:** potensi tsunami ditentukan oleh kombinasi magnitudo, kedalaman, dan lokasi episentrum — bukan magnitudo semata. Gempa M6 dangkal di laut dekat Mentawai lebih berbahaya daripada M6,5 dalam di darat. Penetapan ambang sendiri berarti mengambil keputusan yang merupakan wewenang BMKG.
>
> **Catatan penting:** baris kedua bukan fitur sekunder. Mencegah evakuasi yang tidak perlu memiliki nilai keselamatan tersendiri, karena evakuasi massal yang keliru menimbulkan risiko kepanikan, kepadatan jalan, dan kecelakaan.

**Modul institusional:**

| ID | Fitur | Deskripsi |
|---|---|---|
| F-13 | Simulasi titik sumbat | Menghitung beban tiap ruas jalan jika seluruh warga zona merah bergerak serentak |
| F-14 | Analisis distribusi shelter | Mengidentifikasi shelter kelebihan/kekurangan beban |
| F-15 | Mode drill | Pencatatan waktu tempuh riil peserta saat Padang Tsunami Drill |

### 6.2 Tidak termasuk (dan alasannya)

| Yang ditolak | Alasan |
|---|---|
| Sistem peringatan dini gempa (EEWS) | Sudah ditangani BMKG (InaEEWS) & Google. Butuh jaringan seismometer fisik. GEMASTIK melarang hardware khusus. |
| **Deteksi guncangan otomatis berbasis accelerometer** | **Dipertimbangkan lalu ditolak.** Akselerometer perangkat tidak dapat membedakan guncangan gempa dari perangkat yang terjatuh, pengguna yang berlari, atau getaran kendaraan dengan tingkat keandalan yang memadai. Pada sistem keselamatan, kesalahan deteksi memiliki konsekuensi yang tidak dapat diterima: peringatan palsu berulang menyebabkan pengguna mengabaikan sistem, sementara kegagalan deteksi menciptakan rasa aman yang keliru. Digantikan oleh F-01 (widget) dengan pemicu utama berupa guncangan yang dirasakan pengguna sendiri. |
| Pelacakan lokasi keluarga real-time | **Risiko keselamatan.** Mendorong perilaku berbalik arah menjemput keluarga — penyebab korban terdokumentasi. Diganti dengan F-07. |
| Heatmap kepadatan real-time dari pengguna | Butuh massa kritis pengguna; data bias; efek penggiringan (mengalihkan semua orang ke jalur alternatif menciptakan kemacetan baru). |
| Integrasi data lalu lintas Google Maps | Data kendaraan bermotor, bukan pejalan kaki. Butuh jaringan. ToS melarang cache offline. Model dibangun dari pola harian normal, bukan evakuasi massal. |
| Modul logistik relawan pasca-bencana | Produk kedua yang berbeda (fase, pengguna, dan cara validasi berbeda). Masuk *future work*. |
| Prediksi tsunami / pemodelan gelombang | Di luar kompetensi tim dan ranah BMKG. |

---

## 7. Arsitektur Tiga Mode Konektivitas

| Mode | Kondisi | Yang aktif | Yang ditampilkan |
|---|---|---|---|
| **Penuh** | Internet normal | Semua fitur | Rute + status BMKG + okupansi shelter + jalur terhalang |
| **Hemat** | Sinyal lemah / kuota menipis | Rute lokal + tarik data teks minimal | Rute + status tsunami saja (payload beberapa byte) |
| **Mandiri** | Tanpa jaringan | Rute lokal murni | Rute + peringatan jujur: *"Status tsunami belum terkonfirmasi. Ikuti prosedur standar: guncangan kuat di zona merah berarti evakuasi."* |

**Aturan teknis wajib:** seluruh panggilan jaringan memiliki **timeout maksimum 2–3 detik**. Lewat batas itu, sistem langsung beralih ke mode berikutnya. Pengguna tidak boleh menunggu layar loading saat evakuasi.

---

## 7A. Strategi Routing — Prakomputasi Hibrida

> **Bagian ini bersifat mengikat bagi seluruh anggota tim.** Perbedaan pemahaman pada bagian ini sempat terjadi (5 Agustus) dan telah diselesaikan. Setiap jawaban kepada juri harus konsisten dengan bagian ini.

### Keputusan

Sistem menggunakan **prakomputasi hibrida**: seluruh rute dihitung di tahap prakomputasi, dan setiap titik asal menyimpan **beberapa alternatif tujuan**. Perangkat pengguna **tidak menjalankan algoritma pencarian rute** saat kejadian.

### Pembagian peran komputasi

| Tahap | Tempat | Yang dilakukan |
|---|---|---|
| **Prakomputasi** (sebelum bencana) | Perangkat pengembang | Pembangunan graf jalan; simulasi beban kolektif; perhitungan rute dari seluruh persimpangan ke TES/TEA dengan bobot yang telah disesuaikan; ekspor ke basis data ringan |
| **Runtime** (saat kejadian) | Perangkat pengguna | Membaca posisi GPS → mencari persimpangan terdekat → **mengambil** rute dari basis data lokal → menampilkan di peta |

**Konsekuensi yang harus dipahami bersama:**
- Perangkat pengguna **tidak** menyimpan graf jalan untuk keperluan perhitungan
- Perangkat pengguna **tidak** menjalankan Dijkstra, A*, maupun algoritma pencarian lintasan lainnya
- Yang dijalankan perangkat hanyalah pencarian basis data — waktu respons tidak bergantung pada spesifikasi perangkat

### Struktur data rute

Setiap persimpangan menyimpan **2–3 alternatif**, diurutkan berdasarkan prioritas, dan diupayakan menuju TES yang berbeda:

| Kolom | Isi |
|---|---|
| `id_persimpangan` | Pengenal titik asal |
| `prioritas` | 1 (utama), 2, 3 |
| `id_tujuan` | TES/TEA tujuan |
| `polyline` | Deretan koordinat jalur |
| `jarak_m`, `estimasi_detik` | Berdasarkan kecepatan **berjalan cepat**, bukan berlari |

### Penanganan kegagalan berjenjang

| Kondisi | Perilaku sistem |
|---|---|
| Rute prioritas 1 tersedia | Tampilkan rute utama |
| Rute prioritas 1 terputus | Beralih ke prioritas 2, lalu 3 — **tanpa perhitungan ulang** |
| Seluruh alternatif terputus | Tampilkan **arah dan jarak garis lurus** ke TES terdekat, disertai keterangan bahwa jalur tidak dapat diverifikasi dan pengingat menjauhi arah pantai |

> **Prinsip:** sistem tidak pernah menampilkan layar kosong atau pesan kegagalan rute.

### Keterbatasan yang wajib dinyatakan jujur

1. **Informasi jalur terputus (F-11) hanya berlaku dalam Mode Penuh dan Hemat.** Status blokir tersimpan di backend; dalam Mode Mandiri perangkat menggunakan data terakhir yang tersinkronisasi. Ini **tidak boleh** diklaim sebagai fitur yang selalu aktif.
2. **Sistem tidak dapat beradaptasi terhadap kondisi yang belum terwakili dalam data prakomputasi.** Pilihan ini diambil secara sadar: kepastian respons diutamakan di atas fleksibilitas.
3. **Pengguna tidak selalu berada tepat di persimpangan.** Sistem memilih persimpangan terdekat dan menampilkan penghubung dari posisi aktual.

### Alasan pemilihan (untuk proposal & tanya jawab)

- **Kecepatan respons.** Dengan rentang evakuasi 20–30 menit, waktu komputasi adalah waktu yang hilang dari waktu tempuh.
- **Kesetaraan antar perangkat.** Pengguna dengan ponsel lama memperoleh kecepatan yang sama dengan pengguna ponsel terbaru.
- **Kelayakan implementasi.** Beban kerja sisi Android berkurang secara signifikan, memungkinkan fokus pada kualitas antarmuka yang berbobot 20%.

> Perhitungan ulang di perangkat ditempatkan sebagai **pengembangan lanjutan**, bukan kekurangan yang disembunyikan.

---

## 8. Kemampuan Perangkat

| Komponen | Fungsi | Butuh jaringan |
|---|---|---|
| GPS/GNSS | Posisi pengguna | Tidak — sinyal satelit, tetap berfungsi meski seluruh BTS mati |
| ~~Accelerometer~~ | *Tidak digunakan — lihat bagian 6.2* | — |
| Magnetometer (kompas) | Arah hadap pengguna | Tidak |
| Penyimpanan lokal | Peta, jalur, shelter, graf jalan | Tidak |
| CPU | Pencarian basis data & rendering peta | Tidak |
| Koneksi data | Konfirmasi & sinkronisasi | Ya |

**Yang tidak dapat dilakukan perangkat — jangan pernah diklaim:**
- Mendeteksi gempa sebelum terjadi
- Membedakan guncangan gempa dari getaran lain secara andal melalui sensor perangkat
- Menentukan potensi tsunami tanpa data BMKG
- Mengetahui lokasi orang lain tanpa jaringan
- Mengetahui jalan yang runtuh tanpa laporan

---

## 9. Tech Stack

**Mobile (Mikail) — Android Native**
- Kotlin, Android SDK
- UI: XML Layout + View *(Jetpack Compose hanya jika waktu memungkinkan)*
- Peta offline: **MapLibre Native Android SDK**
- Format tile offline: MBTiles atau PMTiles
- Lokasi: `FusedLocationProviderClient` (Google Play Services Location)
- Sensor: `SensorManager` — magnetometer & rotation vector untuk arah kompas
- `AppWidgetProvider` untuk widget layar utama (F-01)
- Penyimpanan lokal: **Room** (di atas SQLite)
- Foreground Service untuk pemantauan sensor berkelanjutan
- **Pencarian rute dari basis data lokal** — bukan perhitungan algoritma pencarian lintasan (lihat Bagian 7A)

> **Justifikasi pemilihan native (untuk proposal & tanya jawab juri):** sistem membutuhkan akses sensor berkelanjutan dan kemampuan bereaksi otomatis saat guncangan terdeteksi — termasuk membuka layar evakuasi tanpa interaksi pengguna. Kontrol atas foreground service dan lifecycle aplikasi lebih langsung di Android native dibandingkan melalui lapisan abstraksi lintas platform.

**Backend (Sheva)**
- PostgreSQL + PostGIS
- FastAPI (Python)
- Peran: distribusi data awal, sinkronisasi, integrasi BMKG, status shelter, laporan jalur
- **Bukan** untuk perhitungan maupun penyediaan rute saat darurat — seluruh rute sudah tersimpan di perangkat

**Analisis Spasial & Simulasi (Habib)**
- Python: GeoPandas, NetworkX, Shapely
- QGIS untuk verifikasi visual
- Simulasi beban jaringan jalan & distribusi TES/TEA
- **Prakomputasi seluruh rute** dari setiap persimpangan (2–3 alternatif) → ekspor ke basis data lokal (lihat Bagian 7A)

**Sumber data**
- OpenStreetMap — jaringan jalan pejalan kaki, bangunan
- BNPB GIS Server (`gis.bnpb.go.id`) — layer `Arah_jalur_evakuasi`, `batas_administrasi`, `INARISKPOP_2020`, `global_tsunami_modelling`
- BPBD Kota Padang — lokasi & kapasitas TES/TEA, peta rendaman *(perlu audiensi)*. Kontak resmi: **(0751) 78775** (Pusdalops/BPBD) · **112** (Padang Command Center, bebas pulsa) · Jl. Bagindo Aziz Chan No. 1, Aie Pacah, Koto Tangah
- BPS — kepadatan penduduk per kelurahan
- BMKG — API status gempa & tsunami

> **Wajib GEMASTIK:** daftar seluruh komponen/library beserta lisensinya harus disusun sebagai dokumen pendukung. Mulai catat sejak sekarang, jangan di akhir.

---

## 10. Metrik Keberhasilan

### 10.1 Boleh diklaim (dapat kami ukur sendiri)

| Metrik | Cara pengukuran |
|---|---|
| Waktu komputasi rute sejak aplikasi dibuka | Pengukuran langsung di perangkat |
| Waktu pembaruan status zona pada widget | Pengukuran langsung di perangkat |
| Persentase warga yang mencapai TES/TEA dalam 20 menit **dengan kecepatan jalan cepat** | Output simulasi F-13 — asumsi kecepatan wajib mengikuti instruksi BPBD, bukan berlari |
| **Selisih hasil: rute berbobot kapasitas vs lintasan terpendek konvensional** | Jalankan dua perhitungan pada skenario identik, bandingkan persentase warga yang tercapai dalam 20 menit dan tingkat kelebihan beban TES. **Metrik ini wajib ada** — tanpanya klaim keunggulan pendekatan kolektif hanya bersifat argumentatif |
| Persentase populasi zona merah yang dapat mencapai zona aman dalam 20 menit | Output simulasi F-13 |
| Titik sumbat teridentifikasi | Output simulasi, divalidasi dengan pengamatan BPBD saat drill |
| Selisih waktu tempuh: rute sistem vs rute default terpendek | Perbandingan hasil simulasi |
| Ukuran paket data offline Padang Barat | Pengukuran langsung |
| Penilaian kegunaan oleh BPBD | Wawancara terstruktur |

### 10.2 Dilarang diklaim

- Jumlah nyawa yang diselamatkan
- Persentase pengurangan korban
- Klaim apa pun yang tidak dapat kami tunjukkan buktinya jika juri meminta

> **Pelajaran dari proyek sebelumnya:** angka akurasi 85–90% pada PERISAI adalah estimasi untuk keperluan pitching, bukan hasil pengukuran. Kesalahan ini tidak boleh terulang. Setiap angka dalam proposal harus memiliki bukti yang bisa ditunjukkan.

---

## 11. Asumsi yang Belum Tervalidasi

Diurutkan berdasarkan tingkat fatal jika ternyata salah.

| # | Asumsi | Cara validasi | Status |
|---|---|---|---|
| A-1 | Data shelter & jalur evakuasi tersedia dalam format vektor, atau dapat kami digitalkan | Audiensi BPBD | ⬜ Belum |
| A-2 | Jaringan jalan pejalan kaki di zona merah cukup lengkap di OpenStreetMap | Cek langsung data OSM | ⬜ Belum |
| A-3 | Aplikasi serupa belum pernah dicoba & gagal di Padang | Tanya BPBD | ⬜ Belum |
| A-4 | BPBD memiliki catatan penumpukan saat drill (untuk validasi F-13) | Audiensi BPBD | ⬜ Belum |
| A-5 | Perilaku "berbalik menjemput keluarga" memang menjadi kekhawatiran lapangan | Audiensi BPBD | ⬜ Belum |
| A-6 | Warga jarang membuka peta evakuasi digital yang tersedia sekarang | Audiensi BPBD + survei kecil | ⬜ Belum |
| A-7 | Ukuran paket data offline Padang Barat masih wajar (<100 MB) | Uji teknis mandiri | ⬜ Belum |
| A-8 | Fitur peringatan gempa bawaan Android tidak tersedia di wilayah Padang | Uji pada 3–5 perangkat **berbeda merek**, catat: merek/tipe, versi Android, hasil, screenshot, tanggal. Sertakan minimal satu perangkat dengan lokasi aktif & tanpa pembatasan baterai | 🟡 Sebagian — screenshot awal sudah ada, perlu diperluas ke beberapa merek |

**Aturan tim:** jangan investasi besar di fitur yang bergantung pada asumsi yang belum tervalidasi.

---

## 12. Batasan (untuk dicantumkan di proposal)

1. Aplikasi harus **sudah terpasang dan data terunduh sebelum bencana**. Mitigasi adopsi melalui Padang Tsunami Drill tahunan.
2. Cakupan MVP dibatasi pada **Kecamatan Padang Barat**, bukan seluruh Kota Padang. Perluasan wilayah menjadi pengembangan lanjutan.
3. MVP dikembangkan untuk **platform Android**; dukungan iOS masuk pengembangan lanjutan.
4. Sistem **bukan pengganti** peringatan dini resmi BMKG maupun arahan petugas di lapangan.
5. Sistem **tidak menjamin keselamatan**; merupakan alat bantu pengambilan keputusan.
6. Akurasi GPS di dalam bangunan atau lorong sempit dapat menurun.
7. Data okupansi shelter bergantung pada pelaporan manusia.

---

## 13. Risiko

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Data BPBD hanya tersedia dalam bentuk raster | Tinggi | Digitalisasi mandiri; jadikan sebagai kontribusi yang diklaim |
| Jaringan jalan OSM tidak lengkap di gang-gang kecil | Tinggi | Survei & kontribusi balik ke OSM; batasi cakupan |
| Ukuran data offline terlalu besar | Sedang | Batasi cakupan wilayah; kompresi tile; pilih level zoom seperlunya |
| Widget diabaikan/dihapus pengguna karena dianggap tidak berguna sehari-hari | Sedang | Widget menampilkan informasi yang bernilai dalam kondisi normal (status zona, TES terdekat), bukan sekadar tombol darurat |
| Kurva belajar Android native | Sedang | **Tenggat evaluasi 13 Agustus:** jika peta offline & posisi GPS belum tampil di perangkat, pertimbangkan ganti pendekatan. Gunakan XML Layout, bukan Compose, kecuali sudah nyaman |
| Ketergantungan berlebih pada AI dalam menulis kode | Sedang | Setiap solusi yang tidak dipahami wajib dipelajari sebelum dipakai — 45% nilai final berasal dari kemampuan menjawab juri, dan itu tidak bisa didelegasikan |
| Scope creep | **Tinggi** | Kunci dokumen ini. Usulan baru masuk *future work*, bukan MVP |
| Administrasi kampus terlambat | **Fatal** | Selesaikan sebelum 14 Agustus |

---

## 14. Timeline

| Tanggal | Milestone |
|---|---|
| 14 Agustus 2026 | **Batas pendaftaran PT & tim peserta** |
| 2 September 2026 | **Batas unggah proposal + video** (progres software minimal 50%) |
| Akhir Oktober 2026 | Pengumuman finalis |
| 26–30 Oktober 2026 | Daftar ulang finalis |
| 11–12 November 2026 | Presentasi & demo babak final |

### Rencana per minggu

**Minggu 1 (31 Jul – 6 Ags) — Fondasi & validasi asumsi**
- Mikail: urus administrasi kampus, cari dosen pembimbing, kontak BPBD
- Sheva: setup PostGIS + FastAPI; unduh & evaluasi kelengkapan data OSM zona merah
- Habib: eksplorasi layer BNPB GIS; uji kelayakan routing dengan NetworkX

**Minggu 2 (7–13 Ags) — Pipeline inti**
- Digitalisasi data shelter & jalur (jika perlu)
- Graf jalan pejalan kaki siap; routing lokal berjalan
- Prototipe peta offline di perangkat

**Minggu 3 (14–20 Ags) — Fitur pembeda**
- F-01 s.d. F-06 berfungsi end-to-end
- F-13 simulasi titik sumbat versi awal
- Mulai penulisan proposal (Latar Belakang, Tujuan, Batasan)

**Minggu 4 (21–27 Ags) — Validasi & video**
- Uji lapangan; demo ke BPBD
- Rekam video demo (maks 3 menit) — **wajib menampilkan mode pesawat**
- Proposal: Metodologi, Analisis Kebutuhan & Desain

**Minggu 5 (28 Ags – 2 Sep) — Finalisasi**
- Proposal: Implementasi, Screenshot, Dokumentasi Penggunaan
- Surat pernyataan orisinalitas *(karya baru — centang "belum pernah juara")*
- Daftar komponen & lisensi
- Review akhir: pastikan tidak ada klaim tanpa bukti
- **Submit**

---

## 15. Bobot Penilaian GEMASTIK

**Babak penyisihan:**

| Kriteria | Bobot | Di mana kita menjawabnya |
|---|---|---|
| Inovasi | 20% | Offline-first + simulasi pergerakan kolektif (F-13) |
| Dampak & sustainability | 20% | Validasi BPBD, mode drill (F-15), aset data yang bertahan |
| UI/UX & usability | 20% | Desain kondisi panik: satu instruksi, kompas, kontras tinggi |
| Metodologi SDLC | 20% | **Belum tergarap — perlu perhatian khusus** |
| Kesesuaian ide–produk | 10% | Scope terkunci & realistis |
| Urgensi | 10% | Golden time 20 menit, 200.000 jiwa |

**Babak final:** Presentasi & demo 45% · Menjawab pertanyaan juri 45% · Nilai penyisihan 10%

> Lolos final adalah tiket masuk; juara ditentukan ulang dari nol di ruang presentasi.

---

## 16. Pertanyaan Juri yang Harus Siap Dijawab

1. *"Pemkot sudah punya peta evakuasi resmi dan InaRISK sudah ada. Apa bedanya?"*
2. *"Saat gempa besar, jaringan mati. Bagaimana aplikasi Anda bekerja?"*
3. *"Aplikasi harus terpasang sebelum bencana. Bagaimana adopsinya?"*
4. *"Kenapa tidak ada fitur pelacakan keluarga?"* → jawaban keselamatan, bukan keterbatasan teknis
5. *"Sudah ada penelitian Dijkstra untuk evakuasi Padang. Apa kebaruan Anda?"*
6. *"Bagaimana jika jaringan lambat, bukan mati?"* → timeout agresif
7. *"Siapa yang akan mengelola sistem ini setelah kompetisi?"*
8. *"Bagaimana Anda memvalidasi bahwa rute yang dihasilkan benar-benar aman?"*
9. *"Kenapa tidak ada deteksi gempa otomatis? Bukankah itu akan lebih cepat?"* → jawaban keandalan, bukan keterbatasan teknis
10. *"Jika pemicunya adalah pengguna sendiri, apa gunanya aplikasi ini dibanding peta cetak?"* → jawaban: yang dijawab bukan "apakah ada gempa", tapi "ke mana dari posisi saya sekarang"
11. *"Kenapa rute dihitung sebelumnya, bukan saat dibutuhkan?"* → kecepatan respons + kesetaraan antar perangkat
12. *"Berarti sistem tidak bisa beradaptasi terhadap kondisi baru?"* → akui sebagai pilihan sadar, bukan kekurangan tersembunyi
13. *"Berapa ukuran data yang harus diunduh pengguna?"* → **wajib punya angka nyata**
14. *"Bagaimana Anda tahu rute Anda lebih baik?"* → **wajib punya angka pembanding** (lihat Bagian 10.1)
15. *"Jika semua pengguna diarahkan ke jalur alternatif, bukankah jalur itu yang jadi padat?"* → perhitungan iteratif; jika belum diterapkan, **jawab jujur** dan tempatkan sebagai pengembangan lanjutan

---

## 17. Future Work

- Modul logistik & distribusi bantuan pasca-bencana
- Integrasi dengan InaEEWS jika cakupan Padang tersedia
- Perluasan dari Padang Barat ke seluruh 8 kecamatan zona merah dan kota pesisir lain
- Mesh networking antar-perangkat untuk kondisi tanpa infrastruktur
- Dukungan multi-bahaya (banjir, longsor)

---

## Lampiran A — Aturan Tim

1. Setiap klaim dalam proposal harus memiliki bukti yang dapat ditunjukkan: angka hasil pengukuran, tangkapan layar pengujian, atau baris kode. **Tidak ada bukti → hapus klaim.**
2. Jalur offline dibangun **lebih dulu**. Membangun versi online terlebih dahulu akan mengunci arsitektur pada ketergantungan server.
3. Usulan fitur baru masuk ke bagian *Future Work*, bukan ke MVP — kecuali seluruh tim sepakat mengubah PRD ini.
4. Pencatatan lisensi library dilakukan sejak awal, bukan di minggu terakhir.
