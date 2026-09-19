from datetime import datetime
from typing import Optional

from pydantic import BaseModel
from pydantic import BaseModel, Field


class ActiveEventResponse(BaseModel):
    active: bool
    event_external_id: Optional[str] = None
    source: Optional[str] = None
    started_at: Optional[datetime] = None
    is_simulation: Optional[bool] = None

class EventCreate(BaseModel):
    external_event_id: Optional[str] = Field(None, description="ID event darurat dari sumber eksternal (misal: ID gempa BMKG)")
    source: str = Field(..., description="Sumber informasi, contoh: 'BMKG'")
    is_simulation: bool = Field(False, description="Tandai true jika ini adalah event latihan/drill, bukan bencana nyata")
    change_reason: Optional[str] = Field(None, description="Alasan pengaktifan event")

class EventUpdateStatus(BaseModel):
    status: str = Field(..., description="Status event baru (ACTIVE, CLOSED, CANCELLED)")
    change_reason: Optional[str] = Field(None, description="Alasan penutupan atau perubahan status")

class EventResponse(BaseModel):
    id: int
    external_event_id: Optional[str] = None
    source: str
    status: str
    started_at: datetime
    ended_at: Optional[datetime] = None
    
    class Config:
        from_attributes = True
