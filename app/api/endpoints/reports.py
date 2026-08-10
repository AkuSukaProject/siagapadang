from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import func, cast
from sqlalchemy.exc import IntegrityError
from geoalchemy2 import Geography
from datetime import datetime, timedelta
import pytz
from app.database import get_db
from app.models.domain import EmergencyEvent, Obstruction, ObstructionReport, EventStatus, ObstructionStatus, RouteEdge
from app.schemas.reports import ObstructionReportCreate, ObstructionReportResponse
from app.middleware.security import get_device_id

router = APIRouter()

@router.post("/obstruction", response_model=ObstructionReportResponse)
def report_obstruction(
    report: ObstructionReportCreate,
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
            detail="Tidak ada kejadian darurat (Emergency Event) yang aktif. Laporan tidak dapat diterima."
        )
        
    # 2. Validasi Jarak dengan Ruas Jalan (jika data RouteEdge ada di database)
    point_wkt = f"SRID=4326;POINT({report.longitude} {report.latitude})"
    
    edge = db.query(RouteEdge).filter(
        RouteEdge.dataset_version_id == report.dataset_version_id,
        RouteEdge.edge_external_id == report.edge_external_id
    ).first()
    
    if edge:
        is_within_distance = db.scalar(
            func.ST_DWithin(
                func.ST_GeographyFromText(point_wkt),
                cast(edge.geometry, Geography),
                50  # 50 meter radius
            )
        )
        if not is_within_distance:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Lokasi laporan terlalu jauh (>50m) dari ruas jalan yang dilaporkan."
            )
        
    # 3. Cari atau Buat Obstruction (Induk hambatan)
    obstruction = db.query(Obstruction).filter(
        Obstruction.event_id == active_event.id,
        Obstruction.dataset_version_id == report.dataset_version_id,
        Obstruction.edge_external_id == report.edge_external_id
    ).first()
    
    if not obstruction:
        try:
            obstruction = Obstruction(
                event_id=active_event.id,
                dataset_version_id=report.dataset_version_id,
                edge_external_id=report.edge_external_id,
                status=ObstructionStatus.PENDING,
                expires_at=datetime.now(pytz.utc) + timedelta(hours=6)
            )
            db.add(obstruction)
            db.commit()
            db.refresh(obstruction)
        except IntegrityError:
            db.rollback()
            # Race condition handling: obstruction sudah dibuat oleh request lain
            obstruction = db.query(Obstruction).filter(
                Obstruction.event_id == active_event.id,
                Obstruction.dataset_version_id == report.dataset_version_id,
                Obstruction.edge_external_id == report.edge_external_id
            ).first()
            
    if obstruction and obstruction.status == ObstructionStatus.EXPIRED:
         raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Hambatan ini sudah ditandai kadaluarsa/selesai."
        )

    # 4. Simpan Laporan Warga
    try:
        new_report = ObstructionReport(
            obstruction_id=obstruction.id,
            device_hash=device_hash,
            location=point_wkt,
            description=report.description
        )
        db.add(new_report)
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Perangkat Anda sudah melaporkan hambatan ini sebelumnya pada event yang sama."
        )
    
    # 5. Evaluasi Threshold Laporan dalam 15 menit terakhir
    time_threshold = datetime.now(pytz.utc) - timedelta(minutes=15)
    recent_reports_count = db.query(ObstructionReport.device_hash).filter(
        ObstructionReport.obstruction_id == obstruction.id,
        ObstructionReport.reported_at >= time_threshold
    ).distinct().count()
    
    is_confirmed_blocked = False
    
    if recent_reports_count >= 3 and obstruction.status == ObstructionStatus.PENDING:
        obstruction.status = ObstructionStatus.CONFIRMED
        obstruction.confirmed_at = datetime.now(pytz.utc)
        obstruction.expires_at = datetime.now(pytz.utc) + timedelta(hours=6)
        db.commit()
        is_confirmed_blocked = True
    elif obstruction.status == ObstructionStatus.CONFIRMED:
        is_confirmed_blocked = True
        
    return ObstructionReportResponse(
        status="success",
        message="Laporan diterima" if not is_confirmed_blocked else "Jalan ini kini ditandai PUTUS untuk pengguna lain.",
        obstruction_id=obstruction.id,
        is_confirmed_blocked=is_confirmed_blocked
    )
