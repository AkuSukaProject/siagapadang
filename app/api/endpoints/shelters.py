from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from datetime import datetime
from app.database import get_db
from app.models.domain import EvacuationPoint, Checkin, EmergencyEvent, EventStatus
from app.schemas.shelters import CheckInRequest, CheckInResponse
from app.middleware.security import get_device_id

router = APIRouter()

@router.post("/checkin", response_model=CheckInResponse)
def shelter_checkin(
    request: CheckInRequest,
    db: Session = Depends(get_db),
    device_hash: str = Depends(get_device_id)
):
    # 1. Cari event aktif
    active_event = db.query(EmergencyEvent).filter(EmergencyEvent.status == EventStatus.ACTIVE).order_by(EmergencyEvent.started_at.desc()).first()
    if not active_event:
        active_event = EmergencyEvent(source="USER_CHECKIN", status=EventStatus.ACTIVE)
        db.add(active_event)
        db.commit()
        db.refresh(active_event)
        
    # 2. Cari TES/TEA berdasarkan external_id
    point = db.query(EvacuationPoint).filter(EvacuationPoint.external_id == request.external_id).first()
    if not point:
        raise HTTPException(status_code=404, detail="Tempat Evakuasi tidak ditemukan di database backend.")
    
    # 3. Cek apakah sudah pernah checkin untuk event ini (upsert logic)
    checkin = db.query(Checkin).filter(
        Checkin.event_id == active_event.id,
        Checkin.device_hash == device_hash
    ).first()
    
    if checkin:
        # Update lokasi checkin terakhir
        checkin.evacuation_point_id = point.id
        checkin.status = request.status
        checkin.checked_in_at = datetime.utcnow()
    else:
        checkin = Checkin(
            event_id=active_event.id,
            device_hash=device_hash,
            evacuation_point_id=point.id,
            status=request.status,
            checked_in_at=datetime.utcnow()
        )
        db.add(checkin)
        
    db.commit()
    db.refresh(checkin)
    
    return CheckInResponse(
        status="success",
        message="Berhasil lapor selamat! Tetap tenang dan tunggu arahan petugas.",
        evacuation_point_external_id=point.external_id,
        checked_in_at=checkin.checked_in_at
    )
