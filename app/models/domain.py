from sqlalchemy import Column, Integer, BigInteger, String, Float, Boolean, DateTime, Enum, ForeignKey, UniqueConstraint
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from geoalchemy2 import Geometry
import enum
from app.database import Base

class DataVersion(Base):
    __tablename__ = "data_versions"
    
    id = Column(Integer, primary_key=True, index=True)
    dataset_name = Column(String, index=True)
    version = Column(String)
    schema_version = Column(String, nullable=True)
    checksum = Column(String) # SHA-256
    download_url = Column(String, nullable=True)
    size_bytes = Column(BigInteger, nullable=True)
    minimum_app_version = Column(String, nullable=True)
    published_at = Column(DateTime(timezone=True), server_default=func.now())
    is_active = Column(Boolean, default=True)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    __table_args__ = (UniqueConstraint('dataset_name', 'version', name='uq_dataset_version'),)

class InundationZone(Base):
    """Zona Merah Tsunami (Area Bahaya Rendaman)"""
    __tablename__ = "inundation_zones"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=True)
    danger_level = Column(String)
    geometry = Column(Geometry(geometry_type='MULTIPOLYGON', srid=4326))

class SafeZone(Base):
    """Zona Biru Bebas Tsunami (Area Aman)"""
    __tablename__ = "safe_zones"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=True)
    geometry = Column(Geometry(geometry_type='MULTIPOLYGON', srid=4326))

class EvacuationPointType(str, enum.Enum):
    TES = "TES"
    TEA = "TEA"

class OperationalStatus(str, enum.Enum):
    LAYAK = "LAYAK"
    RUSAK_RINGAN = "RUSAK_RINGAN"
    RUSAK_BERAT = "RUSAK_BERAT"
    TIDAK_LAYAK = "TIDAK_LAYAK"

class EvacuationPoint(Base):
    """Tempat Evakuasi Sementara (TES) / Akhir (TEA) Tsunami"""
    __tablename__ = "evacuation_points"
    
    id = Column(BigInteger, primary_key=True, index=True)
    external_id = Column(String, unique=True, nullable=False, index=True)
    name = Column(String, nullable=False, index=True)
    capacity = Column(Integer, nullable=True)
    floors = Column(Integer, nullable=True)
    elevation_m = Column(Float, nullable=True)
    type = Column(Enum(EvacuationPointType), default=EvacuationPointType.TES)
    operational_status = Column(Enum(OperationalStatus), default=OperationalStatus.LAYAK)
    address = Column(String, nullable=True)
    source = Column(String, nullable=True)
    
    location = Column(Geometry(geometry_type='POINT', srid=4326))
    entrance_coord = Column(Geometry(geometry_type='POINT', srid=4326))
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    checkins = relationship("Checkin", back_populates="evacuation_point")

class EventStatus(str, enum.Enum):
    ACTIVE = "ACTIVE"
    RESOLVED = "RESOLVED"
    FALSE_ALARM = "FALSE_ALARM"

class EmergencyEvent(Base):
    """Kejadian Darurat / Gempa Bumi"""
    __tablename__ = "emergency_events"
    
    id = Column(Integer, primary_key=True, index=True)
    external_event_id = Column(String, unique=True, nullable=True, index=True)
    source = Column(String, nullable=False) # e.g. "BMKG"
    status = Column(Enum(EventStatus), default=EventStatus.ACTIVE)
    started_at = Column(DateTime(timezone=True), server_default=func.now())
    ended_at = Column(DateTime(timezone=True), nullable=True)
    
    checkins = relationship("Checkin", back_populates="event")
    obstructions = relationship("Obstruction", back_populates="event")

class Checkin(Base):
    """Penanda keselamatan warga di TES/TEA"""
    __tablename__ = "checkins"
    
    id = Column(Integer, primary_key=True, index=True)
    event_id = Column(Integer, ForeignKey("emergency_events.id"), nullable=False, index=True)
    evacuation_point_id = Column(BigInteger, ForeignKey("evacuation_points.id"), nullable=False)
    device_hash = Column(String, nullable=False, index=True)
    status = Column(String, default="Selamat")
    checked_in_at = Column(DateTime(timezone=True), server_default=func.now())
    
    __table_args__ = (UniqueConstraint('event_id', 'device_hash', name='uq_event_device'),)
    
    event = relationship("EmergencyEvent", back_populates="checkins")
    evacuation_point = relationship("EvacuationPoint", back_populates="checkins")

class ObstructionStatus(str, enum.Enum):
    PENDING = "PENDING"
    CROWD_CONFIRMED = "CROWD_CONFIRMED"
    OFFICIAL_CONFIRMED = "OFFICIAL_CONFIRMED"
    REJECTED = "REJECTED"
    EXPIRED = "EXPIRED"

class Obstruction(Base):
    """Data Jalan Terhalang yang sudah digregasi"""
    __tablename__ = "obstructions"
    
    id = Column(Integer, primary_key=True, index=True)
    event_id = Column(Integer, ForeignKey("emergency_events.id"), nullable=True, index=True)
    edge_id = Column(BigInteger, nullable=False, index=True)
    dataset_version = Column(String, nullable=False) # Version of the graph dataset
    status = Column(Enum(ObstructionStatus), default=ObstructionStatus.PENDING)
    confirmed_at = Column(DateTime(timezone=True), nullable=True)
    expires_at = Column(DateTime(timezone=True), nullable=True)
    
    event = relationship("EmergencyEvent", back_populates="obstructions")
    reports = relationship("ObstructionReport", back_populates="obstruction")

class ObstructionReport(Base):
    """Laporan Jalan Terhalang Mentah dari Pengguna"""
    __tablename__ = "obstruction_reports"
    
    id = Column(Integer, primary_key=True, index=True)
    obstruction_id = Column(Integer, ForeignKey("obstructions.id"), nullable=False, index=True)
    device_hash = Column(String, nullable=False, index=True)
    location = Column(Geometry(geometry_type='POINT', srid=4326))
    description = Column(String, nullable=True)
    reported_at = Column(DateTime(timezone=True), server_default=func.now())
    
    obstruction = relationship("Obstruction", back_populates="reports")

