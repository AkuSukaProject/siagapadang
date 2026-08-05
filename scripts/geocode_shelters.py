import urllib.request
import urllib.parse
import json
import time
import csv
import os

places = [
    "Mesjid Istiqlal, Padang",
    "SDN 26 Rimbo Kaluang, Padang",
    "Dinas PU Sumatera Barat, Padang",
    "Hotel Ibis, Padang",
    "Universitas Taman Siswa, Padang",
    "Bappeda Sumatera Barat, Padang",
    "Mesjid Raya Sumatera Barat, Padang",
    "SMAN 3 Padang",
    "SMKN 5 Padang",
    "SMPN 7 Padang",
    "STMIK Indonesia, Padang",
    "SMK Teknologi, Padang",
    "SMAN 1 Padang",
    "SMPN 25 Padang",
    "Kantor Telkomsel, Padang",
    "SDN 15 Lolong, Padang",
    "Ditjen Perbendaharaan Sumatera Barat, Padang",
    "STIKES Alifah, Padang",
    "BPK Perwakilan Sumatera Barat, Padang",
    "SMP Pertiwi, Padang",
    "Mesjid Darul Mukhlisin, Padang",
    "STIE AKBP, Padang",
    "PT Sukafajar, Padang",
    "Pasca Sarjana UBH, Padang",
    "Gedung Daihatsu, Padang",
    "SDN 14 Belanti, Padang",
    "DPRD Sumatera Barat, Padang",
    "Dinas PSDA Sumatera Barat, Padang",
    "Villa Hadis, Padang",
    "Sekolah Al Azhar 32 Padang",
    "Swalayan SJS, Padang",
    "SD 20 Berok Belanti, Padang",
    "Institut Teknologi Padang",
    "SMPN 12 Padang",
    "Mesjid Muhsinin, Padang"
]

out_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "data")
os.makedirs(out_dir, exist_ok=True)
csv_file = os.path.join(out_dir, "shelter_coordinates.csv")

print("Mulai mencari koordinat dari Nominatim OSM...")

with open(csv_file, mode='w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(["Nama Tempat", "Latitude", "Longitude", "Alamat OSM"])
    
    for place in places:
        query = urllib.parse.quote(place)
        url = f"https://nominatim.openstreetmap.org/search?q={query}&format=json&limit=1"
        
        req = urllib.request.Request(url, headers={'User-Agent': 'PadangSiaga-Evakuasi/1.0'})
        
        try:
            with urllib.request.urlopen(req) as response:
                data = json.loads(response.read().decode())
                if data:
                    lat = data[0]['lat']
                    lon = data[0]['lon']
                    display_name = data[0]['display_name']
                    writer.writerow([place, lat, lon, display_name])
                    print(f"✅ {place} -> {lat}, {lon}")
                else:
                    writer.writerow([place, "", "", "TIDAK DITEMUKAN"])
                    print(f"❌ {place} -> Tidak ditemukan")
        except Exception as e:
            print(f"Error fetching {place}: {e}")
            writer.writerow([place, "ERROR", "ERROR", str(e)])
        
        # Nominatim usage policy: 1 request per second
        time.sleep(1.5)

print(f"\nSelesai! Hasil disimpan di: {csv_file}")
