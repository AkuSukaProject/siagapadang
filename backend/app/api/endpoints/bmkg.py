import json
import logging
import time
import urllib.request
from datetime import datetime, timezone
from typing import Optional, TypedDict

from fastapi import APIRouter, HTTPException, status

from app.schemas.bmkg import BMKGStatusResponse

router = APIRouter()
logger = logging.getLogger(__name__)


class BMKGCache(TypedDict):
    data: Optional[BMKGStatusResponse]
    last_fetched: float


# Cache lima menit mengurangi beban ke layanan publik BMKG.
bmkg_cache: BMKGCache = {
    "data": None,
    "last_fetched": 0.0,
}

BMKG_URL = "https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json"
BMKG_ATTRIBUTION = "BMKG (Badan Meteorologi, Klimatologi, dan Geofisika)"


@router.get("", response_model=BMKGStatusResponse)
def get_bmkg_status():
    current_time = time.time()

    if bmkg_cache["data"] and current_time - bmkg_cache["last_fetched"] < 300:
        return bmkg_cache["data"]

    try:
        request = urllib.request.Request(
            BMKG_URL,
            headers={
                "User-Agent": "SiagaPadang/0.1 (+https://github.com/AkuSukaProject/siagapadang)",
                "Accept": "application/json",
            },
        )
        with urllib.request.urlopen(request, timeout=3) as response:
            data = json.loads(response.read().decode("utf-8"))
            gempa = data.get("Infogempa", {}).get("gempa", {})

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
            data_status="live",
            fetched_at=datetime.now(timezone.utc),
            source=BMKG_ATTRIBUTION,
        )
        bmkg_cache["data"] = result
        bmkg_cache["last_fetched"] = current_time
        return result
    except Exception as exc:
        logger.warning("Gagal mengambil status BMKG: %s", exc)
        if bmkg_cache["data"]:
            return bmkg_cache["data"].model_copy(update={"data_status": "stale"})

        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Status BMKG sedang tidak tersedia. Coba lagi beberapa saat.",
        )
