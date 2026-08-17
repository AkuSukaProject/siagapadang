from pydantic import BaseModel, ConfigDict
from typing import List, Dict, Any, Optional
from datetime import datetime

class DataVersionBase(BaseModel):
    dataset_name: str
    version: str
    schema_version: Optional[str] = None
    checksum: str
    download_url: Optional[str] = None
    size_bytes: Optional[int] = None
    minimum_app_version: Optional[str] = None
    published_at: datetime
    is_active: bool
    
    model_config = ConfigDict(from_attributes=True)

class SyncCheckResponse(BaseModel):
    has_update: bool
    latest_versions: List[DataVersionBase]
    message: str

class SyncDataResponse(BaseModel):
    dataset_name: str
    version: str
    checksum: str
    data: Dict[str, Any]  # Akan memuat GeoJSON FeatureCollection
