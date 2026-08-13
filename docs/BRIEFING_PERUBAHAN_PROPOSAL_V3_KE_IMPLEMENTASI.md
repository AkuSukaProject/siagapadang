# Briefing Perubahan Siaga Padang: Proposal V3 ke Implementasi Terkini

**Tanggal snapshot:** 10 Agustus 2026
**Tujuan dokumen:** menjadi satu konteks utama yang mudah dibaca manusia maupun AI untuk memahami rancangan awal, keputusan yang berubah, keadaan aplikasi saat ini, bukti yang tersedia, dan pekerjaan yang masih tersisa.

> Dokumen ini menggabungkan isi `PROPOSAL VERSI 3.pdf`, `CONTEXT.md`, `PRD.md`, `CLAUDE.md`, dokumentasi repository, pemeriksaan basis data, dan inspeksi kode Android lokal. Dokumen ini tidak menggantikan sumber resmi BPBD/BMKG dan tidak mengubah batas keselamatan produk.

## 1. Cara Membaca dan Urutan Kebenaran

Jika sumber saling bertentangan, gunakan urutan berikut:

1. **Kode, basis data, hasil build, dan hasil pengujian terbaru** untuk menjawab apa yang benar-benar sudah bekerja.
2. **Dokumen ini** untuk memahami perubahan dan status keseluruhan per 10 Agustus 2026.
3. **`CLAUDE.md` (8 Agustus 2026)** untuk keputusan teknis Android yang dikunci.
4. **Proposal V3 (10 Agustus 2026)** untuk narasi kompetisi, landasan, tujuan, dan klaim yang harus dibuktikan; proposal masih memiliki bagian lama dan penanda `[ISI]`.
5. **`PRD.md` dan `CONTEXT.md` (5 Agustus 2026)** sebagai catatan rancangan awal/historis.

Status yang dipakai di dokumen ini:

- **Selesai:** terlihat di kode/data dan dapat diverifikasi.
- **Sebagian:** alur utama ada, tetapi perilaku belum sepenuhnya sama dengan proposal.
- **Belum:** tidak ditemukan pada aplikasi Android saat ini.
- **Tidak terverifikasi di branch ini:** diklaim proposal, tetapi sumber/kode pembuktiannya tidak ada pada snapshot repository ini.
- **Sengaja tidak dibangun:** keputusan keselamatan atau batas lingkup, bukan kelalaian.

## 2. Ringkasan Eksekutif

Siaga Padang tetap mempertahankan gagasan utama rancangan awal: aplikasi Android native untuk navigasi evakuasi tsunami yang mengutamakan fungsi luring, mengambil rute yang telah diprakomputasi, menggunakan posisi aktual pengguna, dan menyajikan instruksi sederhana dalam kondisi darurat.

Perubahan terbesarnya adalah:

- Cakupan data berkembang dari MVP **Kecamatan Padang Barat** menjadi basis data jaringan yang mencakup wilayah Kota Padang dan akses menuju kawasan lebih tinggi.
- UI berpindah dari rencana awal XML menjadi **Jetpack Compose** dengan MapLibre.
- Layar evakuasi berkembang dari satu mockup menjadi dua mode: mode instruksi dominan dan mode peta dominan, berpindah lewat panel geser vertikal.
- Navigasi bukan lagi sekadar panah arah umum. Aplikasi menghitung manuver lurus, belok, belok tajam, putar balik, pendekatan menuju jalan, dan kedatangan.
- Basis data diperbarui dengan pendekatan **hybrid zona**: `is_safe` pada node untuk keputusan cepat dan poligon WKT untuk visualisasi serta pemeriksaan spasial.
- Rute utama dan dua alternatif dapat dipilih secara luring. Rute sebelumnya tetap terlihat abu-abu beserta perbandingan waktunya.
- Widget sekarang memiliki dua ukuran nyata: **2×2** berbentuk kotak dan **4×2** berbentuk memanjang.
- Kata-kata keselamatan diperbaiki agar menyatakan posisi terhadap zona rendaman, bukan menjamin pengguna aman.
- Hitung mundur 20 menit kini secara eksplisit dimulai sejak aplikasi dibuka, bukan diklaim sejak guncangan berhenti.
- Rute, zona, GPS, kompas, tujuan, dan petunjuk berasal dari data/perangkat lokal, tetapi **basemap penuh belum benar-benar luring**. Ubin OpenStreetMap masih menggunakan internet atau cache.
- Fitur rencana titik temu keluarga, integrasi BMKG, check-in aman, sinkronisasi data, dan pengiriman laporan jalan terhalang ke peladen belum menjadi bagian aplikasi Android saat ini.

Kesimpulan singkat: alur navigasi luring inti sudah jauh melampaui rancangan mockup awal, tetapi MVP proposal F01–F08 belum sepenuhnya selesai karena F07 ditunda, F05/F06/F08 masih memiliki perbedaan perilaku, basemap luring belum dikemas, dan sejumlah metrik proposal belum diuji secara formal.

## 3. Rancangan Awal

### 3.1 Masalah yang hendak diselesaikan

Rancangan awal berangkat dari beberapa masalah:

- Peta evakuasi statis berbasis kelurahan tidak langsung menjawab “ke mana saya harus bergerak dari posisi saya sekarang?”.
- Pendatang, mahasiswa, pekerja, dan warga yang sedang berada di luar lokasi rutinnya harus mengorientasikan diri sendiri terhadap peta.
- Jaringan internet dapat melambat atau terputus setelah gempa.
- Pengguna berada dalam tekanan waktu dan dapat mengalami beban kognitif tinggi.
- Rute terpendek individual dapat mengarahkan terlalu banyak orang ke titik atau koridor yang sama.

Solusi yang dirancang adalah navigasi berbasis posisi aktual, bekerja luring, memakai rute prakomputasi yang mempertimbangkan distribusi kapasitas, dan menampilkan satu instruksi yang mudah dipahami pada satu waktu.

### 3.2 Prinsip yang tetap dikunci

- Sistem dibuka pengguna setelah merasakan guncangan kuat; aplikasi **tidak mendeteksi gempa sendiri**.
- Sistem bukan pengganti BMKG, BPBD, prosedur resmi, atau arahan petugas.
- Sistem tidak menjamin keselamatan.
- Evakuasi menggunakan istilah **berjalan cepat**, bukan berlari dan bukan kendaraan bermotor.
- Android tidak menjalankan Dijkstra, A*, BFS, atau pencarian lintasan runtime.
- Rute telah dihitung sebelumnya dan dibaca dari basis data lokal.
- Basis data aplikasi bersifat read-only.
- Panah/instruksi adalah sarana navigasi utama; peta merupakan pendukung.
- Ketidakpastian harus dinyatakan jujur.
- Tidak ada pelacakan keluarga secara real-time.
- Tidak ada klaim okupansi TES secara luring.
- Tidak ada ambang magnitudo buatan aplikasi.

### 3.3 Arsitektur rancangan Proposal V3

Proposal membagi sistem menjadi tiga lapisan:

1. Tahap prakomputasi spasial: OSM, data BPBD, kepadatan penduduk, simulasi kolektif, reverse single-source Dijkstra, dan alokasi berbatas kapasitas.
2. Aplikasi Android: GPS, node terdekat, pembacaan rute lokal, MapLibre, kompas, widget, zona, dan hitung mundur.
3. Peladen: sinkronisasi versi data, GeoJSON titik evakuasi, status BMKG, check-in aman, dan laporan jalan terhalang.

Pada aplikasi Android saat ini, hanya lapisan Android dan basis data keluarannya yang dapat diverifikasi dari branch aktif. Folder `backend/` dan `spatial/` hanya berisi `.gitkeep`.

## 4. Perubahan dari Awal hingga Sekarang

| Area | Rancangan awal / Proposal V3 | Implementasi terkini | Dampak |
|---|---|---|---|
| Cakupan | PRD/Context: Padang Barat; Proposal bercampur antara Padang Barat dan seluruh Kota Padang | Basis data berisi jaringan regional Kota Padang dan rute untuk 31.813 node | Cakupan data lebih luas, tetapi dokumen harus diseragamkan |
| Teknologi UI | PRD mengutamakan XML Layout; Compose hanya jika waktu memungkinkan | Jetpack Compose dipakai penuh | UI lebih mudah dibuat adaptif dan dianimasikan |
| Peta | Mockup peta pendukung dengan klaim ubin luring | MapLibre merender overlay lokal, tetapi basemap OSM masih online/cache | Navigasi lokal tetap bekerja, namun klaim “peta luring penuh” belum boleh dibuat |
| Tampilan navigasi | Satu layar utama dengan panah besar dan peta di bawah | Dua mode vertikal: instruksi dominan dan peta dominan | Pengguna bisa memilih fokus tanpa meninggalkan alur evakuasi |
| Peralihan mode | Garis tarik sederhana | Tab/chevron hijau yang menonjol, dapat ditekan dan digeser, dengan transisi halus | Affordance geser lebih terlihat |
| Orientasi | Panah mengikuti arah perangkat; detail map belum dikunci | Heading-up: saat mengikuti pengguna, peta berputar sehingga arah perjalanan berada di atas | Mengurangi orientasi mental pengguna |
| Interaksi peta | Peta sebagai pendukung pasif | Peta dapat pan, zoom, dan rotasi pada kedua mode; ada tombol pusatkan kembali | Pengguna dapat memeriksa konteks lalu kembali ke navigasi |
| Penanda pengguna | Panah generik pada peta | Marker pengguna berada pada koordinat GPS, selalu pada layer atas, dengan lingkaran putih semi-transparan | Posisi diri lebih jelas dan tidak tertutup rute |
| Kompas | Indikator desain | Kompas nyata berdasarkan sensor, dilengkapi U/T/S/B dan arah antara | Fungsi dan keterbacaan meningkat |
| Tujuan | Nama TES dan titik tujuan | Marker lokasi TES, callout seperti chatbox, nama dan jarak; tujuan asli selalu dipertahankan | Tujuan tidak hilang saat pengguna belum berada di jalan |
| Pengguna di luar jalan | Belum dirinci; proposal mengasumsikan node terdekat | Proyeksi ke titik terdekat pada segmen rute, garis putus-putus menuju jalan, dan marker titik masuk | Pengguna dipandu ke jaringan jalan sebelum mengikuti rute utama |
| Instruksi awal | Arah langsung menuju tujuan/rute | Selama >18 m dari rute, instruksi mengarah ke titik masuk jalan dan dihitung ulang saat ponsel berputar | Menghindari instruksi TES sebelum pengguna mencapai jalan |
| Instruksi belokan | Panah besar generik | Lurus tetap menjadi instruksi utama sampai jarak belokan ≤50 m; tersedia slight/regular/sharp/U-turn | Instruksi tidak terlalu dini dan lebih sesuai perilaku berjalan |
| Daftar instruksi | 3–4 manuver berikutnya | Mode 1 selalu menyediakan 4 slot; mode 2 mengecil adaptif sesuai jumlah instruksi | Sesuai revisi desain pengguna |
| Rute alternatif | 2–3 alternatif tersimpan; tombol jalan terhalang | Maksimal dua perpindahan setelah rute utama; rute lama abu-abu dan ETA pembanding ditampilkan | Alternatif benar-benar bekerja tanpa pencarian ulang |
| Semua alternatif gagal | Proposal: arah garis lurus ke titik terdekat + peringatan menjauhi pantai | Belum ditemukan sebagai fallback final di Android | Gap keselamatan yang perlu diputuskan/diimplementasikan |
| Zona tsunami | Awalnya tabel zona kosong; Proposal: status dan dua tabel poligon | Hybrid: flag node + 31 poligon luar zona rendaman + 73 poligon rendaman rendah/sedang/tinggi | Status cepat dan layer visual tersedia |
| Warna zona | Warna kuat sempat menutup rute | Fill sangat transparan: luar 10%, rendah 12%, sedang 14%, tinggi 16%; tanpa garis administrasi internal | Rute tetap mudah terlihat |
| Informasi zona | Sempat memakai “Anda berada di zona risiko rendah” | “Lokasi Anda di zona rendaman” / “Lokasi Anda di luar zona rendaman” | Tidak memberi jaminan keselamatan palsu |
| Perubahan zona | Belum dirinci | Status awal dan notifikasi saat berpindah kategori, dengan ambang gerak serta konfirmasi berulang untuk meredam jitter GPS | Informasi zona lebih stabil |
| Hitung mundur | Proposal: waktu sejak guncangan berhenti | 20:00 sejak ViewModel/aplikasi dibuka, label “Dihitung sejak aplikasi dibuka” | Asumsi menjadi transparan, tetapi proposal perlu diperbaiki |
| Kedatangan | Proposal: berhenti saat masuk kawasan aman | Android mengonfirmasi tiba dekat TES/ujung rute: radius 20 m, 3 pembacaan, akurasi ≤35 m; lalu dialog dan getaran | Selesai untuk kedatangan TES, belum identik dengan masuk poligon luar rendaman |
| Nama TES panjang | Satu baris dapat terpotong | Auto-size dan dapat menjadi dua baris | Nama tujuan tetap dapat dikenali |
| GPS dan jaringan | Indikator mode konektivitas umum | Ikon GPS dan jaringan dengan warna status, badge `!` saat bermasalah, dapat ditekan untuk penjelasan | Status perangkat lebih mudah dipahami |
| Overlay mode 1 | Bar rute/zona menutup peta | Di mode 1 menjadi ikon ringkas; mode 2 dapat menjadi bar/legenda | Area peta tidak terlalu tertutup |
| Orientasi perangkat | Belum tegas | Activity dikunci portrait | UI tidak rusak ketika ponsel dimiringkan |
| Widget | Widget umum untuk akses cepat | Dua varian: 2×2 kotak dan 4×2 memanjang, berbasis AppWidget XML | Sesuai grid launcher yang diminta |
| Ukuran paket | Target DB ≤75 MB; basemap regional direncanakan | ABI split dan R8 diterapkan; basemap regional belum dikemas | APK per-ABI lebih ringan, tetapi penggunaan ruang total tetap perlu diukur lagi |

## 5. Arsitektur Android Aktual

Alur data saat ini:

```text
GPS perangkat
  -> cari kandidat node dalam bounding box
  -> pilih node terdekat
  -> baca rank rute dari tb_routes
  -> ambil seluruh node dan edge terkait secara batch
  -> balik geometri edge bila arah penyimpanan berlawanan
  -> susun dan sederhanakan polyline
  -> hitung proyeksi posisi, manuver, jarak, ETA, dan kedatangan
  -> EvacuationUiState
  -> Compose + MapLibre + widget
```

Aturan implementasi:

- `ui -> domain -> data`; Composable tidak mengakses DAO.
- Room membuka `ranah_siaga.db` dengan `createFromAsset()` dan hanya melakukan `SELECT`.
- WKT memakai urutan `lon lat`, bukan `lat lon`.
- Edge dapat tersimpan berlawanan dengan arah rute sehingga koordinat harus dibalik.
- Rute lama digambar di bawah rute aktif; rute aktif berada di bawah marker tujuan dan marker pengguna.
- Ketika pengguna menggeser/merotasi peta, mode follow berhenti. Tombol pusatkan mengaktifkan kembali heading-up follow.
- Pencarian rute tidak dilakukan ulang saat pengguna bergerak; petunjuk memproyeksikan posisi ke geometri rute aktif.

### 5.1 Logika pendekatan jalan

- Ambang dianggap berada pada rute: **18 meter**.
- Jika lebih jauh dari rute, aplikasi mencari proyeksi terdekat pada segmen rute aktif.
- Peta menampilkan garis putus-putus krem dari pengguna menuju proyeksi tersebut.
- Instruksi pendekatan memperhitungkan heading perangkat dan dapat berubah menjadi lurus, kiri, kanan, atau putar balik.
- Nama besar dan marker tetap menunjukkan TES asli; titik jalan hanyalah sasaran sementara.
- Setelah jarak ke rute ≤18 meter, instruksi berpindah ke fase jalur menuju TES.

Catatan: ini adalah proyeksi menuju **rute aktif**, bukan pencarian jaringan baru menuju jalan terdekat. Bila rute aktif tidak berada pada ruas yang secara fisik paling mudah dicapai, perilakunya perlu diuji lapangan.

### 5.2 Logika manuver

- Geometri tersisa disampel kira-kira setiap 12 m.
- Perubahan arah <28° tidak dianggap belokan.
- Instruksi belok tidak dijadikan instruksi utama sebelum pengguna berada dalam jarak 50 m dari belokan.
- Manuver yang berjarak <25 m dari manuver sebelumnya digabung/dilewati untuk mengurangi instruksi beruntun.
- Getaran manuver diberikan ketika belokan non-lurus berada dalam 30 m.
- Tipe manuver: lurus, sedikit kiri/kanan, kiri/kanan, tajam kiri/kanan, putar balik, dan tiba.

### 5.3 Logika kedatangan aktual

Aplikasi menyatakan tiba jika:

- jarak ke koordinat TES atau ujung rute ≤20 m;
- akurasi GPS ≤35 m, atau akurasi belum tersedia;
- kondisi tersebut terkonfirmasi tiga pembacaan berturut-turut.

Setelah tiba, hitung mundur berhenti, perangkat bergetar, dan UI menampilkan keadaan kedatangan. Ini berbeda dari kalimat proposal “berhenti saat masuk kawasan aman”; status poligon zona tetap merupakan informasi terpisah.

## 6. Fakta Basis Data Aktif

Lokasi aset: `android/app/src/main/assets/ranah_siaga.db`.

| Fakta | Nilai terverifikasi |
|---|---:|
| Ukuran | 70.176.768 byte = sekitar 66,93 MiB / 70,18 MB desimal |
| Node jalan | 31.813 |
| Edge jalan | 39.216 |
| Baris rute | 31.813 |
| Rute rank 1 kosong | 0 |
| Rute rank 2 kosong | 0 |
| Rute rank 3 kosong | 0 |
| TES | 143 nama unik |
| Total kapasitas TES dalam DB | 290.856 jiwa |
| Node `is_safe = 1` | 16.172 |
| Node `is_safe = 0` | 15.641 |
| Poligon luar zona rendaman | 31 |
| Poligon rendaman | 73 |
| Rendaman rendah | 44 |
| Rendaman sedang | 20 |
| Rendaman tinggi | 9 |

Makna hybrid zona:

- Flag node dipakai untuk keputusan posisi secara cepat.
- Poligon rendaman dipakai untuk menentukan kategori rendah/sedang/tinggi dan layer peta.
- Poligon luar rendaman dipakai sebagai layer visual/fallback.
- UI harus mengatakan “di luar zona rendaman”, bukan “aman”, karena sistem hanya mengetahui posisi terhadap data rendaman.

## 7. Status Kebutuhan Fungsional Proposal V3

Kode di bawah mengikuti penomoran **Proposal V3**, bukan PRD lama yang memiliki pergeseran nomor.

| Kode | Kebutuhan Proposal V3 | Status Android | Catatan |
|---|---|---|---|
| F01 | Widget status zona, TES, ETA, akses evakuasi | Selesai | Varian 2×2 dan 4×2; bergantung izin/lokasi perangkat, bukan internet |
| F02 | Menentukan status zona | Selesai | Hybrid node flag dan point-in-polygon; ada status awal dan transisi |
| F03 | Menyajikan jalur lokal | Selesai | Rank 1–3, polyline edge, tujuan, ETA, tanpa runtime shortest-path |
| F04 | Penunjuk arah berbasis kompas | Selesai | Panah/manuver dan heading-up map; perlu uji perangkat/magnetometer lapangan |
| F05 | Countdown dan perbandingan ETA | Sebagian | Tampil dan berjalan, tetapi dimulai sejak aplikasi dibuka, bukan sejak guncangan berhenti |
| F06 | Deteksi tiba di kawasan aman | Sebagian | Deteksi tiba di TES/ujung rute sudah ada; belum sama dengan masuk poligon luar rendaman |
| F07 | Rencana titik temu keluarga | Belum / ditunda | Tetap ide masa tenang; tidak boleh diganti live tracking |
| F08 | Peralihan alternatif atas laporan pengguna | Sebagian besar | Rute utama + dua alternatif bekerja luring; fallback setelah semua alternatif gagal belum ditemukan |
| F09 | Status resmi BMKG | Belum di Android | Ikon jaringan hanya menunjukkan konektivitas, bukan status BMKG |
| F10 | Penandaan keselamatan setelah tiba | Belum di Android | Kedatangan lokal tidak mengirim check-in |
| F11 | Mengirim laporan jalur terhalang | Belum di Android | Tombol hanya mengganti rute lokal; tidak mengirim ke server |
| F12 | Sinkronisasi versi/data wilayah | Belum di Android | DB dikemas sebagai aset aplikasi |
| F13 | Simulasi pergerakan kolektif | Tidak terverifikasi di branch ini | Proposal memuat hasil, tetapi `spatial/` tidak memuat source pada snapshot ini |
| F14 | Analisis distribusi beban/pengelola | Tidak terverifikasi di branch ini | Tidak ada UI analisis pada Android dan source modul pengelola tidak tersedia di branch ini |

### Penilaian MVP inti

Jika F01–F08 dianggap MVP proposal:

- Selesai secara utama: F01, F02, F03, F04.
- Sebagian: F05, F06, F08.
- Ditunda: F07.

Maka MVP proposal **belum lengkap sepenuhnya**, walaupun alur demo navigasi luring dari GPS hingga rute, kompas, zona, alternatif, dan kedatangan sudah tersedia.

## 8. Status Kebutuhan Nonfungsional

| Kode | Target Proposal | Status bukti saat ini |
|---|---|---|
| NF01 | Seluruh inti berjalan mode pesawat | Sebagian: rute, zona, tujuan, ETA, kompas, dan widget berbasis data lokal; basemap area baru belum dijamin dan F07 belum ada |
| NF02 | Arahan tersedia <1 detik | Belum ada hasil pengukuran formal lintas skenario/perangkat yang dapat dicantumkan |
| NF03 | Basis data ≤75 MB | Lulus: sekitar 66,93 MiB / 70,18 MB desimal |
| NF04 | Selisih respons antarperangkat ≤1 detik | Belum diuji formal pada perangkat yang ditetapkan |
| NF05 | Timeout jaringan 2–3 detik | Belum relevan penuh karena alur API BMKG/sinkronisasi belum ada |
| NF06 | Kontras ≥4,5:1 dan target sentuh ≥48 dp | Menjadi prinsip desain, tetapi audit aksesibilitas formal belum terdokumentasi |
| NF07 | Selaras prosedur/istilah BPBD | Sebagian besar diterapkan; tetap perlu validasi BPBD atas rute dan redaksi akhir |
| NF08 | Basemap/ubin luring | Belum selesai; style pengembangan memakai raster OpenStreetMap melalui jaringan/cache |

## 9. UI/UX Aktual yang Harus Dipertahankan

### Mode 1: instruksi dominan

- Panah/manuver dan nama TES menjadi fokus.
- Countdown dan label sumber waktunya terlihat.
- Empat slot instruksi berikutnya dipertahankan ukurannya walau hanya satu atau dua yang terisi.
- Map tetap dapat digeser, diputar, dan diperbesar.
- Informasi rute lama dan zona dipadatkan menjadi ikon agar tidak menutupi map.
- Tab hijau menonjol ke atas untuk menunjukkan bahwa panel dapat ditarik menuju mode peta.

### Mode 2: peta dominan

- Header instruksi berada di atas dengan warna navy/cream mengikuti rancangan.
- Tab/handle berada di dalam komponen header dan mengarah ke bawah untuk kembali ke mode 1.
- GPS dan jaringan tampil vertikal di kiri kartu manuver.
- Jumlah kotak instruksi menyesuaikan isi; dua instruksi menghasilkan dua kotak, bukan empat slot kosong.
- Peta interaktif, kompas nyata, recenter, rute aktif, rute lama, zona, tujuan, pengguna, dan pendekatan jalan tetap terlihat.

### Urutan layer peta

Urutan konseptual dari bawah ke atas:

```text
basemap
  -> fill zona tsunami
  -> rute lama abu-abu
  -> rute aktif
  -> garis pendekatan ke jalan dan marker titik masuk
  -> marker/callout TES
  -> marker pengguna
```

Marker pengguna dan tujuan tidak boleh tertutup garis rute.

## 10. Perubahan Redaksi Keselamatan

Gunakan redaksi berikut secara konsisten:

| Pakai | Hindari |
|---|---|
| “Lokasi Anda di zona rendaman” | “Anda berada di zona risiko rendah” |
| “Lokasi Anda di luar zona rendaman” | “Anda aman” |
| “Berjalan cepat” | “Lari/berlari” |
| “TES/TEA” | “Shelter” jika merujuk istilah resmi lokal |
| “Alternatif tujuan/rute” | “TES penuh” saat tidak ada data okupansi runtime |
| “Dihitung sejak aplikasi dibuka” | Kesan bahwa aplikasi mendeteksi saat guncangan berhenti |

Alasannya: aplikasi mengetahui posisi terhadap data spasial, bukan keselamatan faktual; aplikasi juga tidak mengetahui waktu guncangan berhenti maupun keterisian TES secara real-time.

## 11. Basemap dan Ukuran Aplikasi

### 11.1 Keadaan saat ini

- Tidak ada paket peta dunia yang dapat “dihapus”.
- MapLibre adalah mesin render, bukan isi peta.
- Style pengembangan memakai ubin raster OpenStreetMap ketika jaringan tersedia.
- Bila ubin tidak tersedia, overlay lokal masih dapat menggambar rute, zona, pengguna, tujuan, dan petunjuk pada latar lokal.
- Cache MapLibre dibatasi sekitar 16 MB.
- APK dipisah per ABI: `arm64-v8a`, `armeabi-v7a`, `x86`, dan `x86_64`; universal tetap dibuat untuk pengembangan.
- Release menggunakan R8/resource shrinking, tetapi artefak release yang tersedia belum ditandatangani.

### 11.2 Artefak build yang ditemukan

Artefak berikut bertanggal 9 Agustus 2026 dan **dapat tertinggal dari perubahan lokal terbaru**:

| Artefak | Ukuran byte | Keterangan |
|---|---:|---|
| Debug arm64-v8a | 44.977.053 | APK instalasi pengujian perangkat ARM64 |
| Debug armeabi-v7a | 41.640.545 | APK instalasi perangkat ARM 32-bit |
| Debug universal | 80.878.733 | Membawa semua ABI |
| Release arm64-v8a | 21.454.695 | Unsigned, belum untuk distribusi akhir |
| Release universal | 57.356.375 | Unsigned, belum untuk distribusi akhir |

Jangan memakai ukuran lama di `OFFLINE_BASEMAP_PLAN.md` atau Proposal V3 tanpa memperbarui pengukuran, karena penambahan database hybrid dan perubahan packaging telah mengubah hasil.

### 11.3 Rencana yang belum dikerjakan

- Siapkan vector/raster tiles regional hanya untuk cakupan yang benar-benar dibutuhkan.
- Jangan memotong database rute sebelum kontinuitas rute menuju kawasan tinggi dibuktikan.
- Uji mode pesawat pada area yang belum pernah dibuka agar cache tidak menyamarkan ketergantungan jaringan.
- Ukur ukuran APK, data aplikasi setelah instalasi, cache, dan ruang total pada perangkat nyata.
- Tandatangani release dengan signing key tim sebelum distribusi APK final.

## 12. Inkonsistensi Proposal V3 yang Harus Diperbaiki

Proposal V3 masih memuat fragmen dari beberapa versi. Sebelum dikirim atau dijadikan sumber AI, perbaiki hal berikut:

1. **Cakupan wilayah:** halaman 15 menyebut 8 kecamatan/55 kelurahan dan seluruh zona rawan Kota Padang, tetapi halaman 28 kembali menyebut Kecamatan Padang Barat + buffer.
2. **Jumlah titik evakuasi:** beberapa bagian menyebut 143 gedung/139 simpul tujuan, halaman 28 dan paragraf lama halaman 34 menyebut 82 titik.
3. **Ukuran database:** muncul 65,04 MB, `[12] MB`, dan instruksi unduh database 12 MB. Aset aktif berukuran 70.176.768 byte.
4. **Cara distribusi database:** aplikasi kini mengemas DB melalui asset, sedangkan halaman 41 menyatakan pengguna memasang APK 65 MB lalu mengunduh DB 12 MB secara terpisah.
5. **Countdown:** tujuan Bab II menyatakan 20 menit sejak guncangan berhenti, sedangkan aplikasi tidak mendeteksi waktu tersebut dan kini menghitung sejak dibuka.
6. **NF01:** uraian menyebut F01–F08 harus luring, tetapi tabel hanya menulis F01–F07.
7. **Basemap:** Bab VI menyatakan MapLibre menggunakan berkas ubin luring seolah selesai; implementasi sekarang masih OSM online/cache.
8. **F06/kedatangan:** proposal menyebut masuk kawasan aman, sedangkan kode mengonfirmasi kedekatan dengan TES/ujung rute.
9. **Fallback alternatif:** proposal menjanjikan arah garis lurus setelah seluruh alternatif gagal; Android belum menunjukkan fallback itu.
10. **Mode konektivitas:** Proposal menjelaskan Penuh/Hemat/Mandiri dan okupansi/status BMKG. Android baru menampilkan status ketersediaan jaringan, belum mengorkestrasi ketiga mode fitur.
11. **Peladen:** Bab VI mengklaim layanan FastAPI dan rata-rata respons <100 ms, tetapi source backend tidak ada pada branch aktif sehingga klaim perlu bukti terpisah.
12. **Simulasi:** Proposal mengklaim pipeline dan hasil, tetapi source spatial tidak ada pada branch aktif sehingga perlu repository/artefak pembuktian terpisah.
13. **Tabel 6.4:** unit dan label tampak tertukar. “Jumlah ruas jalan melebihi kapasitas” memakai unit gedung, sementara “jumlah titik evakuasi” menampilkan 1.008. Paragraf sesudah tabel justru menyatakan 52 titik evakuasi melebihi kapasitas pada pendekatan konvensional dan nol pada pendekatan berbatas kapasitas.
14. **Persentase simulasi:** tabel menulis 39,52% vs 74,49% “mencapai tujuan dalam 20 menit”, tetapi paragraf menulis 83,44% tiba dalam 20 menit dan 39,52% tertampung. Definisi metrik harus dipisahkan dan dihitung ulang.
15. **Jumlah ruas dengan kapasitas berlebih:** nilai 1.008 perlu diberi unit dan definisi yang benar.
16. **Tabel usang:** halaman 34 mengulang uraian dengan `[ISI]`, 82 TES, dan `[12] MB` setelah angka final sudah ditulis pada halaman 33.
17. **Placeholder:** masih ada banyak `[ISI]`, `[GAMBAR]`, bagian hasil uji kosong, kendala kosong, minimum Android kosong, dan beberapa referensi silang belum selesai.
18. **Klaim “tidak pernah gagal menentukan jalur”:** harus diselaraskan dengan kondisi posisi di luar cakupan, data rusak, izin GPS, dan semua alternatif terhalang.
19. **TEA:** sumber menyebut 29 TEA, tetapi routing aktif menggunakan 143 TES/139 simpul tujuan; peran TEA harus dinyatakan dengan jelas agar tidak terkesan sudah dirutekan.
20. **Status aman:** pertahankan prinsip Bab VII bahwa aplikasi tidak boleh berkata “Anda aman”; sesuaikan seluruh mockup, widget, screenshot, dan narasi.

## 13. Bukti yang Sudah Ada dan yang Masih Dibutuhkan

### 13.1 Bukti yang tersedia pada snapshot ini

- Database aktif dengan jumlah baris dan flag zona yang dapat diaudit read-only.
- Kode Android native Kotlin + Compose + Room + MapLibre.
- Navigasi dari GPS, rute lokal, geometri, manuver, alternatif, zona, widget, kompas, dan kedatangan.
- 38 unit test domain/UI lulus pada 10 Agustus 2026.
- APK debug/release per-ABI pernah dibangun pada 9 Agustus 2026.
- Riwayat commit fondasi, navigasi luring/interaktif, penyempurnaan alur, dan optimasi packaging.

### 13.2 Bukti yang belum cukup

- Uji end-to-end mode pesawat di area basemap yang belum pernah masuk cache.
- Pengukuran NF02 <1 detik pada beberapa posisi dan beberapa perangkat.
- Pengukuran NF04 selisih respons antarperangkat.
- Audit kontras 4,5:1 dan ukuran seluruh target sentuh.
- Uji lapangan akurasi instruksi belok, proyeksi masuk jalan, dan GPS di gang/gedung.
- Validasi rute, titik TES, kapasitas, dan redaksi oleh BPBD.
- Bukti source/commit pipeline spatial yang menghasilkan DB.
- Bukti source, deployment, dan benchmark backend yang diklaim proposal.
- Bukti perhitungan ulang metrik simulasi dan definisi setiap indikator.
- Screenshot final dari seluruh layar yang sama dengan build yang akan dikumpulkan.
- Signing release dan artefak APK final yang memuat seluruh perubahan lokal.

## 14. Keadaan Git Saat Snapshot

- Branch aktif: `feat/android-foundation`.
- Commit terakhir terdeteksi: `c30502a Optimize Android APK packaging and map cache`.
- Banyak perubahan Android terbaru masih **modified/untracked** secara lokal, termasuk zona hybrid, widget, network status, perbaikan navigasi, dan UI terbaru.
- Artinya, remote GitHub atau APK lama belum tentu berisi keadaan yang dijelaskan dokumen ini.
- Database `.db` sengaja tidak dilacak Git dan perlu didistribusikan melalui media/release terpisah sesuai aturan proyek.

Jangan menyatakan “sudah di GitHub” atau “APK final sudah jadi” sebelum commit, push, build ulang, signing, dan instalasi ulang selesai.

## 15. Prioritas Pekerjaan Berikutnya

Urutan yang disarankan untuk fokus rute terlebih dahulu:

1. Build ulang APK dari perubahan lokal terbaru dan uji pada perangkat nyata.
2. Uji mode pesawat tanpa cache; putuskan serta kemas basemap regional yang benar-benar luring.
3. Uji lapangan beberapa skenario: di jalan, >18 m dari jalan/rute, mendekati simpang, salah arah, rotasi ponsel, alternatif, dan tiba di TES.
4. Implementasikan fallback ketika ketiga rute telah dilaporkan terhalang, dengan redaksi ketidakpastian yang aman.
5. Putuskan penyelarasan F06: tiba di TES, masuk luar zona rendaman, atau dua keadaan yang berbeda.
6. Ukur NF02, NF04, ukuran instalasi, cache, dan konsumsi memori; jangan menggunakan perkiraan.
7. Commit dan push perubahan Android terbaru tanpa memasukkan DB/APK ke riwayat Git.
8. Perbaiki Proposal V3 berdasarkan daftar inkonsistensi di atas.
9. Dapatkan atau tautkan repository/artefak spatial dan backend agar klaim lintas-komponen dapat diverifikasi.
10. Tunda F07 dan fitur jaringan sampai alur rute luring stabil, kecuali kebutuhan kompetisi mengubah prioritas.

## 16. Batas Lingkup dan Non-Goal yang Tidak Boleh Dihidupkan Kembali Sembarangan

- Tidak mendeteksi guncangan dengan akselerometer.
- Tidak membuat sistem peringatan dini sendiri.
- Tidak membuat ambang magnitudo sendiri.
- Tidak melacak lokasi keluarga secara langsung.
- Tidak menyatakan TES sedang penuh berdasarkan data luring.
- Tidak menghitung shortest path di Android.
- Tidak menjanjikan keselamatan pengguna.
- Tidak mengganti prosedur BPBD/BMKG.
- Tidak menggunakan kendaraan bermotor sebagai dasar rute.
- Tidak mengklaim jumlah nyawa yang diselamatkan atau pengurangan korban tanpa bukti.

## 17. Konteks Ringkas untuk AI Brainstorming

Salin bagian ini bila AI lain membutuhkan briefing singkat:

```text
Proyek: Siaga Padang, aplikasi Android native Kotlin/Jetpack Compose untuk navigasi evakuasi tsunami offline-first di Kota Padang.

Keputusan terkunci:
- Pengguna membuka aplikasi setelah merasakan guncangan; aplikasi tidak mendeteksi gempa.
- Rute sudah diprakomputasi; Android tidak boleh menjalankan Dijkstra/A*/BFS.
- Database read-only via Room createFromAsset.
- Evakuasi berjalan cepat, bukan berlari/kendaraan.
- Tidak ada live family tracking, ambang magnitudo sendiri, atau klaim okupansi TES luring.
- Sistem bukan pengganti BPBD/BMKG dan tidak menjamin keselamatan.

Data aktif:
- ranah_siaga.db = 70.176.768 byte (~66,93 MiB).
- 31.813 node, 39.216 edge, 31.813 set rute rank 1–3.
- 143 TES, total kapasitas 290.856.
- 16.172 node is_safe=1, 15.641 is_safe=0.
- 31 poligon luar zona rendaman, 73 poligon rendaman: 44 rendah, 20 sedang, 9 tinggi.

Sudah ada:
- GPS -> node -> rute lokal -> polyline -> manuver -> tujuan/ETA.
- Dua mode UI dengan panel vertikal halus.
- Map pan/zoom/rotate, heading-up follow, kompas nyata, recenter.
- Marker pengguna di layer atas, tujuan TES + callout.
- Jika >18 m dari rute: garis putus-putus ke proyeksi titik masuk jalan dan instruksi mengikuti heading.
- Lurus diprioritaskan sampai belokan berjarak <=50 m.
- Rute utama + dua alternatif; rute lama abu-abu dan ETA pembanding.
- Zona hybrid + layer transparan + notifikasi perpindahan.
- Countdown 20 menit sejak aplikasi dibuka dengan label eksplisit.
- Kedatangan TES: radius 20 m, 3 konfirmasi, akurasi <=35 m.
- Widget 2x2 dan 4x2.
- 38 unit test lulus.

Belum/gap:
- Basemap luring penuh; OSM masih internet/cache.
- F07 rencana titik temu keluarga ditunda.
- BMKG, check-in aman, kirim laporan terhalang, dan sinkronisasi belum di Android.
- Fallback setelah semua alternatif gagal belum ada.
- Deteksi tiba saat masuk poligon luar rendaman belum sama dengan deteksi tiba di TES.
- NF02/NF04, uji mode pesawat tanpa cache, uji lapangan, audit aksesibilitas, dan validasi BPBD belum terdokumentasi.
- Backend dan spatial source tidak ada pada branch ini; klaim proposal belum dapat diverifikasi dari repository ini.
- Perubahan terbaru masih lokal/uncommitted dan APK lama dapat tertinggal.

Aturan bahasa:
- Katakan “Lokasi Anda di zona rendaman” atau “di luar zona rendaman”, bukan “Anda aman”.
- Katakan “Dihitung sejak aplikasi dibuka”.
- Gunakan TES/TEA, berjalan cepat, alternatif tujuan/rute.

Fokus saat ini: sempurnakan dan buktikan alur rute luring sebelum memperluas fitur lain.
```

## 18. Sumber yang Digabungkan

- `C:\Users\user\Downloads\PROPOSAL VERSI 3.pdf` — 44 halaman, dibaca penuh.
- `CLAUDE.md` — keputusan implementasi Android dan batas fitur.
- `CONTEXT.md` — sejarah brainstorming, aturan GEMASTIK, keputusan yang ditolak, dan konteks tim.
- `PRD.md` — kebutuhan awal dan arsitektur tiga mode.
- `docs/ARCHITECTURE.md` — alur dan batas komponen Android.
- `docs/DATABASE_AUDIT.md` — audit database versi lama; sebagian temuannya sudah tidak berlaku setelah DB hybrid diperbarui.
- `docs/OFFLINE_BASEMAP_PLAN.md` — rencana ubin regional dan optimasi ukuran.
- Kode di `android/app/src/main/` dan unit test di `android/app/src/test/`.
- Pemeriksaan SQLite read-only terhadap database aset aktif.
- Pemeriksaan Git, artefak APK, dan hasil `:android:app:testDebugUnitTest` pada 10 Agustus 2026.

---

**Pesan utama untuk tim dan AI:** jangan menyamakan “tertulis di prmoposal” dengan “sudah terverifikasi”. Siaga Padang saat ini memiliki alur navigasi Android lokal yang kuat, tetapi klaim basemap luring, backend, simulasi, performa lintas perangkat, dan validasi lapangan harus dibuktikan secara terpisah sebelum dipakai dalam proposal atau presentasi juri.
