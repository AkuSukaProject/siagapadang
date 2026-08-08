from sqlalchemy import Column, Integer, String, Float, Boolean, DateTime, Enum, ForeignKey
from sqlalchemy.orm import relationship
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
    
    evacuation_points = relationship("EvacuationPoint", back_populates="inundation_zone")

class SafeZone(Base):
    """Zona Biru Bebas Tsunami (Area Aman)"""
    __tablename__ = "safe_zones"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=True) # e.g. "Bukit Gado-Gado"
    geometry = Column(Geometry(geometry_type='POLYGON', srid=4326))
    
    evacuation_points = relationship("EvacuationPoint", back_populates="safe_zone")

class EvacuationPointType(str, enum.Enum):
    TES = "TES"
    TEA = "TEA"

class EvacuationPoint(Base):
    """Tempat Evakuasi Sementara (TES) / Akhir (TEA) Tsunami"""
    __tablename__ = "evacuation_points"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, index=True)
    capacity = Column(Integer, default=0)
    elevation_m = Column(Float) # Ketinggian lantai aman (sangat krusial untuk evakuasi vertikal)
    floors = Column(Integer) # Kriteria BPBD >4 lantai
    type = Column(Enum(EvacuationPointType), default=EvacuationPointType.TES)
    is_operational = Column(Boolean, default=True)
    address = Column(String, nullable=True)
    
    # Foreign Keys untuk standar ERD (mewakili relasi spasial yang didenormalisasi)
    inundation_zone_id = Column(Integer, ForeignKey("inundation_zones.id"), nullable=True)
    safe_zone_id = Column(Integer, ForeignKey("safe_zones.id"), nullable=True)
    
    # Lokasi fisik bangunan (Tengah bangunan)
    location = Column(Geometry(geometry_type='POINT', srid=4326))
    
    # Koordinat akses masuk utama (pintu/gerbang) -> penting untuk akurasi graf jalan
    entrance_coord = Column(Geometry(geometry_type='POINT', srid=4326))
    
    inundation_zone = relationship("InundationZone", back_populates="evacuation_points")
    safe_zone = relationship("SafeZone", back_populates="evacuation_points")
    checkins = relationship("Checkin", back_populates="evacuation_point")

class Checkin(Base):
    """Penanda keselamatan warga di TES/TEA"""
    __tablename__ = "checkins"
    
    id = Column(Integer, primary_key=True, index=True)
    device_id = Column(String, index=True)
    evacuation_point_id = Column(Integer, ForeignKey("evacuation_points.id"))
    status = Column(String, default="Selamat") # e.g., "Selamat", "Butuh Bantuan Medis"
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    evacuation_point = relationship("EvacuationPoint", back_populates="checkins")

class ObstructionReport(Base):
    """Laporan Jalan Terhalang dari Pengguna/Petugas"""
    __tablename__ = "obstruction_reports"
    
    id = Column(Integer, primary_key=True, index=True)
    device_id = Column(String, index=True) # Mencegah spam dari device yang sama
    location = Column(Geometry(geometry_type='POINT', srid=4326))
    edge_id = Column(String, nullable=True) # ID ruas jalan graf (misal OSM edge ID)
    description = Column(String, nullable=True)
    is_verified = Column(Boolean, default=False) # Laporan tervalidasi jika 3 pelapor radius 50m
    reported_at = Column(DateTime(timezone=True), server_default=func.now())
    expires_at = Column(DateTime(timezone=True)) # Kedaluwarsa 6 jam
