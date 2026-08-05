from pydantic import BaseModel

class CheckInResponse(BaseModel):
    status: str
    message: str
    shelter_id: int
    current_occupancy: int
