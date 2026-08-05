from pydantic import BaseModel
from typing import Optional

class BMKGStatusResponse(BaseModel):
    tanggal: str
    jam: str
    datetime: str
    coordinates: str
    lintang: str
    bujur: str
    magnitude: str
    kedalaman: str
    wilayah: str
    potensi: str
    dirasakan: str
    shakemap: str
    is_tsunami_potential: bool
