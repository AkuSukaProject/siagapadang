from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import func, cast
from geoalchemy2 import Geography
from collections import Counter
from datetime import datetime, timedelta
import pytz
from app.database import get_db
from app.models.domain import (
    Checkin,
    EmergencyEvent,
    EvacuationPoint,
    EventStatus,
    OccupancyLevel,
    ShelterOccupancyReport,
)
from app.schemas.shelters import (
    CheckInRequest,
    CheckInResponse,
    OccupancyReportRequest,
    OccupancyStatusResponse,
)
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

    # 2. Event dipilih backend agar klien tidak harus menebak ID kejadian aktif.
    event_query = db.query(EmergencyEvent).filter(
        EmergencyEvent.status == EventStatus.ACTIVE,
    )
    if request.event_external_id:
        event_query = event_query.filter(
            EmergencyEvent.external_event_id == request.event_external_id,
        )
    active_event = event_query.order_by(EmergencyEvent.started_at.desc()).first()
    
    if not active_event:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Tidak ada kejadian darurat aktif yang sesuai."
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
        event_external_id=active_event.external_event_id,
        evacuation_point_external_id=point.external_id,
        checked_in_at=checkin.checked_in_at
    )


@router.post("/occupancy", response_model=OccupancyStatusResponse)
def report_occupancy(
    request: OccupancyReportRequest,
    db: Session = Depends(get_db),
    device_hash: str = Depends(get_device_id),
):
    active_event = db.query(EmergencyEvent).filter(
        EmergencyEvent.status == EventStatus.ACTIVE,
    ).order_by(EmergencyEvent.started_at.desc()).first()
    if not active_event:
        raise HTTPException(status_code=400, detail="Tidak ada kejadian darurat aktif.")

    point = db.query(EvacuationPoint).filter(
        EvacuationPoint.external_id == request.evacuation_point_external_id,
    ).first()
    if not point:
        raise HTTPException(status_code=404, detail="Tempat evakuasi tidak ditemukan.")

    checkin = db.query(Checkin).filter(
        Checkin.event_id == active_event.id,
        Checkin.evacuation_point_id == point.id,
        Checkin.device_hash == device_hash,
    ).first()
    if not checkin:
        raise HTTPException(
            status_code=403,
            detail="Laporan okupansi hanya dapat dikirim setelah check-in di tempat ini.",
        )

    report = db.query(ShelterOccupancyReport).filter(
        ShelterOccupancyReport.event_id == active_event.id,
        ShelterOccupancyReport.evacuation_point_id == point.id,
        ShelterOccupancyReport.device_hash == device_hash,
    ).first()
    if report:
        report.level = OccupancyLevel(request.level)
        report.reported_at = datetime.now(pytz.utc)
    else:
        report = ShelterOccupancyReport(
            event_id=active_event.id,
            evacuation_point_id=point.id,
            device_hash=device_hash,
            level=OccupancyLevel(request.level),
            reported_at=datetime.now(pytz.utc),
        )
        db.add(report)
    db.commit()
    return _occupancy_status(db, active_event.id, point)


@router.get("/{external_id}/occupancy", response_model=OccupancyStatusResponse)
def get_occupancy_status(external_id: str, db: Session = Depends(get_db)):
    active_event = db.query(EmergencyEvent).filter(
        EmergencyEvent.status == EventStatus.ACTIVE,
    ).order_by(EmergencyEvent.started_at.desc()).first()
    if not active_event:
        raise HTTPException(status_code=404, detail="Tidak ada kejadian darurat aktif.")
    point = db.query(EvacuationPoint).filter(EvacuationPoint.external_id == external_id).first()
    if not point:
        raise HTTPException(status_code=404, detail="Tempat evakuasi tidak ditemukan.")
    return _occupancy_status(db, active_event.id, point)


def _occupancy_status(
    db: Session,
    event_id: int,
    point: EvacuationPoint,
) -> OccupancyStatusResponse:
    threshold = datetime.now(pytz.utc) - timedelta(minutes=30)
    reports = db.query(ShelterOccupancyReport).filter(
        ShelterOccupancyReport.event_id == event_id,
        ShelterOccupancyReport.evacuation_point_id == point.id,
        ShelterOccupancyReport.reported_at >= threshold,
    ).all()
    if not reports:
        return OccupancyStatusResponse(
            evacuation_point_external_id=point.external_id,
            level="UNKNOWN",
            report_count=0,
        )
    counts = Counter(report.level.value for report in reports)
    level = max(
        ("LOW", "MODERATE", "FULL"),
        key=lambda value: (counts[value], ("LOW", "MODERATE", "FULL").index(value)),
    )
    return OccupancyStatusResponse(
        evacuation_point_external_id=point.external_id,
        level=level,
        report_count=len(reports),
        updated_at=max(report.reported_at for report in reports),
    )
