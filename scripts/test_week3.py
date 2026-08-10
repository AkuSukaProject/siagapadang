import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_rate_limit():
    print("Testing Rate Limiting (25 requests rapidly)...")
    success = 0
    blocked = 0
    for i in range(25):
        # We need to hit an endpoint that requires X-Device-ID.
        res = client.post(
            "/api/v1/reports/obstruction",
            json={
                "latitude": -0.9, 
                "longitude": 100.3, 
                "dataset_version_id": 1,
                "edge_external_id": "test_edge",
                "description": "spam"
            },
            headers={"X-Device-ID": "SPAM-DEVICE"}
        )
        if res.status_code == 429:
            blocked += 1
        else:
            success += 1
            
    assert blocked > 0, "Rate limiting should block some requests"
    print(f"Success: {success}, Blocked: {blocked}")

def test_missing_device_id():
    res = client.post(
        "/api/v1/reports/obstruction",
        json={
            "latitude": -0.9, 
            "longitude": 100.3, 
            "dataset_version_id": 1,
            "edge_external_id": "test_edge"
        }
    )
    assert res.status_code == 400
    assert "X-Device-ID" in res.text
