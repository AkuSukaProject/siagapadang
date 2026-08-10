from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.database import get_db
from app.models.domain import DataVersion, EvacuationPoint, InundationZone, SafeZone
from app.schemas.sync import SyncCheckResponse, SyncDataResponse, DataVersionBase
import json
from typing import List

router = APIRouter()

@router.get("/check", response_model=SyncCheckResponse)
def check_updates(db: Session = Depends(get_db)):
    """
    Endpoint untuk mendapatkan metadata versi terbaru dari semua dataset yang aktif.
    Android akan membandingkan daftar ini dengan versi lokal SQLite-nya.
    """
    # Ambil versi terbaru per dataset_name
    subquery = db.query(
        DataVersion.dataset_name, 
        func.max(DataVersion.id).label('max_id')
    ).filter(DataVersion.is_active == True).group_by(DataVersion.dataset_name).subquery()
    
    latest_versions = db.query(DataVersion).join(
        subquery, 
        (DataVersion.dataset_name == subquery.c.dataset_name) & (DataVersion.id == subquery.c.max_id)
    ).all()
    
    # Gunakan pydantic model_validate untuk konversi langsung jika DataVersionBase dikonfigurasi from_attributes
    return SyncCheckResponse(
        has_update=len(latest_versions) > 0,
        latest_versions=[DataVersionBase.model_validate(v) for v in latest_versions],
        message="Daftar versi dataset terbaru berhasil diambil."
    )

@router.get("/shelters", response_model=SyncDataResponse)
def get_sync_shelters(db: Session = Depends(get_db)):
    """
    Mengunduh seluruh data shelter beserta metadata (elevasi, kapasitas, akses masuk) dalam format GeoJSON standar.
    """
    latest_shelter = db.query(DataVersion).filter_by(dataset_name="shelters", is_active=True).order_by(DataVersion.id.desc()).first()
    if not latest_shelter:
        raise HTTPException(status_code=404, detail="No shelter data version available.")
        
    shelters = db.query(EvacuationPoint).all()
    
    features = []
    for s in shelters:
        # Konversi PostGIS ke GeoJSON string, lalu ke Dict
        geom_json_str = db.scalar(s.location.ST_AsGeoJSON())
        geom = json.loads(geom_json_str) if geom_json_str else None
        
        entrance_json_str = db.scalar(s.entrance_coord.ST_AsGeoJSON()) if s.entrance_coord is not None else None
        entrance_geom = json.loads(entrance_json_str) if entrance_json_str else None
        
        features.append({
            "type": "Feature",
            "geometry": geom,
            "properties": {
                "id": s.id,
                "external_id": s.external_id,
                "name": s.name,
                "capacity": s.capacity,
                "floors": s.floors,
                "elevation_m": s.elevation_m,
                "type": s.type.value if hasattr(s.type, 'value') else str(s.type),
                "operational_status": s.operational_status.value if hasattr(s.operational_status, 'value') else str(s.operational_status),
                "address": s.address,
                "source": s.source,
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
    """
    return {"message": "Network graph patch endpoint (Not implemented yet)"}
