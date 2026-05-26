# app/weather_service.py
import requests
from datetime import date
from app.models import WeatherData

class WeatherService:
    OPEN_METEO_URL = "https://archive-api.open-meteo.com/v1/archive"
    
    def get_weather_for_date(self, latitude: float, longitude: float, current_date: date) -> WeatherData:
        """Obtiene datos climáticos para una fecha específica"""
        try:
            url = f"{self.OPEN_METEO_URL}?latitude={latitude:.4f}&longitude={longitude:.4f}&start_date={current_date}&end_date={current_date}&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
            
            print(f"[WEATHER] Consultando: {url}")
            
            response = requests.get(url, timeout=5)
            response.raise_for_status()
            
            data = response.json()
            print(f"[WEATHER] Response: {data}")
            
            if 'daily' not in data:
                print("[WEATHER] No 'daily' en response")
                return None
            
            daily = data['daily']
            
            temps_max = daily.get('temperature_2m_max')
            temps_min = daily.get('temperature_2m_min')
            
            print(f"[WEATHER] Tmax: {temps_max}, Tmin: {temps_min}")
            
            if not temps_max or not temps_min or len(temps_max) == 0 or len(temps_min) == 0:
                print("[WEATHER] Temperaturas vacías")
                return None
            
            temp_max_value = float(temps_max[0])
            temp_min_value = float(temps_min[0])
            
            print(f"[WEATHER] Tmax={temp_max_value}, Tmin={temp_min_value}")
            
            weather = WeatherData(
                date=current_date,
                temp_max=temp_max_value,
                temp_min=temp_min_value
            )
            
            print(f"[WEATHER] WeatherData creado: {weather}")
            return weather
            
        except requests.exceptions.Timeout:
            print("[WEATHER] Error: Timeout")
            return None
        except requests.exceptions.RequestException as e:
            print(f"[WEATHER] Error HTTP: {e}")
            return None
        except Exception as e:
            print(f"[WEATHER] Error general: {e}")
            return None