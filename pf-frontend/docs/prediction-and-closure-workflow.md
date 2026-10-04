# Predicciones y finalización de cultivos

El cálculo ML sigue siendo automático. Esta pantalla consulta predicciones existentes y envía la observación de campo; no llama a `/api/v1/ml/predict` ni recalcula al reintentar una respuesta.

- Error de lectura: «Volver a cargar».
- Envío en curso: «Enviando…», con acciones deshabilitadas.
- Acuse completo que coincide con la predicción y respuesta: «Respuesta guardada». La presencia requiere también el resultado biofix.
- Conexión perdida, error reintentable o acuse incompleto: respuesta pendiente, aviso de que no se pudo confirmar el guardado y «Reintentar envío». Se reutilizan el UUID y la selección biofix originales.
- Fallo de Room: no se envía al backend. La selección permanece en memoria en esa pantalla; el reintento vuelve a guardar antes del envío. No se afirma que esa selección sobreviva al cierre del proceso hasta confirmar el almacenamiento local.
- Presencia conserva su reintento automático existente desde Room/WorkManager. Se comprueba también su acuse antes de retirar el borrador. Cambiar de cuenta no permite enviar el borrador de otro usuario.

Finalizar una plantación conserva el historial y es irreversible. El diálogo permite cancelar antes de confirmar y explica que se finalizarán los monitoreos y se archivarán sus ciclos activos. Los filtros muestran «Finalizados». Se refleja el cierre confirmado en memoria y Room sin presentar ciclos completados como archivados ni alterar otros cultivos.

Backend asociado: [PR149](https://github.com/diegohcanetti/plag-out/pull/149). Sus migraciones deben estar aplicadas y el backend desplegado para garantizar la cascada e irreversibilidad. Este PR no cambia los contratos de lectura ni de confirmación existentes.

## Reproducción local

Con Android SDK 36, JDK compatible con Gradle 8.14.5 y la configuración Firebase local de pruebas:

```bash
cd pf-frontend
./gradlew :app:testDebugUnitTest --no-daemon
./gradlew :app:lintDebug --no-daemon
```

Las pruebas JVM incluyen Compose/Robolectric y servicios simulados. No equivalen a una prueba E2E contra servicios desplegados. Se verifican acuses incompletos/incompatibles, reintento con el mismo UUID, guardado local fallido, aislamiento por cuenta y los mensajes de pantalla.

Lint tiene tres errores `NewApi` preexistentes en main: `FcmTokenRegistrar.kt` (líneas 44 y 48) y `PlagOutApplication.kt` (línea 29), por API 26 con `minSdk=24`. Los dos archivos coinciden con main. Los fallos se registran como deuda existente; no se agrega un baseline para ocultarlos.
