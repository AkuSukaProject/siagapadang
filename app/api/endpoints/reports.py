from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy import func
from datetime import datetime, timedelta
from app.database import get_db
from app.models.domain import EmergencyEvent, Obstruction, ObstructionReport, EventStatus, ObstructionStatus
from app.schemas.reports import ObstructionReportCreate, ObstructionReportResponse
from app.middleware.security import get_device_id

router = APIRouter()

@router.post("/obstruction", response_model=ObstructionReportResponse)
def report_obstruction(
    report: ObstructionReportCreate,
    db: Session = Depends(get_db),
    device_hash: str = Depends(get_device_id)
):
    if not report.edge_id:
        raise HTTPException(status_code=400, detail="Pembaruan saat ini mewajibkan edge_id untuk sinkronisasi graf.")

    # 1. Cari atau Buat EmergencyEvent Aktif
    active_event = db.query(EmergencyEvent).filter(EmergencyEvent.status == EventStatus.ACTIVE).order_by(EmergencyEvent.started_at.desc()).first()
    if not active_event:
        active_event = EmergencyEvent(
            source="USER_REPORT",
            status=EventStatus.ACTIVE
        )
        db.add(active_event)
        db.commit()
        db.refresh(active_event)
        
    # 2. Cari atau Buat Obstruction (Induk hambatan)
    obstruction = db.query(Obstruction).filter(
        Obstruction.event_id == active_event.id,
        Obstruction.edge_id == report.edge_id,
        Obstruction.dataset_version == report.dataset_version
    ).first()
    
    if not obstruction:
        obstruction = Obstruction(
            event_id=active_event.id,
            edge_id=report.edge_id,
            dataset_version=report.dataset_version,
            status=ObstructionStatus.PENDING,
            expires_at=datetime.utcnow() + timedelta(hours=6)
        )
        db.add(obstruction)
        db.commit()
        db.refresh(obstruction)
        
    # 3. Simpan Laporan Warga
    point = f"SRID=4326;POINT({report.longitude} {report.latitude})"
    new_report = ObstructionReport(
        obstruction_id=obstruction.id,
        device_hash=device_hash,
        location=point,
        description=report.description
    )
    db.add(new_report)
    db.commit()
    
    # 4. Evaluasi Threshold Laporan (Crowd-Sourced Validation)
    unique_reports_count = db.query(ObstructionReport.device_hash).filter(
        ObstructionReport.obstruction_id == obstruction.id
    ).distinct().count()
    
    is_confirmed_blocked = False
    if unique_reports_count >= 3 and obstruction.status == ObstructionStatus.PENDING:
        obstruction.status = ObstructionStatus.CROWD_CONFIRMED
        obstruction.confirmed_at = datetime.utcnow()
        db.commit()
        is_confirmed_blocked = True
    elif obstruction.status in [ObstructionStatus.CROWD_CONFIRMED, ObstructionStatus.OFFICIAL_CONFIRMED]:
        is_confirmed_blocked = True
        
    return ObstructionReportResponse(
        status="success",
        message="Laporan diterima" if not is_confirmed_blocked else "Jalan ini kini ditandai PUTUS untuk pengguna lain.",
        obstruction_id=obstruction.id,
        is_confirmed_blocked=is_confirmed_blocked
    )
