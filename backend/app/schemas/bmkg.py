from pydantic import BaseModel, Field
from typing import Optional

class BMKGStatusResponse(BaseModel):
    tanggal: str = ""
    jam: str = ""
    datetime: str = ""
    coordinates: str = ""
    lintang: str = ""
    bujur: str = ""
    magnitude: str = ""
    kedalaman: str = ""
    wilayah: str = ""
    potensi: str = ""
    dirasakan: Optional[str] = ""
    shakemap: Optional[str] = ""
    is_tsunami_potential: bool = False
    source: str = Field(
        default="BMKG (Badan Meteorologi, Klimatologi, dan Geofisika)",
        description="Atribusi sumber data resmi sesuai ketentuan lisensi BMKG"
    )
