# app/routes.py
from fastapi import APIRouter, HTTPException
from datetime import datetime, date
from app.models import GDDSimulationRequest, GDDSimulationResponse
from app.weather_service import WeatherService
from app.gdd_calculator_service import GDDCalculatorService

router = APIRouter()

weather_service = WeatherService()
gdd_calculator = GDDCalculatorService()

@router.get("/health")
def health():
    """Health check"""
    return {"status": "GDD API is running"}

@router.post("/simulate-day", response_model=GDDSimulationResponse)
def simulate_day(request: GDDSimulationRequest):
    try:
        # Parsear fecha
        current_date = datetime.strptime(request.currentDate, '%Y-%m-%d').date()
        
        # Obtener datos climáticos
        weather_data = weather_service.get_weather_for_date(
            request.latitude,
            request.longitude,
            current_date
        )
        
        if weather_data is None:
            raise HTTPException(status_code=400, detail="No weather data available for this date")
        
        # Calcular GDD del día
        daily_gdd = gdd_calculator.calculate_daily_gdd(weather_data, request.baseTemperature)
        
        # Actualizar GDD acumulado
        new_gdd = request.initialGDD + int(daily_gdd)
        
        # Calcular progreso
        progress_percentage = min((new_gdd / request.targetGDD) * 100, 100) if request.targetGDD > 0 else 0
        target_reached = new_gdd >= request.targetGDD
        
        # Mensaje personalizado
        if target_reached:
            message = "¡Objetivo de GDD alcanzado! Plagas pueden estar en desarrollo"
        else:
            remaining = request.targetGDD - new_gdd
            message = f"GDD: {new_gdd}/{request.targetGDD} ({remaining} GDD restantes)"

        # Retornar respuesta
        return GDDSimulationResponse(
            current_gdd=new_gdd,
            target_gdd=request.targetGDD,
            progress_percentage=progress_percentage,
            date=str(current_date),
            avg_temp=float(weather_data.average_temp),
            gdd_gained=float(daily_gdd),
            target_reached=target_reached,
            message=message
        )
        
    except ValueError as e:
        raise HTTPException(status_code=400, detail=f"Invalid data format: {str(e)}")
    except Exception as e:
        print(f"Error en simulate_day: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error: {str(e)}")