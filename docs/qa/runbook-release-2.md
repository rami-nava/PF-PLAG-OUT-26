# Runbook de validación — Release 2 (PF-PLAG-OUT-26 + plag-out)

## Context

Release 2 de la app Android (`codex/release2-cleanup-compatibility`, PRs #20/#21/#22) es
la *puerta de activación* del backend: los contratos
`docs/android/reports-v2-phase-4-feedback.md` y `docs/android/reports-v2-phase-6-risk-alerts.md`
(ambos viven en el repo **backend** `/Users/diegoh/Documents/GitHub/plag-out/docs/android/`,
**no** en el repo Android) declaran que las alertas ML automáticas, el modo `execute` del
worker y cualquier cron permanecen deshabilitados hasta que un build Android de producción
demuestre: detalle de predicción, separación umbral GDD vs ML, idempotencia preservada en
reintentos, las tres respuestas de feedback, estados vencido/ya-respondido, y un smoke
autenticado de dos usuarios que pruebe el aislamiento por ownership.

Este runbook existe para producir esa evidencia **una sola vez**, de forma controlada, sobre
producción, antes del merge. No propone cambios de arquitectura ni de código.

**Nada en este documento se ejecuta sin las autorizaciones humanas explícitas de los Gates A, B y C.**

---

## 0. Invariantes de seguridad (aplican a todos los checks)

| # | Invariante |
|---|---|
| I1 | El schedule permanece desactivado durante **todos** los checks. `.github/workflows/ml_risk_worker.yml` sólo tiene `on: workflow_dispatch`. Si aparece un bloque `schedule:` → **BLOCKED** inmediato y se aborta el runbook. |
| I2 | El worker en modo `execute` se ejecuta **exactamente una vez** en todo el runbook (CH-20). Una segunda ejecución invalida la corrida. |
| I3 | `config/ml_risk_worker.v2.yaml` → `approved_realert_increase_percentage_points: null`. No se modifica. Las re-alertas por incremento material siguen deshabilitadas. |
| I4 | Ningún secreto (JWT, `DATABASE_URL_ML_ALERTS`, `FCM_CREDENTIALS_JSON_B64`, `google-services.json`, contenido de `local.properties`) se copia en evidencia, capturas ni tickets. Las capturas de red se redactan antes de guardarse. |
| I5 | Las filas de confirmación y las predicciones son **evidencia inmutable** (el rol API no tiene grant de UPDATE/DELETE). La "limpieza" nunca es borrado de esas filas; ver §6. |
| I6 | Toda escritura productiva ocurre después del Gate A. Escrituras productivas = `PATCH consentimiento-modelo`, `PATCH /monitoreos/{id}`, `POST /reportes`, `POST /usuarios/fcm-token`, `POST /predicciones/{id}/confirmacion`. |
| I7 | Un check **BLOCKED** no se convierte en PASS por inferencia, por resultado de un unit test, ni por analogía con otro check. Se reporta BLOCKED con su causa. |

**Regla PASS/FAIL/BLOCKED global**
- **PASS**: el resultado esperado se observó completo en el entorno indicado y la evidencia quedó guardada.
- **FAIL**: se observó un resultado distinto al esperado.
- **BLOCKED**: no se pudo ejecutar (falta un `MISSING_INPUT`, falta autorización de un Gate, o depende de un check previo en FAIL/BLOCKED).

---

## 1. Registro de MISSING_INPUT

Ningún ítem de esta tabla puede ser inventado. Los checks que dependen de uno sin resolver quedan **BLOCKED**.

| ID | Dato faltante | Bloquea |
|---|---|---|
| MI-01 | Credenciales de los dos usuarios de prueba en producción: **U1** (owner) y **U2** (no-owner). Son cuentas reales de Supabase Auth — no existe seed, fixture ni management command que las cree. El backend ya usa este par en `scripts/security_acceptance.py` vía `SMOKE_TEST_EMAIL`/`SMOKE_TEST_PASSWORD` y `SMOKE_TEST_EMAIL_B`/`SMOKE_TEST_PASSWORD_B`; confirmar si se reutilizan esas mismas cuentas. | Todo |
| MI-02 | `backend.url` de producción para `pf-frontend/local.properties` (el archivo está gitignored; el default del build es `http://10.0.2.2:8000/`). El backend documenta `https://plag-out-backend.vercel.app` como base productiva por defecto en `scripts/security_acceptance.py`; **confirmar** que es la correcta antes de compilar. | Todo |
| MI-03 | `google-services.json` de producción para `pf-frontend/app/` (no está versionado; el build limpio falla sin él). El stub de CI es sólo para `ci-tests` y **no sirve** para FCM real. | CH-2x |
| MI-04 | Tres dispositivos/emuladores Android con Google Play Services, todos con sesión de **U1** (D1, D2, D3) y uno con **U2** (D4). Modelo/OS a registrar. | CH-2x, CH-06 |
| MI-05 | `plantacion_id` + `monitoreo_id` de U1, activo, con especie **soportada** por el manifiesto aprobado (el audit de producción del 2026-09-02 sólo soportó *Dalbulus*). | CH-04, CH-20 |
| MI-06 | `monitoreo_id` de U1 con especie **no soportada** por el modelo aprobado (para el 422 de umbral ML). | CH-05 |
| MI-07 | `prediccion_id` **histórica** de U1 sin solicitud de confirmación (para el 409 `not_requested`). | CH-31 |
| MI-08 | Aprobador humano y acceso al GitHub Environment protegido `production-ml-risk-worker`. | Gate B, CH-20 |
| MI-09 | Estado actual del secret `ML_RISK_EXECUTION_ENABLED` y quién puede ponerlo en `true` y volver a revertirlo. | Gate B, CH-20 |
| MI-10 | Credencial/canal **read-only** aprobado para verificar en base: `predicciones_ml`, solicitudes de confirmación, notificaciones in-app y outbox. Sin esto, la verificación se limita a lo observable por API/UI. | CH-2x, CH-3x |
| MI-11 | Decisión del owner del backend sobre el reporte canónico creado por `presente`: no existe endpoint de borrado de reportes. Las dos vías reales son (a) inscribir los `usuario_id` de U1/U2 en la denylist server-managed `private.first_party_ml_account_exclusions` (se puebla manualmente; no hay script en el repo), o (b) `DELETE /usuarios/me`, que anonimiza reportes y borra la identidad Auth. Definir cuál aplica. | §6 limpieza |
| MI-12 | Ventana de mantenimiento acordada y responsable de rollback. | Gate A |

---

## 2. Preparación (read-only, sin autorización especial)

### P-01 — Verificar que el schedule está desactivado
- **Objetivo**: probar I1 antes de cualquier otra cosa.
- **Precondiciones**: checkout del backend.
- **Usuario/dispositivo**: operador, terminal.
- **Acción exacta**:
  ```
  cd /Users/diegoh/Documents/GitHub/plag-out
  grep -n "^on:" -A 20 .github/workflows/ml_risk_worker.yml
  grep -rn "schedule:" .github/workflows/ml_risk_worker.yml
  ```
- **Resultado esperado**: el primer comando muestra únicamente `workflow_dispatch` con los inputs `mode`, `processing_date`, `execution_confirmation`. El segundo no devuelve nada.
- **Evidencia**: `evidencia/P-01-schedule.txt` (salida completa + `git rev-parse HEAD`).
- **Limpieza**: ninguna.
- **PASS/FAIL/BLOCKED**: PASS si no hay `schedule:`. FAIL si existe → abortar runbook.

### P-02 — Congelar los contratos vigentes
- **Objetivo**: registrar las políticas vigentes sin modificarlas.
- **Acción exacta**:
  ```
  cd /Users/diegoh/Documents/GitHub/plag-out
  cat config/ml_risk_worker.v2.yaml
  cat config/ml_feedback.v1.yaml
  cat config/model_consent.v1.yaml
  ```
- **Resultado esperado** (valores que el runbook asume): `version: ml-risk-worker-v2`,
  `effective_interval_hours: 48`, `confirmation_window_hours: 48`,
  `approved_realert_increase_percentage_points: null`,
  `notification_type: ALERTA_ML_RIESGO`, `prediction_target: severe_outbreak_risk`,
  `execution_confirmation: RUN_ML_RISK_AUDIT`.
  De `ml_feedback.v1.yaml`: `version: ml-feedback-contract-v1`, `confirmation_window_hours: 48`,
  `presence_response: presente`, `allowed_responses: [presente, no_observada, no_verificada]`,
  `prediction_confirmation_origin: prediction_confirmation`.
  De `model_consent.v1.yaml`: `contract_version: report-ml-consent-v1`, `default: false`.
- **Evidencia**: `evidencia/P-02-contracts/` (copias textuales).
- **Limpieza**: ninguna.
- **PASS/FAIL**: FAIL si algún valor difiere → el runbook debe re-derivarse antes de continuar.
- **Nota**: el YAML se valida contra el **registro privado de política en base** (`_validate_database_policy`). Si el worker responde `ml_risk_worker_policy_drift`, `ml_risk_worker_cell_contract_drift`, `ml_risk_worker_feedback_contract_drift` o `ml_risk_worker_confirmation_invalid` en CH-19/CH-20, es drift → **BLOCKED**, no se reintenta con otros valores.

### P-03 — Build de producción del APK
- **Objetivo**: tener el binario exacto que se somete a los gates.
- **Precondiciones**: MI-02, MI-03.
- **Acción exacta**:
  ```
  cd /Users/diegoh/Documents/GitHub/PF-PLAG-OUT-26
  git rev-parse HEAD          # debe ser la punta de codex/release2-cleanup-compatibility
  # colocar google-services.json en pf-frontend/app/ (MI-03)
  # colocar backend.url en pf-frontend/local.properties (MI-02)
  cd pf-frontend && ./gradlew testDebugUnitTest && ./gradlew assembleRelease
  ```
- **Resultado esperado**: unit tests verdes y APK release generado. `BUILD SUCCESSFUL`.
- **Evidencia**: `evidencia/P-03-build.txt` (commit SHA, salida de gradle, sha256 del APK). **No** incluir `local.properties`.
- **Limpieza**: al cierre, borrar `google-services.json` y `local.properties` del working tree (I4).
- **PASS/FAIL/BLOCKED**: BLOCKED si falta MI-02 o MI-03.
- **Nota**: `pf-frontend/app/build.gradle.kts` no define `productFlavors`; la URL de backend viene sólo de `local.properties` → `BuildConfig.BACKEND_URL`. Verificar que el APK apunta a producción antes de instalar.

### P-04 — Instalar y capturar estado inicial
- **Precondiciones**: P-03 PASS, MI-04.
- **Acción exacta**: instalar el mismo APK en D1, D2, D3 (sesión U1) y D4 (sesión U2). Registrar en cada uno: modelo, versión de Android, `versionName` de la app.
- **Resultado esperado**: login exitoso en los cuatro. En Perfil, "Consentimiento ML" aparece **desactivado** (default off).
- **Evidencia**: `evidencia/P-04-devices.md` + capturas de Perfil en D1 y D4.
- **Limpieza**: ver §6.
- **PASS/FAIL/BLOCKED**: BLOCKED sin MI-04.

---

## 🔒 GATE A — Autorización antes de cualquier escritura productiva

**No se ejecuta ningún check numerado CH-xx hasta que esto esté firmado.**

- **Qué se autoriza**: escrituras de la app contra producción bajo las cuentas U1/U2:
  `PATCH /api/v1/usuarios/me/consentimiento-modelo`, `PATCH /monitoreos/{id}`,
  `POST /reportes`, `POST /usuarios/fcm-token`, `POST /api/v1/predicciones/{id}/confirmacion`.
- **Qué NO autoriza**: ejecutar el worker ML (Gate B) ni habilitar cron alguno (Gate C).
- **Requisitos**: P-01 PASS, P-02 PASS, MI-01, MI-05, MI-12 resueltos.
- **Efectos irreversibles declarados**:
  1. La respuesta `presente` **crea o vincula un reporte canónico real** con origen `prediction_confirmation`.
  2. Las filas de consentimiento son historia append-only prospectiva: aceptar y revocar deja **dos** filas permanentes.
  3. Las respuestas de confirmación son inmutables y no se pueden corregir.
- **Registro**: nombre del aprobador, fecha/hora, alcance, ventana de mantenimiento → `evidencia/GATE-A.md`.
- **Si no está firmado**: todos los CH-xx quedan **BLOCKED**.

---

## 3. Bloque 1 — Checks sin alerta ML (post Gate A, pre Gate B)

### CH-01 — Reporte simple
- **Objetivo**: el flujo de reporte simple funciona tras la limpieza del PR #22 (se eliminó el fallback legacy `POST /api/reports` y los campos `terreno_id`/`latitud`/`longitud` del request).
- **Precondiciones**: Gate A firmado, P-04 PASS, MI-05.
- **Usuario/dispositivo**: U1 / D1.
- **Acción exacta**: en la app, crear un reporte sobre la plantación de MI-05: elegir plaga, nivel de severidad y etapa biológica; confirmar. Luego abrir "Mis reportes" y abrir el reporte creado.
- **Resultado esperado**: `POST /reportes` → 2xx. El reporte aparece en la lista y su detalle se abre. No hay ninguna llamada a `/api/reports`.
- **Evidencia**: capturas de creación + detalle; `reporte_id` resultante; nota de que no hubo fallback legacy → `evidencia/CH-01/`.
- **Limpieza**: registrar `reporte_id` en el inventario de datos de prueba (§6). No hay borrado disponible (MI-11).
- **PASS/FAIL/BLOCKED**: FAIL si el POST falla, si el reporte no aparece, o si se observa tráfico a `/api/reports`.

### CH-02 — Aceptación del consentimiento
- **Objetivo**: opt-in explícito con la versión vigente `report-ml-consent-v1`.
- **Precondiciones**: Gate A. Consentimiento en OFF (P-04).
- **Usuario/dispositivo**: U1 / D1.
- **Acción exacta**: Perfil → tarjeta "Consentimiento ML" → activar el switch → en el diálogo "¿Autorizar uso de reportes?" verificar que muestra `Versión: report-ml-consent-v1` y el texto de alcance prospectivo → "Aceptar".
- **Resultado esperado**: `PATCH .../consentimiento-modelo` con body `{"consentido": true, "version_contrato": "report-ml-consent-v1"}` → 2xx. La tarjeta pasa a "Aceptado · podés revocarlo cuando quieras". Sin error visible.
- **Evidencia**: captura del diálogo con la versión visible + captura del estado aceptado + `vigente_desde` devuelto → `evidencia/CH-02/`.
- **Limpieza**: se revoca en CH-03 (parte del propio flujo).
- **PASS/FAIL/BLOCKED**: FAIL si el diálogo no muestra la versión, si no envía `version_contrato`, o si responde 409 (mismatch de versión → la app quedó desalineada con el backend).

### CH-03 — Revocación del consentimiento
- **Objetivo**: la revocación está disponible, es prospectiva y no reescribe historial.
- **Precondiciones**: CH-02 PASS.
- **Usuario/dispositivo**: U1 / D1.
- **Acción exacta**: Perfil → desactivar el switch → diálogo "¿Revocar consentimiento?" → "Revocar". Luego cerrar y reabrir la app y volver a Perfil.
- **Resultado esperado**: `PATCH` con `consentido: false` → 2xx; la tarjeta vuelve a estado no aceptado y **persiste** tras reabrir. El reporte de CH-01, creado antes de la revocación, sigue existiendo.
- **Evidencia**: capturas antes/después + tras reinicio → `evidencia/CH-03/`.
- **Limpieza**: dejar el consentimiento en el estado acordado en Gate A (§6, CL-02).
- **PASS/FAIL/BLOCKED**: FAIL si la revocación no está disponible, no persiste, o altera datos previos.
- **Nota de alcance**: el consentimiento **no** es precondición de las predicciones ni de la confirmación — los endpoints de riesgo no lo evalúan. Su efecto es sobre la elegibilidad de los reportes como evidencia de modelo (`consentimiento_modelo_snapshot`). Por eso este check puede ejecutarse antes de CH-20 sin condicionarlo.

### CH-04 — Campos ML del monitoreo (GDD vs ML)
- **Objetivo**: gate de activación #1 — la UI distingue `umbral_riesgo` (GDD) de los umbrales ML.
- **Precondiciones**: Gate A, MI-05.
- **Usuario/dispositivo**: U1 / D1.
- **Acción exacta**: abrir el detalle del monitoreo de MI-05. Leer la tarjeta "Alerta predictiva (ML)". Tocar `btnInfoUmbralMl`. Tocar `btnEditarUmbralMl`, fijar un override **igual o mayor** al recomendado y ≤100%, guardar. Reabrir la pantalla. Luego volver a editar y usar la opción de volver al recomendado (envía `null`).
- **Resultado esperado**: la tarjeta ML muestra `umbral_alerta_ml_recomendado`, `umbral_alerta_ml_efectivo`, `modelo_alerta_ml_id` y `horizonte_alerta_ml_dias`, separados del umbral GDD. El guardado → `PATCH /monitoreos/{id}` con `umbral_alerta_ml` numérico → 2xx; el efectivo pasa a `max(override, recomendado)`. El reset envía `null` y el efectivo vuelve al recomendado.
- **Evidencia**: capturas de la tarjeta con ambos umbrales visibles, del override guardado y del reset → `evidencia/CH-04/`.
- **Limpieza**: dejar el override en `null` (recomendado) al finalizar el runbook (§6).
- **PASS/FAIL/BLOCKED**: FAIL si la UI mezcla GDD y ML, o si el efectivo no respeta `max(override, recomendado)`.

### CH-05 — Errores 422 observables desde la app
- **Objetivo**: cubrir el 422 del contrato de umbral ML.
- **Precondiciones**: CH-04 PASS. Casos (b) y (c) requieren MI-06.
- **Usuario/dispositivo**: U1 / D1.
- **Acción exacta**, tres sub-casos:
  - **CH-05a**: en el monitoreo de MI-05, intentar guardar un override **por debajo** del recomendado.
  - **CH-05b**: intentar un override **mayor a 100%**.
  - **CH-05c**: en el monitoreo de MI-06 (plaga no soportada), intentar fijar un override.
- **Resultado esperado**: en los tres, el backend responde **422** y la app muestra un error sin corromper el estado local; al reabrir la pantalla el valor previo sigue intacto. Detalles esperados: 05a → `ml_alert_threshold_below_model_recommendation`; 05c → `ml_alert_model_unavailable`; 05b → rechazo de validación (la columna tiene CHECK 0..100).
- **Evidencia**: captura del error por sub-caso + código HTTP observado → `evidencia/CH-05/`.
- **Limpieza**: ninguna (no hubo escritura).
- **PASS/FAIL/BLOCKED**: BLOCKED para 05b/05c sin MI-06. FAIL si alguno pasa a 2xx o si la UI queda con un valor que el backend no aceptó.
- **Nota**: el 422 `invalid_prediction_id` (id ≤ 0) **no es alcanzable desde la UI** y no se fuerza artificialmente; queda cubierto por CH-05 a nivel contrato.

### CH-06 — Aislamiento por ownership (404)
- **Objetivo**: gate de activación #5 — sólo el owner ve la predicción; ajeno y faltante devuelven ambos 404.
- **Precondiciones**: Gate A, MI-04 (D4 con U2). Un `prediccion_id` de U1 conocido — usar el de MI-07 si está disponible; si no, este check se **repite** tras CH-20 con la predicción alertada.
- **Usuario/dispositivo**: U2 / D4.
- **Acción exacta**: con sesión de U2, forzar la apertura de la ruta `prediccion/{id}` para un `prediccion_id` de **U1** (vía deep link de prueba local al `EXTRA_PREDICCION_ID` de `MainActivity`, o repitiendo CH-24 con el push recibido por U1 — nunca reenviando credenciales entre usuarios). Repetir con un `prediccion_id` inexistente.
- **Resultado esperado**: `GET /api/v1/predicciones/{id}` → **404** en ambos casos, indistinguibles entre sí. La app muestra el estado "no disponible" (`noDisponible = true`) y **no** cae a la pantalla de crear reporte. U2 no ve probabilidad, modelo, ni ningún dato de U1.
- **Evidencia**: captura del estado no-disponible en D4 en ambos casos + código HTTP → `evidencia/CH-06/`.
- **Limpieza**: ninguna.
- **PASS/FAIL/BLOCKED**: FAIL si U2 ve cualquier dato de la predicción, si el código difiere entre ajeno y faltante, o si hay fallback a crear reporte.
- **Herramienta ya existente para el lado servidor**: el backend tiene un smoke de dos cuentas autenticadas contra producción,
  `PYTHONPATH=. python scripts/security_acceptance.py --api-base-url <base> --supabase-url <url> --output <ruta.json>`,
  que consume `SMOKE_TEST_EMAIL[_B]`/`SMOKE_TEST_PASSWORD[_B]`. Usarlo para la evidencia HTTP del aislamiento
  en lugar de construir llamadas nuevas. Ojo: **crea alguna plantación de prueba** en su recorrido → inventariar en §6.
  El check de UI en D4 sigue siendo necesario: prueba que la app no filtra datos ni cae al flujo de reporte.

---

## 🔒 GATE B — Autorización antes de ejecutar el worker ML en modo escritura

**Requiere Gate A firmado y CH-01…CH-06 en PASS.**

- **Qué se autoriza**: **una única** ejecución de `ML Risk Worker Audit` con `mode = execute`.
- **Precondiciones verificables antes de firmar**:
  1. P-01 PASS (sin `schedule:`) y re-verificado en el commit que se va a despachar.
  2. Bloque 1 completo en PASS.
  3. MI-08 y MI-09 resueltos.
  4. Se ejecutó primero una corrida en `mode = audit` (read-only) el mismo día y su recibo muestra 0 escrituras. Ver CH-19.
- **Efectos irreversibles declarados**: la corrida `execute` persiste predicciones, crea solicitudes de confirmación con ventana de **48 h**, inserta notificaciones in-app `ALERTA_ML_RIESGO`, encola trabajos de outbox y **dispara push FCM reales** a los dispositivos registrados del owner.
- **Tres cerrojos que deben alinearse** (los tres son necesarios; ninguno se relaja):
  `--mode execute` + `--execution-confirmation` con el valor de la política + secret `ML_RISK_EXECUTION_ENABLED = true`.
- **Condición de reversión**: tras CH-20, `ML_RISK_EXECUTION_ENABLED` vuelve a su valor previo **en el mismo turno de trabajo** (§6, CL-05).
- **Registro**: aprobador, fecha/hora, `run_id` autorizado, ventana → `evidencia/GATE-B.md`.
- **Si no está firmado**: CH-20 y todo el Bloque 2 quedan **BLOCKED**.

---

## 4. Ejecución controlada del worker

### CH-19 — Corrida de auditoría previa (read-only)
- **Objetivo**: confirmar que hay candidatos alertables y que el worker no escribe en modo audit.
- **Precondiciones**: P-01/P-02 PASS. No requiere Gate B (es read-only), pero es **requisito** para firmarlo.
- **Usuario/dispositivo**: operador con acceso a Actions.
- **Acción exacta**: GitHub Actions → workflow **ML Risk Worker Audit** → *Run workflow* con
  `mode = audit`, `processing_date` vacío, `execution_confirmation` vacío.
- **Resultado esperado**: el job termina OK. El recibo `ml-risk-worker-receipt-<run_id>` indica
  `mode: audit`, monitoreos considerados/soportados > 0, y **cero** predicciones, runs,
  notificaciones, solicitudes y envíos FCM. El paso de grants (`loaders.grant_audit --role plagout_ml_alerts`) pasa.
- **Evidencia**: `run_id`, recibo JSON descargado, Actions summary → `evidencia/CH-19/`.
- **Limpieza**: ninguna (no hubo escritura).
- **PASS/FAIL/BLOCKED**: FAIL si el recibo reporta cualquier escritura, o si 0 monitoreos soportados (entonces CH-20 no produciría alertas → replanificar MI-05).

### CH-20 — Ejecución única del worker en modo escritura
- **Objetivo**: generar, una sola vez, el material de prueba real (predicciones + solicitudes + notificaciones + outbox + push).
- **Precondiciones**: **Gate B firmado**. CH-19 PASS. D1 **cerrada**, D2 en **background**, D3 en **foreground** en la pantalla de inicio, las tres con sesión de U1 y token FCM registrado (verificar en Ajustes que las notificaciones están activadas en los tres). D4 con U2, app cerrada. Cronómetro y grabación de pantalla listos en los tres dispositivos de U1.
- **Usuario/dispositivo**: operador (Actions) + D1/D2/D3/D4 en observación.
- **Acción exacta**: GitHub Actions → **ML Risk Worker Audit** → *Run workflow*:
  - `mode` = `execute`
  - `processing_date` = vacío (hoy, hora Argentina)
  - `execution_confirmation` = el valor de `execution_confirmation` de la política vigente (P-02)

  Aprobar el environment protegido `production-ml-risk-worker` cuando lo solicite. **Ejecutar una sola vez. No reintentar ante fallo** — un fallo va a CH-20 = FAIL y el runbook se detiene para análisis.
- **Resultado esperado**: job OK. Recibo con `mode: execute`, N predicciones persistidas, M solicitudes de confirmación creadas (M ≥ 1), notificaciones `ALERTA_ML_RIESGO` sólo para owners, y despacho de outbox con resultados `accepted`/`deferred`/`skipped`/`dead-letter` registrados. El recibo no contiene coordenadas, identidad de usuario ni nombre de campo. Los dispositivos de U1 reciben push; D4 (U2) **no** recibe nada.
- **Evidencia**: `run_id`, recibo JSON, Actions summary, grabaciones de pantalla de D1/D2/D3, y confirmación explícita de que D4 no recibió push → `evidencia/CH-20/`.
- **Limpieza**: revertir `ML_RISK_EXECUTION_ENABLED` (CL-05). Las predicciones y solicitudes generadas son evidencia inmutable y **no** se borran.
- **PASS/FAIL/BLOCKED**: BLOCKED sin Gate B. FAIL si el job falla, si `M = 0` (no hay alertas para probar el Bloque 2 → todo el Bloque 2 queda BLOCKED), o si D4 recibe cualquier notificación.
- **Asignación de predicciones**: de las M solicitudes creadas, designar y anotar:
  - **P1** = la predicción que se responderá (CH-25/CH-26/CH-30).
  - **P2** = una predicción que se deja **sin responder** hasta que expire (CH-32, +48 h).
  - **P3** (si M ≥ 3) = segunda respuesta, para cubrir un tercer valor de feedback.
  Si M = 1, CH-32 queda **BLOCKED** y las tres respuestas se cubren parcialmente (ver CH-26).

---

## 5. Bloque 2 — FCM, deep link, feedback e idempotencia

### CH-21 — FCM con la app en foreground
- **Objetivo**: recepción y presentación del push `ALERTA_ML_RIESGO` con la app abierta.
- **Precondiciones**: CH-20 PASS.
- **Usuario/dispositivo**: U1 / **D3** (foreground).
- **Acción exacta**: observar D3 durante y después de CH-20 (grabación de pantalla).
- **Resultado esperado**: el push llega; se presenta la notificación en el canal `alertas_gdd` ("Alertas y avisos", importancia alta) con título/cuerpo. La app no crashea ni navega sola.
- **Evidencia**: grabación + captura de la notificación, hora de llegada → `evidencia/CH-21/`.
- **Limpieza**: descartar la notificación.
- **PASS/FAIL/BLOCKED**: BLOCKED si CH-20 FAIL o si D3 no estaba disponible (MI-04). FAIL si no llega o no se presenta.

### CH-22 — FCM con la app en background
- **Usuario/dispositivo**: U1 / **D2** (app abierta pero en segundo plano).
- **Acción exacta**: idéntica observación durante CH-20.
- **Resultado esperado**: notificación en la bandeja del sistema, canal `alertas_gdd`.
- **Evidencia**: `evidencia/CH-22/` (grabación + captura de bandeja).
- **Limpieza / PASS-FAIL**: igual que CH-21.

### CH-23 — FCM con la app cerrada
- **Usuario/dispositivo**: U1 / **D1** (app terminada, no en recientes).
- **Acción exacta**: idéntica observación durante CH-20.
- **Resultado esperado**: notificación en la bandeja aun con el proceso terminado.
- **Evidencia**: `evidencia/CH-23/`.
- **Limpieza / PASS-FAIL**: igual que CH-21. Este es el estado más estricto: si D1 no recibe, es FAIL aunque D2/D3 pasen.
- **Nota**: los tres estados se cubren en una única corrida porque el outbox encola **un job por dispositivo vigente** del owner. Si sólo hay 1 o 2 dispositivos disponibles, los estados no cubiertos quedan **BLOCKED** — no se fuerza una segunda corrida del worker (I2).

### CH-24 — Deep link por `prediccion_id`
- **Objetivo**: gate de activación #2 — el evento ML abre la predicción correcta y nunca la pantalla de crear reporte.
- **Precondiciones**: CH-21/22/23 con al menos uno en PASS.
- **Usuario/dispositivo**: U1 / D1 (app cerrada, caso más exigente) y repetir en D2.
- **Acción exacta**: tocar la notificación `ALERTA_ML_RIESGO`.
- **Resultado esperado**: la app abre y navega a `prediccion/{prediccion_id}` (`MainActivity.leerDeepLink` → `EXTRA_PREDICCION_ID`), tras autenticar la sesión. Se muestra la pantalla de detalle (`testTag detallePrediccion`) de **esa** predicción. No hay paso por la pantalla de crear reporte.
- **Contrato del payload** (verificar en el recibo/logs, sin volcar el mensaje completo a evidencia): el `data` trae `tipo=ALERTA_ML_RIESGO`, `prediccion_id`, `entidad_id` (mismo id), `click_action=VER_PREDICCION`, `probabilidad_porcentaje`, `umbral_efectivo_porcentaje`, `model_id`, `target=severe_outbreak_risk`, `horizon_days`, `expira_en`. **No** debe traer coordenadas, identidad de usuario ni nombre de campo — si trae alguno, es FAIL de privacidad.
- **Observación conocida, no bloqueante**: el backend no usa deep link por URI/scheme; el destino se define por `click_action` + `prediccion_id`. El cliente Android navega por `prediccion_id` (extra de Intent) e **ignora** `click_action`. Cumple el contrato ("abrir la pantalla de detalle"). Registrarlo como observación, no como FAIL.
- **Evidencia**: grabación del tap → pantalla de detalle, con el `prediccion_id` visible/verificable → `evidencia/CH-24/`.
- **Limpieza**: ninguna.
- **PASS/FAIL/BLOCKED**: FAIL si abre otra pantalla, si abre otra predicción, o si cae al flujo de reporte.

### CH-25 — Detalle de predicción y campos ML
- **Objetivo**: gate de activación #1/#2 — renderizado correcto del detalle.
- **Precondiciones**: CH-24 PASS.
- **Usuario/dispositivo**: U1 / D1, predicción **P1**.
- **Acción exacta**: en la pantalla de detalle, leer todos los campos. Cerrar la app, reabrir por deep link y volver a leer.
- **Resultado esperado**: se muestran `plaga_nombre_cientifico`, `probabilidad_porcentaje`, `umbral_efectivo_porcentaje`, `horizon_days`, `model_id` y la vigencia (`expira_en`, formato `dd/MM/yyyy HH:mm`). El umbral efectivo mostrado **coincide con el de `GET /api/v1/predicciones/{id}`** y no con lo que trae el payload del push. El estado de confirmación es `pendiente` y se ofrecen exactamente tres botones: `Sí, está presente`, `Revisé y no la observé`, `No pude verificar`.
- **Evidencia**: captura del detalle completo + valor de `umbral_efectivo_porcentaje` del API para contraste → `evidencia/CH-25/`.
- **Limpieza**: ninguna.
- **PASS/FAIL/BLOCKED**: FAIL si falta un campo, si el umbral efectivo se reconstruye desde el push, si se sustituye por el recomendado cuando es `null`, o si hay más/menos de tres opciones.
- **Nota de contrato**: `umbral_efectivo_porcentaje` es nullable en predicciones históricas / no generadas por worker; en ese caso la UI debe mostrar "No disponible", **nunca** el recomendado.

### CH-26 — Las tres respuestas de feedback
- **Objetivo**: gate de activación #3 — enviar `presente`, `no_observada` y `no_verificada`.
- **Precondiciones**: CH-25 PASS. Cobertura completa requiere M ≥ 3 predicciones alertadas (CH-20).
- **Usuario/dispositivo**: U1 / D1.
- **Acción exacta**, en orden y sobre predicciones distintas:
  - **CH-26a** en P1: `Sí, está presente` → `presente`.
  - **CH-26b** en P3 (si existe): `Revisé y no la observé` → `no_observada`.
  - **CH-26c** en una tercera predicción (si existe): `No pude verificar` → `no_verificada`.
- **Resultado esperado**: cada `POST /api/v1/predicciones/{id}/confirmacion` con
  `{"respuesta": "<valor>", "idempotency_key": "<UUID v4>"}` → 2xx. El estado pasa a `respondida`.
  Sólo `presente` devuelve `reporte_id` / `reporte_creado` y crea o vincula un reporte canónico.
  `no_observada` y `no_verificada` **no** crean reporte y la UI **no** los presenta como ausencia verificada ni como error del modelo.
- **Evidencia**: por sub-caso, captura del estado `respondida`, `idempotency_key` usado (es un UUID, no un secreto), y respuesta del API → `evidencia/CH-26/`.
- **Limpieza**: registrar el `reporte_id` creado por `presente` en el inventario (§6, MI-11).
- **PASS/FAIL/BLOCKED**: BLOCKED por sub-caso si no hay predicciones suficientes; se reporta explícitamente cuáles quedaron sin cubrir. FAIL si `no_observada`/`no_verificada` crean un reporte o si el copy los presenta como ausencia verificada.

### CH-27 — Notificación in-app y consistencia con el push
- **Objetivo**: cobertura del canal in-app y del outbox.
- **Precondiciones**: CH-20 PASS.
- **Usuario/dispositivo**: U1 / D2; y U2 / D4 para el contraste.
- **Acción exacta**: en D2, abrir la campana de notificaciones in-app y tocar la entrada `ALERTA_ML_RIESGO`. En D4, abrir la campana de U2.
- **Resultado esperado**: en D2 existe **una** entrada `ALERTA_ML_RIESGO` por alerta, que navega a `prediccion/{entidad_id}` (mismo id que el push). En D4 **no** hay ninguna entrada `ALERTA_ML_RIESGO`. El recibo de CH-20 muestra un job de `private.notification_outbox` por dispositivo vigente del owner (o un resultado `no_device`), con resultados en `accepted | transient_failure | invalid_token | no_device | disabled`. La notificación in-app es única por `delivery_key = 'ml-risk:<prediccion_id>'` (índice único) → una sola entrada aunque el despacho se reintente.
- **Evidencia**: capturas de ambas campanas + extracto del recibo con los conteos de outbox → `evidencia/CH-27/`. Si MI-10 está resuelto, adjuntar la verificación read-only en base de notificaciones y outbox.
- **Limpieza**: ninguna.
- **PASS/FAIL/BLOCKED**: FAIL si U2 ve la notificación, si hay duplicados, o si el destino in-app difiere del destino del push.

### CH-30 — Retry y replay con exactamente el mismo UUID
- **Objetivo**: gate de activación #3 — idempotencia preservada entre reintentos.
- **Precondiciones**: CH-26a PASS sobre P1.
- **Usuario/dispositivo**: U1 / D1.
- **Acción exacta**, dos fases:
  - **CH-30a (retry offline→online)**: en una predicción pendiente aún no respondida (o repitiendo el patrón en P3 antes de CH-26b), poner el dispositivo en modo avión, tocar una respuesta, verificar que la app conserva el pendiente ("Podés reintentar sin duplicarla"), restaurar conectividad y tocar `btnReintentarPrediccion`.
  - **CH-30b (replay puro)**: sobre P1 ya respondida con `presente`, volver a enviar **la misma respuesta** con **el mismo `idempotency_key`** (la app lo reusa desde Room: `feedback_prediccion_pendiente`, PK `owner_id`+`prediccion_id`). Forzar el reintento sin borrar datos de la app.
- **Resultado esperado**:
  - 30a: el envío diferido usa el **mismo UUID** generado antes del primer intento (una sola `UUID.randomUUID()` persistida en Room), responde 2xx y no duplica nada.
  - 30b: el backend devuelve **el resultado original** (mismo `id`, mismo `respondido_en`, mismo `reporte_id`) y **no** crea un segundo reporte ni una segunda respuesta.
- **Evidencia**: captura del UUID persistido antes y después del reintento (mismo valor), ambas respuestas del API lado a lado, y conteo de reportes de U1 antes/después → `evidencia/CH-30/`.
- **Limpieza**: ninguna.
- **PASS/FAIL/BLOCKED**: FAIL si el UUID cambia entre intentos, si se crea un segundo reporte, o si el replay devuelve un resultado distinto al original.

### CH-31 — Errores 409
- **Objetivo**: cubrir las tres condiciones de 409 del contrato.
- **Precondiciones**: CH-26a PASS. CH-31c requiere MI-07.
- **Usuario/dispositivo**: U1 / D1 (y D2 para el caso concurrente).
- **Acción exacta**:
  - **CH-31a (respuesta cambiada)**: sobre P1 ya respondida, intentar enviar una respuesta **distinta** (con el mismo o con otro UUID).
  - **CH-31b (ya respondida desde otro dispositivo)**: en D2, con la pantalla de P1 cargada **antes** de CH-26a (estado `pendiente` en caché), tocar una respuesta.
  - **CH-31c (sin solicitud)**: abrir la predicción histórica de MI-07 (sin solicitud de confirmación) e intentar responder.
  - **CH-31d (UUID reusado)**: sobre una segunda predicción con solicitud pendiente, forzar el envío con **la misma `idempotency_key`** ya usada en P1. Requiere manipular el pendiente en Room o M ≥ 2 con acceso al valor persistido; si no es alcanzable desde la app, marcar **BLOCKED** con esa razón.
- **Resultado esperado**: **409** en todos. Detalles esperados: 31a/31b → `prediction_confirmation_already_answered`; 31c → `prediction_confirmation_not_requested`; 31d → `prediction_confirmation_idempotency_key_reused`. La app borra el pendiente local, re-sincroniza vía `cargar(prediccion_id)` y muestra el estado real (`respondida`) sin crear reporte adicional. En CH-31c el detalle debe mostrar estado `no_solicitada` y **no** ofrecer botones de respuesta (si los ofrece, el 409 debe manejarse igual sin fallback a crear reporte).
- **Evidencia**: código HTTP + `detail` + captura del estado resultante por sub-caso → `evidencia/CH-31/`.
- **Limpieza**: ninguna.
- **PASS/FAIL/BLOCKED**: BLOCKED en 31c sin MI-07; BLOCKED en 31d si no es alcanzable desde la app. FAIL si sobrescribe la respuesta original, si crea un reporte, o si cae al flujo de crear reporte.
- **Nota de orden**: el backend evalúa "ya respondida" **antes** que "expirada". Una solicitud respondida y luego vencida sigue devolviendo 409, no 410.
- **Existe un quinto 409** — `prediction_confirmation_lineage_mismatch`, cuando la solicitud no coincide con la predicción en plantación, plaga, modelo u horizonte. No es reproducible desde la app sin corromper datos: **no se intenta**. Se documenta como cubierto sólo por la suite del backend.

### CH-32 — Error 410 (solicitud vencida)
- **Objetivo**: gate de activación #4 — manejo de expiración a las 48 h.
- **Precondiciones**: CH-20 PASS con **M ≥ 2**, y **P2 dejada sin responder** (crítico: si P2 ya tuviera una respuesta, un replay exacto devolvería 200 aun vencida — el chequeo de expiración corre **después** del de replay; el 410 sólo aparece si no existe respuesta previa). Requiere esperar > `confirmation_window_hours` (48 h) desde `solicitada_en` de P2. No hay entorno de staging: Vercel Preview no tiene credenciales, así que esto sólo se puede observar en producción.
- **Usuario/dispositivo**: U1 / D1.
- **Acción exacta**: transcurridas más de 48 h desde CH-20, abrir el detalle de **P2** e intentar responder. Si la UI ya la muestra vencida y no ofrece botones, forzar el envío del pendiente encolado (dejar un pendiente en Room con la app en modo avión antes de que expire, y reintentar después de la expiración).
- **Resultado esperado**: **410**. La app borra el pendiente y fija el estado `vencida`, sin crear reporte y sin fallback a crear reporte.
- **Evidencia**: `solicitada_en`/`expira_en` de P2, timestamp del intento, código 410, captura del estado `vencida` → `evidencia/CH-32/`.
- **Limpieza**: ninguna.
- **PASS/FAIL/BLOCKED**: **BLOCKED** si M = 1, si no hay ventana de 48 h disponible antes del merge, o si el equipo decide no esperar. En ese caso reportar BLOCKED explícito — **no** sustituirlo por el unit test de `PrediccionDetalleViewModel` ni por una ejecución adicional del worker (I2).
- **Riesgo de calendario**: este es el único check que impone +48 h al cronograma. Si el merge no puede esperar, la decisión de mergear con CH-32 BLOCKED es una decisión humana a registrar junto al Gate C.

---

## 🔒 GATE C — Decisión humana separada para habilitar la ejecución cada 48 horas

**Es una decisión distinta y posterior. No se toma en la misma firma que A o B.**

- **Qué se decidiría**: agregar un disparador programado al workflow ML (hoy sólo `workflow_dispatch`), acorde al `effective_interval_hours: 48` de `ml-risk-worker-v2`, y dejar `ML_RISK_EXECUTION_ENABLED` habilitado de forma permanente.
- **Estado en este runbook**: **NO se habilita**. Durante todos los checks el schedule permanece desactivado (I1) y `ML_RISK_EXECUTION_ENABLED` se revierte tras CH-20 (CL-05).
- **Insumos para la decisión** (a presentar al aprobador, no a resolver aquí):
  1. Resultado consolidado de CH-01…CH-32, con los cinco gates de activación de la Fase 6 explícitamente mapeados (ver §7).
  2. Recibo de CH-20 con costos, llamadas a proveedor de clima y grupos incompletos.
  3. Situación de `approved_realert_increase_percentage_points: null` — la política de re-alerta por incremento material sigue sin historia suficiente y **no** se aprueba en este runbook.
  4. Lista de checks BLOCKED y su justificación.
- **Requisitos mínimos para siquiera considerarla**: CH-01…CH-31 en PASS, CH-32 en PASS o BLOCKED con aceptación firmada, y Release 2 mergeado y desplegado en producción.
- **Registro**: aprobador, fecha, decisión (habilitar / no habilitar / diferir), y condiciones → `evidencia/GATE-C.md`.
- **Fuera de alcance de este runbook**: el cambio de workflow y la revisión de seguridad que requeriría.

---

## 6. Limpieza y restauración de datos de prueba

Ejecutar en orden al cierre. Registrar cada ítem en `evidencia/CLEANUP.md`.

| ID | Acción | Verificación |
|---|---|---|
| CL-01 | Volver el `umbral_alerta_ml` de los monitoreos de MI-05/MI-06 a su valor original (enviar `null` para restaurar el recomendado si no había override previo). | Reabrir el detalle: el efectivo coincide con el recomendado. |
| CL-02 | Dejar el consentimiento ML de U1 en el estado acordado en Gate A (por defecto: **revocado**, igual al estado inicial). Nota: el historial append-only conserva ambas transiciones — esto es esperado, no un defecto. | Perfil muestra el estado acordado tras reiniciar la app. |
| CL-03 | Desregistrar tokens FCM de D1, D2, D3, D4 (`DELETE /usuarios/fcm-token/{fcm_token}` vía la opción de la app de desactivar notificaciones / cerrar sesión). | Ningún dispositivo de prueba queda como destino vigente. |
| CL-04 | Cerrar sesión y desinstalar el APK en los cuatro dispositivos (elimina la base Room y los pendientes de `feedback_prediccion_pendiente`). | App ausente. |
| CL-05 | **Revertir `ML_RISK_EXECUTION_ENABLED` a su valor previo** (MI-09) inmediatamente después de CH-20. | El secret vuelve a su estado pre-Gate-B; una corrida `execute` accidental sería rechazada. |
| CL-06 | Re-verificar I1: `.github/workflows/ml_risk_worker.yml` sigue sin `schedule:`. | Comando de P-01 sin resultados. |
| CL-07 | Borrar del working tree `pf-frontend/app/google-services.json` y `pf-frontend/local.properties`; confirmar que no fueron commiteados. | `git status` limpio. |
| CL-08 | Inventariar lo **no borrable**: predicciones de CH-20, solicitudes y respuestas de confirmación, filas de historial de consentimiento, reportes de CH-01 y CH-26a, y cualquier plantación creada por `scripts/security_acceptance.py` en CH-06. | Tabla con ids en `evidencia/CLEANUP.md` + nota de MI-11. |
| CL-09 | Pedir al owner del backend la inscripción de los `usuario_id` de U1/U2 en la denylist server-managed `private.first_party_ml_account_exclusions` (`usuario_id`, `reason`, `evidence_reference`), para que los datos de prueba no contaminen evidencia ni entrenamiento. **No se ejecuta desde este runbook**: no hay script ni migración que la pueble. | Confirmación escrita del owner. |
| CL-10 | Verificación read-only de que la exclusión tomó efecto: `PYTHONPATH=. python -m loaders.first_party_report_evidence --output <ruta.json>` y comprobar que los reportes de prueba aparecen excluidos. | El JSON lista los reportes con su `exclusion_reason`. |

**Advertencia explícita**: los ítems de CL-08 son evidencia inmutable por diseño (el rol API no
tiene grants de UPDATE/DELETE sobre confirmaciones). No existe ningún comando de purga de datos
de prueba en `scripts/`, `loaders/` ni `automation/`, y **no se debe improvisar uno**. Las dos
únicas vías reales son la denylist de CL-09 y `DELETE /usuarios/me` (anonimiza los reportes del
usuario y borra su identidad Auth) — esta última es destructiva e inutiliza la cuenta para
futuros runbooks, así que sólo se usa con decisión explícita del owner (MI-11).

---

## 7. Mapeo a los cinco gates de activación de la Fase 6

Tabla de cierre a incluir en el reporte final.

| Gate del contrato | Checks que lo prueban | Estado |
|---|---|---|
| 1. Distinguir umbrales GDD y ML en la UI de monitoreo | CH-04, CH-05 | |
| 2. Deep link del evento ML a la predicción correcta del owner | CH-24, CH-25 | |
| 3. Enviar las tres respuestas de forma idempotente | CH-26, CH-30 | |
| 4. Manejar solicitudes vencidas y ya respondidas | CH-31, CH-32 | |
| 5. Smoke autenticado de dos usuarios: sólo el owner ve la alerta | CH-06, CH-27 | |

Cobertura obligatoria del pedido: 1→CH-01 · 2→CH-02/CH-03 · 3→CH-04/CH-25 · 4→CH-26 ·
5→CH-30 · 6→CH-06/CH-27 · 7→CH-21/22/23 · 8→CH-24/CH-27 · 9→CH-05(422)/CH-06(404)/CH-31(409)/CH-32(410) · 10→§6.

---

## 8. Verificación del runbook

Antes de entregarlo a ejecución, confirmar que:
1. P-01 y P-02 devuelven exactamente lo declarado (si no, el runbook se re-deriva).
2. Los tres Gates tienen aprobador asignado y son personas distintas del ejecutor donde la política lo exija.
3. Los MISSING_INPUT que bloquean checks obligatorios están resueltos o el check está aceptado como BLOCKED por escrito.
4. Existe ventana de +48 h para CH-32, o su BLOCKED está aceptado.

---

# Handoff para Astra

**Autosuficiente. Leer completo antes de tocar nada.**

## Qué vas a hacer
Validar Release 2 de la app Android contra el backend de producción y producir la evidencia
que autoriza (o no) activar las alertas ML. Ejecutás los checks de este documento en orden.
No cambiás código, ni arquitectura, ni configuración fuera de lo que un check indica.

## Repos
- Android: `/Users/diegoh/Documents/GitHub/PF-PLAG-OUT-26`, rama `codex/release2-cleanup-compatibility` (PRs #20, #21, #22 abiertos).
- Backend: `/Users/diegoh/Documents/GitHub/plag-out`.
- Los dos contratos de referencia están en el **backend**: `docs/android/reports-v2-phase-4-feedback.md` y `docs/android/reports-v2-phase-6-risk-alerts.md`. Leelos antes de empezar.

## Las tres reglas que no se rompen
1. **El schedule queda desactivado todo el tiempo.** Si ves un `schedule:` en `.github/workflows/ml_risk_worker.yml`, parás y reportás.
2. **El worker en `mode: execute` corre UNA sola vez** (CH-20). Si falla, no reintentás: marcás FAIL y escalás.
3. **Nada se ejecuta sin la firma del Gate correspondiente.** Gate A antes de cualquier escritura, Gate B antes del worker en escritura, Gate C es una decisión posterior que vos no tomás.

## Orden de ejecución
```
P-01 → P-02 → P-03 → P-04
   ↓
[GATE A firmado]
   ↓
CH-01 → CH-02 → CH-03 → CH-04 → CH-05 → CH-06
   ↓
CH-19 (audit read-only)
   ↓
[GATE B firmado]  ← requiere CH-19 PASS y Bloque 1 en PASS
   ↓
CH-20 (execute, UNA vez, con D1 cerrada / D2 background / D3 foreground / D4 = U2)
   ↓  → CL-05 inmediatamente (revertir ML_RISK_EXECUTION_ENABLED)
CH-21 → CH-22 → CH-23 → CH-24 → CH-25 → CH-26 → CH-27 → CH-30 → CH-31
   ↓
CH-32 (+48 h desde CH-20, sobre P2 sin responder)
   ↓
Limpieza CL-01…CL-08 → tabla §7 → [GATE C: decisión humana, no tuya]
```

## Datos que necesitás pedir antes de arrancar
Están en la tabla §1 como MI-01…MI-12. Los críticos: credenciales de U1 y U2, `backend.url`
de producción, `google-services.json`, cuatro dispositivos, y el `monitoreo_id` de U1 con
plaga soportada. **No inventes ninguno.** Si falta uno, el check que depende de él es BLOCKED.

## Cómo reportar
Por cada check: ID, estado (PASS / FAIL / BLOCKED), evidencia guardada, y una línea de
observación. BLOCKED nunca se convierte en PASS por inferencia ni porque el unit test
correspondiente pase. Al final entregás la tabla de §7 completa y la lista de datos no
borrables de CL-08.

## Referencias exactas que vas a usar
- Workflow: `ML Risk Worker Audit` (`.github/workflows/ml_risk_worker.yml`), inputs `mode` / `processing_date` / `execution_confirmation`, environment protegido `production-ml-risk-worker`.
- Política: `config/ml_risk_worker.v2.yaml` — ventana de confirmación 48 h, tipo `ALERTA_ML_RIESGO`, re-alerta por incremento material en `null` (deshabilitada).
- Endpoints usados por la app: `GET|PATCH /api/v1/usuarios/me/consentimiento-modelo`, `PATCH /monitoreos/{id}`, `POST /reportes`, `GET /api/v1/predicciones/{id}`, `POST /api/v1/predicciones/{id}/confirmacion`, `POST /usuarios/fcm-token`, `DELETE /usuarios/fcm-token/{fcm_token}`.
- Idempotencia: campo del body `idempotency_key` (UUID), **no** un header — no existe `Idempotency-Key` en el backend. La app lo genera una vez y lo persiste en Room (`feedback_prediccion_pendiente`, PK `owner_id`+`prediccion_id`) antes de enviar.
- Consentimiento: versión `report-ml-consent-v1`, default `false`; ambos campos (`consentido`, `version_contrato`) son obligatorios; versión desconocida → 409 `model_consent_contract_version_mismatch`. **No** condiciona predicciones ni confirmaciones: afecta la elegibilidad de reportes como evidencia de modelo.
- Semántica de errores: 404 `prediction_not_found` = ajena o inexistente (indistinguibles) · 409 = `not_requested`, `already_answered`, `idempotency_key_reused`, `lineage_mismatch` · 410 `prediction_confirmation_expired` a las 48 h, evaluado **después** del replay (un replay exacto devuelve 200 aunque haya vencido) · 422 = `ml_alert_threshold_below_model_recommendation`, `ml_alert_model_unavailable`, `invalid_prediction_id`, o respuesta fuera de `allowed_responses`.
- Canal de notificación Android: `alertas_gdd`. Navegación por extras de Intent hacia `MainActivity` (`EXTRA_PREDICCION_ID`), ruta `prediccion/{prediccion_id}`. **No hay deep link por URI/scheme** en ninguno de los dos lados: el backend define el destino con `click_action=VER_PREDICCION` + `prediccion_id`, y Android navega por `prediccion_id`.
- Outbox: `private.notification_outbox`, despacho `dispatch_pending(workload="ml")`, resultados `accepted | transient_failure | invalid_token | no_device | disabled`; notificación in-app única por `delivery_key = 'ml-risk:<prediccion_id>'`.
- Herramienta de smoke de dos usuarios ya existente: `PYTHONPATH=. python scripts/security_acceptance.py --api-base-url <base> --supabase-url <url> --output <ruta.json>` con `SMOKE_TEST_EMAIL[_B]`/`SMOKE_TEST_PASSWORD[_B]`. Usala en lugar de armar llamadas nuevas.

## Lo que NO hacés
No habilitás el cron. No modificás `ml_risk_worker.v2.yaml` ni la política en base. No corrés el
worker una segunda vez. No borrás confirmaciones ni predicciones, ni improvisás un script de
purga (no existe ninguno). No corrés `DELETE /usuarios/me` sin decisión explícita del owner.
No pegás secretos en la evidencia. No proponés cambios de arquitectura: si encontrás un defecto,
lo reportás como FAIL con su evidencia.

## Advertencias de calendario
- CH-32 impone **+48 h** desde CH-20. Es el único check con esa restricción y no hay staging donde adelantarlo.
- El worker aplica su propio gate de intervalo: tras una corrida `execute` exitosa, `private.begin_ml_risk_cycle_v1` saltea los ciclos siguientes con `interval_not_elapsed` durante 48 h. Otra razón más por la que CH-20 es irrepetible dentro de este runbook.
