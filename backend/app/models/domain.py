from sqlalchemy import Column, Integer, BigInteger, String, Float, Boolean, DateTime, Enum, ForeignKey, UniqueConstraint, Index
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
    
    __table_args__ = (
        UniqueConstraint('dataset_name', 'version', name='uq_dataset_version'),
        Index('uq_active_data_version', 'dataset_name', postgresql_where=(is_active == True), unique=True)
    )

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

class StructuralCondition(str, enum.Enum):
    UNKNOWN = "UNKNOWN"
    LAYAK = "LAYAK"
    RUSAK_RINGAN = "RUSAK_RINGAN"
    RUSAK_SEDANG = "RUSAK_SEDANG"
    RUSAK_BERAT = "RUSAK_BERAT"

class OperationalStatus(str, enum.Enum):
    UNKNOWN = "UNKNOWN"
    OPEN = "OPEN"
    CLOSED = "CLOSED"
    LIMITED = "LIMITED"

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
    structural_condition = Column(Enum(StructuralCondition), default=StructuralCondition.UNKNOWN)
    operational_status = Column(Enum(OperationalStatus), default=OperationalStatus.UNKNOWN)
    
    status_source = Column(String, nullable=True)
    status_reason = Column(String, nullable=True)
    status_updated_at = Column(DateTime(timezone=True), nullable=True)
    
    address = Column(String, nullable=True)
    source = Column(String, nullable=True)
    
    location = Column(Geometry(geometry_type='POINT', srid=4326))
    entrance_coord = Column(Geometry(geometry_type='POINT', srid=4326))
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    
    checkins = relationship("Checkin", back_populates="evacuation_point")

class EventStatus(str, enum.Enum):
    DRAFT = "DRAFT"
    ACTIVE = "ACTIVE"
    CLOSED = "CLOSED"
    CANCELLED = "CANCELLED"

class EmergencyEvent(Base):
    """Kejadian Darurat / Gempa Bumi"""
    __tablename__ = "emergency_events"
    
    id = Column(Integer, primary_key=True, index=True)
    external_event_id = Column(String, unique=True, nullable=True, index=True)
    source = Column(String, nullable=False) # e.g. "BMKG"
    status = Column(Enum(EventStatus), default=EventStatus.DRAFT)
    started_at = Column(DateTime(timezone=True), server_default=func.now())
    ended_at = Column(DateTime(timezone=True), nullable=True)
    
    __table_args__ = (
        Index('uq_single_active_tsunami_event', 'status', postgresql_where=(status == 'ACTIVE'), unique=True),
    )
    
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
    CONFIRMED = "CONFIRMED"
    REJECTED = "REJECTED"
    EXPIRED = "EXPIRED"

class Obstruction(Base):
    """Data Jalan Terhalang yang sudah digregasi"""
    __tablename__ = "obstructions"
    
    id = Column(Integer, primary_key=True, index=True)
    event_id = Column(Integer, ForeignKey("emergency_events.id"), nullable=False, index=True)
    dataset_version_id = Column(Integer, ForeignKey("data_versions.id"), nullable=False, index=True)
    edge_external_id = Column(String, nullable=False, index=True)
    
    status = Column(Enum(ObstructionStatus), default=ObstructionStatus.PENDING)
    confirmed_at = Column(DateTime(timezone=True), nullable=True)
    expires_at = Column(DateTime(timezone=True), nullable=True)
    
    __table_args__ = (
        UniqueConstraint('event_id', 'dataset_version_id', 'edge_external_id', name='uq_event_dataset_edge'),
    )
    
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
    
    __table_args__ = (
        UniqueConstraint('obstruction_id', 'device_hash', name='uq_obstruction_device'),
    )
    
    obstruction = relationship("Obstruction", back_populates="reports")

class RouteEdge(Base):
    """Data Ruas Jalan dari Graf Android untuk Pengecekan Jarak"""
    __tablename__ = "route_edges"
    
    id = Column(Integer, primary_key=True, index=True)
    dataset_version_id = Column(Integer, ForeignKey("data_versions.id"), nullable=False, index=True)
    edge_external_id = Column(String, nullable=False, index=True)
    geometry = Column(Geometry(geometry_type='LINESTRING', srid=4326))
    
    __table_args__ = (
        UniqueConstraint('dataset_version_id', 'edge_external_id', name='uq_dataset_edge'),
    )
