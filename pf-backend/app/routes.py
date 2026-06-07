# app/routes.py
from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from database import get_db
from datetime import datetime, date
from app.models import GDDSimulationRequest, GDDSimulationResponse
from app.weather_service import WeatherService
from app.gdd_calculator_service import GDDCalculatorService
from app.models import Plantacion
from app.models import HistorialRiesgo
from app.models import Plaga
from app.models import MonitoreoResponse
from app.models import PlantacionResponse

router = APIRouter()

weather_service = WeatherService()
gdd_calculator = GDDCalculatorService()

@router.get("/health")
def health():
    """Health check"""
    return {"status": "GDD API is running"}

@router.post("/update-daily-gdd")
def update_daily_gdd(db: Session = Depends(get_db)):
    """Actualiza GDD de TODOS los monitoreos"""
    try:
        current_date = date.today() - timedelta(days=1)  # Ayer
        monitoreos_actualizados = []

        # Obtener todas los monitoreos
        monitoreos = db.query(HistorialRiesgo).all()
        
        for monitoreo in monitoreos:
            
            # Obtener plantación
            #plantacionActual = db.query(Plantacion).filter(
            #    monitoreo.plantacion_id == Plantacion.id
            #).all()

            # Obtener clima de plantación

            weather_data = weather_service.get_weather_for_date(
                monitoreo.plantacion.terreno.latitude,
                monitoreo.plantacion.terreno.longitude,
                current_date
            )

            # Obtener plaga
            #plagaActual = db.query(Plaga).filter(
            #    monitoreo.plaga_id == Plaga.id
            #).all()
            
            # Obtener GDD para plaga
            daily_gdd = gdd_calculator.calculate_daily_gdd(
                    weather_data, 
                    monitoreo.plaga.temp_base
            )

            # Actualizar GDD acumulado
            new_gdd = monitoreo.gdd_acumulado + int(daily_gdd)

            # Calcular progreso
            # progress = min((new_gdd / plagaActual.target_gdd) * 100, 100) if plague.target_gdd > 0 else 0
            # target_reached = new_gdd >= plague.target_gdd

            # Guardar en BD
            monitoreo.gdd_acumulado = new_gdd
            monitoreo.gdd_diario = daily_gdd
            # monitoreo.porc_riesgo_calculado = progress

        db.commit()

        
    
    except Exception as e:
        print(f"❌ Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/monitoreos")
def monitoreos_actualizados(db: Session = Depends(get_db)):

    response = []
         
    # Obtener todas las plantaciones
    plantaciones = db.query(Plantacion).all()

    # Obtener todos los monitoreos para esa plantacion
    for plantacionActual in plantaciones:
        
        monitoreosActuales = db.query(HistorialRiesgo).filter(
                plantacionActual.id == HistorialRiesgo.id
        ).all()

        plantacionResponse = PlantacionResponse(
            id = plantacionActual.id,
            terreno_nombre = plantacionActual.terreno.nombre,
            terreno_hectareas = plantacionActual.terreno.area_hectareas,
            terreno_latitud = plantacionActual.terreno.latitud,
            terreno_longitud = plantacionActual.terreno.longitud,
            cultivo_nombre = plantacionActual.cultivo.nombre,
            cultivo_nombre_cientifico = plantacionActual.cultivo.nombre_cientifico,
            fecha_siembra = plantacionActual.fecha_siembra
        )

        response.append(monitoreoResponse = MonitoreoResponse(
            plantacion = plantacionResponse,
            monitoreos = monitoreosActuales
        ))


    return response



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