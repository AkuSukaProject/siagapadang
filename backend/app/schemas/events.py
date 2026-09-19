from datetime import datetime
from typing import Optional

from pydantic import BaseModel


class ActiveEventResponse(BaseModel):
    active: bool
    event_external_id: Optional[str] = None
    source: Optional[str] = None
    started_at: Optional[datetime] = None

class EventCreate(BaseModel):
    source: str = "BMKG"
    external_event_id: Optional[str] = None

class EventUpdateStatus(BaseModel):
    status: str

class EventResponse(BaseModel):
    id: int
    external_event_id: Optional[str] = None
    source: str
    status: str
    started_at: datetime
    ended_at: Optional[datetime] = None
    
    class Config:
        from_attributes = True
