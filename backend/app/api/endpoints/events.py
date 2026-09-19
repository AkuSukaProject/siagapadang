from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
from datetime import datetime
import pytz

from app.database import get_db
from app.models.domain import EmergencyEvent, EventStatus
from app.schemas.events import ActiveEventResponse, EventCreate, EventUpdateStatus, EventResponse

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

@router.post("", response_model=EventResponse)
def start_emergency_event(event_in: EventCreate, db: Session = Depends(get_db)):
    try:
        new_event = EmergencyEvent(
            source=event_in.source,
            external_event_id=event_in.external_event_id,
            status=EventStatus.ACTIVE
        )
        db.add(new_event)
        db.commit()
        db.refresh(new_event)
        return new_event
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Terdapat event darurat lain yang masih ACTIVE. Harap tutup event sebelumnya terlebih dahulu."
        )

@router.put("/{event_id}/status", response_model=EventResponse)
def update_event_status(event_id: int, update_data: EventUpdateStatus, db: Session = Depends(get_db)):
    event = db.query(EmergencyEvent).filter(EmergencyEvent.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Event tidak ditemukan")
        
    try:
        new_status = EventStatus(update_data.status)
    except ValueError:
        raise HTTPException(status_code=400, detail="Status tidak valid. Gunakan ACTIVE, CLOSED, atau CANCELLED")

    if new_status in [EventStatus.CLOSED, EventStatus.CANCELLED] and event.status == EventStatus.ACTIVE:
        event.ended_at = datetime.now(pytz.utc)
        
    event.status = new_status
    
    try:
        db.commit()
        db.refresh(event)
        return event
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Tidak dapat mengupdate ke ACTIVE karena sudah ada event lain yang ACTIVE."
        )
