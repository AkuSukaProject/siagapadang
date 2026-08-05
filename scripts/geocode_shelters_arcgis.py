import urllib.request
import urllib.parse
import json
import csv
import os
import time

places = [
    "Masjid Istiqlal, Padang",
    "SDN 26 Rimbo Kaluang, Padang",
    "Dinas PU Provinsi Sumatera Barat, Padang",
    "Hotel Ibis, Padang",
    "Universitas Tamansiswa, Padang",
    "Bappeda Provinsi Sumatera Barat, Padang",
    "Masjid Raya Sumatera Barat, Padang",
    "SMAN 3 Padang",
    "SMKN 5 Padang",
    "SMPN 7 Padang",
    "STMIK Indonesia, Padang",
    "SMK Teknologi, Padang",
    "SMAN 1 Padang",
    "SMPN 25 Padang",
    "Graha Telkomsel, Padang",
    "SDN 15 Lolong, Padang",
    "Kanwil Ditjen Perbendaharaan Provinsi Sumatera Barat, Padang",
    "STIKES Alifah, Padang",
    "BPK Perwakilan Provinsi Sumatera Barat, Padang",
    "SMP Pertiwi, Padang",
    "Masjid Darul Mukhlisin, Padang",
    "STIE AKBP, Padang",
    "PT Suka Fajar, Padang",
    "Pascasarjana Universitas Bung Hatta, Padang",
    "Astra Daihatsu Padang",
    "SDN 14 Belanti, Padang",
    "DPRD Provinsi Sumatera Barat, Padang",
    "Dinas PSDA Provinsi Sumatera Barat, Padang",
    "Villa Hadis, Padang",
    "Perguruan Islam Al Azhar 32 Padang",
    "SJS Plaza, Padang",
    "SDN 20 Berok Belanti, Padang",
    "Institut Teknologi Padang",
    "SMPN 12 Padang",
    "Masjid Muhsinin, Padang"
]

out_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "data")
os.makedirs(out_dir, exist_ok=True)
csv_file = os.path.join(out_dir, "shelter_coordinates_complete.csv")

print("Mencari koordinat menggunakan ArcGIS Geocoder (lebih cerdas dari OSM)...")

with open(csv_file, mode='w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(["Nama Tempat", "Latitude", "Longitude", "Alamat Ditemukan"])
    
    for place in places:
        query = urllib.parse.quote(place)
        # Menggunakan ArcGIS REST API yang jauh lebih toleran terhadap salah ketik/format nama
        url = f"https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/findAddressCandidates?f=json&singleLine={query}&outFields=Match_addr&maxLocations=1"
        
        req = urllib.request.Request(url)
        
        try:
            with urllib.request.urlopen(req) as response:
                data = json.loads(response.read().decode())
                if data and 'candidates' in data and len(data['candidates']) > 0:
                    candidate = data['candidates'][0]
                    lat = candidate['location']['y']
                    lon = candidate['location']['x']
                    address = candidate['address']
                    writer.writerow([place, lat, lon, address])
                    print(f"DITEMUKAN: {place}")
                else:
                    writer.writerow([place, "", "", "TIDAK DITEMUKAN"])
                    print(f"GAGAL: {place}")
        except Exception as e:
            print(f"Error fetching {place}: {e}")
            writer.writerow([place, "ERROR", "ERROR", str(e)])
            
        time.sleep(0.5)

print(f"\nSelesai! Hasil disimpan di: {csv_file}")
