from app.database import engine
from app.models import domain

print("Memastikan seluruh tabel (termasuk ObstructionReport) ada di PostGIS...")
domain.Base.metadata.create_all(bind=engine)
print("Berhasil!")
