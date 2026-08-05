from fastapi import Request, HTTPException
import time
from collections import defaultdict

# In-memory store for rate limiting (For production, use Redis)
# Structure: { "device_id": [timestamp1, timestamp2, ...] }
RATE_LIMIT_STORE = defaultdict(list)

# Rate limiting config
MAX_REQUESTS_PER_MINUTE = 10

def get_device_id(request: Request):
    """
    Dependency untuk memvalidasi keberadaan X-Device-ID di Header.
    Sekaligus melakukan proteksi Rate Limiting.
    """
    device_id = request.headers.get("X-Device-ID")
    if not device_id:
        raise HTTPException(
            status_code=400, 
            detail="Header X-Device-ID wajib disertakan untuk alasan keamanan."
        )
        
    # Rate Limiting Logic
    current_time = time.time()
    
    # Hapus request yang lebih lama dari 1 menit (60 detik)
    RATE_LIMIT_STORE[device_id] = [
        t for t in RATE_LIMIT_STORE[device_id] 
        if current_time - t < 60
    ]
    
    # Cek apakah melebihi limit
    if len(RATE_LIMIT_STORE[device_id]) >= MAX_REQUESTS_PER_MINUTE:
        raise HTTPException(
            status_code=429,
            detail="Too Many Requests. Harap tunggu sebelum mengirim permintaan baru."
        )
        
    # Catat request baru
    RATE_LIMIT_STORE[device_id].append(current_time)
    
    return device_id
