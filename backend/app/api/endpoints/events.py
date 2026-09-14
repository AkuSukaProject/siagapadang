from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.domain import EmergencyEvent, EventStatus
from app.schemas.events import ActiveEventResponse

router = APIRouter()


@router.get("", response_model=ActiveEventResponse)
def get_active_event(db: Session = Depends(get_db)):
    event = db.query(EmergencyEvent).filter(
        EmergencyEvent.status == EventStatus.ACTIVE,
    ).order_by(EmergencyEvent.started_at.desc()).first()
    if not event:
        return ActiveEventResponse(active=False)
    return ActiveEventResponse(
        active=True,
        event_external_id=event.external_event_id,
        source=event.source,
        started_at=event.started_at,
    )
