import sys
import os

sys.path.append(os.path.dirname(os.path.dirname(__file__)))

from app.database import SessionLocal
from app.models.domain import Shelter, InundationZone, DataVersion, ShelterStatus
import hashlib

def hash_str(s):
    return hashlib.md5(s.encode()).hexdigest()

def seed_data():
    db = SessionLocal()
    try:
        # Cek jika data sudah ada
        if db.query(Shelter).count() > 0:
            print("Data mock sudah ada di database. Tidak perlu seeding ulang.")
            return

        print("Menambahkan mock data Inundation Zones (Zona Merah)...")
        # Poligon kotak sederhana merepresentasikan pantai Padang Barat
        polygon_wkt = "POLYGON((100.345 -0.920, 100.365 -0.920, 100.365 -0.950, 100.345 -0.950, 100.345 -0.920))"
        iz = InundationZone(
            name="Zona Merah Padang Barat (0-10m mdpl)",
            danger_level="High",
            geometry=f"SRID=4326;{polygon_wkt}"
        )
        db.add(iz)

        print("Menambahkan 5 mock data Shelters (TES)...")
        shelters_data = [
            {"name": "Masjid Raya Sumatera Barat", "cap": 5000, "elev": 15.5, "lon": 100.3601, "lat": -0.9234},
            {"name": "Gedung Polda Sumbar", "cap": 3000, "elev": 12.0, "lon": 100.3592, "lat": -0.9251},
            {"name": "Kantor Gubernur Sumbar", "cap": 2500, "elev": 14.2, "lon": 100.3615, "lat": -0.9248},
            {"name": "Hotel Ibis Padang", "cap": 1500, "elev": 20.0, "lon": 100.3621, "lat": -0.9195},
            {"name": "Pasar Raya Padang Blok III", "cap": 4000, "elev": 10.5, "lon": 100.3585, "lat": -0.9482}
        ]

        for s in shelters_data:
            # entrance coord kita buat sedikit bergeser dari titik tengah bangunan
            sh = Shelter(
                name=s["name"],
                capacity=s["cap"],
                elevation_m=s["elev"],
                status=ShelterStatus.LAYAK,
                location=f"SRID=4326;POINT({s['lon']} {s['lat']})",
                entrance_coord=f"SRID=4326;POINT({s['lon'] + 0.0001} {s['lat']})"
            )
            db.add(sh)

        print("Menambahkan data versioning...")
        v_shelter = DataVersion(dataset_name="shelters", version="v1.0.0", checksum=hash_str("shelter_v1"))
        v_inundation = DataVersion(dataset_name="inundation_zones", version="v1.0.0", checksum=hash_str("inund_v1"))
        db.add_all([v_shelter, v_inundation])

        db.commit()
        print("Mock data berhasil di-insert secara permanen ke PostGIS!")

    except Exception as e:
        db.rollback()
        print(f"Error seeding data: {e}")
    finally:
        db.close()

if __name__ == "__main__":
    seed_data()
