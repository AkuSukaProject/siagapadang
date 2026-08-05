import requests
import time

BASE_URL = "http://localhost:8000/api/v1"
headers = {"X-Device-ID": "TEST-DEVICE-01"}

def test_bmkg():
    print("Testing BMKG API...")
    res = requests.get(f"{BASE_URL}/status/bmkg")
    print(res.status_code, res.json())
    print("-" * 30)

def test_obstruction():
    print("Testing Obstruction API (1st Report)...")
    payload = {"latitude": -0.9, "longitude": 100.3, "description": "Pohon tumbang"}
    res = requests.post(f"{BASE_URL}/reports/obstruction", json=payload, headers=headers)
    print("1st:", res.status_code, res.json())
    
    # 2nd report from different device
    print("Testing Obstruction API (2nd Report)...")
    res = requests.post(f"{BASE_URL}/reports/obstruction", json=payload, headers={"X-Device-ID": "TEST-DEVICE-02"})
    print("2nd:", res.status_code, res.json())
    
    # 3rd report from different device (Should trigger is_blocked=True)
    print("Testing Obstruction API (3rd Report - Should Block)...")
    res = requests.post(f"{BASE_URL}/reports/obstruction", json=payload, headers={"X-Device-ID": "TEST-DEVICE-03"})
    print("3rd:", res.status_code, res.json())
    print("-" * 30)

def test_rate_limit():
    print("Testing Rate Limiting (15 requests rapidly)...")
    success = 0
    blocked = 0
    for i in range(15):
        res = requests.post(
            f"{BASE_URL}/reports/obstruction", 
            json={"latitude": -0.9, "longitude": 100.3}, 
            headers={"X-Device-ID": "SPAM-DEVICE"}
        )
        if res.status_code == 200:
            success += 1
        elif res.status_code == 429:
            blocked += 1
    print(f"Success: {success}, Blocked (429): {blocked}")
    assert success <= 10, "Rate limiting failed!"
    print("-" * 30)

if __name__ == "__main__":
    try:
        # Check if server is running
        requests.get("http://localhost:8000/")
        test_bmkg()
        test_obstruction()
        test_rate_limit()
        print("All tests passed!")
    except requests.exceptions.ConnectionError:
        print("Server is not running on http://localhost:8000")
