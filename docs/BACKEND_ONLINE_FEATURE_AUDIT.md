# Audit Backend dan Tahapan Fitur Online

Audit ini dilakukan terhadap branch `feat/android-foundation` setelah dibandingkan dengan branch backend terbaru. Source endpoint backend pada branch Android sudah setara dengan revisi backend terbaru, sementara konfigurasi rahasia pada branch ini lebih aman karena tidak menyimpan kredensial bawaan.

## Status saat ini

| Fitur | Backend | Android | Status |
|---|---|---|---|
| Status resmi BMKG | Endpoint tersedia, timeout dan status kesegaran sudah diperbaiki | Sudah terhubung, memiliki indikator, detail sumber, muat ulang, dan layar peringatan tsunami | Tahap 1 selesai |
| Check-in aman | Endpoint, pemilihan event aktif oleh server, dan validasi jarak tersedia | Dialog tiba masih lokal | Backend siap; integrasi Android belum dibuat |
| Laporan jalur terhalang | Endpoint, deduplikasi, dan konfirmasi tiga perangkat tersedia | Tombol masih hanya mengganti rute lokal | Backend dan pemetaan data siap; integrasi Android belum dibuat |
| Okupansi TES/TEA | Endpoint laporan dan status agregat tersedia; pengirim wajib sudah check-in | Belum ada tampilan data okupansi | Backend siap; integrasi Android belum dibuat |
| Sinkronisasi data | Metadata dan GeoJSON TES tersedia | Belum ada pengunduh/penerapan update | Endpoint graf masih placeholder; database Android masih aset read-only |

## Hasil verifikasi backend

- PostGIS dapat dibuat dari database kosong dan pemeriksaan `/health/db` berhasil.
- Sepuluh tes endpoint event, check-in, validasi GPS, laporan jalur, okupansi, dan BMKG lulus pada database uji terisolasi.
- Kegagalan database sekarang menghasilkan HTTP 503.
- Kegagalan BMKG tanpa cache sekarang menghasilkan HTTP 503, bukan data contoh yang tampak aktual.
- Cache BMKG membawa penanda `live` atau `stale`, waktu pengambilan, dan atribusi sumber.
- Endpoint `/api/v1/status/emergency` memungkinkan klien mengetahui event aktif tanpa menebak ID kejadian.
- Skrip `bootstrap_from_android_db.py` menyamakan ID TES, ID ruas, dan versi dataset dari SQLite Android ke PostGIS.
- Snapshot `backend/openapi.json` sudah dibuat ulang dari kontrak FastAPI aktif.
- Skrip `init_all_tables.py` sudah dapat dijalankan langsung dan `pytest` hanya mengambil tes dari folder `tests`.

## Penghambat yang harus diselesaikan sebelum tahap berikutnya

1. Backend belum memiliki alur administrasi untuk membuat dan mengaktifkan satu `EmergencyEvent`. Check-in dan laporan jalan tetap menolak permintaan bila event aktif tidak tersedia.
2. Android belum membawa `tes_id`, `edge_id`, dan versi dataset sampai ke klien API. Bootstrap backend sudah tersedia, tetapi kontrak ini belum digunakan oleh aplikasi.
3. `/api/v1/sync/network` masih placeholder. Memasang data baru membutuhkan format paket, checksum, migrasi atomik, rollback, dan strategi database Room yang dapat ditulis.
4. Status okupansi backend berasal dari laporan pengguna yang sudah check-in, bukan kapasitas aktual. Android harus menampilkan sumber, jumlah laporan, dan waktu pembaruan secara jelas.
5. Migrasi Alembic lama memuat operasi destruktif. Migrasi itu perlu dirapikan sebelum diarahkan ke database berisi data produksi.

## Urutan implementasi berikutnya

1. Bawa ID TES, ID ruas, dan versi dataset dari repository lokal ke state navigasi Android.
2. Kirim laporan jalur dan check-in dari Android dengan antrean lokal, retry singkat, dan status pengiriman yang jujur.
3. Hubungkan laporan dan tampilan okupansi Android beserta sumber dan waktu pembaruan.
4. Implementasikan sinkronisasi paket bertanda checksum secara atomik dengan rollback bila validasi gagal.

Navigasi, zona, GPS, kompas, dan pemilihan rute tetap berjalan dari data lokal pada semua tahap.
