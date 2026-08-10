from pydantic import BaseModel
from typing import Optional
from datetime import datetime

class CheckInRequest(BaseModel):
    external_id: str
    status: Optional[str] = "Selamat"

class CheckInResponse(BaseModel):
    status: str
    message: str
    evacuation_point_external_id: str
    checked_in_at: datetime
