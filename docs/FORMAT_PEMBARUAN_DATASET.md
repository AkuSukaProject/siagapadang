# Format Pembaruan Dataset SIAGA PADANG

## Paket

Pembaruan menggunakan satu berkas SQLite lengkap dengan media type
`application/vnd.sqlite3`. Paket harus memuat tabel berikut:

- `tb_nodes`
- `tb_edges`
- `tb_routes`
- `tb_tes`
- `tb_inundation_zones`
- `tb_safe_zones`

Skema yang didukung Android saat ini adalah `android-v1`. Paket dibatasi maksimum 250 MiB.

## Metadata

Versi aktif dicatat pada `data_versions` dengan nilai berikut:

- `dataset_name`: `network`
- `version`: versi numerik bertingkat, misalnya `2026.09.16`
- `schema_version`: `android-v1`
- `checksum`: SHA-256 berkas SQLite lengkap
- `size_bytes`: ukuran berkas yang tepat
- `minimum_app_version`: versi Android minimum yang dapat membuka paket

`GET /api/v1/sync/check` menerima `dataset_name`, `current_version`, dan
`current_checksum`. Server hanya menawarkan versi numerik yang lebih baru dan checksum yang
berbeda. URL unduhan dikembalikan melalui `download_url`.

`GET /api/v1/sync/network?version=...` memeriksa ulang ukuran dan checksum paket aktif sebelum
mengirim berkas. Jika paket server tidak sesuai metadata, endpoint mengembalikan HTTP 503.

## Publikasi

1. Hasilkan `ranah_siaga.db` baru dengan seluruh tabel yang diperlukan.
2. Jalankan `PRAGMA quick_check` dan pengujian rute terhadap paket tersebut.
3. Jalankan bootstrap dengan label versi yang lebih baru:

   ```bash
   python scripts/bootstrap_from_android_db.py /lokasi/ranah_siaga.db --version 2026.09.16
   ```

4. Tempatkan berkas yang sama pada lokasi `DATASET_PACKAGE_PATH` server.
5. Mulai ulang API jika berkas pada path yang sama diganti agar cache checksum proses dibersihkan.
6. Periksa `/api/v1/sync/check`, lalu unduh `/api/v1/sync/network` dan cocokkan SHA-256 secara
   terpisah sebelum mengumumkan versi.

## Aktivasi Android dan rollback

Android menulis unduhan ke berkas dengan akhiran `.download` di direktori database. Berkas baru
tidak mengubah database yang sedang dipakai sampai ukuran, SHA-256, `PRAGMA quick_check`, tabel,
dan kolom wajib lulus validasi. Setelah valid, berkas dipindahkan ke nama berbasis checksum dan
pointer database aktif disimpan secara atomik.

Database baru dibuka pada peluncuran aplikasi berikutnya. Jika validasi Room gagal, aplikasi
mengembalikan pointer ke database sebelumnya dan menghapus paket yang gagal. Jika aplikasi
ditutup saat mengunduh, pointer tidak berubah dan berkas `.download` dibersihkan saat aplikasi
dibuka kembali.
