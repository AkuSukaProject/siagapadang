from fastapi import Request, HTTPException
import time
import os
import hmac
import hashlib
from collections import defaultdict

# In-memory store for rate limiting (For production, use Redis)
RATE_LIMIT_STORE = defaultdict(list)

# Rate limiting config
MAX_REQUESTS_PER_MINUTE = 20
HMAC_SECRET = os.getenv("HMAC_SECRET", "fallback-secret-for-dev").encode("utf-8")

def get_device_id(request: Request) -> str:
    """
    Dependency untuk memvalidasi keberadaan X-Device-ID di Header,
    menghasilkan device_hash (HMAC-SHA256) untuk privasi, dan memproteksi Rate Limiting.
    """
    device_id = request.headers.get("X-Device-ID")
    if not device_id:
        raise HTTPException(
            status_code=400, 
            detail="Header X-Device-ID wajib disertakan untuk alasan keamanan."
        )
        
    # Buat HMAC-SHA256 hash dari device_id (identitas anonim)
    device_hash = hmac.new(
        key=HMAC_SECRET,
        msg=device_id.encode("utf-8"),
        digestmod=hashlib.sha256
    ).hexdigest()
    
    # Rate Limiting Logic berdasarkan IP dan device_hash
    client_ip = request.client.host if request.client else "unknown"
    rate_limit_key = f"{client_ip}:{device_hash}"
    
    current_time = time.time()
    
    # Hapus request yang lebih lama dari 1 menit (60 detik)
    RATE_LIMIT_STORE[rate_limit_key] = [
        t for t in RATE_LIMIT_STORE[rate_limit_key] 
        if current_time - t < 60
    ]
    
    # Cek apakah melebihi limit
    if len(RATE_LIMIT_STORE[rate_limit_key]) >= MAX_REQUESTS_PER_MINUTE:
        raise HTTPException(
            status_code=429,
            detail="Too Many Requests. Harap tunggu sebelum mengirim permintaan baru."
        )
        
    # Catat request baru
    RATE_LIMIT_STORE[rate_limit_key].append(current_time)
    
    return device_hash
