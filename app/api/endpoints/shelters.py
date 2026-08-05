from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.domain import Shelter
from app.schemas.shelters import CheckInResponse
from app.middleware.security import get_device_id

router = APIRouter()

@router.post("/{shelter_id}/checkin", response_model=CheckInResponse)
def shelter_checkin(
    shelter_id: int,
    db: Session = Depends(get_db),
    device_id: str = Depends(get_device_id)
):
    # Cari shelter berdasarkan ID
    shelter = db.query(Shelter).filter(Shelter.id == shelter_id).first()
    if not shelter:
        raise HTTPException(status_code=404, detail="Shelter tidak ditemukan.")
    
    # Pendekatan Atomic Increment untuk performa ekstrim
    # Idealnya, kita butuh Redis untuk atomic incr. 
    # Di PostgreSQL, kita bisa gunakan UPDATE query langsung untuk menghindari race condition
    db.query(Shelter).filter(Shelter.id == shelter_id).update(
        {Shelter.current_occupancy: Shelter.current_occupancy + 1},
        synchronize_session=False
    )
    db.commit()
    
    # Ambil nilai terbaru
    db.refresh(shelter)
    
    return CheckInResponse(
        status="success",
        message="Berhasil lapor selamat! Menunggu arahan petugas BPBD di lokasi.",
        shelter_id=shelter.id,
        current_occupancy=shelter.current_occupancy
    )
