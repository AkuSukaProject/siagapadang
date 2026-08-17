from fastapi import FastAPI, Depends
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text
from sqlalchemy.orm import Session
from app.database import engine, get_db
from app.api.endpoints import sync, reports, shelters, bmkg

app = FastAPI(
    title="API Evakuasi Tsunami Padang (Offline-First Backend)",
    description="Backend untuk sinkronisasi data prabencana dan simulasi bottleneck.",
    version="1.0.0"
)

# Tambahkan CORS Middleware untuk akses publik / Android / Frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Registrasi Router API Sinkronisasi
app.include_router(sync.router, prefix="/api/v1/sync", tags=["Synchronization"])

# Registrasi Router API Darurat & Keamanan
app.include_router(reports.router, prefix="/api/v1/reports", tags=["Reports"])
app.include_router(shelters.router, prefix="/api/v1/shelter", tags=["Shelters"])
app.include_router(bmkg.router, prefix="/api/v1/status/bmkg", tags=["BMKG"])


@app.get("/")
def read_root():
    return {"message": "Sistem Evakuasi Tsunami Padang API Aktif!"}

@app.get("/health/db")
def health_check_db(db: Session = Depends(get_db)):
    try:
        # Test if PostGIS is installed and running
        result = db.execute(text("SELECT PostGIS_Version();")).fetchone()
        return {"status": "Database & PostGIS Aktif!", "postgis_version": result[0]}
    except Exception as e:
        return {"status": "Error", "detail": str(e)}
