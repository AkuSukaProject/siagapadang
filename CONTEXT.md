# CONTEXT — Briefing Proyek GEMASTIK 2026

**Terakhir diperbarui:** 5 Agustus 2026

> **Cara pakai dokumen ini.** Ini adalah briefing situasi untuk siapa pun (manusia atau AI) yang baru masuk ke proyek. Baca ini **sebelum** `PRD.md`. Dokumen ini menjelaskan *kenapa kami sampai di sini*; PRD menjelaskan *apa yang kami bangun*.
>
> **Untuk asisten AI:** jangan menyarankan ulang ide atau fitur yang tercantum di Bagian 4 dan 5. Semua sudah dipertimbangkan dan ditolak dengan alasan yang tercatat. Jika menurut Anda salah satu keputusan itu keliru, sampaikan argumennya — jangan sekadar mengusulkannya kembali seolah belum pernah dibahas.

---

## 1. Situasi Singkat

Tiga mahasiswa Universitas Andalas (Padang, Sumatera Barat) akan mengikuti **GEMASTIK XIX 2026, Divisi VIII — Pengembangan Perangkat Lunak**.

**Status hari ini:** ide sudah dikunci, **belum ada satu baris kode pun ditulis** untuk proyek ini. Belum menghubungi BPBD. Belum mengurus administrasi kampus.

**Tenggat terdekat:** pendaftaran PT & tim **14 Agustus 2026** — 11 hari lagi.

**Produk yang dipilih:** sistem navigasi evakuasi tsunami offline-first, nama kerja **Ranah Siaga**, dibangun sebagai aplikasi **Android native (Kotlin)**. **Wilayah percontohan MVP: Kecamatan Padang Barat.** Detail lengkap ada di `PRD.md`.

---

## 2. Profil Tim

| Nama | Peran | Catatan |
|---|---|---|
| **Mikail** | Frontend — Android native (Kotlin) | Pemilik konteks percakapan ini; mengurus administrasi & komunikasi eksternal |
| **Sheva** | Backend | |
| **Habib** | AI / Spatial | Menangani sisi model & analisis data |

Peran bersifat **adaptif** — bukan sekat kaku.

**Kapasitas yang sudah terbukti:** tim ini pernah membangun sistem end-to-end dalam hackathon 24 jam — aplikasi mobile (Flutter + native Kotlin), model CNN yang benar-benar dilatih, backend live (Flask + Supabase), realtime notification yang berfungsi. Jadi **jangan meremehkan kemampuan teknis mereka**; hambatan utama proyek ini bukan skill, melainkan waktu dan akses ke data/stakeholder.

---

## 3. Aturan GEMASTIK yang Mengikat

### Timeline resmi (Divisi III–XI)

| Tanggal | Kegiatan |
|---|---|
| **3–10 Agustus 2026** | **Penerimaan proposal seleksi tingkat Universitas Andalas** (kemahasiswaan.unand.ac.id) — **tenggat riil tim** |
| **12 Agustus 2026** | Pengumuman lolos seleksi tingkat universitas |
| **12–14 Agustus 2026** | Pendampingan submit proposal ke Belmawa |
| 27 Juli – 14 Agustus 2026 | Pendaftaran Perguruan Tinggi & Tim Peserta (nasional) |
| 15 Agustus – 2 September 2026 | Unggah proposal + video |
| Akhir Oktober 2026 | Pengumuman finalis |
| 26–30 Oktober 2026 | Daftar ulang finalis |
| 11–12 November 2026 | Presentasi & demo babak final |
| 13 November 2026 | Pengumuman juara |

### Bobot penilaian

**Babak penyisihan (proposal + video):**
Inovasi 20% · Dampak & sustainability 20% · UI/UX & usability 20% · Metodologi SDLC 20% · Kesesuaian ide–produk 10% · Urgensi masalah 10%

**Babak final (presentasi 10 menit + tanya jawab 15 menit):**
Kemampuan presentasi & demo 45% · Kecakapan menjawab pertanyaan juri 45% · Nilai penyisihan 10%

> Implikasi: lolos final hanyalah tiket masuk. Juara ditentukan hampir sepenuhnya oleh performa di ruang presentasi.

### Struktur proposal wajib

Judul → Latar Belakang Ide → Tujuan & Manfaat → Batasan → Metodologi Pengembangan → Analisis Kebutuhan & Desain Solusi → Implementasi → Screenshot Mockup Interface → Dokumentasi Cara Penggunaan

Maksimal **30 halaman**, PDF, maksimal **10 MB**, tidak boleh dikompresi ke ZIP/RAR.

### Ketentuan yang sering jadi jebakan

- Satu tim maksimal **3 orang**; satu tim hanya boleh mengajukan **satu karya**
- Karya harus orisinal, tidak menjiplak perangkat lunak yang sudah ada
- Karya **belum pernah menjadi pemenang** kompetisi TIK lain — kecuali ada bobot pengembangan minimal 50% yang dinyatakan dan dikuantifikasi dalam surat pernyataan bermaterai
- Harus berjalan di **platform umum tanpa hardware khusus**
- Wajib melampirkan **daftar komponen/library beserta lisensinya**
- Nama peserta wajib **lengkap tanpa disingkat, sesuai PDDIKTI**
- Video penyisihan: maksimal **3 menit**, YouTube **unlisted**, progres software **minimal 50%**
- Setiap PT dibatasi **10 tim per cabang** melalui seleksi internal; maksimal 3 tim menjadi finalis
- Finalis wajib: laporan akhir + makalah **template IEEE**, uji similaritas **maksimal 25%**, **pendaftaran HKI** (surat pencatatan ciptaan DJKI), video profil 60 detik
- Penggunaan generative AI yang tidak wajar dapat berujung diskualifikasi

> **Catatan penting soal HKI:** prosesnya memakan waktu. Jarak antara pengumuman finalis (akhir Oktober) dan daftar ulang (26–30 Oktober) sangat sempit. Harus diurus jauh lebih awal.

---

## 4. Ide yang Sudah Dipertimbangkan dan Ditolak

**Jangan usulkan ulang tanpa argumen baru.**

| Ide | Alasan ditolak |
|---|---|
| **PERISAI** — parental control deteksi judi online berbasis CNN | Pernah juara Hackathon CORE3D 2026 (tingkat nasional, Unand). Bisa dipakai dengan pengembangan >50%, tapi ada ambiguitas antara Ketentuan Khusus Divisi VIII poin (b) yang melarang karya pernah juara dan poin (f) + Persyaratan Umum no. 9 yang mengizinkan karya incremental. Belum dikonfirmasi ke panitia. Ditinggalkan demi kepastian. |
| **AgriSight AI** — deteksi penyakit padi via NDVI Sentinel-2 + computer vision | Timeline aslinya 4–5 bulan, tidak muat di 5 minggu. Tutupan awan tropis membuat NDVI tidak andal. Resolusi 10 m terlalu kasar untuk petak sawah kecil (<1 ha). Insentif petani lemah — mereka sudah berkeliling lahan setiap hari. Validasi butuh perjalanan ke sawah berulang kali. |
| **UMKM — asisten kesiapan kredit** | Data urgensinya kuat (69,5% UMKM tak terakses pembiayaan perbankan) dan stakeholder mudah dihubungi, tapi sudah ada inisiatif serupa (SAPA UMKM, Credit Bureau Indonesia × Kementerian UMKM). |
| **Blockchain — ketertelusuran produk UMKM** | Pertanyaan "kenapa tidak database biasa?" sulit dijawab meyakinkan. Validasi dampak sulit dalam 5 minggu. |
| **Bansos — perbaikan data penerima** | Data paling mengejutkan (exclusion error ~70%), tapi akses ke pendamping PKH sulit dalam 5 minggu. |
| **E-waste** | Urgensi kalah dramatis; peran AI kurang esensial. |
| **Pertanian, kesehatan (AMR), pasar kerja regional** | Dipertimbangkan, kalah dari kriteria kedekatan tim dengan masalah. |

**Alasan utama memilih tsunami Padang:** masalahnya ada di kota tempat tim tinggal. Tim adalah bagian dari ±200.000 jiwa di zona merah. Dari pola pemenang yang dipelajari (DOAlert/TB Vector ITS, NaviGo UI, CopyFlag Imagine Cup), faktor paling konsisten adalah **kedekatan personal tim dengan masalah** — dan itu terdengar jelas di sesi tanya jawab.

---

## 4A. Keputusan Arsitektur yang Sudah Dikunci

> Bagian ini mencatat keputusan yang sempat menimbulkan perbedaan pemahaman antar anggota tim. **Jangan mengusulkan ulang tanpa argumen baru.**

### Strategi routing: prakomputasi hibrida (dikunci 5 Agustus)

Sempat terjadi perbedaan pemahaman: satu anggota mengasumsikan perangkat pengguna menjalankan Dijkstra secara lokal, sementara anggota lain merancang arsitektur prakomputasi penuh. PRD versi sebelumnya hanya menulis "routing lokal dijalankan di perangkat" — rumusan yang ambigu dan menjadi sumber kesalahpahaman.

**Keputusan final:**
- Seluruh rute dihitung di **tahap prakomputasi** pada perangkat pengembang
- Setiap persimpangan menyimpan **2–3 alternatif tujuan**, diupayakan menuju TES berbeda
- Perangkat pengguna **hanya melakukan pencarian basis data** — tidak menjalankan Dijkstra, A*, maupun algoritma pencarian lintasan lainnya
- Perhitungan ulang di perangkat = **pengembangan lanjutan**, bukan bagian MVP

**Alasan:** kecepatan respons (setiap detik komputasi adalah detik yang hilang dari waktu tempuh), kesetaraan antar perangkat, dan kelayakan implementasi dalam sisa waktu.

Rincian lengkap ada di `PRD.md` Bagian 7A. **Seluruh jawaban kepada juri harus konsisten dengan bagian tersebut.**

### Keterbatasan yang wajib dinyatakan jujur

1. Fitur laporan jalur terputus (F-11) **hanya berlaku saat ada jaringan**. Dalam Mode Mandiri, perangkat menggunakan data terakhir yang tersinkronisasi.
2. Sistem **tidak dapat beradaptasi** terhadap kondisi yang belum terwakili dalam data prakomputasi.
3. Pengguna tidak selalu berada tepat di persimpangan; sistem memilih yang terdekat.

### Metrik yang masih kosong dan wajib diisi

**Perbandingan hasil rute berbobot kapasitas vs lintasan terpendek konvensional** pada skenario simulasi yang identik. Tanpa angka ini, klaim keunggulan pendekatan kolektif hanya bersifat argumentatif — dan ini pertanyaan yang hampir pasti diajukan juri.

---

## 5. Fitur yang Sudah Ditolak

Rincian lengkap ada di PRD Bagian 6.2. Ringkasnya:

| Fitur | Alasan |
|---|---|
| Sistem peringatan dini gempa (EEWS) sendiri | Sudah ada InaEEWS (BMKG) & Google Android Earthquake Alerts. Butuh jaringan seismometer fisik. |
| **Deteksi guncangan otomatis via accelerometer** | Dipertimbangkan lalu **ditolak (5 Agustus)**. Tidak dapat membedakan gempa dari perangkat jatuh/pengguna berlari/getaran kendaraan secara andal. Pada sistem keselamatan, false positive berulang → pengguna mengabaikan sistem; false negative → rasa aman yang keliru. Diganti dengan **widget layar utama**; pemicu utama adalah guncangan yang dirasakan pengguna sendiri, sesuai prosedur BPBD. |
| **Ambang magnitudo yang ditetapkan sendiri** | Ditolak. Potensi tsunami ditentukan kombinasi magnitudo, kedalaman, dan lokasi episentrum. Sistem mengikuti **status resmi BMKG**, bukan aturan buatan sendiri. |
| **Pelacakan lokasi keluarga real-time** | **Alasan keselamatan, bukan teknis.** Mendorong perilaku berbalik arah menjemput keluarga — penyebab korban terdokumentasi. Dengan golden time 20 menit, berbalik arah sering berarti tidak sempat. Diganti dengan rencana titik temu yang disusun sebelum bencana. |
| Heatmap kepadatan real-time dari pengguna | Butuh massa kritis; data bias; efek penggiringan (mengalihkan semua pengguna ke jalur alternatif menciptakan kemacetan baru di sana). |
| Data lalu lintas Google Maps | Data kendaraan bermotor, bukan pejalan kaki. Butuh jaringan. ToS melarang cache offline. Model dibangun dari pola harian normal, bukan evakuasi massal. |
| Modul logistik relawan pasca-bencana | Produk kedua yang berbeda (fase, pengguna, cara validasi). Masuk *future work*. |
| Pindah ke Divisi VI (Kota Cerdas) | Divisi VI menuntut 4 pilar: regulasi, kelembagaan, infrastruktur, literasi — aplikasi hanya 1 dari 4. Tim ini backend/frontend/AI, kekuatannya di software. |

---

## 6. Lanskap Kompetitor (hasil riset)

Ini **wajib dicantumkan di proposal**. Juri akan menemukannya dengan pencarian singkat.

| Yang sudah ada | Fokusnya | Celah yang kami isi |
|---|---|---|
| **Padang Sigap** (`padang.go.id/padangsigap`) | Peta rencana evakuasi resmi per kelurahan, **format gambar raster** | Statis; tidak menjawab "dari posisi saya sekarang ke mana?" |
| **InaRISK Personal** (BNPB) | Peta risiko & potensi bahaya berbasis GPS | Bukan navigasi rute; keluhan *connection timed out* di Play Store menunjukkan ketergantungan koneksi |
| **InaTEWS / InaEEWS** (BMKG) | Peringatan dini tsunami & gempa | Kami **mengonsumsi** outputnya, tidak menduplikasi |
| Penelitian Dijkstra evakuasi Padang | Rute terpendek individual | Berbasis peta online; tidak menangani pergerakan kolektif |
| **SIG Evakuasi Padang Barat (Android)** | Pemetaan jalur + edukasi | ⚠️ Wilayah sama dengan MVP kami — wajib dibahas eksplisit di proposal |
| **Android Earthquake Alerts** (Google) | Deteksi gempa via accelerometer ponsel, aktif di 98 negara | Mensyaratkan koneksi internet aktif; menjawab "apakah terjadi gempa", bukan "ke mana saya lari" |
| Aplikasi Info BMKG | Notifikasi gempa resmi | Informasi kejadian, bukan navigasi evakuasi |
| Analisis Network Analyst Padang | Peta jalur terdekat per kelurahan | Output statis, bukan sistem interaktif |

**Posisi pembeda:** semua karya terdahulu mencari *rute terpendek untuk satu orang dengan asumsi ada internet*. Kami membangun sistem yang *bekerja tanpa internet* dan *memperhitungkan apa yang terjadi ketika ratusan ribu orang bergerak serentak*.

---

## 7. Pelajaran dari Proyek Sebelumnya (PERISAI)

Konteks penting karena membentuk aturan kerja tim sekarang.

PERISAI adalah aplikasi parental control deteksi judi online yang dibangun dalam 24 jam dan memenangkan Hackathon CORE3D 2026. Setelah audit jujur, ditemukan:

1. **Klaim akurasi 85–90% adalah angka karangan untuk pitching**, bukan hasil pengukuran. Model tidak pernah dievaluasi formal — tidak ada confusion matrix, precision, atau recall yang tercatat.
2. **Service role key Supabase tertanam di kode client (Kotlin).** Key ini melewati seluruh Row Level Security. Siapa pun yang mendekompilasi APK bisa mengakses seluruh database, termasuk screenshot semua anak dari semua keluarga.
3. Beberapa fitur diklaim berfungsi padahal setengah jalan (layer Trustpositif di-hardcode `false`, BootReceiver tidak berfungsi karena MediaProjection butuh token interaktif, FCM tidak terintegrasi, toggle settings tidak tersimpan).
4. Dataset hanya ~1.300 gambar, sebagian dari Rico Dataset (UI umum, bukan judi), sehingga proporsi data judol asli jauh lebih kecil.
5. False positive tidak pernah diukur — game dengan elemen spin/gacha sangat mungkin salah terdeteksi.

**Aturan yang lahir dari ini:**
- Setiap angka dalam proposal harus punya bukti yang bisa ditunjukkan
- Kredensial tidak pernah masuk ke client
- Fitur yang belum selesai dinyatakan belum selesai, bukan diklaim berfungsi

---

## 8. Status Faktual Hari Ini

| Hal | Status |
|---|---|
| Ide & scope | ✅ Terkunci (lihat PRD) |
| PRD | ✅ Draft v0.1 selesai |
| Struktur Drive & README | ✅ Selesai |
| Nama produk | ✅ **Siaga Padang** |
| Repo GitHub | ✅ Dibuat — pastikan tetap privat |
| Kode Android (Kotlin) | ❌ Belum mulai |
| Backend (FastAPI) | 🟡 Sebagian besar endpoint selesai: sync, laporan jalur terhalang dengan validasi 3 pelapor radius 50 m, check-in atomic increment, proxy BMKG dengan cache, rate limiting per device ID |
| Pipeline spasial (Python) | 🟡 Sedang dikerjakan |
| Dosen pembimbing | ✅ **Nisa Dwi Angresti, S.Si., M.Kom.** (Sistem Informasi FTI Unand) — sudah konfirmasi. Pertemuan Jumat 7 Agustus |
| Status pendaftaran PT Unand | ✅ Dikonfirmasi |
| Mekanisme seleksi internal Unand | ✅ **Penerimaan proposal 3–10 Agustus** via kemahasiswaan.unand.ac.id · Pengumuman lolos 12 Agustus · Pendampingan submit Belmawa 12–14 Agustus |
| Kontak BPBD Kota Padang | 🟡 Email terkirim, **belum ada respons** (hari ke-2) |
| Data shelter format vektor | ❌ Belum diketahui ketersediaannya |
| Kelengkapan jalan pejalan kaki di OSM | ✅ **Tervalidasi** (asumsi A-2) |

---

## 9. Data yang Dipakai — Status Verifikasi

| Klaim | Angka | Sumber | Status |
|---|---|---|---|
| Golden time Kota Padang | 20–30 menit setelah gempa | **BPBD Kota Padang** (materi sosialisasi resmi) | ✅ **Terverifikasi** |
| Golden time Mentawai | ~10 menit | Sama | ⚠️ Perlu konfirmasi |
| Populasi zona merah | ±200.000 jiwa | Pemberitaan Pemkot | ⚠️ Perlu konfirmasi ke BPBD |
| Cakupan wilayah rawan | 8 kecamatan, 55 kelurahan | padang.go.id | ⚠️ Perlu konfirmasi |
| Android Earthquake Alerts aktif di | 98 negara (akhir 2023) | Google Research | ✅ Sumber primer |
| Jangkauan EEW global | 250 juta (2019) → 2,5 miliar | Google Research | ✅ Sumber primer |
| Syarat Android Earthquake Alerts | Wajib ada koneksi Wi-Fi/seluler | Google Research | ✅ Sumber primer — **kunci argumen offline-first** |
| Blue Line Tsunami Safe Zone | 22 titik | BPBD Padang (via pemberitaan) | ⚠️ Perlu konfirmasi |
| Kriteria evakuasi vertikal | Bangunan >4 lantai | BPBD Kota Padang | ✅ Terverifikasi |
| Kontak BPBD Kota Padang | (0751) 78775 · 112 (bebas pulsa) | BPBD Kota Padang | ✅ Terverifikasi |

**Prosedur resmi BPBD yang mengikat desain (dari materi sosialisasi):**
- **Jangan berlari** saat evakuasi → perhitungan waktu pakai kecepatan jalan cepat; kata "lari" dilarang di antarmuka
- **Jangan pakai kendaraan** → jaringan jalan pejalan kaki
- **Jangan mendekati pantai** → rute wajib menghindari arah laut
- Terminologi resmi: **TES** (Tempat Evakuasi Sementara), **TEA** (Tempat Evakuasi Akhir), **Tsunami Safe Zone**
- Bangunan **>4 lantai** → evakuasi vertikal mandiri (parameter penyaringan kandidat TES di OSM)
- Setelah tiba di TES/TEA, tunggu arahan RT/RW selama 30 menit

> **Catatan koreksi:** angka 55 adalah jumlah **kelurahan**, bukan jumlah shelter. Kekeliruan ini sempat terjadi dalam diskusi dan sudah diperbaiki. Jangan ulangi.

**Sumber data teknis yang sudah teridentifikasi:**
- BNPB GIS Server (`gis.bnpb.go.id`) — tersedia layer `Arah_jalur_evakuasi`, `batas_administrasi`, `INARISKPOP_2020`, `global_tsunami_modelling`
- OpenStreetMap — jaringan jalan & bangunan
- BPS — kepadatan penduduk per kelurahan
- BMKG — API status gempa & tsunami

---

## 10. Cara Kerja yang Diharapkan dari Pendamping/Asisten

Ini permintaan eksplisit dari pemilik proyek:

1. **Berikan kritik jujur seperti juri sungguhan** — termasuk pertanyaan tajam yang mungkin muncul di sesi tanya jawab final. Jangan sekadar memvalidasi.
2. **Kaitkan saran dengan bobot kriteria.** Prioritaskan aspek berbobot 20% (inovasi, dampak, UX, metodologi SDLC).
3. **Tuntut bukti data, bukan asumsi.** Ini kelemahan paling umum tim yang gagal lolos.
4. **Sajikan opsi terstruktur dengan tradeoff**, bukan brainstorming terbuka tanpa arah.
5. **Ingatkan ketentuan kompetisi** bila relevan dengan yang sedang dikerjakan.
6. **Jangan menyarankan ulang** ide/fitur di Bagian 4 dan 5 tanpa argumen baru.

---

## 11. Prioritas Terdekat

**Sampai 10 Agustus — seluruh fokus pada proposal, bukan pengembangan kode.**

Pengembangan Android (Kotlin) **ditunda** sampai proposal seleksi kampus terkirim. Alasannya: aplikasi setengah jadi tidak menyelamatkan proposal yang tidak terkirim, sementara proposal yang tidak lolos seleksi kampus membatalkan seluruh rencana.

| Tanggal | Fokus |
|---|---|
| 5–6 Agustus | Data primer (uji baca peta, uji perangkat); Habib & Sheva menyiapkan gambar prioritas |
| 7–8 Agustus | Penulisan Bab 1–4 |
| 9 Agustus | Bab 5–8 dengan bahan yang tersedia |
| **7 Agustus** | **Pertemuan dengan dosen pembimbing** — draf dikirim Kamis malam |
| 10 Agustus | Review bersama, kirim |

**Catatan:** data BPBD kemungkinan besar tidak tersedia sebelum 10 Agustus. Gunakan data sementara (kandidat TES dari OSM, kriteria bangunan >4 lantai) dan nyatakan di Batasan bahwa permohonan data resmi sedang berproses.

---

## 12. Dokumen Terkait

| Dokumen | Isi |
|---|---|
| `PRD.md` | Spesifikasi produk: fitur, arsitektur, tech stack, metrik, risiko, timeline |
| `README-Drive.md` | Struktur folder Google Drive & checklist berkas administratif |
| Panduan GEMASTIK 2026 (PDF) | Sumber resmi seluruh ketentuan |
