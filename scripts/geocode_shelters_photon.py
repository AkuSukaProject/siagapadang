import urllib.request
import urllib.parse
import json
import csv
import os
import time

places = [
    "Masjid Istiqlal Padang",
    "SDN 26 Rimbo Kaluang Padang",
    "Dinas PU Padang",
    "Hotel Ibis Padang",
    "Universitas Tamansiswa Padang",
    "Bappeda Sumatera Barat",
    "Masjid Raya Sumatera Barat",
    "SMAN 3 Padang",
    "SMKN 5 Padang",
    "SMPN 7 Padang",
    "STMIK Indonesia Padang",
    "SMK Teknologi Plus Padang",
    "SMAN 1 Padang",
    "SMPN 25 Padang",
    "Telkomsel Padang",
    "SDN 15 Lolong Padang",
    "Ditjen Perbendaharaan Padang",
    "STIKES Alifah Padang",
    "BPK Sumatera Barat",
    "SMP Pertiwi Padang",
    "Masjid Darul Mukhlisin Padang",
    "STIE AKBP Padang",
    "PT Suka Fajar Padang",
    "Universitas Bung Hatta Padang",
    "Astra Daihatsu Padang",
    "SDN 14 Belanti Padang",
    "DPRD Sumatera Barat",
    "Dinas PSDA Sumatera Barat",
    "Villa Hadis Padang",
    "Sekolah Al Azhar 32 Padang",
    "SJS Plaza Padang",
    "SDN 20 Padang",
    "Institut Teknologi Padang",
    "SMPN 12 Padang",
    "Masjid Muhsinin Padang"
]

out_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "data")
csv_file = os.path.join(out_dir, "shelter_coordinates_complete.csv")

print("Mencari koordinat menggunakan Photon Geocoder...")

with open(csv_file, mode='w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(["Nama Tempat", "Latitude", "Longitude", "Sumber"])
    
    for place in places:
        # Search constrained to Indonesia bounds & preferring Padang area
        # Padang center approx: -0.9471, 100.3546
        query = urllib.parse.quote(place)
        url = f"https://photon.komoot.io/api/?q={query}&limit=1&lat=-0.9471&lon=100.3546"
        
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        
        try:
            with urllib.request.urlopen(req) as response:
                data = json.loads(response.read().decode('utf-8'))
                if data and 'features' in data and len(data['features']) > 0:
                    feature = data['features'][0]
                    lon, lat = feature['geometry']['coordinates']
                    name = feature['properties'].get('name', 'Unknown')
                    
                    # Validate if it's somewhat in Padang (-0.7 to -1.1 lat, 100.2 to 100.6 lon)
                    if -1.1 <= lat <= -0.7 and 100.2 <= lon <= 100.6:
                        writer.writerow([place, lat, lon, f"Photon: {name}"])
                        print(f"DITEMUKAN: {place} -> {lat}, {lon} ({name})")
                    else:
                        writer.writerow([place, "", "", f"Di luar Padang: {name}"])
                        print(f"OUT OF BOUNDS: {place} -> {name} ({lat}, {lon})")
                else:
                    writer.writerow([place, "", "", "TIDAK DITEMUKAN"])
                    print(f"GAGAL: {place}")
        except Exception as e:
            print(f"Error {place}: {e}")
            writer.writerow([place, "ERROR", "ERROR", str(e)])
            
        time.sleep(1)

print(f"\nSelesai! Hasil disimpan di: {csv_file}")
