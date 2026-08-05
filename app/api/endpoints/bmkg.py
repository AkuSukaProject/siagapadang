import urllib.request
import json
import time
from fastapi import APIRouter, HTTPException
from app.schemas.bmkg import BMKGStatusResponse

router = APIRouter()

# Simple In-Memory Cache (30 detik TTL)
bmkg_cache = {
    "data": None,
    "last_fetched": 0
}

BMKG_URL = "https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json"

@router.get("", response_model=BMKGStatusResponse)
def get_bmkg_status():
    current_time = time.time()
    
    # Return cache if valid
    if bmkg_cache["data"] and (current_time - bmkg_cache["last_fetched"] < 30):
        return bmkg_cache["data"]
        
    try:
        req = urllib.request.Request(
            BMKG_URL, 
            headers={
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
                'Accept': 'application/json'
            }
        )
        with urllib.request.urlopen(req) as response:
            data = json.loads(response.read().decode('utf-8'))
            gempa = data["Infogempa"]["gempa"]
            
            # Deteksi manual potensi tsunami dari teks BMKG
            potensi_text = gempa.get("Potensi", "").lower()
            is_tsunami = "tsunami" in potensi_text and "tidak berpotensi" not in potensi_text
            
            result = BMKGStatusResponse(
                tanggal=gempa.get("Tanggal", ""),
                jam=gempa.get("Jam", ""),
                datetime=gempa.get("DateTime", ""),
                coordinates=gempa.get("Coordinates", ""),
                lintang=gempa.get("Lintang", ""),
                bujur=gempa.get("Bujur", ""),
                magnitude=gempa.get("Magnitude", ""),
                kedalaman=gempa.get("Kedalaman", ""),
                wilayah=gempa.get("Wilayah", ""),
                potensi=gempa.get("Potensi", ""),
                dirasakan=gempa.get("Dirasakan", ""),
                shakemap=gempa.get("Shakemap", ""),
                is_tsunami_potential=is_tsunami
            )
            
            # Update cache
            bmkg_cache["data"] = result
            bmkg_cache["last_fetched"] = current_time
            
            return result
            
    except Exception as e:
        # Fallback ke cache usang jika BMKG down
        if bmkg_cache["data"]:
            return bmkg_cache["data"]
            
        raise HTTPException(status_code=503, detail=f"Gagal mengambil data dari BMKG: {str(e)}")
