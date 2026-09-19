import os
import sys
import pytest
from sqlalchemy import create_engine, text

# Gunakan URL Database Supabase Anda
DB_URL = "postgresql+psycopg://postgres.gvqsapapirtclvwpomzv:3wzP1EqhsChM6dAv@aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres"

def main():
    print("Menyiapkan skema khusus 'siaga_test' di Supabase agar data production aman...")
    
    # 1. Bikin skema khusus untuk testing agar tidak mengganggu tabel utama (public)
    engine = create_engine(DB_URL, isolation_level="AUTOCOMMIT")
    with engine.connect() as conn:
        conn.execute(text("CREATE SCHEMA IF NOT EXISTS siaga_test;"))
        # Pastikan ekstensi postgis tersedia di skema test
        conn.execute(text("CREATE EXTENSION IF NOT EXISTS postgis SCHEMA siaga_test;"))
    engine.dispose()

    # 2. Set environment variables untuk pytest
    # Tambahkan opsi search_path agar SQLAlchemy menggunakan skema 'siaga_test', 'public', dan 'extensions'
    test_db_url = f"{DB_URL}?options=-c%20search_path=siaga_test,public,extensions"
    
    os.environ["DATABASE_URL"] = test_db_url
    os.environ["HMAC_SECRET"] = "secret_rahasia_untuk_testing_123"
    os.environ["PYTHONPATH"] = "."

    print("Skema test siap! Menjalankan pytest...")
    
    # 3. Jalankan pytest pada folder tests
    sys.exit(pytest.main(["-v", "tests/test_v3_comprehensive.py", "tests/test_dataset_packages.py"]))

if __name__ == "__main__":
    main()
