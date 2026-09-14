from datetime import datetime
from typing import Optional

from pydantic import BaseModel


class ActiveEventResponse(BaseModel):
    active: bool
    event_external_id: Optional[str] = None
    source: Optional[str] = None
    started_at: Optional[datetime] = None
