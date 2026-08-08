from pydantic import BaseModel, Field
from typing import Optional

class ObstructionReportCreate(BaseModel):
    latitude: float = Field(..., description="Garis lintang halangan (Y)")
    longitude: float = Field(..., description="Garis bujur halangan (X)")
    edge_id: Optional[str] = Field(None, description="Opsional ID ruas jalan (jika Android bisa menghitungnya)")
    description: Optional[str] = Field(None, description="Opsional deskripsi halangan")

class ObstructionReportResponse(BaseModel):
    status: str
    message: str
    is_blocked: bool
    report_count: int
