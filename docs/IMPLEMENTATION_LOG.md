# Log Implementasi SIAGA PADANG

Dokumen ini mencatat perubahan pengembangan, hasil pemeriksaan, dan validasi yang masih ditunda.

## 16 September 2026 — P1-01 Sinkronisasi Dataset yang Aman

- Branch: `feat/p1-safe-dataset-sync`
- Commit: `5ddf489` (`feat: implement safe dataset package updates`)
- Backend menyediakan paket SQLite jaringan lengkap melalui endpoint sinkronisasi beserta versi,
  versi skema, ukuran file, checksum SHA-256, versi minimum aplikasi, dan URL unduhan.
- Android mengunduh paket ke file sementara, memeriksa metadata, checksum, dan struktur SQLite,
  lalu menjadwalkan aktivasi pada pembukaan aplikasi berikutnya.
- Database lama dipertahankan ketika unduhan atau pemeriksaan gagal. Jika Room gagal membuka
  database baru, aplikasi mengembalikan database cadangan.
- File unduhan yang terputus dibersihkan dan tidak mengganti dataset aktif.
- Unit test Android selesai dan APK debug pernah dipasang serta dibuka pada perangkat terhubung.
- **Validasi ditunda atas arahan pengguna:** pembaruan end-to-end memakai dua versi dataset nyata,
  penolakan checksum salah, gangguan unduhan, dan rollback pada perangkat/backend.

## 16 September 2026 — P1-03 Kedatangan di Luar Zona Rendaman

- Branch: `feat/p1-arrival-zone-transition`
- Menambahkan `ZoneExitConfirmationTracker` untuk membedakan posisi awal di luar zona dari
  perpindahan nyata dari dalam ke luar zona rendaman.
- Keluar zona dikonfirmasi setelah tiga pembacaan berturut-turut dengan akurasi GPS maksimal
  35 meter. Pembacaan dengan akurasi buruk, data zona yang tidak tersedia, atau pembacaan kembali
  di dalam zona memutus rangkaian konfirmasi.
- Pemeriksaan zona dijalankan setelah perpindahan 12 meter atau paling lambat setiap 1,5 detik
  selama data lokasi diterima.
- Menambahkan alasan penyelesaian evakuasi: sampai di titik evakuasi atau keluar dari zona rendaman.
- Dialog dan tombol navigasi menampilkan redaksi yang sesuai dengan alasan tersebut. Redaksi keluar
  zona tidak memakai kata “aman” dan tetap menyuruh pengguna menjauhi pantai serta mengikuti petugas.
- Check-in posko dan pelaporan okupansi hanya dapat dilakukan ketika kedatangan dikonfirmasi di TES.
- Unit test `:android:app:testDebugUnitTest` lulus, termasuk empat skenario baru untuk posisi awal
  di luar zona, tiga konfirmasi keluar, pembacaan batas/dalam, dan akurasi GPS buruk.
- **Validasi ditunda atas arahan pengguna:** simulasi lokasi pada perangkat, pengujian batas poligon
  secara end-to-end, dan pemeriksaan tampilan dialog di berbagai ukuran layar.
