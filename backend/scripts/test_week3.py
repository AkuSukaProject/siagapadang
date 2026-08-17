import pytest
import httpx
from app.main import app

@pytest.fixture
def anyio_backend():
    return 'asyncio'

@pytest.mark.anyio
async def test_root():
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        res = await client.get("/")
        assert res.status_code == 200

@pytest.mark.anyio
async def test_rate_limit():
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        blocked = 0
        success = 0
        for i in range(25):
            res = await client.post(
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
        assert blocked > 0

@pytest.mark.anyio
async def test_missing_device_id():
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        res = await client.post(
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
