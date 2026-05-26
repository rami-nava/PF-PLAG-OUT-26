from pydantic import BaseModel
from datetime import date

class WeatherData(BaseModel):
    date: date
    temp_max: float
    temp_min: float
    
    @property
    def average_temp(self) -> float:
        return (self.temp_max + self.temp_min) / 2.0

class GDDSimulationRequest(BaseModel):
    latitude: float
    longitude: float
    startDate: str 
    currentDate: str 
    initialGDD: int 
    targetGDD: int 
    baseTemperature: float 
    cropName: str 
    notes: str 
    

class GDDSimulationResponse(BaseModel):
    current_gdd: int
    target_gdd: int
    progress_percentage: float
    date: str
    avg_temp: float
    gdd_gained: float
    target_reached: bool
    message: str