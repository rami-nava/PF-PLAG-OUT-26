# Revisión de ciclos por biofix y cambio de cuenta

Base revisada: main c7f8f0e. La implementación existente ya permite varios ciclos por monitoreo. El diálogo obtiene los ciclos actuales, exige elegir asociación o creación adicional si hay ciclos activos y envía biofix-v1 con UUID y confirmación explícita. Valida la fecha entre el inicio del monitoreo y el día actual de Argentina.

El detalle muestra cada ciclo con su fecha de biofix, GDD y estado. El nivel agregado considera los activos; Room conserva los ciclos. Los avisos BIOFIX_CICLO y ALERTA_GDD_CICLO navegan por ID de ciclo, sin confundirlo con un monitoreo. Los reintentos automáticos conservan payload/UUID por propietario, usan red disponible y no eliminan una operación más nueva.

Se encontró una carrera en los envíos manuales: entre comprobar el propietario y agregar el token HTTP podía cambiar la cuenta. Una respuesta 404 bajo la cuenta nueva podía borrar el pendiente de la cuenta anterior. Biofix manual y feedback usan ahora el cliente ligado al propietario; una respuesta tardía de otra sesión no borra el pendiente ni actualiza la pantalla con el resultado anterior. La regresión conserva el feedback ante un 404 tardío.

El ajuste de FCM mantiene el conflicto 409 recuperable: renueva una vez, serializa registro y cierre de sesión, preserva el pendiente sin conexión y respeta notificaciones desactivadas. No transfiere tokens ajenos.

Validación: 195 pruebas de unidad/Robolectric, cero errores, fallos u omisiones. APK debug compilado. Incluye renovación FCM, segunda colisión, falta de conexión, cambio A→B, respuesta tardía, aislamiento del token HTTP, ciclos, fechas, migraciones Room y reintentos. Esto verifica las decisiones del cliente y su integración local; no constituye una prueba de entrega real de Firebase en un dispositivo físico.
