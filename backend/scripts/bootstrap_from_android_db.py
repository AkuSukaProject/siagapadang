import argparse
import hashlib
import sqlite3
import sys
from pathlib import Path

from geoalchemy2.elements import WKTElement

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.database import SessionLocal
from app.models.domain import (
    DataVersion,
    EmergencyEvent,
    EvacuationPoint,
    EvacuationPointType,
    EventStatus,
    OperationalStatus,
    RouteEdge,
    StructuralCondition,
)


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def activate_version(db, dataset_name: str, version: str, checksum: str) -> DataVersion:
    db.query(DataVersion).filter(
        DataVersion.dataset_name == dataset_name,
        DataVersion.is_active.is_(True),
    ).update({DataVersion.is_active: False})
    db.flush()
    data_version = db.query(DataVersion).filter_by(
        dataset_name=dataset_name,
        version=version,
    ).first()
    if data_version is None:
        data_version = DataVersion(
            dataset_name=dataset_name,
            version=version,
            schema_version="android-v1",
            checksum=checksum,
            size_bytes=None,
            minimum_app_version="0.1.0",
            is_active=True,
        )
        db.add(data_version)
    else:
        data_version.schema_version = "android-v1"
        data_version.checksum = checksum
        data_version.minimum_app_version = "0.1.0"
        data_version.is_active = True
    db.flush()
    return data_version


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Samakan ID TES dan ruas PostGIS dengan database SQLite Android.",
    )
    parser.add_argument("database", type=Path, help="Lokasi ranah_siaga.db")
    parser.add_argument("--version", help="Label versi; default berasal dari checksum")
    parser.add_argument("--ensure-dev-event", action="store_true")
    args = parser.parse_args()

    sqlite_path = args.database.resolve()
    if not sqlite_path.is_file():
        raise SystemExit(f"Database Android tidak ditemukan: {sqlite_path}")
    checksum = file_sha256(sqlite_path)
    version_label = args.version or f"android-{checksum[:12]}"

    android_db = sqlite3.connect(sqlite_path)
    db = SessionLocal()
    try:
        shelter_version = activate_version(db, "shelters", version_label, checksum)
        network_version = activate_version(db, "network", version_label, checksum)

        shelters = android_db.execute(
            "SELECT tes_id, nama_tes, kapasitas, lat, lon FROM tb_tes",
        ).fetchall()
        for external_id, name, capacity, latitude, longitude in shelters:
            point = db.query(EvacuationPoint).filter_by(external_id=external_id).first()
            if point is None:
                point = EvacuationPoint(external_id=external_id, name=name)
                db.add(point)
            point.name = name
            point.capacity = int(capacity) if capacity is not None else None
            point.type = EvacuationPointType.TES
            point.structural_condition = StructuralCondition.UNKNOWN
            point.operational_status = OperationalStatus.UNKNOWN
            point.source = "Database offline Siaga Padang"
            point.location = WKTElement(f"POINT({longitude} {latitude})", srid=4326)
        db.flush()

        existing_edge_count = db.query(RouteEdge).filter_by(
            dataset_version_id=network_version.id,
        ).count()
        if existing_edge_count == 0:
            nodes = {
                node_id: (longitude, latitude)
                for node_id, longitude, latitude in android_db.execute(
                    "SELECT node_id, lon, lat FROM tb_nodes",
                )
            }
            mappings = []
            for edge_id, node_u, node_v, geometry in android_db.execute(
                "SELECT edge_id, u, v, geometry FROM tb_edges",
            ):
                edge_wkt = geometry
                if not edge_wkt:
                    from_coordinate = nodes.get(node_u)
                    to_coordinate = nodes.get(node_v)
                    if from_coordinate is None or to_coordinate is None:
                        continue
                    edge_wkt = (
                        f"LINESTRING({from_coordinate[0]} {from_coordinate[1]},"
                        f"{to_coordinate[0]} {to_coordinate[1]})"
                    )
                mappings.append(
                    {
                        "dataset_version_id": network_version.id,
                        "edge_external_id": str(edge_id),
                        "geometry": WKTElement(edge_wkt, srid=4326),
                    },
                )
            db.bulk_insert_mappings(RouteEdge, mappings)

        if args.ensure_dev_event:
            active_event = db.query(EmergencyEvent).filter_by(status=EventStatus.ACTIVE).first()
            if active_event is None:
                db.add(
                    EmergencyEvent(
                        external_event_id="EVENT-PADANG-DEV",
                        source="DEVELOPMENT",
                        status=EventStatus.ACTIVE,
                    ),
                )
        shelter_version.size_bytes = sqlite_path.stat().st_size
        network_version.size_bytes = sqlite_path.stat().st_size
        db.commit()
        print(
            f"Sinkron: {len(shelters)} TES dan dataset jaringan versi {version_label}.",
        )
    except Exception:
        db.rollback()
        raise
    finally:
        android_db.close()
        db.close()


if __name__ == "__main__":
    main()
