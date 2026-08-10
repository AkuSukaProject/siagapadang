from pydantic import BaseModel, Field
from typing import Optional

class ObstructionReportCreate(BaseModel):
    latitude: float = Field(..., description="Garis lintang halangan (Y)")
    longitude: float = Field(..., description="Garis bujur halangan (X)")
    dataset_version: str = Field(..., description="Versi dataset graf rute yang digunakan Android saat ini")
    edge_id: Optional[int] = Field(None, description="Opsional ID ruas jalan (BigInteger)")
    description: Optional[str] = Field(None, description="Opsional deskripsi halangan")

class ObstructionReportResponse(BaseModel):
    status: str
    message: str
    obstruction_id: int
    is_confirmed_blocked: bool
