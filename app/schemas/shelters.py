from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime

class CheckInRequest(BaseModel):
    external_id: str = Field(..., description="External ID TES/TEA")
    latitude: float = Field(..., description="Garis lintang lokasi pengguna")
    longitude: float = Field(..., description="Garis bujur lokasi pengguna")
    accuracy_m: float = Field(..., description="Akurasi koordinat pengguna dalam satuan meter")
    status: Optional[str] = Field("Selamat", description="Status pengguna")

class CheckInResponse(BaseModel):
    status: str
    message: str
    evacuation_point_external_id: str
    checked_in_at: datetime
