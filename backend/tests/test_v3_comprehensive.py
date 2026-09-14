import pytest
import httpx
import json
from unittest.mock import patch
from app.main import app
from app.api.endpoints import bmkg
from app.database import SessionLocal
from app.models.domain import EmergencyEvent, EventStatus, EvacuationPoint, EvacuationPointType, StructuralCondition, OperationalStatus, DataVersion, Obstruction, ObstructionReport, Checkin, ShelterOccupancyReport

@pytest.fixture
def anyio_backend():
    return 'asyncio'

@pytest.fixture(autouse=True)
def setup_db_fixtures():
    db = SessionLocal()
    # Clean up dependent tables first to avoid FK errors
    db.query(ShelterOccupancyReport).delete()
    db.query(ObstructionReport).delete()
    db.query(Obstruction).delete()
    db.query(Checkin).delete()
    db.query(EmergencyEvent).delete()
    db.query(EvacuationPoint).delete()
    db.query(DataVersion).delete()
    db.commit()

    # Seed 1 Active Event
    active_event = EmergencyEvent(
        external_event_id="EVENT-PADANG-TEST-01",
        source="BMKG_ADMIN",
        status=EventStatus.ACTIVE
    )
    db.add(active_event)

    # Seed 1 Evacuation Point (Polda Sumbar: -0.9511, 100.3538)
    point = EvacuationPoint(
        external_id="TES-PADANG-01",
        name="Polda Sumbar",
        capacity=3000,
        type=EvacuationPointType.TES,
        structural_condition=StructuralCondition.LAYAK,
        operational_status=OperationalStatus.OPEN,
        location="SRID=4326;POINT(100.3538 -0.9511)"
    )
    db.add(point)

    # Seed Data Version
    version = DataVersion(
        dataset_name="shelters",
        version="2026.08.01",
        schema_version="v3",
        checksum="e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        is_active=True
    )
    db.add(version)
    db.commit()
    db.close()
    yield

@pytest.mark.anyio
async def test_single_active_event_and_admin():
    """Memastikan event admin berjalan dan hanya 1 active event yang diperbolehkan"""
    db = SessionLocal()
    active_count = db.query(EmergencyEvent).filter(EmergencyEvent.status == EventStatus.ACTIVE).count()
    assert active_count == 1
    db.close()

@pytest.mark.anyio
async def test_checkin_required_external_ids():
    """Check-in wajib memuat event_external_id dan evacuation_point_external_id"""
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        res = await client.post(
            "/api/v1/shelter/checkin",
            json={
                "event_external_id": "EVENT-PADANG-TEST-01",
                "evacuation_point_external_id": "TES-PADANG-01",
                "latitude": -0.9511,
                "longitude": 100.3538,
                "accuracy_m": 10.0,
                "status": "Selamat"
            },
            headers={"X-Device-ID": "DEVICE-UUID-01"}
        )
        assert res.status_code == 200
        data = res.json()
        assert data["status"] == "success"
        assert data["evacuation_point_external_id"] == "TES-PADANG-01"


@pytest.mark.anyio
async def test_checkin_can_use_server_selected_active_event():
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        res = await client.post(
            "/api/v1/shelter/checkin",
            json={
                "evacuation_point_external_id": "TES-PADANG-01",
                "latitude": -0.9511,
                "longitude": 100.3538,
                "accuracy_m": 10.0,
            },
            headers={"X-Device-ID": "DEVICE-SERVER-EVENT"},
        )
    assert res.status_code == 200
    assert res.json()["event_external_id"] == "EVENT-PADANG-TEST-01"


@pytest.mark.anyio
async def test_active_event_status():
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        res = await client.get("/api/v1/status/emergency")
    assert res.status_code == 200
    assert res.json()["active"] is True
    assert res.json()["event_external_id"] == "EVENT-PADANG-TEST-01"

@pytest.mark.anyio
async def test_checkin_accuracy_max_35m():
    """Check-in menolak akurasi GPS > 35m"""
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        res = await client.post(
            "/api/v1/shelter/checkin",
            json={
                "event_external_id": "EVENT-PADANG-TEST-01",
                "evacuation_point_external_id": "TES-PADANG-01",
                "latitude": -0.9511,
                "longitude": 100.3538,
                "accuracy_m": 40.0,
                "status": "Selamat"
            },
            headers={"X-Device-ID": "DEVICE-UUID-01"}
        )
        assert res.status_code == 400
        assert "Akurasi GPS" in res.json()["detail"]

@pytest.mark.anyio
async def test_checkin_distance_formula_limit_55m():
    """Check-in menolak jarak > min(20 + accuracy_m, 55)"""
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        res = await client.post(
            "/api/v1/shelter/checkin",
            json={
                "event_external_id": "EVENT-PADANG-TEST-01",
                "evacuation_point_external_id": "TES-PADANG-01",
                "latitude": -0.8000,
                "longitude": 100.3538,
                "accuracy_m": 15.0,
                "status": "Selamat"
            },
            headers={"X-Device-ID": "DEVICE-UUID-01"}
        )
        assert res.status_code == 400
        assert "terlalu jauh" in res.json()["detail"]

@pytest.mark.anyio
async def test_obstruction_report_3_uuids_transition():
    """Laporan pertama PENDING. 3 UUID berbeda mengubah status menjadi CONFIRMED"""
    db = SessionLocal()
    version = db.query(DataVersion).first()
    version_id = version.id
    db.close()

    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        payload = {
            "latitude": -0.9,
            "longitude": 100.3,
            "dataset_version_id": version_id,
            "edge_external_id": "RUAS-OSM-1001",
            "description": "Pohon tumbang"
        }
        
        # 1st Report (UUID 1) -> PENDING (is_confirmed_blocked = False)
        res1 = await client.post("/api/v1/reports/obstruction", json=payload, headers={"X-Device-ID": "UUID-DEVICE-01"})
        assert res1.status_code == 200
        assert res1.json()["is_confirmed_blocked"] is False
        
        # 2nd Report (UUID 2) -> PENDING (is_confirmed_blocked = False)
        res2 = await client.post("/api/v1/reports/obstruction", json=payload, headers={"X-Device-ID": "UUID-DEVICE-02"})
        assert res2.status_code == 200
        assert res2.json()["is_confirmed_blocked"] is False
        
        # 3rd Report (UUID 3) -> CONFIRMED (is_confirmed_blocked = True)
        res3 = await client.post("/api/v1/reports/obstruction", json=payload, headers={"X-Device-ID": "UUID-DEVICE-03"})
        assert res3.status_code == 200
        assert res3.json()["is_confirmed_blocked"] is True

@pytest.mark.anyio
async def test_bmkg_status_uses_explicit_live_metadata():
    """Endpoint BMKG memberi label sumber dan kesegaran data secara eksplisit."""
    payload = {
        "Infogempa": {
            "gempa": {
                "Tanggal": "13 Sep 2026",
                "Jam": "10:00:00 WIB",
                "DateTime": "2026-09-13T03:00:00+00:00",
                "Coordinates": "-0.95,100.35",
                "Lintang": "0.95 LS",
                "Bujur": "100.35 BT",
                "Magnitude": "5.2",
                "Kedalaman": "10 km",
                "Wilayah": "Pesisir Barat Sumatera",
                "Potensi": "Tidak berpotensi tsunami",
                "Dirasakan": "II Padang",
            }
        }
    }

    class FakeResponse:
        def __enter__(self):
            return self

        def __exit__(self, *_args):
            return False

        def read(self):
            return json.dumps(payload).encode("utf-8")

    with patch("app.api.endpoints.bmkg.urllib.request.urlopen", return_value=FakeResponse()):
        async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
            res = await client.get("/api/v1/status/bmkg")

    assert res.status_code == 200
    data = res.json()
    assert data["is_tsunami_potential"] is False
    assert data["data_status"] == "live"
    assert data["fetched_at"]
    assert data["source"].startswith("BMKG")


@pytest.mark.anyio
async def test_occupancy_requires_checkin_and_returns_crowd_status():
    headers = {"X-Device-ID": "DEVICE-OCCUPANCY-01"}
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        denied = await client.post(
            "/api/v1/shelter/occupancy",
            json={"evacuation_point_external_id": "TES-PADANG-01", "level": "FULL"},
            headers=headers,
        )
        assert denied.status_code == 403

        checkin = await client.post(
            "/api/v1/shelter/checkin",
            json={
                "evacuation_point_external_id": "TES-PADANG-01",
                "latitude": -0.9511,
                "longitude": 100.3538,
                "accuracy_m": 10.0,
            },
            headers=headers,
        )
        assert checkin.status_code == 200

        submitted = await client.post(
            "/api/v1/shelter/occupancy",
            json={"evacuation_point_external_id": "TES-PADANG-01", "level": "FULL"},
            headers=headers,
        )
        assert submitted.status_code == 200
        assert submitted.json()["level"] == "FULL"

        status_response = await client.get("/api/v1/shelter/TES-PADANG-01/occupancy")
        assert status_response.status_code == 200
        assert status_response.json()["report_count"] == 1


@pytest.mark.anyio
async def test_bmkg_unavailable_is_not_reported_as_live_data():
    """Gangguan BMKG tanpa cache harus jujur menghasilkan status 503."""
    bmkg.bmkg_cache["data"] = None
    bmkg.bmkg_cache["last_fetched"] = 0.0
    with patch("app.api.endpoints.bmkg.urllib.request.urlopen", side_effect=TimeoutError()):
        async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
            res = await client.get("/api/v1/status/bmkg")

    assert res.status_code == 503
    assert "tidak tersedia" in res.json()["detail"]
