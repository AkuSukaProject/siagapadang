from sqlalchemy import create_engine, text
from app.database import SQLALCHEMY_DATABASE_URL

engine = create_engine(SQLALCHEMY_DATABASE_URL)
with engine.connect() as conn:
    try:
        conn.execute(text("ALTER TABLE obstruction_reports ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;"))
        conn.commit()
        print("Schema updated successfully.")
    except Exception as e:
        print(f"Error (maybe column already exists): {e}")
