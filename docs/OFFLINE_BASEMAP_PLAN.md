# Rencana Basemap Offline Regional

## Status

Dokumen ini mencatat keputusan pengembangan lanjutan setelah alur navigasi rute
evakuasi stabil. Rencana ini **belum diimplementasikan** pada APK saat ini.

Saat ini MapLibre berfungsi sebagai mesin render. Basemap OpenStreetMap masih
diambil dari internet dan hanya dapat muncul tanpa jaringan bila ubinnya sudah
masuk cache. Rute, tujuan, ETA, dan petunjuk arah tetap berasal dari basis data
lokal.

## Keputusan cakupan

- Jangan mengemas seluruh Sumatera Barat untuk MVP.
- Prioritaskan **Kecamatan Padang Barat** sesuai PRD.
- Cakupan dapat diperluas ke **Kota Padang** setelah ukuran dan kelengkapan data
  terukur.
- Batasi kamera ke wilayah data agar pengguna tidak melihat area tanpa rute.
- Pertahankan fallback navigasi tanpa basemap: marker pengguna, polyline, arah,
  nama TES, jarak, dan waktu harus tetap berfungsi.

Basis data rute yang tersedia sudah bersifat regional, dengan rentang koordinat
node sekitar:

- lintang: `-1.132064` sampai `-0.7932407`
- bujur: `100.294113` sampai `100.5354837`

Tidak ada paket peta dunia di dalam APK yang dapat dihapus. Membundel basemap
offline baru akan menambah ukuran APK.

## Strategi teknis

1. Siapkan vector tiles regional untuk Padang Barat terlebih dahulu.
2. Pilih tingkat zoom minimum yang tetap terbaca untuk pejalan kaki; kandidat
   awal `z12–z17`, lalu ukur ukuran sebenarnya.
3. Simpan paket tiles sebagai aset regional yang dapat dibaca MapLibre tanpa
   jaringan.
4. Tambahkan batas kamera dan batas zoom sesuai cakupan paket.
5. Batasi ukuran cache MapLibre dan sediakan kebijakan pembersihan cache.
6. Uji mode pesawat pada beberapa perangkat tanpa cache jaringan sebelumnya.
7. Nyatakan atribusi sumber data peta secara benar di UI dan dokumentasi.

## Strategi ukuran aplikasi

- Distribusikan APK terpisah untuk `arm64-v8a` dan `armeabi-v7a`; jangan membawa
  library MapLibre untuk seluruh ABI dalam satu APK pengguna.
- Aktifkan minifikasi pada build release setelah aturan ProGuard/R8 MapLibre
  terverifikasi.
- Jangan memangkas basis data rute hanya demi ukuran sebelum kontinuitas dan
  keselamatan seluruh rute diuji.
- Catat ukuran APK, ukuran setelah instalasi, ukuran database, dan cache MapLibre
  untuk setiap kandidat paket tiles.

Baseline saat dokumen dibuat:

| Komponen | Ukuran |
|---|---:|
| APK debug universal | 68,38 MB |
| Basis data rute terpasang | 65,04 MB |
| Cache awal MapLibre pada perangkat uji | sekitar 12 MB |
| Total ruang aplikasi pada perangkat uji | sekitar 147 MB |

Target awal APK khusus `arm64-v8a` tanpa basemap offline diperkirakan sekitar
34 MB. Angka final harus diukur dari artefak release, bukan dijadikan klaim
sebelum build tersedia.

## Kriteria selesai

- Basemap tampil pada mode pesawat di seluruh cakupan MVP.
- Navigasi tetap dapat digunakan bila basemap gagal dimuat.
- Tidak ada panning keluar cakupan data.
- Waktu penyajian arahan tetap di bawah target NF-02.
- Ukuran paket dan ruang instalasi terdokumentasi dari perangkat nyata.
- Rute yang melintasi batas cakupan tidak terpotong.
