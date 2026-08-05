from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.database import get_db
from app.models.domain import ObstructionReport
from app.schemas.reports import ObstructionReportCreate, ObstructionReportResponse
from app.middleware.security import get_device_id

router = APIRouter()

@router.post("/obstruction", response_model=ObstructionReportResponse)
def report_obstruction(
    report: ObstructionReportCreate,
    db: Session = Depends(get_db),
    device_id: str = Depends(get_device_id)
):
    # 1. Simpan laporan baru
    point = f"SRID=4326;POINT({report.longitude} {report.latitude})"
    new_report = ObstructionReport(
        device_id=device_id,
        location=point,
        description=report.description
    )
    db.add(new_report)
    db.commit()
    db.refresh(new_report)
    
    # 2. Algoritma Cerdas: Cek radius 50 meter
    # Pastikan kita menghitung user yang unik (berdasarkan device_id)
    # ST_DWithin membandingkan 2 geometri. Parameter ke-3 adalah jarak (dalam derajat jika SRID 4326, 
    # tapi kita cast ke geography agar jaraknya dalam meter)
    nearby_reports = db.query(ObstructionReport.device_id).filter(
        func.ST_DWithin(
            func.Geography(ObstructionReport.location),
            func.Geography(func.ST_GeomFromText(f"POINT({report.longitude} {report.latitude})", 4326)),
            50.0  # 50 meter
        )
    ).distinct().count()
    
    # 3. Threshold 3 laporan unik
    is_blocked = nearby_reports >= 3
    
    # Jika is_blocked = True, di sistem nyata kita akan memasukkan data ini 
    # ke antrean Graph Update (RabbitMQ / Kafka) untuk disinkronkan ke HP warga.
    
    return ObstructionReportResponse(
        status="success",
        message="Laporan diterima" if not is_blocked else "Jalan ini kini ditandai PUTUS untuk pengguna lain.",
        is_blocked=is_blocked,
        report_count=nearby_reports
    )
