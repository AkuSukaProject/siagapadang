from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.domain import DataVersion, EvacuationPoint, InundationZone
from app.schemas.sync import SyncCheckResponse, SyncDataResponse
import json

router = APIRouter()

@router.get("/check", response_model=SyncCheckResponse)
def check_updates(client_shelter_version: str = "v0.0.0", client_inundation_version: str = "v0.0.0", db: Session = Depends(get_db)):
    """
    Endpoint untuk mengecek apakah ada versi data terbaru di server (dibandingkan dengan versi SQLite di HP).
    Sangat ringan, dieksekusi di Background Worker Android.
    """
    latest_shelter = db.query(DataVersion).filter_by(dataset_name="shelters").order_by(DataVersion.id.desc()).first()
    latest_inund = db.query(DataVersion).filter_by(dataset_name="inundation_zones").order_by(DataVersion.id.desc()).first()
    
    if not latest_shelter or not latest_inund:
        raise HTTPException(status_code=404, detail="Data versioning belum disetel di server.")
        
    has_update = (latest_shelter.version != client_shelter_version) or (latest_inund.version != client_inundation_version)
    
    return SyncCheckResponse(
        has_update=has_update,
        latest_shelter_version=latest_shelter.version,
        latest_inundation_version=latest_inund.version,
        message="Update available, silakan panggil /shelters" if has_update else "Peta di perangkat sudah yang paling baru."
    )

@router.get("/shelters", response_model=SyncDataResponse)
def get_sync_shelters(db: Session = Depends(get_db)):
    """
    Mengunduh seluruh data shelter beserta metadata (elevasi, kapasitas, akses masuk) dalam format GeoJSON standar.
    Hanya dipanggil Android jika /check mengembalikan has_update=True.
    """
    latest_shelter = db.query(DataVersion).filter_by(dataset_name="shelters").order_by(DataVersion.id.desc()).first()
    if not latest_shelter:
        raise HTTPException(status_code=404, detail="No shelter data available.")
        
    shelters = db.query(EvacuationPoint).all()
    
    features = []
    for s in shelters:
        # Menggunakan ST_AsGeoJSON dari GeoAlchemy2 untuk konversi aman dari PostGIS (WKB) ke JSON
        geom_json_str = db.scalar(s.location.ST_AsGeoJSON())
        geom = json.loads(geom_json_str) if geom_json_str else None
        
        entrance_json_str = db.scalar(s.entrance_coord.ST_AsGeoJSON())
        entrance_geom = json.loads(entrance_json_str) if entrance_json_str else None
        
        features.append({
            "type": "Feature",
            "geometry": geom,
            "properties": {
                "id": s.id,
                "name": s.name,
                "capacity": s.capacity,
                "elevation_m": s.elevation_m,
                "status": "active" if s.is_operational else "inactive",
                "entrance_geometry": entrance_geom
            }
        })
        
    feature_collection = {
        "type": "FeatureCollection",
        "features": features
    }
    
    return SyncDataResponse(
        dataset_name="shelters",
        version=latest_shelter.version,
        checksum=latest_shelter.checksum,
        data=feature_collection
    )
    
@router.get("/network")
def get_sync_network():
    """
    Endpoint placeholder untuk mengunduh patch/update graf (node/edge OSM) terbaru.
    Ini dipisahkan dari shelters karena ukuran graf jauh lebih besar.
    """
    return {"message": "Network graph patch endpoint (Not implemented yet)"}
