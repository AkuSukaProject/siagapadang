# Arsitektur Android Siaga Padang

Dokumen ini merapikan rancangan MVVM awal dan menyelaraskannya dengan keputusan terbaru di `CLAUDE.md`.

## Prinsip

- Kotlin + Jetpack Compose dengan unidirectional data flow.
- UI hanya merender state dan mengirim event pengguna.
- Composable tidak mengakses DAO, lokasi, atau sensor secara langsung.
- Seluruh akses rute berasal dari basis data lokal read-only.
- Perhitungan rute tidak dilakukan di Android.
- Logika domain tidak bergantung pada Android agar dapat diuji tanpa emulator.
- Angka performa hanya dicantumkan setelah diukur pada perangkat nyata.

## Aliran ketergantungan

```text
Compose UI
    |
    v
ViewModel -- UiState + UiEvent
    |
    v
Domain use cases
    |
    v
Repository interfaces
    |
    +--> Room/SQLite (rute dan zona)
    +--> LocationProvider (GPS)
    `--> CompassProvider (sensor)
```

Ketergantungan kode utama tetap `ui → domain → data`. Implementasi sumber perangkat berada di `sensor/` dan dipakai melalui abstraksi yang sesuai; kalkulasi bearing berada di `domain/BearingCalculator.kt`, bukan ditanam di Composable atau ViewModel.

## Struktur paket yang dituju

```text
data/
|-- local/          # Entity, DAO, dan Room database read-only
|-- repository/     # Implementasi repository
`-- model/          # Model data
domain/
|-- NearestNodeFinder.kt
|-- PolylineAssembler.kt
|-- ZoneChecker.kt
`-- BearingCalculator.kt
ui/
|-- evacuation/     # Screen, UiState, UiEvent, dan ViewModel
|-- zonestatus/
|-- familyplan/
`-- theme/
sensor/
|-- LocationProvider.kt
`-- CompassProvider.kt
widget/             # AppWidgetProvider berbasis XML
```

## Alur evakuasi inti

```text
GPS
  → kandidat node dalam bounding box
  → node terdekat
  → rank rute dari tb_routes
  → ruas terkait dari tb_edges dalam satu kueri batch
  → normalisasi arah dan parsing WKT
  → fallback koordinat node jika WKT ruas kosong
  → polyline + TES tujuan + ETA
  → UiState
  → peta, panah arah, dan hitung mundur
```

`PolylineAssembler` menjadi satu-satunya tempat untuk parsing WKT dan pembalikan urutan koordinat ruas. Koordinat WKT dibaca sebagai `lon lat`.

Peta dasar pengembangan memakai ubin OpenStreetMap saat jaringan tersedia. Style selalu memiliki latar polos lokal; marker dan polyline berasal dari data perangkat sehingga tetap dapat dirender ketika ubin tidak tersedia. Ubin luring produksi belum disertakan.

## Batas tanggung jawab

| Komponen | Tanggung jawab |
|---|---|
| Composable | Merender `UiState`, meneruskan `UiEvent`, mengelola interop MapLibre secara aman |
| ViewModel | Mengorkestrasi alur, coroutine, dan perubahan state |
| Domain | Pencarian node terdekat, perakitan polyline, pemeriksaan zona, dan bearing |
| Repository | Menyatukan akses DAO dan sumber data perangkat |
| DAO | Kueri `SELECT` terhadap database aset |
| Sensor provider | Membungkus API lokasi dan sensor Android |

MapLibre dibungkus dengan `AndroidView`. Siklus hidup `MapView` harus mengikuti host dan resource-nya dilepas melalui `onDispose` untuk mencegah kebocoran memori.

## Aturan data dan bahasa

- `ranah_siaga.db` dibuka dengan Room `createFromAsset()` dan tidak dimigrasikan atau ditulis.
- Tidak ada `INSERT`, `UPDATE`, atau `DELETE` terhadap basis data tersebut.
- Gunakan TES/TEA, bukan *shelter*.
- Gunakan “berjalan cepat”, bukan “lari” atau “berlari”.
- Jalur berikutnya disebut “alternatif tujuan”, bukan “TES penuh”.

## Verifikasi minimum

- Uji domain untuk WKT, pembalikan ruas, bearing, node terdekat, dan point-in-polygon.
- Uji DAO terhadap salinan database aset.
- Uji perangkat dalam mode pesawat untuk alur end-to-end.
- Ukur waktu dari permintaan lokasi yang tersedia sampai arahan siap menggunakan pencatatan waktu nyata; jangan mengasumsikan angka performa.
