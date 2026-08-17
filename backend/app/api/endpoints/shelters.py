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
    # 1. Validasi Akurasi GPS (Maksimal 35 meter)
    if request.accuracy_m > 35:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Akurasi GPS Anda terlalu buruk ({request.accuracy_m}m). Akurasi maksimal yang diperbolehkan adalah 35m."
        )

    # 2. Cari EmergencyEvent Aktif berdasarkan event_external_id
    active_event = db.query(EmergencyEvent).filter(
        EmergencyEvent.external_event_id == request.event_external_id,
        EmergencyEvent.status == EventStatus.ACTIVE
    ).first()
    
    if not active_event:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Kejadian darurat dengan ID '{request.event_external_id}' tidak ditemukan atau tidak dalam status ACTIVE."
        )
        
    # 3. Cari TES/TEA berdasarkan evacuation_point_external_id
    point = db.query(EvacuationPoint).filter(
        EvacuationPoint.external_id == request.evacuation_point_external_id
    ).first()
    
    if not point:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Tempat Evakuasi dengan ID '{request.evacuation_point_external_id}' tidak ditemukan."
        )
        
    # 4. Validasi Jarak Presisi: Jarak maksimal = min(20 + accuracy_m, 55) meter
    max_allowed_distance = min(20.0 + request.accuracy_m, 55.0)
    point_wkt = f"SRID=4326;POINT({request.longitude} {request.latitude})"
    
    if point.location is not None:
        is_within_distance = db.query(
            func.ST_DWithin(
                cast(EvacuationPoint.location, Geography),
                func.ST_GeographyFromText(point_wkt),
                max_allowed_distance
            )
        ).filter(EvacuationPoint.id == point.id).scalar()
        
        if not is_within_distance:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Lokasi Anda terlalu jauh dari Tempat Evakuasi ({request.evacuation_point_external_id}). Jarak maksimal toleransi adalah {max_allowed_distance}m."
            )
    
    # 5. Upsert Checkin Logic (Terikat pada event_id dan device_hash)
    checkin = db.query(Checkin).filter(
        Checkin.event_id == active_event.id,
        Checkin.device_hash == device_hash
    ).first()
    
    if checkin:
        # Perpindahan TES atau update status
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
