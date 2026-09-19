from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
from datetime import datetime
import pytz

from app.database import get_db
from app.models.domain import EmergencyEvent, EventStatus, EventStatusHistory
from app.schemas.events import ActiveEventResponse, EventCreate, EventUpdateStatus, EventResponse
from app.middleware.security import verify_admin_token

router = APIRouter()

@router.get("", response_model=ActiveEventResponse)
def get_active_event(db: Session = Depends(get_db)):
    """Masyarakat hanya melihat event NYATA secara publik, KECUALI ingin query yang simulasi."""
    event = db.query(EmergencyEvent).filter(
        EmergencyEvent.status == EventStatus.ACTIVE,
        EmergencyEvent.is_simulation == False
    ).order_by(EmergencyEvent.started_at.desc()).first()
    if not event:
        return ActiveEventResponse(active=False)
    return ActiveEventResponse(
        active=True,
        event_external_id=event.external_event_id,
        source=event.source,
        started_at=event.started_at,
        is_simulation=event.is_simulation
    )

@router.post("", response_model=EventResponse)
def start_emergency_event(
    event_in: EventCreate, 
    db: Session = Depends(get_db),
    operator_name: str = Depends(verify_admin_token)
):
    # Logika Override: Jika ini event NYATA, tutup event SIMULASI yang sedang aktif (jika ada)
    if not event_in.is_simulation:
        active_sim = db.query(EmergencyEvent).filter(
            EmergencyEvent.status == EventStatus.ACTIVE,
            EmergencyEvent.is_simulation == True
        ).first()
        
        if active_sim:
            active_sim.status = EventStatus.CLOSED
            active_sim.ended_at = datetime.now(pytz.utc)
            # Log audit override
            sim_history = EventStatusHistory(
                event_id=active_sim.id,
                old_status=EventStatus.ACTIVE,
                new_status=EventStatus.CLOSED,
                operator_name="SYSTEM",
                change_reason="ditutup otomatis karena event nyata diaktifkan"
            )
            db.add(sim_history)

    try:
        new_event = EmergencyEvent(
            source=event_in.source,
            external_event_id=event_in.external_event_id,
            status=EventStatus.ACTIVE,
            is_simulation=event_in.is_simulation
        )
        db.add(new_event)
        db.flush() # Untuk mendapatkan new_event.id
        
        # Log Audit Creation
        history = EventStatusHistory(
            event_id=new_event.id,
            old_status=None,
            new_status=EventStatus.ACTIVE,
            operator_name=operator_name,
            change_reason=event_in.change_reason or "Inisiasi event darurat baru"
        )
        db.add(history)
        
        db.commit()
        db.refresh(new_event)
        return new_event
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Terdapat event darurat lain dengan tipe yang sama yang masih ACTIVE. Harap tutup event sebelumnya terlebih dahulu."
        )

@router.put("/{event_id}/status", response_model=EventResponse)
def update_event_status(
    event_id: int, 
    update_data: EventUpdateStatus, 
    db: Session = Depends(get_db),
    operator_name: str = Depends(verify_admin_token)
):
    event = db.query(EmergencyEvent).filter(EmergencyEvent.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Event tidak ditemukan")
        
    try:
        new_status = EventStatus(update_data.status)
    except ValueError:
        raise HTTPException(status_code=400, detail="Status tidak valid. Gunakan ACTIVE, CLOSED, atau CANCELLED")

    old_status = event.status

    if new_status in [EventStatus.CLOSED, EventStatus.CANCELLED] and old_status == EventStatus.ACTIVE:
        event.ended_at = datetime.now(pytz.utc)
        
    event.status = new_status
    
    # Audit log
    history = EventStatusHistory(
        event_id=event.id,
        old_status=old_status,
        new_status=new_status,
        operator_name=operator_name,
        change_reason=update_data.change_reason
    )
    db.add(history)
    
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
