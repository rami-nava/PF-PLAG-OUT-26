package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.CargandoCentrado
import com.example.plag_out.ui.theme.EtiquetaInfo
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.AlmacenamientoLocal.AppDatabase
import com.example.plag_out.AlmacenamientoLocal.BiofixPendiente
import com.example.plag_out.Service.RetrofitClient
import com.google.gson.Gson
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
internal fun fechasDeBiofixValidas(inicioMonitoreo: LocalDate?, hoy: LocalDate): SelectableDates =
    object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            val fecha = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
            if (fecha.isAfter(hoy)) return false
            return inicioMonitoreo == null || !fecha.isBefore(inicioMonitoreo)
        }

        override fun isSelectableYear(year: Int): Boolean =
            year <= hoy.year && (inicioMonitoreo == null || year >= inicioMonitoreo.year)
    }

@RequiresApi(Build.VERSION_CODES.O)
private fun formatearFechaLarga(fecha: LocalDate): String =
    fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es")))

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiofixDialog(monitoreoId: Int, onDismiss: () -> Unit, onConfirm: (BiofixRequest) -> Unit, service: com.example.plag_out.Service.GDDService = RetrofitClient.gddService) {
    val hoy = remember { LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")) }
    var monitor by remember { mutableStateOf<MonitoreoResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var fecha by remember { mutableStateOf(hoy) }
    var mostrarCalendario by remember { mutableStateOf(false) }
    var opcion by remember { mutableStateOf<Int?>(null) } // -1 = explicitly create another

    LaunchedEffect(monitoreoId) {
        try {
            val response = service.getMonitoreo(monitoreoId)
            if (response.isSuccessful) monitor = response.body()
            else error = "Monitoreo no disponible (${response.code()})."
        } catch (_: Exception) { error = "No se pudo cargar. Revisá tu conexión y volvé a abrir." }
    }

    val activos = monitor?.ciclos.orEmpty().filter { it.estado == "activo" }
    val numeros = numerosDeCiclo(monitor?.ciclos.orEmpty())
    val inicio = monitor?.fecha_inicio
    val valido = inicio != null && !fecha.isBefore(inicio) && !fecha.isAfter(hoy)

    if (mostrarCalendario) {
        val calendario = rememberDatePickerState(
            initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = fechasDeBiofixValidas(inicio, hoy)
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            colors = DatePickerDefaults.colors(containerColor = PlagOutColors.Surface),
            confirmButton = {
                TextButton(onClick = {
                    calendario.selectedDateMillis?.let {
                        fecha = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    mostrarCalendario = false
                }) { Text("Aceptar", color = PlagOutColors.Forest, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar", color = PlagOutColors.Forest) }
            }
        ) {
            DatePicker(
                state = calendario,
                title = null,
                colors = DatePickerDefaults.colors(
                    containerColor = PlagOutColors.Surface,
                    selectedDayContainerColor = PlagOutColors.Forest,
                    todayContentColor = PlagOutColors.Forest,
                    todayDateBorderColor = PlagOutColors.Forest,
                    headlineContentColor = PlagOutColors.Forest
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PlagOutColors.Surface,
        shape = RoundedCornerShape(26.dp),
        icon = {
            Box(
                Modifier.size(52.dp).background(PlagOutColors.Leaf.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.BugReport,
                    contentDescription = null,
                    tint = PlagOutColors.Forest,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                "Registrar presencia",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PlagOutColors.TextMain,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Al confirmar arranca el conteo de GDD desde la fecha que elijas.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = PlagOutColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    "FECHA DE OBSERVACIÓN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = PlagOutColors.TextSecondary
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    onClick = { mostrarCalendario = true },
                    enabled = monitor != null,
                    color = PlagOutColors.Cream,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (valido || monitor == null) PlagOutColors.Divider else PlagOutColors.RiskDanger),
                    modifier = Modifier.fillMaxWidth().testTag("btnFechaBiofix")
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = PlagOutColors.Forest, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                formatearFechaLarga(fecha),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlagOutColors.TextMain
                            )
                            if (fecha == hoy) {
                                Text("Hoy", fontSize = 11.sp, color = PlagOutColors.TextSecondary)
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = PlagOutColors.TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                if (monitor == null && error == null) {
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = PlagOutColors.Forest, modifier = Modifier.size(22.dp))
                    }
                }

                if (activos.isEmpty()) {
                    if (monitor != null) {
                        Spacer(Modifier.height(14.dp))
                        EtiquetaInfo(Icons.Outlined.Schedule, "Se inicia un ciclo GDD nuevo", PlagOutColors.Forest)
                    }
                } else {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "¿Es la presencia que ya seguís o querés iniciar otro ciclo?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlagOutColors.TextMain
                    )
                    Spacer(Modifier.height(10.dp))
                    activos.forEach { ciclo ->
                        OpcionCiclo(
                            seleccionada = opcion == ciclo.id,
                            onSeleccionar = { opcion = ciclo.id },
                            titulo = "Ciclo ${numeros[ciclo.id] ?: ciclo.id}",
                            detalle = "Biofix ${ciclo.fecha_biofix} · ${ciclo.progreso.toInt()}%"
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    OpcionCiclo(
                        seleccionada = opcion == -1,
                        onSeleccionar = { opcion = -1 },
                        titulo = "Iniciar otro ciclo",
                        detalle = "Una presencia distinta, en paralelo a la que ya seguís"
                    )
                }

                error?.let {
                    Spacer(Modifier.height(14.dp))
                    EtiquetaInfo(Icons.Filled.ErrorOutline, it, PlagOutColors.RiskDanger)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = monitor?.activo == true && valido && (activos.isEmpty() || opcion != null),
                onClick = {
                    val asociar = activos.isNotEmpty() && opcion != -1
                    onConfirm(BiofixRequest(UUID.randomUUID().toString(), fecha.toString(),
                        if (asociar) "asociar_ciclo" else "iniciar_ciclo",
                        if (asociar) opcion else null, activos.isNotEmpty() && !asociar))
                }
            ) { Text("Confirmar presencia", color = PlagOutColors.Forest, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = PlagOutColors.TextSecondary) }
        }
    )
}

@Composable
private fun OpcionCiclo(
    seleccionada: Boolean,
    onSeleccionar: () -> Unit,
    titulo: String,
    detalle: String
) {
    Surface(
        color = if (seleccionada) PlagOutColors.Leaf.copy(alpha = 0.12f) else PlagOutColors.Cream,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (seleccionada) PlagOutColors.Leaf else PlagOutColors.Divider),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = seleccionada, role = Role.RadioButton, onClick = onSeleccionar)
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = seleccionada,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = PlagOutColors.Forest)
            )
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                Text(detalle, fontSize = 12.sp, color = PlagOutColors.TextSecondary)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BiofixManual(monitoreo: MonitoreoResponse, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
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

    val ciclos = remember(monitoreo.ciclos, resultado) {
        val recien = resultado?.ciclo?.takeIf { nuevo -> monitoreo.ciclos.orEmpty().none { it.id == nuevo.id } }
        ordenarCiclosPorProgreso(monitoreo.ciclos.orEmpty() + listOfNotNull(recien))
    }
    val numeros = remember(ciclos) { numerosDeCiclo(ciclos) }

    Column(modifier.fillMaxSize()) {
        if (ciclos.isEmpty()) {
            // Sin ciclos no hay nada que scrollear: el estado vacío se centra en el espacio libre.
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    mensaje?.let {
                        EtiquetaInfo(Icons.Filled.ErrorOutline, it, PlagOutColors.RiskWarn)
                    }
                    EstadoEsperandoBiofix(Modifier.fillMaxWidth())
                }
            }
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                mensaje?.let {
                    Spacer(Modifier.height(12.dp))
                    EtiquetaInfo(Icons.Filled.ErrorOutline, it, PlagOutColors.RiskWarn)
                }
                Spacer(Modifier.height(14.dp))
                ciclos.forEach { ciclo ->
                    CicloCard(
                        ciclo = ciclo,
                        numero = numeros[ciclo.id],
                        objetivoMonitoreo = monitoreo.gdd_objetivo,
                        umbralRiesgo = monitoreo.umbral_riesgo
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                "El modelo evalúa el riesgo; el conteo de GDD arranca al confirmar la presencia.",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                color = PlagOutColors.TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            BotonRegistrarBiofix(
                texto = if (pendiente == null) "Registrar nuevo biofix" else "Reintentar biofix pendiente",
                habilitado = pendienteCargado && monitoreo.activo && owner != null,
                cargando = enviando,
                onClick = { pendiente?.let { enviar(it) } ?: run { mostrar = true } }
            )
        }
    }
    if (mostrar) BiofixDialog(monitoreo.monitoreo_id, { mostrar = false }, {
        mostrar = false; enviar(it)
    })
}

@RequiresApi(Build.VERSION_CODES.O)
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
    Column(Modifier.fillMaxSize().background(PlagOutColors.Cream)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PlagOutColors.TextMain)
            }
            Text("Ciclo GDD", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = PlagOutColors.TextMain)
        }
        Box(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            val actual = ciclo
            when {
                actual != null -> CicloCard(actual, Modifier.padding(top = 8.dp))
                error != null -> EtiquetaInfo(Icons.Filled.ErrorOutline, error!!, PlagOutColors.RiskDanger)
                else -> CargandoCentrado()
            }
        }
    }
}
