import sys
from pathlib import Path

backend_dir = Path(__file__).resolve().parent.parent
if str(backend_dir) not in sys.path:
    sys.path.insert(0, str(backend_dir))

import hashlib
from sqlalchemy import text
from app.database import engine

TABLES = [
    "data_versions",
    "inundation_zones",
    "safe_zones",
    "evacuation_points",
    "emergency_events",
    "checkins",
    "obstructions",
    "obstruction_reports",
    "route_edges"
]

def get_row_counts():
    counts = {}
    with engine.connect() as conn:
        for t in TABLES:
            try:
                res = conn.execute(text(f"SELECT COUNT(*) FROM {t}")).fetchone()
                counts[t] = res[0] if res else 0
            except Exception as e:
                counts[t] = f"Error: {e}"
    return counts

def generate_sql_backup(output_file="backup_dev.sql"):
    with engine.connect() as conn:
        with open(output_file, "w", encoding="utf-8") as f:
            f.write("-- SIAGA PADANG DEV DATABASE BACKUP DUMP --\n")
            for t in TABLES:
                f.write(f"\n-- Table: {t} --\n")
                try:
                    res = conn.execute(text(f"SELECT * FROM {t}")).fetchall()
                    f.write(f"-- Total rows: {len(res)} --\n")
                    for r in res:
                        f.write(f"{str(r)}\n")
                except Exception as e:
                    f.write(f"-- Table error: {e} --\n")
    
    with open(output_file, "rb") as f:
        sha256 = hashlib.sha256(f.read()).hexdigest()
        
    print(f"Backup created: {output_file}")
    print(f"Backup SHA-256: {sha256}")
    return sha256

if __name__ == "__main__":
    print("Auditing row counts...")
    counts = get_row_counts()
    for t, c in counts.items():
        print(f"  {t}: {c}")
    print("-" * 40)
    sha256 = generate_sql_backup()
