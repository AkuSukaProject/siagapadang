# Backlog Fitur SIAGA PADANG yang Belum Selesai

Dokumen ini menjadi daftar kerja untuk pengembangan lanjutan SIAGA PADANG. Audit dilakukan pada branch `main`, commit `61bed05`, tanggal 14 September 2026.

## Arti Status

- **Belum**: fitur belum tersedia pada aplikasi yang digunakan pengguna.
- **Sebagian**: sebagian alur sudah bekerja, tetapi belum memenuhi perilaku akhir.
- **Backend siap**: endpoint tersedia dan sudah diuji, tetapi aplikasi Android belum memakainya.
- **Perlu validasi**: kode tersedia, tetapi bukti pengujian atau validasi lapangan belum cukup.

## Fitur yang Sudah Ada

Bagian berikut tidak perlu dibuat ulang:

- Widget akses cepat SIAGA PADANG.
- Pembacaan GPS dan pencarian simpul jalan terdekat.
- Penentuan status zona dari data lokal.
- Penyajian rute utama dan dua alternatif dari SQLite lokal.
- Petunjuk arah, kompas, jarak, ETA, dan hitung mundur.
- Pergantian rute lokal ketika pengguna menekan tombol **Jalur Terhalang**.
- Konfirmasi tiba di dekat TES atau ujung rute.
- Overlay lokal jaringan jalan, rute, tujuan, dan zona.
- Integrasi Android dengan status gempa terbaru BMKG melalui backend.
- Layar peringatan ketika data resmi BMKG menyatakan potensi tsunami.
- Backend untuk check-in, laporan hambatan, okupansi, event aktif, dan metadata sinkronisasi dasar.

## Prioritas 0 — Dikerjakan Lebih Dahulu

### P0-01 — Membawa ID Lokal sampai ke State Navigasi Android

**Status:** Belum
**Tujuan:** Menyediakan ID yang diperlukan saat Android mengirim check-in dan laporan jalur.

Saat ini `tes_id` dan `edge_id` ada di SQLite, tetapi belum seluruhnya dibawa ke `EvacuationRoute` dan UI state. API tidak boleh mengandalkan nama TES karena nama dapat berubah atau tidak unik.

Pekerjaan:

- Tambahkan `destinationExternalId` pada model rute.
- Pertahankan urutan ID ruas yang membentuk setiap rute.
- Simpan versi dataset lokal yang sedang digunakan.
- Tentukan ruas aktif atau ruas terdekat saat pengguna melaporkan hambatan.
- Tambahkan tes DAO dan repository untuk memastikan ID tidak hilang.

Lokasi kode awal:

- `android/app/src/main/java/com/akusukaproject/siagapadang/data/local/DatabaseRows.kt`
- `android/app/src/main/java/com/akusukaproject/siagapadang/data/local/EvacuationDao.kt`
- `android/app/src/main/java/com/akusukaproject/siagapadang/data/model/EvacuationRoute.kt`
- `android/app/src/main/java/com/akusukaproject/siagapadang/data/repository/EvacuationRepository.kt`
- `android/app/src/main/java/com/akusukaproject/siagapadang/ui/evacuation/EvacuationUiState.kt`

Kriteria selesai:

- Rute aktif memiliki ID tujuan, versi dataset, dan daftar ID ruas.
- Pergantian rank tidak mencampurkan ID dari rute sebelumnya.
- Tes membuktikan ID dari SQLite sama dengan payload yang akan dikirim ke backend.

### P0-02 — Infrastruktur API Android untuk Fitur Daring

**Status:** Sebagian
**Tujuan:** Menyediakan klien bersama untuk endpoint selain BMKG tanpa mengganggu navigasi luring.

Pekerjaan:

- Buat ID perangkat anonim yang stabil dan simpan pada penyimpanan aplikasi.
- Kirim ID tersebut melalui header `X-Device-ID`.
- Tambahkan model request/response untuk event aktif, check-in, laporan hambatan, dan okupansi.
- Gunakan timeout singkat serta pesan `berhasil`, `tertunda`, atau `gagal` yang jujur.
- Pastikan semua permintaan berjalan di thread I/O dan dapat dibatalkan.
- Jangan menunggu respons API sebelum menampilkan atau mengganti rute lokal.
- Tambahkan pengujian parsing JSON dan kegagalan jaringan.

Lokasi kode awal:

- `android/app/src/main/java/com/akusukaproject/siagapadang/data/remote/BmkgApiClient.kt`
- `android/app/src/main/java/com/akusukaproject/siagapadang/SiagaPadangApplication.kt`
- `android/app/src/main/java/com/akusukaproject/siagapadang/ui/evacuation/EvacuationViewModel.kt`

Kriteria selesai:

- Setiap endpoint mempunyai DTO dan penanganan error yang teruji.
- Putusnya internet tidak menutup, menunda, atau mereset navigasi lokal.
- Tidak ada token, rahasia, atau identitas pribadi yang ditanam di APK.

### P0-03 — Check-in Keselamatan pada Android

**Status:** Backend siap; Android belum
**Dependensi:** P0-01 dan P0-02.

Pekerjaan:

- Tampilkan tombol check-in hanya setelah kedatangan dikonfirmasi.
- Ambil event aktif dari `GET /api/v1/status/emergency`.
- Kirim `POST /api/v1/shelter/checkin` dengan ID TES/TEA, koordinat, dan akurasi GPS.
- Tampilkan alasan yang jelas saat event tidak aktif, lokasi terlalu jauh, atau akurasi lebih buruk dari batas backend.
- Cegah pengguna mengira check-in berhasil ketika permintaan masih tertunda atau gagal.
- Simpan status pengiriman saat rotasi layar atau aplikasi masuk latar belakang.

Kriteria selesai:

- Check-in berhasil dari dua perangkat uji menuju event dan TES yang sama.
- Percobaan sebelum tiba tidak tersedia dari alur normal.
- Respons duplikat, timeout, GPS buruk, dan event tidak aktif ditangani di UI.
- Navigasi dan dialog kedatangan tetap bekerja tanpa internet.

### P0-04 — Pengiriman Laporan Jalur Terhalang

**Status:** Pergantian lokal selesai; pengiriman Android belum
**Dependensi:** P0-01 dan P0-02.

Pekerjaan:

- Pertahankan urutan: ganti rute lokal terlebih dahulu, lalu kirim laporan.
- Kirim `edge_external_id`, `dataset_version_id`, posisi, dan keterangan ke `POST /api/v1/reports/obstruction`.
- Buat antrean lokal untuk laporan ketika internet tidak tersedia.
- Hapus atau tandai selesai laporan setelah backend menerimanya.
- Tampilkan status laporan tanpa menghalangi petunjuk evakuasi.
- Tambahkan endpoint backend untuk membaca daftar ruas yang sudah dikonfirmasi terhalang pada event dan versi dataset aktif.
- Hubungkan daftar hambatan terkonfirmasi ke Android agar laporan benar-benar bermanfaat bagi perangkat lain.

Kriteria selesai:

- Rute berganti seketika dalam mode pesawat.
- Laporan tertunda terkirim ketika koneksi kembali.
- Tiga perangkat berbeda dapat mengubah status hambatan menjadi terkonfirmasi sesuai aturan backend.
- Perangkat lain dapat menerima daftar hambatan terkonfirmasi tanpa salah versi dataset.

### P0-05 — Okupansi TES/TEA pada Android

**Status:** Backend siap; Android belum
**Dependensi:** P0-02 dan P0-03.

Pekerjaan:

- Setelah check-in berhasil, tampilkan pilihan `LOW`, `MODERATE`, atau `FULL` dengan istilah Indonesia yang mudah dipahami.
- Kirim laporan ke `POST /api/v1/shelter/occupancy`.
- Ambil status agregat dari `GET /api/v1/shelter/{external_id}/occupancy`.
- Tampilkan jumlah laporan, waktu pembaruan, dan sumber **laporan pengguna yang sudah check-in**.
- Tampilkan `belum ada data` untuk status `UNKNOWN`.
- Jangan menyebut okupansi sebagai data luring atau kapasitas aktual terverifikasi.

Kriteria selesai:

- Pengguna yang belum check-in tidak dapat mengirim laporan.
- Status berubah sesuai laporan terbaru dan tetap membawa sumber serta waktu.
- Gangguan jaringan tidak mengubah rute lokal secara otomatis.

### P0-06 — Deployment Backend dan Konfigurasi Release Android

**Status:** Belum
**Tujuan:** Membuat fitur daring dapat dipakai tanpa `adb reverse` atau komputer pengembang.

Pekerjaan:

- Deploy FastAPI dan PostGIS pada server yang dapat dijangkau perangkat.
- Gunakan HTTPS dan domain tetap.
- Atur `DATABASE_URL`, `HMAC_SECRET`, CORS, logging, backup, dan health check melalui secret server.
- Isi `SIAGA_BACKEND_BASE_URL` saat build release; saat ini nilai default release kosong.
- Pisahkan konfigurasi development, staging, dan production.
- Uji APK release pada jaringan seluler tanpa kabel USB.

Kriteria selesai:

- APK release dapat mengambil status BMKG dari jaringan seluler.
- Check-in dan laporan berhasil tanpa `adb reverse`.
- Tidak ada kredensial server di repository atau APK.
- Gangguan backend menghasilkan pesan singkat dan navigasi luring tetap berjalan.

## Prioritas 1 — Menyelesaikan Klaim Offline-First

### P1-01 — Sinkronisasi Dataset yang Aman

**Status:** Sebagian di backend; belum di Android.

Pekerjaan:

- Tetapkan format paket untuk nodes, edges, routes, TES/TEA, zona rendaman, dan versi data.
- Selesaikan `GET /api/v1/sync/network`; endpoint ini masih placeholder.
- Pastikan `/sync/check` hanya menyatakan update jika versi server lebih baru dari versi lokal klien.
- Sertakan ukuran, checksum, schema version, minimum app version, dan URL unduh.
- Unduh ke file sementara, validasi checksum, lalu terapkan secara atomik.
- Pertahankan data lama jika unduhan, validasi, atau migrasi gagal.
- Sediakan rollback dan tes saat aplikasi ditutup di tengah pembaruan.

Kriteria selesai:

- Instalasi dengan dataset lama dapat diperbarui tanpa memasang ulang APK.
- File rusak atau checksum salah selalu ditolak.
- Navigasi masih menggunakan data lama setelah update gagal.

### P1-02 — Basemap Benar-benar Luring

**Status:** Belum.

Saat ini overlay lokal tetap tampil tanpa internet, tetapi ubin OpenStreetMap bergantung pada jaringan atau cache.

Pekerjaan:

- Pilih format dan pipeline ubin luring yang kompatibel dengan MapLibre.
- Batasi cakupan pada wilayah percontohan dan level zoom yang diperlukan.
- Sertakan atribusi dan lisensi sumber peta.
- Pastikan paket tidak membuat APK atau data aplikasi melebihi target ukuran.
- Uji instalasi bersih dalam mode pesawat; jangan mengandalkan cache dari pengujian sebelumnya.

Kriteria selesai:

- Jalan dasar, label penting, overlay, dan rute tampil setelah instalasi bersih tanpa internet.
- Peta tetap dapat digeser dan diperbesar dalam batas cakupan paket.
- Ukuran paket dan lisensinya terdokumentasi.

### P1-03 — Kedatangan di Luar Zona Rendaman

**Status:** Sebagian.

Saat ini kedatangan dikonfirmasi dekat TES atau ujung rute. Perilaku ini belum sama dengan F-06 yang meminta transisi ketika pengguna memasuki kawasan di luar zona rendaman.

Pekerjaan:

- Tentukan aturan kedatangan untuk TES/TEA dan keluar dari poligon rendaman.
- Bedakan teks **tiba di tujuan** dan **berada di luar zona rendaman**.
- Jangan menggunakan kata **aman** hanya berdasarkan pemeriksaan poligon.
- Pertahankan syarat akurasi dan beberapa pembacaan GPS agar dialog tidak muncul akibat satu titik yang melompat.

Kriteria selesai:

- Transisi zona diuji pada sisi dalam, batas, dan sisi luar poligon.
- Pembacaan GPS buruk tidak memicu kedatangan.
- Redaksi UI tidak memberikan jaminan keselamatan yang tidak dapat diketahui aplikasi.

### P1-04 — Fallback Setelah Semua Rute Alternatif Habis

**Status:** Belum.

Pekerjaan:

- Setelah rank 1–3 ditolak, tampilkan bahwa tidak ada jalur jalan yang dapat diverifikasi.
- Tampilkan arah dan jarak garis lurus sebagai orientasi terakhir.
- Nyatakan bahwa garis tersebut bukan rute aman atau rute yang sudah diperiksa.
- Ingatkan pengguna untuk menjauhi arah pantai dan mengikuti petugas atau rambu lapangan.
- Jangan membuat garis melewati bangunan terlihat sebagai jalur jalan.

Kriteria selesai:

- Aplikasi tidak macet atau kembali diam-diam ke rute yang sudah ditolak.
- Pesan keterbatasan dan tindakan berikutnya mudah dibaca dalam satu layar.

### P1-05 — Administrasi Event Darurat

**Status:** Endpoint baca event aktif tersedia; pengelolaan belum.

Pekerjaan:

- Buat mekanisme berotorisasi untuk membuat, mengaktifkan, menutup, dan membatalkan event.
- Pastikan hanya satu event tsunami aktif pada satu waktu.
- Simpan sumber, alasan perubahan, waktu, dan identitas operator.
- Jangan membuka endpoint administrasi tanpa autentikasi.
- Pisahkan event simulasi/drill dari kejadian nyata.

Kriteria selesai:

- Operator dapat mengelola siklus event tanpa mengubah database secara manual.
- Event simulasi selalu berlabel simulasi pada API dan Android.
- Audit log perubahan tersedia.

### P1-06 — Migrasi Database Produksi

**Status:** Belum aman untuk data produksi.

Pekerjaan:

- Buat migrasi Alembic baru untuk `shelter_occupancy_reports`.
- Hapus operasi `TRUNCATE` dan perubahan enum destruktif dari jalur upgrade produksi.
- Uji upgrade dari schema yang saat ini dipakai tanpa kehilangan check-in atau laporan hambatan.
- Buat backup otomatis dan latihan restore.
- Dokumentasikan perintah migrasi dan rollback.

Kriteria selesai:

- Migrasi diuji pada salinan database berisi data.
- Jumlah record sebelum dan sesudah migrasi tetap sesuai.
- Deployment dapat dibatalkan tanpa menghapus data operasional.

## Prioritas 2 — Fitur Lanjutan

### P2-01 — Rencana Evakuasi Keluarga

**Status:** Belum / sebelumnya ditunda.

Fitur ini disusun pada masa tenang dan disimpan lokal. Fitur ini bukan pelacakan lokasi anggota keluarga secara langsung.

Pekerjaan:

- Tambah anggota keluarga dan lokasi rutinnya.
- Pilih tujuan evakuasi untuk setiap anggota/lokasi.
- Simpan rencana secara lokal dan sediakan tampilan ringkas yang dapat dibuka tanpa internet.
- Sediakan ekspor atau berbagi rencana tanpa membagikan lokasi real-time.

Kriteria selesai:

- Seluruh rencana tetap dapat dibaca dalam mode pesawat.
- Tidak ada klaim bahwa aplikasi mengetahui posisi anggota keluarga saat bencana.

### P2-02 — Mode Drill

**Status:** Belum.

Pekerjaan:

- Buat event latihan yang tidak dapat disalahartikan sebagai peringatan nyata.
- Catat waktu mulai, waktu tiba, tujuan, dan hasil latihan dengan persetujuan pengguna.
- Sediakan ringkasan hasil dan ekspor data anonim.
- Pisahkan warna, label, notifikasi, dan backend drill dari kejadian nyata.

Kriteria selesai:

- Setiap layar latihan menampilkan label **SIMULASI/DRILL**.
- Tidak ada data latihan yang masuk ke okupansi atau event darurat nyata.

### P2-03 — Simulasi Pergerakan Kolektif yang Reproducible

**Status:** Hasil/data ada; source pipeline belum lengkap di repository.

Pekerjaan:

- Masukkan source code pembentukan graf, pembangkitan populasi, pembobotan kapasitas, dan perhitungan rute.
- Simpan konfigurasi, seed, versi dataset, dan parameter kecepatan.
- Bandingkan rute berbobot kapasitas dengan lintasan terpendek pada input identik.
- Hasil minimum: persentase tiba dalam target waktu, beban tiap ruas, antrean titik sumbat, dan kelebihan kapasitas tujuan.
- Buat perintah tunggal untuk menghasilkan ulang SQLite Android dan laporan simulasi.

Kriteria selesai:

- Anggota tim lain dapat menjalankan simulasi dari repository dan memperoleh hasil yang sama.
- Setiap database Android dapat ditelusuri ke konfigurasi dan versi dataset pembentuknya.
- Klaim keunggulan metode didukung angka pembanding.

### P2-04 — Dashboard Analisis untuk Pengelola

**Status:** Belum.

Pekerjaan:

- Tampilkan distribusi beban TES/TEA dan titik sumbat hasil simulasi.
- Tampilkan status event, laporan hambatan terkonfirmasi, check-in, dan okupansi dengan sumber/waktu.
- Pisahkan data simulasi dari data kejadian nyata.
- Terapkan autentikasi dan pembatasan peran.

Kriteria selesai:

- Pengelola dapat melihat data tanpa menjalankan query database manual.
- Setiap angka menampilkan sumber, versi dataset, dan waktu pembaruan.

## Pekerjaan Validasi dan Kualitas

### Q-01 — Pengujian Perangkat dan Kinerja

- Ukur waktu dari aplikasi dibuka sampai arahan pertama tampil.
- Uji mode pesawat pada instalasi bersih.
- Uji minimal 3–5 perangkat berbeda merek dan versi Android.
- Uji GPS buruk, izin ditolak, sensor kompas tidak tersedia, baterai hemat, rotasi layar, dan aplikasi kembali dari latar belakang.
- Catat ukuran APK per ABI dan ukuran dataset setelah basemap luring ditambahkan.

### Q-02 — Aksesibilitas dan Keterbacaan Darurat

- Audit kontras minimum, target sentuh minimal 48 dp, ukuran teks, TalkBack, dan content description.
- Uji layar kecil, font sistem besar, mode gelap, serta penggunaan di bawah cahaya luar ruangan.
- Pastikan informasi utama tidak hanya dibedakan menggunakan warna.

### Q-03 — Validasi Data dan Redaksi dengan BPBD

- Validasi lokasi, nama, kapasitas, kondisi, dan akses masuk TES/TEA.
- Validasi zona rendaman, terminologi, waktu sasaran, dan arahan setelah tiba.
- Catat sumber, tanggal, penanggung jawab, serta versi setiap dataset.
- Ganti data kandidat atau OSM dengan data resmi ketika tersedia.

### Q-04 — Release dan Otomasi

- Siapkan signing release di luar repository.
- Tambahkan CI untuk unit test Android, build debug, pemeriksaan Python, tes backend, dan validasi OpenAPI.
- Perbarui `backend/openapi.json` secara otomatis ketika kontrak API berubah.
- Buat catatan versi, prosedur rollback APK/data, serta pemeriksaan lisensi aset.

### Q-05 — Rapikan Dokumentasi yang Tertinggal

- Perbarui `CONTEXT.md` yang masih menyatakan Android belum dimulai.
- Perbarui tabel status lama yang masih menyebut integrasi BMKG Android belum tersedia.
- Perbaiki karakter rusak pada beberapa dokumen hasil konversi.
- Jadikan dokumen ini atau satu issue tracker sebagai sumber status utama agar daftar tidak saling bertentangan.

## Saran Pembagian kepada Teman

Pekerjaan berikut dapat dimulai paralel:

| Bagian | Tugas | Dependensi |
|---|---|---|
| Android data | P0-01 | Tidak ada |
| Android API dasar | P0-02 | Tidak ada |
| Backend | Endpoint baca hambatan pada P0-04, P1-05, P1-06 | Tidak ada |
| Peta | P1-02 | Tidak ada |
| Simulasi | P2-03 | Tidak ada |
| Validasi | Q-01, Q-02, Q-03 | Dapat dimulai dari fitur yang sudah tersedia |

Setelah P0-01 dan P0-02 selesai, lanjutkan P0-03, P0-04, dan P0-05. P0-06 diperlukan untuk pengujian lintas perangkat melalui internet. Sinkronisasi dataset pada P1-01 sebaiknya dikerjakan setelah format data dan migrasi pada P1-06 disepakati.

## Aturan Keselamatan yang Harus Dipertahankan

- Sistem tidak mendeteksi gempa secara otomatis.
- Potensi tsunami selalu mengikuti pernyataan resmi BMKG.
- Gunakan istilah **TES**, **TEA**, dan **Tsunami Safe Zone** sesuai konteks.
- Gunakan arahan **berjalan cepat**, bukan berlari.
- Gunakan teks **di luar zona rendaman**, bukan klaim **aman**, jika sistem hanya memeriksa poligon.
- Pergantian rute lokal tidak boleh menunggu internet.
- Okupansi harus mempunyai sumber dan waktu pembaruan; jangan mengklaim data tersebut tersedia saat luring.
- Navigasi inti harus tetap berjalan ketika seluruh layanan backend gagal.
