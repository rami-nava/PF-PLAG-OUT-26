from app.models import WeatherData

class GDDCalculatorService:
    """
    Calcula GDD: ((Tmax + Tmin) / 2) - Tbase
    Si es negativo, retorna 0
    """
    
    def calculate_daily_gdd(self, weather_data: WeatherData, base_temperature: float) -> float:
        """Calcula GDD para un día"""
        if weather_data is None:
            return 0.0
        
        avg_temp = weather_data.average_temp
        gdd = avg_temp - base_temperature
        
        return max(gdd, 0.0)