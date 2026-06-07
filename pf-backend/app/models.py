from pydantic import BaseModel
from datetime import date
from sqlalchemy import Column, Integer, String, Float, Boolean, DateTime, Date, ForeignKey, Table, Text
from sqlalchemy.orm import relationship, declarative_base
from datetime import datetime
from geoalchemy2 import Geometry

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




Base = declarative_base()

class Cargo(Base):
    __tablename__ = 'cargos'

    id = Column(Integer, primary_key=True, index=True)
    tipo_cargo = Column(String(100), nullable=False, unique=True)

    # Relationships
    usuarios = relationship("Usuario", back_populates="cargo")


class Usuario(Base):
    __tablename__ = 'usuarios'

    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String(100), nullable=False)
    apellido = Column(String(100), nullable=False)
    email = Column(String(150), unique=True, index=True, nullable=False)
    password_hash = Column(String(255), nullable=False)
    cargo_id = Column(Integer, ForeignKey('cargos.id'), nullable=False)
    creado_en = Column(DateTime, default=datetime.utcnow)
    umbral_riesgo = Column(Integer, default=50)  # User risk warning threshold (0-100)

    # Relationships
    cargo = relationship("Cargo", back_populates="usuarios")
    terrenos = relationship("Terreno", back_populates="usuario", cascade="all, delete-orphan")
    reportes = relationship("ReporteCampo", back_populates="usuario", cascade="all, delete-orphan")
    notificaciones = relationship("Notificacion", back_populates="usuario", cascade="all, delete-orphan")


class Notificacion(Base):
    __tablename__ = 'notificaciones'

    id = Column(Integer, primary_key=True, index=True)
    usuario_id = Column(Integer, ForeignKey('usuarios.id'), nullable=False)
    titulo = Column(String(200), nullable=False)
    mensaje = Column(Text, nullable=False)
    leido = Column(Boolean, default=False)
    tipo = Column(String(50), default="INFO")  # ALERTA_GDD, MONITOREO, INFO
    fecha_envio = Column(DateTime, default=datetime.utcnow)

    # Relationships
    usuario = relationship("Usuario", back_populates="notificaciones")


class Terreno(Base):
    __tablename__ = 'terrenos'

    id = Column(Integer, primary_key=True, index=True)
    usuario_id = Column(Integer, ForeignKey('usuarios.id'), nullable=False)
    nombre = Column(String(150), nullable=False)
    # Using Geography polygon for PostGIS support (SRID 4326)
    geom = Column(Geometry(geometry_type='POLYGON', srid=4326, spatial_index=True), nullable=True)
    area_hectareas = Column(Float, default=0.0)
    latitud = Column(Float, default=0.0)
    longitud = Column(Float, default=0.0)

    # Relationships
    usuario = relationship("Usuario", back_populates="terrenos")
    plantaciones = relationship("Plantacion", back_populates="terreno", cascade="all, delete-orphan")


class Cultivo(Base):
    __tablename__ = 'cultivos'

    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String(100), nullable=False, unique=True)
    nombre_cientifico = Column(String(150), nullable=True)

    # Relationships
    plantaciones = relationship("Plantacion", back_populates="cultivo")
    plagas = relationship("PlagaCultivo", back_populates="cultivo")


class Plaga(Base):
    __tablename__ = 'plagas'

    id = Column(Integer, primary_key=True, index=True)
    nombre_comun = Column(String(100), nullable=False, unique=True)
    nombre_cientifico = Column(String(150), nullable=False)
    temp_base = Column(Float, nullable=False)
    temp_max = Column(Float, nullable=False)
    gdd_generacion = Column(Float, nullable=False)

    # Relationships
    cultivos = relationship("PlagaCultivo", back_populates="plaga")
    alertas_historial = relationship("HistorialRiesgo", back_populates="plaga")
    reportes = relationship("ReporteCampo", back_populates="plaga")


class PlagaCultivo(Base):
    __tablename__ = 'plagas_cultivos'

    plaga_id = Column(Integer, ForeignKey('plagas.id'), primary_key=True)
    cultivo_id = Column(Integer, ForeignKey('cultivos.id'), primary_key=True)
    notas_susceptibilidad = Column(Text, nullable=True)

    # Relationships
    plaga = relationship("Plaga", back_populates="cultivos")
    cultivo = relationship("Cultivo", back_populates="plagas")


class Plantacion(Base):
    __tablename__ = 'plantaciones'

    id = Column(Integer, primary_key=True, index=True)
    terreno_id = Column(Integer, ForeignKey('terrenos.id'), nullable=False)
    cultivo_id = Column(Integer, ForeignKey('cultivos.id'), nullable=False)
    fecha_siembra = Column(Date, nullable=False)
    activa = Column(Boolean, default=True)

    # Relationships
    terreno = relationship("Terreno", back_populates="plantaciones")
    cultivo = relationship("Cultivo", back_populates="plantaciones")
    historial_riesgo = relationship("HistorialRiesgo", back_populates="plantacion", cascade="all, delete-orphan")
    reportes = relationship("ReporteCampo", back_populates="plantacion", cascade="all, delete-orphan")


class HistorialRiesgo(Base):
    __tablename__ = 'historial_riesgo'

    id = Column(Integer, primary_key=True, index=True)
    plantacion_id = Column(Integer, ForeignKey('plantaciones.id'), nullable=False)
    plaga_id = Column(Integer, ForeignKey('plagas.id'), nullable=False)
    fecha = Column(Date, nullable=False)
    gdd_diario = Column(Float, default=0.0)
    gdd_acumulado = Column(Float, default=0.0)
    estadio_biologico = Column(String(100), default="Huevo")
    porc_riesgo_calculado = Column(Float, default=0.0)
    nivel_alerta = Column(Integer, default=0)  # 0=Normal, 1=Precaucion, 2=Peligro

    # Relationships
    plantacion = relationship("Plantacion", back_populates="historial_riesgo")
    plaga = relationship("Plaga", back_populates="alertas_historial")


class ReporteCampo(Base):
    __tablename__ = 'reportes_campo'

    id = Column(Integer, primary_key=True, index=True)
    usuario_id = Column(Integer, ForeignKey('usuarios.id'), nullable=False)
    plantacion_id = Column(Integer, ForeignKey('plantaciones.id'), nullable=False)
    plaga_id = Column(Integer, ForeignKey('plagas.id'), nullable=False)
    fecha_observacion = Column(Date, nullable=False)
    nivel_infestacion = Column(String(50), nullable=False)  # Bajo, Medio, Alto
    notas_imagen_url = Column(Text, nullable=True)

    # Relationships
    usuario = relationship("Usuario", back_populates="reportes")
    plantacion = relationship("Plantacion", back_populates="reportes")
    plaga = relationship("Plaga", back_populates="reportes")


class MonitoreoResponse(BaseModel):
    plantacion: Plantacion
    monitoreos: list[HistorialRiesgo]

class PlantacionResponse(BaseModel):
    id: Integer
    terreno_nombre: String
    terreno_hectareas: Float
    terreno_latitud: Float
    terreno_longitud: Float
    cultivo_nombre: String
    cultivo_nombre_cientifico: String
    fecha_siembra: Date