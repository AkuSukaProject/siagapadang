# CLAUDE.md — Siaga Padang (Android)

**Diperbarui:** 8 Agustus 2026
**Menggantikan:** PRD.md v5 dan CONTEXT.md v3 untuk hal-hal yang bertentangan. Dokumen ini yang menang.

> Dokumen ini untuk membangun **aplikasi Android**. Sisi data (basis data spasial, prakomputasi rute) sudah selesai dan tidak perlu dibangun ulang.

---

## 1. Apa yang dibangun

Aplikasi navigasi evakuasi tsunami yang **berfungsi penuh tanpa koneksi internet**. Pengguna membuka aplikasi setelah merasakan guncangan, dan dalam waktu di bawah satu detik memperoleh arah menuju titik evakuasi terdekat yang masih memiliki kapasitas.

Seluruh perhitungan rute sudah dilakukan sebelumnya. **Aplikasi tidak menjalankan Dijkstra, A*, maupun algoritma pencarian lintasan apa pun.** Aplikasi hanya membaca basis data lokal.

### Target gerbang evaluasi 13 Agustus

Satu alur yang benar-benar berjalan, dengan mode pesawat aktif:

1. Baca GPS → cari persimpangan terdekat
2. Ambil rute dari basis data lokal
3. Rangkai geometri jalan → gambar polyline di peta
4. Panah kompas mengikuti arah hadap perangkat
5. Hitung mundur sisa waktu evakuasi

Fitur lain menyusul. Yang tidak boleh gagal: **semuanya harus jalan tanpa jaringan.**

---

## 2. Tumpukan teknologi

| Lapisan | Pilihan | Catatan |
|---|---|---|
| Bahasa | Kotlin | |
| UI | **Jetpack Compose** | Diputuskan 8 Agustus, menggantikan XML |
| Peta | MapLibre Native Android | Berbasis View — bungkus dengan `AndroidView` |
| Basis data | SQLite via Room | File sudah jadi, taruh di `assets/` |
| Widget | `AppWidgetProvider` + **XML** | Glance tidak dipakai (waktu tidak cukup) |
| Sensor | `FusedLocationProviderClient`, `SensorManager` (magnetometer) | |
| Ubin peta | MBTiles / PMTiles | Opsional — lihat Bagian 6 |

### Catatan MapLibre + Compose

MapLibre adalah pustaka berbasis View. Bungkus dengan `AndroidView`, dan **pastikan `MapView` dilepas pada `onDispose`** untuk mencegah kebocoran memori saat recomposition. Ini titik risiko teknis terbesar di proyek ini — kerjakan lebih dulu, jangan ditunda.

---

## 3. Basis data — `ranah_siaga.db`

**Ukuran 65,04 MB. Sudah jadi, jangan diubah.** Taruh di `app/src/main/assets/`, buka read-only lewat Room dengan `createFromAsset()`.

> ⚠️ File ini **tidak boleh di-commit ke Git**. Sudah masuk `.gitignore`. Distribusi lewat Google Drive atau GitHub Releases.

### Enam tabel

```sql
tb_nodes (
    node_id INTEGER PRIMARY KEY,   -- ID simpul OSM
    lat REAL, lon REAL
)                                   -- 31.813 baris

tb_tes (
    tes_id TEXT PRIMARY KEY,        -- "TES_0", "TES_1", ...
    nama_tes TEXT,                  -- unik, sudah diverifikasi 0 duplikat
    zona TEXT,                      -- kode zona BPBD
    kapasitas REAL,                 -- jiwa
    lat REAL, lon REAL
)                                   -- 143 baris

tb_routes (
    origin_node_id INTEGER PRIMARY KEY,
    rank_1_tes TEXT, rank_1_path TEXT, rank_1_eta REAL,
    rank_2_tes TEXT, rank_2_path TEXT, rank_2_eta REAL,
    rank_3_tes TEXT, rank_3_path TEXT, rank_3_eta REAL
)                                   -- 31.813 baris

tb_edges (
    edge_id INTEGER PRIMARY KEY,
    u INTEGER, v INTEGER,           -- FK ke tb_nodes
    length REAL,                    -- meter
    geometry TEXT                   -- WKT LINESTRING
)                                   -- 39.216 baris

tb_inundation_zones (
    zone_id INTEGER PRIMARY KEY,
    nama_zona TEXT, tingkat_bahaya TEXT,
    geometry_wkt TEXT               -- POLYGON
)

tb_safe_zones (
    safe_zone_id INTEGER PRIMARY KEY,
    nama_zona TEXT, elevasi_m REAL,
    geometry_wkt TEXT               -- POLYGON
)
```

### Bentuk data yang perlu diperhatikan

**`rank_1_path` adalah deretan `node_id` dipisah koma**, bukan koordinat:
```
"348337984,7628077957,12282386159,6095957312"
```

**`rank_1_eta` dalam menit (float).** Konversi: `detik = (eta * 60).roundToInt()`

**`rank_1_tes` menyimpan nama, bukan `tes_id`.** Aman karena 143 nama sudah diverifikasi unik, tapi query pakai `WHERE nama_tes = ?`.

**`geometry` contoh nyata:**
```
LINESTRING (100.4146528 -0.9539962, 100.4147655 -0.9538743, 100.4148164 -0.9537595)
```
Perhatikan urutannya **lon lat**, bukan lat lon. Salah tukar akan menempatkan seluruh peta di Samudra Hindia.

---

## 4. Alur navigasi

```
GPS (lat, lon)
  ↓
cari node terdekat di tb_nodes
  ↓
SELECT rank_1_path, rank_1_eta, rank_1_tes
FROM tb_routes WHERE origin_node_id = ?
  ↓
pecah path jadi List<Long>
  ↓
untuk tiap pasang node berurutan → ambil geometry dari tb_edges
  ↓
sambung jadi satu polyline → render di MapLibre
```

### ⚠️ Jebakan 1 — arah geometri ruas

`tb_edges` menyimpan setiap ruas **hanya dalam satu arah**, padahal jaringan pejalan kaki dua arah. Query harus menangkap keduanya:

```sql
SELECT u, v, geometry FROM tb_edges
WHERE (u = :a AND v = :b) OR (u = :b AND v = :a)
```

Kalau `u` yang kembali **bukan** node asal pada rute, **balik urutan koordinatnya** sebelum disambung:

```kotlin
val coords = parseWkt(edge.geometry)
val ordered = if (edge.u == fromNodeId) coords else coords.reversed()
```

Kalau langkah ini dilewat, garis rutenya akan zigzag bolak-balik, dan perhitungan sudut belokan akan terbalik — "Belok Kanan" jadi "Belok Kiri".

### ⚠️ Jebakan 2 — pencarian node terdekat

31.813 baris. Jangan hitung jarak Haversine untuk semuanya setiap kali GPS berubah. Saring dulu dengan kotak pembatas:

```sql
SELECT node_id, lat, lon FROM tb_nodes
WHERE lat BETWEEN :lat - 0.005 AND :lat + 0.005
  AND lon BETWEEN :lon - 0.005 AND :lon + 0.005
```

Lalu hitung jarak sebenarnya pada hasil yang tersisa. Buat indeks pada `(lat, lon)` kalau belum ada.

### ⚠️ Jebakan 3 — jumlah kueri

Rute sepanjang 50 simpul berarti 49 kueri `tb_edges`. Ambil sekaligus dengan satu kueri `WHERE u IN (...) OR v IN (...)`, lalu susun urutannya di memori.

---

## 5. Fitur

### Inti — wajib jalan tanpa jaringan

| Kode | Fitur | Sumber data |
|---|---|---|
| F-01 | Widget layar utama: status zona, TES terdekat, tombol evakuasi | `tb_inundation_zones`, `tb_tes` |
| F-02 | Penentuan status zona dari posisi | `tb_inundation_zones` (point-in-polygon) |
| F-03 | Penyajian jalur evakuasi | `tb_routes`, `tb_edges` |
| F-04 | Penunjuk arah berbasis kompas | magnetometer |
| F-05 | Hitung mundur sisa waktu vs estimasi tempuh | `rank_1_eta` |
| F-06 | Deteksi tiba di kawasan aman | `tb_safe_zones` (point-in-polygon) |
| F-07 | Rencana titik temu keluarga (disusun pada masa tenang) | lokal |
| F-08 | **Peralihan jalur alternatif atas laporan pengguna** | `rank_2_*`, `rank_3_*` |

**F-08 penting dan sering disalahpahami.** Pengguna yang melihat sendiri jalur terhalang menekan satu tombol; aplikasi beralih ke `rank_2` lalu `rank_3` dari basis data lokal. **Sistem tidak mendeteksi apa pun dan tidak butuh jaringan.**

### Pelengkap — butuh jaringan, boleh gagal

| Kode | Fitur |
|---|---|
| F-09 | Status resmi gempa/tsunami dari BMKG |
| F-10 | Penandaan keselamatan setelah tiba |
| F-11 | Pengiriman laporan jalur terhalang ke peladen |
| F-12 | Sinkronisasi pemutakhiran data |

Batas waktu tunggu seluruh permintaan jaringan: **2–3 detik**, lalu beralih mode tanpa menunggu.

### ❌ Tidak dibangun

| Ditolak | Alasan |
|---|---|
| Deteksi guncangan lewat akselerometer | Tidak dapat membedakan gempa dari getaran lain. Peringatan palsu berulang membuat pengguna mengabaikan sistem. Diganti widget. |
| Pelacakan posisi keluarga langsung | Mendorong perilaku berbalik menjemput. Diganti F-07. |
| **Tingkat keterisian TES saat luring** | Tidak mungkin diketahui tanpa jaringan. Pemerataan sudah dilakukan pada prakomputasi. |
| Penetapan ambang magnitudo sendiri | Kewenangan BMKG. |
| Perhitungan rute di perangkat | Prakomputasi dipilih demi kepastian waktu respons. |

---

## 6. Peta: opsional

Basis data rute (65 MB) **wajib**. Berkas ubin peta (~60–120 MB) **opsional**.

Seluruh fungsi inti — panah arah, nama TES tujuan, jarak, sisa waktu — bersumber dari basis data rute dan tetap berfungsi tanpa satu ubin pun.

**Kalau ubin tidak diunduh:** jangan tampilkan area kosong. Gambar polyline di atas latar polos. Konteks spasialnya tetap terbaca, dan pada kondisi panik itu justru lebih jelas daripada peta penuh nama jalan dan label toko.

Ini sejalan dengan prinsip antarmuka "arah, bukan peta" — bukan kompromi, melainkan rancangan.

---

## 7. Prinsip antarmuka

Diturunkan dari literatur perilaku darurat (Leach 2004): sebagian orang mengalami kelumpuhan kognitif pada menit-menit awal bencana — tidak mampu menyusun tindakan baru di bawah tekanan waktu.

**Satu instruksi pada satu waktu.** Layar tidak menyajikan pilihan yang menuntut pertimbangan.

**Arah, bukan peta.** Panah besar mengikuti arah hadap perangkat. Peta hanya pendukung.

**Keterbacaan pada kondisi buruk.** Kontras minimal 4.5:1 (WCAG 2.1 AA), area sentuh minimal 48×48 dp, skema gelap dengan elemen terang, hindari huruf tipis.

**Kejujuran terhadap ketidakpastian.** Kalau ada yang tidak dapat dipastikan, katakan. Jangan tampilkan layar kosong atau biarkan pengguna menduga.

### Aturan kata — berlaku di kode, komentar, dan UI

| Pakai | Jangan |
|---|---|
| TES / TEA | shelter |
| berjalan cepat | lari / berlari |
| alternatif tujuan | TES penuh |

Alasannya bukan kerapian: BPBD Kota Padang **melarang** evakuasi dengan berlari, dan penyelarasan terminologi adalah salah satu klaim kontribusi proposal.

---

## 8. Target terukur

| Kode | Target |
|---|---|
| NF-01 | Seluruh fungsi inti berjalan pada mode pesawat |
| NF-02 | Waktu penyajian arahan < 1 detik |
| NF-03 | Basis data ≤ 75 MB (aktual: 65,04 MB) |
| NF-04 | Selisih waktu respons antar-perangkat ≤ 1 detik |
| NF-05 | Batas tunggu permintaan jaringan 2–3 detik |
| NF-06 | Kontras ≥ 4.5:1, sentuh ≥ 48×48 dp |

Ukur NF-02 dengan `System.currentTimeMillis()` sebelum dan sesudah alur baca, lalu catat pada empat perangkat yang tersedia.

---

## 9. Repositori

### Struktur

```
siaga-padang/
├── README.md
├── CLAUDE.md                 ← dokumen ini
├── .gitignore
├── android/                  ← Mikail
├── backend/                  ← Sheva
├── spatial/                  ← Habib
└── docs/                     ← proposal, ERD, diagram
```

Ketiga folder **tidak saling mengimpor kode**. Tidak ada build tool lintas-modul, tidak ada workspace config. Ini monorepo hanya dalam arti satu tempat penyimpanan bersama — sehingga riwayat commit menyatu dan papan tugas cukup satu.

Satu-satunya penghubung adalah `ranah_siaga.db`: Habib menghasilkannya lewat `spatial/core.py`, Mikail memakainya di `android/app/src/main/assets/`. Distribusi lewat Google Drive, bukan Git.

**Prinsip:** simpan yang menghasilkan, bukan yang dihasilkan. `core.py` masuk Git, keluarannya tidak.

### `.gitignore` — wajib jadi commit pertama

```gitignore
# Basis data (65 MB, distribusi lewat Drive)
*.db
*.sqlite
*.mbtiles
*.pmtiles

# Android
android/build/
android/app/build/
android/.gradle/
android/local.properties
*.apk
*.aab
*.keystore

# Python
__pycache__/
*.pyc
env/
venv/
.env

# Data spasial mentah
spatial/data/
*.graphml
*.osm.pbf

# IDE
.idea/
.vscode/
.DS_Store
```

Kalau file besar sudah terlanjur ter-commit, menambahkannya ke `.gitignore` **tidak menghapusnya dari riwayat**. Periksa dengan:

```bash
git ls-files | xargs ls -lh 2>/dev/null | sort -k5 -hr | head
```

### Alur kerja

```bash
git pull                          # selalu, sebelum mulai
git checkout -b feat/f03-routing  # satu branch per pekerjaan
# ... kerja di folder sendiri ...
git add android/
git commit -m "feat(android): baca rute dari tb_routes"
git push -u origin feat/f03-routing
```

Lalu buka pull request, minta review anggota lain, merge. Tulis `Closes #12` di deskripsi PR agar issue tertutup otomatis.

Ini bukan formalitas: Subbab 4.5 proposal menjanjikan review silang sebelum penggabungan. Kalau juri membuka repo dan seluruh commit langsung ke `main`, klaim itu terbantahkan.

**Awalan commit:** `feat(android):`, `fix(backend):`, `chore(spatial):`, `docs:`

### Aturan bersama

Tidak ada yang mengedit folder milik orang lain. File di root (`README.md`, `CLAUDE.md`) dan `docs/` hanya diedit Mikail; anggota lain menyampaikan perubahan lewat dia.

### Penomoran issue

Judul issue merujuk kode kebutuhan, agar keterlacakan kebutuhan → kode terlihat:

```
[F-03] Prakomputasi jalur Reverse Dijkstra seluruh Kota Padang
[F-08] Peralihan jalur alternatif atas laporan pengguna
[NF-03] Ekspor basis data SQLite luring
```

Papan GitHub Projects: `Backlog` · `Sprint Berjalan` · `Review` · `Selesai`.

---

## 10. Struktur kode Android

```
android/app/src/main/java/.../
├── data/
│   ├── local/          # Room: entity, DAO, database
│   ├── repository/     # EvacuationRepository, ZoneRepository
│   └── model/          # Route, EvacuationPoint, Zone
├── domain/
│   ├── NearestNodeFinder.kt
│   ├── PolylineAssembler.kt      # rangkai geometri ruas
│   ├── ZoneChecker.kt            # point-in-polygon
│   └── BearingCalculator.kt
├── ui/
│   ├── evacuation/     # layar evakuasi + ViewModel
│   ├── zonestatus/
│   ├── familyplan/
│   └── theme/
├── sensor/             # LocationProvider, CompassProvider
└── widget/             # AppWidgetProvider (XML)
```

**Arah ketergantungan:** `ui` → `domain` → `data`. Tidak boleh terbalik. Layer `domain` tidak mengenal Android sama sekali, sehingga dapat diuji tanpa emulator.

**Composable tidak menyentuh DAO.** Selalu lewat ViewModel → Repository → DAO. `@Composable` yang memanggil query SQL langsung adalah coupling yang harus diperbaiki.

**Satu file satu tanggung jawab.** `PolylineAssembler` hanya merangkai geometri — tidak membaca basis data, tidak menggambar, tidak menghitung ETA. Kalau sebuah kelas butuh kata "dan" untuk dijelaskan, pecah.

**Parsing WKT dan pembalikan arah ruas hanya di satu tempat.** Kalau logika ini tersebar, jebakan arah geometri (Bagian 4) akan muncul lagi di file yang terlupa.

---

## 11. Data acuan

| | |
|---|---|
| Cakupan zona rawan | 8 kecamatan, 55 kelurahan, 242.750 jiwa terpapar |
| Cakupan jaringan jalan | 11 kecamatan (agar rute ke perbukitan tidak terputus) |
| Graf | 31.813 simpul, 39.216 ruas |
| Titik evakuasi | 184 fasilitas BPBD → 155 TES gedung → 143 unik berkoordinat → 139 simpul graf |
| TEA | 29 kawasan perbukitan (belum masuk simulasi) |
| Kecepatan berjalan | 1,2 m/s (SOP BPBD Kota Padang) |
| Waktu evakuasi | 20–30 menit setelah guncangan |
| Algoritma | Reverse single-source Dijkstra dengan alokasi berbatas kapasitas |

Hasil simulasi: pendekatan konvensional menampung 39,52% agen dengan 52 titik evakuasi melampaui kapasitas; pendekatan berbatas kapasitas menampung 74,49% tanpa satu pun titik melampaui kapasitas.

---

## 12. Untuk asisten AI

**Jangan menulis algoritma pencarian lintasan.** Rute sudah dihitung pada tahap prakomputasi; aplikasi hanya membaca. Tidak ada Dijkstra, A*, maupun BFS di sisi Android.

**Basis data bersifat read-only.** Tidak ada `INSERT`, `UPDATE`, `DELETE`, maupun migrasi Room terhadap `ranah_siaga.db`. Buka dengan `createFromAsset()` dan `fallbackToDestructiveMigration()` dinonaktifkan — file 65 MB ini hasil kerja Habib dan tidak boleh ditimpa.

**Jangan menambah fitur dari daftar "Tidak dibangun"** (Bagian 5). Semuanya sudah dipertimbangkan dan ditolak dengan alasan tercatat. Kalau menurut Anda salah satu keputusan itu keliru, sampaikan argumennya — jangan mengusulkannya kembali seolah belum pernah dibahas.

**Jangan pakai `localStorage`, `sessionStorage`, atau penyimpanan browser.** Ini aplikasi Android native.

**Jangan commit `*.db`.** Sudah masuk `.gitignore`.

**Patuhi aturan kata pada Bagian 7** — di nama variabel, komentar, string UI, dan pesan commit.
