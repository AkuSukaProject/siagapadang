# Audit `ranah_siaga.db`

Audit dilakukan secara read-only pada 8 Agustus 2026. Berkas aset tidak diubah.

## Temuan

| Pemeriksaan | Hasil |
|---|---:|
| `tb_nodes` | 31.813 baris |
| `tb_edges` | 39.216 baris |
| Ruas dengan WKT `LINESTRING` | 20.255 |
| Ruas dengan `geometry` kosong | 18.961 |
| Pasangan node dengan ruas paralel | 387 |
| Panjang maksimum path | 154 node |
| `tb_inundation_zones` | 0 baris |
| `tb_safe_zones` | 0 baris |
| `PRAGMA user_version` | 0 |
| Indeks koordinat | Tidak ada |

## Konsekuensi implementasi

- Ruas yang memiliki WKT menggunakan geometri tersebut dan dibalik jika arah `u → v` berbeda dari arah path.
- Ruas yang geometrinya kosong digambar sebagai garis dari koordinat node asal ke node tujuan. Ini menjaga konektivitas rute, tetapi tidak mempertahankan kelengkungan jalan pada ruas tersebut.
- Jika terdapat beberapa ruas untuk pasangan node yang sama, dipilih ruas dengan `length` terkecil karena path hanya menyimpan deretan node dan tidak menyimpan `edge_id`.
- Fitur status zona rawan dan deteksi tiba di kawasan aman belum dapat diaktifkan dari berkas ini karena kedua tabel zona kosong.
- Room memvalidasi struktur `tb_nodes`; tabel lain diakses melalui DAO proyeksi `SELECT`. Tidak ada migrasi destruktif atau DAO tulis.

Temuan ini perlu dikonfirmasi kepada pemilik pipeline spasial sebelum database dinyatakan final.
