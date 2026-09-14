from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime

class CheckInRequest(BaseModel):
    event_external_id: Optional[str] = Field(None, description="External ID kejadian; kosong berarti event aktif terbaru")
    evacuation_point_external_id: str = Field(..., description="External ID Tempat Evakuasi (TES/TEA)")
    latitude: float = Field(..., description="Garis lintang lokasi pengguna")
    longitude: float = Field(..., description="Garis bujur lokasi pengguna")
    accuracy_m: float = Field(..., description="Akurasi GPS (maksimal 35m)")
    status: Optional[str] = Field("Selamat", description="Status keselamatan pengguna")

class CheckInResponse(BaseModel):
    status: str
    message: str
    event_external_id: str
    evacuation_point_external_id: str
    checked_in_at: datetime


class OccupancyReportRequest(BaseModel):
    evacuation_point_external_id: str
    level: str = Field(..., pattern="^(LOW|MODERATE|FULL)$")


class OccupancyStatusResponse(BaseModel):
    evacuation_point_external_id: str
    level: str
    report_count: int
    updated_at: Optional[datetime] = None
    source: str = "Laporan pengguna yang sudah check-in"
