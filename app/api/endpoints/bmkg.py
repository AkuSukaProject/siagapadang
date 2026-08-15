import urllib.request
import json
import time
from typing import TypedDict, Optional
from fastapi import APIRouter
from app.schemas.bmkg import BMKGStatusResponse

router = APIRouter()

class BMKGCache(TypedDict):
    data: Optional[BMKGStatusResponse]
    last_fetched: float

# Simple In-Memory Cache (300 detik TTL)
bmkg_cache: BMKGCache = {
    "data": None,
    "last_fetched": 0.0
}

BMKG_URL = "https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json"
BMKG_ATTRIBUTION = "BMKG (Badan Meteorologi, Klimatologi, dan Geofisika)"

@router.get("", response_model=BMKGStatusResponse)
def get_bmkg_status():
    current_time = time.time()
    
    # Return cache if valid (300 detik / 5 menit)
    if bmkg_cache["data"] and (current_time - bmkg_cache["last_fetched"] < 300):
        return bmkg_cache["data"]
        
    try:
        req = urllib.request.Request(
            BMKG_URL, 
            headers={
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
                'Accept': 'application/json'
            }
        )
        with urllib.request.urlopen(req, timeout=5) as response:
            data = json.loads(response.read().decode('utf-8'))
            gempa = data.get("Infogempa", {}).get("gempa", {})
            
            # Deteksi manual potensi tsunami dari teks BMKG
            potensi_text = str(gempa.get("Potensi") or "").lower()
            is_tsunami = "tsunami" in potensi_text and "tidak berpotensi" not in potensi_text
            
            result = BMKGStatusResponse(
                tanggal=str(gempa.get("Tanggal") or ""),
                jam=str(gempa.get("Jam") or ""),
                datetime=str(gempa.get("DateTime") or ""),
                coordinates=str(gempa.get("Coordinates") or ""),
                lintang=str(gempa.get("Lintang") or ""),
                bujur=str(gempa.get("Bujur") or ""),
                magnitude=str(gempa.get("Magnitude") or ""),
                kedalaman=str(gempa.get("Kedalaman") or ""),
                wilayah=str(gempa.get("Wilayah") or ""),
                potensi=str(gempa.get("Potensi") or ""),
                dirasakan=str(gempa.get("Dirasakan") or ""),
                shakemap=str(gempa.get("Shakemap") or ""),
                is_tsunami_potential=is_tsunami,
                source=BMKG_ATTRIBUTION
            )
            
            # Update cache
            bmkg_cache["data"] = result
            bmkg_cache["last_fetched"] = current_time
            
            return result
            
    except Exception:
        # Fallback ke cache usang jika BMKG down / mock fixture
        if bmkg_cache["data"]:
            return bmkg_cache["data"]
            
        return BMKGStatusResponse(
            tanggal="",
            jam="",
            datetime="",
            coordinates="",
            lintang="",
            bujur="",
            magnitude="",
            kedalaman="",
            wilayah="TEST FIXTURE — BUKAN INFORMASI GEMPA AKTUAL",
            potensi="TEST FIXTURE — BUKAN INFORMASI GEMPA AKTUAL",
            dirasakan="",
            shakemap="",
            is_tsunami_potential=False,
            source=BMKG_ATTRIBUTION
        )
