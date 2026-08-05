import urllib.request
import urllib.parse
import re
import csv
import os
import time

places = [
    "Masjid Istiqlal, Purus, Padang",
    "SDN 26 Rimbo Kaluang, Padang",
    "Dinas PU Provinsi Sumatera Barat, Taman Siswa, Padang",
    "Hotel Ibis Padang",
    "Universitas Tamansiswa Padang",
    "Bappeda Provinsi Sumatera Barat, Khatib Sulaiman, Padang",
    "Masjid Raya Sumatera Barat",
    "SMAN 3 Padang, Gunung Pangilun",
    "SMKN 5 Padang, Lolong Belanti",
    "SMPN 7 Padang, S Parman",
    "STMIK Indonesia Padang, Khatib Sulaiman",
    "SMK Teknologi Plus Padang, Belanti",
    "SMAN 1 Padang, Belanti",
    "SMPN 25 Padang, Beringin",
    "Graha Telkomsel Padang, Khatib Sulaiman",
    "SDN 15 Lolong, Padang",
    "Kanwil Ditjen Perbendaharaan Provinsi Sumbar, Khatib Sulaiman",
    "STIKES Alifah Padang, Khatib Sulaiman",
    "BPK Perwakilan Provinsi Sumbar, Khatib Sulaiman",
    "SMP Pertiwi Padang",
    "Masjid Darul Mukhlisin Padang",
    "STIE AKBP Padang, Khatib Sulaiman",
    "PT Suka Fajar Padang, Khatib Sulaiman",
    "Pascasarjana Universitas Bung Hatta, Ulak Karang",
    "Astra Daihatsu Padang, Khatib Sulaiman",
    "SDN 14 Belanti, Padang",
    "DPRD Provinsi Sumatera Barat, Khatib Sulaiman",
    "Dinas PSDA Provinsi Sumatera Barat, Khatib Sulaiman",
    "Villa Hadis, Padang",
    "Perguruan Islam Al Azhar 32 Padang, Khatib Sulaiman",
    "SJS Plaza Padang, Lapai",
    "SDN 20 Berok Belanti, Padang",
    "Institut Teknologi Padang, Lapai",
    "SMPN 12 Padang, Nanggalo",
    "Masjid Muhsinin, Padang"
]

out_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "data")
csv_file = os.path.join(out_dir, "shelter_coordinates_fixed.csv")

print("Scraping coordinates directly from Google Maps...")

with open(csv_file, mode='w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(["Nama Tempat", "Latitude", "Longitude", "Status"])
    
    for place in places:
        query = urllib.parse.quote(place)
        url = f"https://www.google.com/maps/search/{query}"
        
        req = urllib.request.Request(url, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        })
        
        try:
            with urllib.request.urlopen(req) as response:
                html = response.read().decode('utf-8')
                
                # Regex to find coordinates in Google Maps HTML
                # Usually looks like: [null,null,[-0.9243,100.3625]] or similar center coords
                # Alternatively look for INITIAL_DATA or meta property="og:image" containing markers
                
                match = re.search(r'meta content="https://maps\.google\.com/maps/api/staticmap\?center=([-0-9.]+),([-0-9.]+)', html)
                if match:
                    lat, lon = match.groups()
                    writer.writerow([place, lat, lon, "OK (Google Maps)"])
                    print(f"✅ {place}: {lat}, {lon}")
                else:
                    # Alternative regex
                    match2 = re.search(r'\[null,null,([-0-9.]+),([-0-9.]+)\]', html)
                    if match2:
                        lat = match2.group(1)
                        lon = match2.group(2)
                        writer.writerow([place, lat, lon, "OK (Google Maps)"])
                        print(f"✅ {place}: {lat}, {lon}")
                    else:
                        writer.writerow([place, "", "", "GAGAL"])
                        print(f"❌ {place}: Gagal menemukan koordinat di HTML")
        except Exception as e:
            print(f"Error {place}: {e}")
            writer.writerow([place, "ERROR", "ERROR", str(e)])
        
        time.sleep(1)

print(f"\nSelesai! Disimpan ke {csv_file}")
