# Panduan Deployment Backend & Konfigurasi Release Android (P0-06)

Dokumen ini menjelaskan langkah-langkah implementasi, deployment server, dan build release Android untuk aplikasi Siaga Padang agar fitur daring dapat digunakan di jaringan seluler tanpa tergantung pada `adb reverse` atau komputer pengembang.

---

## 1. Arsitektur Deployment Backend

```
[ Smartphone Android (Jaringan Seluler / Wi-Fi) ]
                      │
                      ▼ HTTPS (Port 443)
       [ Nginx Reverse Proxy / Cloudflare ]
                      │
                      ▼ HTTP (Port 8000)
    [ FastAPI Container: siaga_padang_api ]
                      │
                      ▼ Port 5432
   [ PostGIS Container: postgis_evakuasi ]
```

---

## 2. Langkah Deployment Backend (Docker & Docker Compose)

### A. Persiapan File Environment (`.env`)
Di server Ubuntu/Debian, salin `.env.example` menjadi `.env`:
```bash
cp .env.example .env
```
Isi variabel dengan kredensial produksi yang aman:
```env
POSTGRES_USER=siaga_padang
POSTGRES_PASS=KunciSangatRahasia_Dan_Panjang_123!
POSTGRES_DB=evakuasi_padang
ALLOW_IP_RANGE=0.0.0.0/0

DATABASE_URL=postgresql+psycopg://siaga_padang:KunciSangatRahasia_Dan_Panjang_123!@db:5432/evakuasi_padang
HMAC_SECRET=NilaiAcakPanjangMinimal32KarakterUntukKeamananDevice_987!
```

### B. Menjalankan Layanan
Jalankan stack menggunakan Docker Compose:
```bash
docker compose up -d --build
```
Verifikasi bahwa container `postgis_evakuasi` dan `siaga_padang_api` berstatus `healthy`:
```bash
docker compose ps
```

### C. Inisialisasi Database Graf & Data Bencana
Setelah database aktif, jalankan bootstrap dari basis data Android:
```bash
docker compose exec api python scripts/bootstrap_from_android_db.py android/app/src/main/assets/ranah_siaga.db --ensure-dev-event
```

### D. Konfigurasi Nginx Reverse Proxy & HTTPS (Let's Encrypt)
Contoh konfigurasi Nginx (`/etc/nginx/sites-available/siagapadang`):
```nginx
server {
    server_name api.siagapadang.id;

    location / {
        proxy_pass http://127.0.0.1:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```
Aktifkan SSL gratis menggunakan Certbot:
```bash
sudo certbot --nginx -d api.siagapadang.id
```

---

## 3. Konfigurasi Build & Release Android

### A. Konfigurasi URL Backend
URL backend disuntikkan secara dinamis saat proses build tanpa perlu menanam kredensial di kode sumber. Prioritas pembacaan URL:
1. Gradle parameter `-PSIAGA_BACKEND_BASE_URL=...`
2. Environment variable sistem `SIAGA_BACKEND_BASE_URL`
3. Fallback default `gradle.properties`

### B. Membangun APK Release untuk Produksi (HTTPS)
Jalankan build release dengan menyertakan domain produksi:
```powershell
.\gradlew.bat assembleRelease -PSIAGA_BACKEND_BASE_URL="https://api.siagapadang.id/"
```
*Catatan:* Pada build HTTPS produksi, `usesCleartextTraffic` otomatis bernilai `false` untuk menjamin keamanan transmisi data pengguna.

### C. Membangun APK Release untuk Pengujian Staging (LAN / Wi-Fi)
Untuk menguji APK release pada perangkat nyata tanpa kabel USB / `adb reverse`:
1. Pastikan komputer dan perangkat Android terhubung ke jaringan Wi-Fi yang sama.
2. Cari IP komputer pengembang (misal: `192.168.1.15`).
3. Bangun APK release:
```powershell
.\gradlew.bat assembleRelease -PSIAGA_BACKEND_BASE_URL="http://192.168.1.15:8000/"
```
*Catatan:* Sistem secara otomatis mendeteksi protokol `http://` dan mengaktifkan `usesCleartextTraffic = "true"` khusus untuk endpoint uji tersebut.

### D. Keamanan R8 / ProGuard
Aturan R8 di [`proguard-rules.pro`](file:///c:/siagapadang/android/app/proguard-rules.pro) telah dikonfigurasi untuk:
- Menjaga seluruh kelas DTO (`com.akusukaproject.siagapadang.data.remote.model.**`) agar tidak terhapus atau berubah nama saat minifikasi R8.
- Menjaga entitas SQLite Room (`tb_routes`, `tb_nodes`, `tb_edges`, `tb_tes`).
- Menjaga modul native MapLibre SDK.

---

## 4. Kriteria Selesai & Verifikasi

- [x] Backend stack (PostGIS + FastAPI) terbungkus lengkap dan teruji di `docker-compose.yml`.
- [x] Endpoint liveness `/health` dan `/health/db` siap untuk monitoring uptime.
- [x] URL backend release dapat disuntikkan dinamis melalui CI/CD atau CLI build.
- [x] Aturan ProGuard/R8 aktif dan melindungi integritas model JSON dan Room.
- [x] Pengujian unit `.\gradlew.bat testDebugUnitTest` berhasil 100%.
