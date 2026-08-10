from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import func, cast
from geoalchemy2 import Geography
from datetime import datetime
import pytz
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
    # 1. Pastikan ada EmergencyEvent Aktif
    active_event = db.query(EmergencyEvent).filter(
        EmergencyEvent.status == EventStatus.ACTIVE
    ).order_by(EmergencyEvent.started_at.desc()).first()
    
    if not active_event:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Tidak ada kejadian darurat (Emergency Event) yang aktif. Laporan check-in tidak dapat diterima."
        )
        
    # 2. Cari TES/TEA berdasarkan external_id
    point = db.query(EvacuationPoint).filter(EvacuationPoint.external_id == request.external_id).first()
    if not point:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Tempat Evakuasi tidak ditemukan di database backend."
        )
        
    # 3. Validasi jarak (ST_DWithin) dengan memperhitungkan akurasi GPS pengguna + radius fleksibilitas 50 meter
    point_wkt = f"SRID=4326;POINT({request.longitude} {request.latitude})"
    radius = request.accuracy_m + 50
    
    if point.location is not None:
        is_within_distance = db.scalar(
            func.ST_DWithin(
                cast(point.location, Geography),
                func.ST_GeographyFromText(point_wkt),
                radius
            )
        )
        
        if not is_within_distance:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Lokasi Anda terlalu jauh dari Tempat Evakuasi yang dipilih. Pastikan Anda berada di area evakuasi."
            )
    
    # 4. Cek apakah sudah pernah checkin untuk event ini (upsert logic)
    checkin = db.query(Checkin).filter(
        Checkin.event_id == active_event.id,
        Checkin.device_hash == device_hash
    ).first()
    
    if checkin:
        # Update lokasi checkin terakhir
        checkin.evacuation_point_id = point.id
        checkin.status = request.status
        checkin.checked_in_at = datetime.now(pytz.utc)
    else:
        checkin = Checkin(
            event_id=active_event.id,
            device_hash=device_hash,
            evacuation_point_id=point.id,
            status=request.status,
            checked_in_at=datetime.now(pytz.utc)
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
