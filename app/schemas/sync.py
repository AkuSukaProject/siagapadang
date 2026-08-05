from pydantic import BaseModel
from typing import List, Dict, Any, Optional

class SyncCheckResponse(BaseModel):
    has_update: bool
    latest_shelter_version: str
    latest_inundation_version: str
    message: str

class SyncDataResponse(BaseModel):
    dataset_name: str
    version: str
    checksum: str
    data: Dict[str, Any]  # Akan memuat GeoJSON FeatureCollection
