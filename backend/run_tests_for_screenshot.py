import os
import sys
import pytest

def main():
    # Set environment variables for testing
    os.environ["DATABASE_URL"] = "postgresql+psycopg://postgres.gvqsapapirtclvwpomzv:3wzP1EqhsChM6dAv@aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres"
    os.environ["HMAC_SECRET"] = "secret_rahasia_untuk_testing_123"
    os.environ["PYTHONPATH"] = "."

    print("=========================================================================")
    print("Menjalankan Unit Test ke Database Production Siaga Padang...")
    print("=========================================================================")
    
    # Jalankan pytest
    sys.exit(pytest.main(["-v", "tests/test_v3_comprehensive.py", "tests/test_dataset_packages.py"]))

if __name__ == "__main__":
    main()
