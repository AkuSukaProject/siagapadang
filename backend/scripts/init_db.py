import sys
import os

# Tambahkan direktori root proyek ke path agar bisa import app module
sys.path.append(os.path.dirname(os.path.dirname(__file__)))

from app.database import engine, Base
from app.models.domain import Shelter, InundationZone, SafeZone, DataVersion, ObstructionReport

print("Membangun tabel-tabel di database (Termasuk tabel spasial)...")
try:
    Base.metadata.create_all(bind=engine)
    print("Pembuatan tabel selesai!")
except Exception as e:
    print(f"Error: {e}")
