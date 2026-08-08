from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.domain import EvacuationPoint, Checkin
from app.schemas.shelters import CheckInResponse
from app.middleware.security import get_device_id

router = APIRouter()

@router.post("/{point_id}/checkin", response_model=CheckInResponse)
def shelter_checkin(
    point_id: int,
    db: Session = Depends(get_db),
    device_id: str = Depends(get_device_id)
):
    # Cari TES/TEA berdasarkan ID
    point = db.query(EvacuationPoint).filter(EvacuationPoint.id == point_id).first()
    if not point:
        raise HTTPException(status_code=404, detail="Tempat Evakuasi tidak ditemukan.")
    
    # 1. Catat checkin pengguna ke tabel Checkin
    new_checkin = Checkin(
        device_id=device_id,
        evacuation_point_id=point_id,
        status="Selamat"
    )
    db.add(new_checkin)
    
    db.commit()
    
    return CheckInResponse(
        status="success",
        message="Berhasil lapor selamat! Menunggu arahan petugas BPBD di lokasi.",
        evacuation_point_id=point.id
    )
