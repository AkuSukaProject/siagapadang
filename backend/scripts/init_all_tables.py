import sys
from pathlib import Path

# Memungkinkan skrip dijalankan langsung dari direktori backend.
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.database import engine
from app.models import domain

print("Memastikan seluruh tabel (termasuk ObstructionReport) ada di PostGIS...")
domain.Base.metadata.create_all(bind=engine)
print("Berhasil!")
