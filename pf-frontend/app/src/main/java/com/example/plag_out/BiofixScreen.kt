package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.plag_out.AlmacenamientoLocal.AppDatabase
import com.example.plag_out.AlmacenamientoLocal.BiofixPendiente
import com.example.plag_out.Service.RetrofitClient
import com.google.gson.Gson
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Composable
fun CicloCard(ciclo: GddCicloResponse) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 6.dp), tonalElevation = 2.dp) {
        Column(Modifier.padding(12.dp)) {
            Text("Ciclo ${ciclo.id} · Biofix ${ciclo.fecha_biofix}")
            Text("${ciclo.estado} · ${ciclo.gdd_acumulado} GDD · ${ciclo.progreso.toInt()}%")
            Text("Actualizado: ${ciclo.fecha_actualizacion}")
            Text(if (ciclo.dias_pendientes > 0) "Ciclo iniciado · Cálculo pendiente (${ciclo.dias_pendientes} días)" else "Cálculo actualizado")
        }
    }
}

/** Both entrypoints use the same selection UI; no mutation occurs before confirmation. */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BiofixDialog(monitoreoId: Int, onDismiss: () -> Unit, onConfirm: (BiofixRequest) -> Unit, service: com.example.plag_out.Service.GDDService = RetrofitClient.gddService) {
    var monitor by remember { mutableStateOf<MonitoreoResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var fecha by remember { mutableStateOf(LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")).toString()) }
    var opcion by remember { mutableStateOf<Int?>(null) } // -1 = explicitly create another
    LaunchedEffect(monitoreoId) {
        try {
            val response = service.getMonitoreo(monitoreoId)
            if (response.isSuccessful) monitor = response.body()
            else error = "Monitoreo no disponible (${response.code()})."
        } catch (_: Exception) { error = "No se pudo cargar. Revisá tu conexión y volvé a abrir." }
    }
    val activos = monitor?.ciclos.orEmpty().filter { it.estado == "activo" }
    val date = runCatching { LocalDate.parse(fecha) }.getOrNull()
    val valido = date != null && monitor?.fecha_inicio != null &&
        !date.isBefore(monitor!!.fecha_inicio) && !date.isAfter(LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")))
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Registrar presencia / biofix") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Tu observación será el inicio declarado del seguimiento GDD. No es una confirmación biológica del modelo.")
            OutlinedTextField(fecha, { fecha = it }, label = { Text("Fecha de observación (AAAA-MM-DD)") }, isError = !valido)
            if (monitor == null && error == null) CircularProgressIndicator()
            if (activos.isEmpty()) Text("Se iniciará un ciclo GDD desde esa fecha.")
            else {
                Text("¿Es la presencia que ya seguís o querés iniciar otro ciclo?")
                activos.forEach { ciclo ->
                    Row { RadioButton(opcion == ciclo.id, { opcion = ciclo.id }); Text("Ciclo ${ciclo.id} · ${ciclo.fecha_biofix}") }
                }
                Row { RadioButton(opcion == -1, { opcion = -1 }); Text("Iniciar otro ciclo en paralelo") }
            }
            error?.let { Text(it) }
        }
    }, confirmButton = {
        TextButton(enabled = monitor?.activo == true && valido && (activos.isEmpty() || opcion != null), onClick = {
            val asociar = activos.isNotEmpty() && opcion != -1
            onConfirm(BiofixRequest(UUID.randomUUID().toString(), fecha,
                if (asociar) "asociar_ciclo" else "iniciar_ciclo",
                if (asociar) opcion else null, activos.isNotEmpty() && !asociar))
        }) { Text("Confirmar presencia") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BiofixManual(monitoreo: MonitoreoResponse, onRefresh: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).biofixDao() }
    val owner = SupabaseProvider.client.auth.currentUserOrNull()?.id
    val scope = rememberCoroutineScope()
    var mostrar by remember { mutableStateOf(false) }
    var pendiente by remember { mutableStateOf<BiofixRequest?>(null) }
    var enviando by remember { mutableStateOf(false) }
    var pendienteCargado by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var resultado by remember { mutableStateOf<BiofixResult?>(null) }
    LaunchedEffect(owner, monitoreo.monitoreo_id) {
        if (owner != null) AppDatabase.getDatabase(context).presenceRetryDao().manual(owner).collect { rows ->
            pendienteCargado = true
            val row = rows.firstOrNull { it.monitoreo_id == monitoreo.monitoreo_id }
            pendiente = row?.takeIf { it.estado == "pendiente" }?.let { Gson().fromJson(it.payload, BiofixRequest::class.java) }
            if (row?.estado == "requiere_revision") mensaje = "Revisá la fecha y el ciclo antes de confirmar de nuevo."
        }
    }
    LaunchedEffect(owner, monitoreo.monitoreo_id, onRefresh) {
        PresenceRetryEvents.events.collect { event ->
            if (event.owner == owner && event.kind == "manual" && event.id == monitoreo.monitoreo_id) {
                resultado = event.result
                if (event.result != null) mensaje = "Presencia registrada automáticamente"
                onRefresh()
            }
        }
    }
    fun enviar(payload: BiofixRequest) {
        if (owner == null || enviando) return
        enviando = true
        scope.launch {
            try {
                val anterior = dao.get(owner, monitoreo.monitoreo_id)
                if (anterior?.estado == "requiere_revision") dao.delete(owner, monitoreo.monitoreo_id)
                val guardado = anterior?.takeIf { it.estado == "pendiente" }
                val exacto = guardado?.let { Gson().fromJson(it.payload, BiofixRequest::class.java) } ?: payload.also {
                    dao.insert(BiofixPendiente(owner, monitoreo.monitoreo_id, Gson().toJson(it)))
                }
                pendiente = exacto
                if (SupabaseProvider.client.auth.currentUserOrNull()?.id != owner) return@launch
                val response = RetrofitClient.gddService.registrarBiofix(monitoreo.monitoreo_id, exacto)
                if (response.isSuccessful) {
                    resultado = response.body()
                    dao.delete(owner, monitoreo.monitoreo_id); pendiente = null
                    mensaje = "Presencia registrada"; onRefresh()
                } else if (response.code() in setOf(404, 409, 410, 422)) {
                    dao.delete(owner, monitoreo.monitoreo_id); pendiente = null
                    mensaje = "No se registró: ${response.code()}. Actualizá y revisá fecha y ciclo antes de confirmar de nuevo."
                    onRefresh()
                } else mensaje = "Envío pendiente. Se reintentará automáticamente; también podés reintentar ahora."
            } catch (_: Exception) { mensaje = "Envío pendiente. Se reintentará automáticamente con conexión; también podés reintentar ahora." }
            finally { enviando = false }
        }
    }
    Column {
        Text(if (monitoreo.ciclos.orEmpty().none { it.estado == "activo" }) "Esperando biofix" else "Ciclos GDD independientes")
        Text("ML evalúa riesgo; GDD comienza al confirmar presencia.")
        Button(enabled = pendienteCargado && !enviando && monitoreo.activo && owner != null, onClick = {
            pendiente?.let { enviar(it) } ?: run { mostrar = true }
        }) { Text(if (pendiente == null) "Registrar presencia / biofix" else "Reintentar presencia pendiente") }
        mensaje?.let { Text(it) }
        resultado?.let { CicloCard(it.ciclo) }
        monitoreo.ciclos.orEmpty().forEach { CicloCard(it) }
    }
    if (mostrar) BiofixDialog(monitoreo.monitoreo_id, { mostrar = false }, {
        mostrar = false; enviar(it)
    })
}

@Composable
fun CicloDetalleScreen(cicloId: Int, onBack: () -> Unit) {
    var ciclo by remember { mutableStateOf<GddCicloResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(cicloId) {
        try {
            val response = RetrofitClient.gddService.getCiclo(cicloId)
            if (response.isSuccessful) ciclo = response.body() else error = "Este ciclo no está disponible."
        } catch (_: Exception) { error = "No se pudo cargar el ciclo. Revisá tu conexión." }
    }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick = onBack) { Text("Volver") }
        ciclo?.let { CicloCard(it) } ?: Text(error ?: "Cargando ciclo…")
    }
}
