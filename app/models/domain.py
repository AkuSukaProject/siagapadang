from sqlalchemy import Column, Integer, String, Float, Boolean, DateTime, Enum
from sqlalchemy.sql import func
from geoalchemy2 import Geometry
import enum
from app.database import Base

class DataVersion(Base):
    __tablename__ = "data_versions"
    
    id = Column(Integer, primary_key=True, index=True)
    dataset_name = Column(String, index=True)  # e.g., 'padang_barat_graph', 'shelters'
    version = Column(String)                   # e.g., 'v1.0.0'
    checksum = Column(String)                  # md5 or sha256 hash
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

class InundationZone(Base):
    """Zona Merah Tsunami (Area Bahaya Rendaman)"""
    __tablename__ = "inundation_zones"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=True) # e.g. "Zona Rendaman 0-10m"
    danger_level = Column(String) # "High", "Medium", "Low"
    geometry = Column(Geometry(geometry_type='POLYGON', srid=4326))

class SafeZone(Base):
    """Zona Biru Bebas Tsunami (Area Aman)"""
    __tablename__ = "safe_zones"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=True) # e.g. "Bukit Gado-Gado"
    geometry = Column(Geometry(geometry_type='POLYGON', srid=4326))

class ShelterStatus(str, enum.Enum):
    LAYAK = "Layak"
    RUSAK_RINGAN = "Rusak Ringan"
    RUSAK_BERAT = "Rusak Berat"
    TIDAK_LAYAK = "Tidak Layak"

class Shelter(Base):
    """Bangunan Shelter Evakuasi Tsunami (TES / TEA)"""
    __tablename__ = "shelters"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, index=True)
    capacity = Column(Integer, default=0)
    current_occupancy = Column(Integer, default=0)
    elevation_m = Column(Float) # Ketinggian lantai aman (sangat krusial untuk evakuasi vertikal)
    status = Column(Enum(ShelterStatus), default=ShelterStatus.LAYAK)
    
    # Lokasi fisik bangunan (Tengah bangunan)
    location = Column(Geometry(geometry_type='POINT', srid=4326))
    
    # Koordinat akses masuk utama (pintu/gerbang) -> penting untuk akurasi algoritma Dijkstra
    entrance_coord = Column(Geometry(geometry_type='POINT', srid=4326))

class ObstructionReport(Base):
    """Laporan Jalan Terhalang dari Pengguna/Petugas"""
    __tablename__ = "obstruction_reports"
    
    id = Column(Integer, primary_key=True, index=True)
    device_id = Column(String, index=True) # Mencegah spam dari device yang sama
    location = Column(Geometry(geometry_type='POINT', srid=4326))
    description = Column(String, nullable=True)
    is_verified = Column(Boolean, default=False) # Laporan dari petugas BPBD = True
    reported_at = Column(DateTime(timezone=True), server_default=func.now())
