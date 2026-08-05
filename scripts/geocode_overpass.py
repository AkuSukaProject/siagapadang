import requests
import pandas as pd
from rapidfuzz import process, fuzz
import os
import urllib.parse

df = pd.read_csv("data/sektor_eg.csv")
shelters = df.iloc[:, 7].dropna().unique().tolist()
shelters = [s for s in shelters if s != "LANGSUNG MENUJU TEA"]

# Padang Bounding Box (Lolong, Khatib Sulaiman, Purus, Jati, dsk)
bbox = "-0.9700,100.3400,-0.8900,100.4100"
overpass_query = f"""
[out:json][timeout:25];
(
  node["amenity"]({bbox});
  way["amenity"]({bbox});
  node["building"]({bbox});
  way["building"]({bbox});
  node["office"]({bbox});
  way["office"]({bbox});
);
out center;
"""

print("Mengunduh data Overpass API via GET...")
encoded_query = urllib.parse.quote(overpass_query.strip())
url = f"https://overpass-api.de/api/interpreter?data={encoded_query}"

response = requests.get(url, headers={'User-Agent': 'Mozilla/5.0'})
if response.status_code == 200:
    data = response.json()
    poi_list = []
    for element in data['elements']:
        tags = element.get('tags', {})
        name = tags.get('name')
        if name:
            lat = element.get('lat') or element.get('center', {}).get('lat')
            lon = element.get('lon') or element.get('center', {}).get('lon')
            poi_list.append({"name": name, "lat": lat, "lon": lon})
            
    print(f"Berhasil mengunduh {len(poi_list)} POI bernama dari satelit OSM Padang.")
    poi_names = [p['name'] for p in poi_list]
    results = []
    
    for shelter in shelters:
        clean_name = shelter.replace("(Bangunan yang diusulkan)", "").replace("(Eksisting)", "").replace("(<500)", "").strip()
        match = process.extractOne(clean_name, poi_names, scorer=fuzz.token_set_ratio)
        if match and match[1] >= 55:  # Tolerance threshold
            matched_poi = next(p for p in poi_list if p['name'] == match[0])
            results.append([clean_name, matched_poi['lat'], matched_poi['lon'], match[0], match[1]])
        else:
            results.append([clean_name, "", "", "TIDAK DITEMUKAN DALAM OSM", 0])
            
    out_df = pd.DataFrame(results, columns=["Nama BPBD", "Latitude", "Longitude", "Nama di Peta OSM", "Skor Kecocokan Fuzzy"])
    out_file = os.path.join("data", "sektor_eg_coords.csv")
    out_df.to_csv(out_file, index=False)
    print(f"Selesai! 100% dipaksa hanya di Padang. Disimpan di {out_file}")
else:
    print(f"Error {response.status_code}: {response.text}")
