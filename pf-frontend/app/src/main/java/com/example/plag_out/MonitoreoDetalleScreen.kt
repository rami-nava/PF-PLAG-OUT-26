package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.plag_out.ui.theme.BotonInfoCampo
import com.example.plag_out.ui.theme.CargandoCentrado
import com.example.plag_out.ui.theme.EtiquetaInfo
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.estiloDeNivel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.math.BigDecimal
import kotlin.math.ceil
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonitoreoDetalleScreen(
    monitoreoId: Int,
    viewModel: MonitoreoDetalleViewModel,
    onBack: () -> Unit,
    onFinalizado: () -> Unit,
    onVerPlantacion: (Int) -> Unit,
    onVerTerreno: (Int) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var mostrarDialogoFinalizar by remember { mutableStateOf(false) }
    var notaAlFinalizar by remember { mutableStateOf("") }
    var mostrarInfoNivel by remember { mutableStateOf(false) }
    var mostrarInfoUmbral by remember { mutableStateOf(false) }

    LaunchedEffect(monitoreoId) { viewModel.cargar(monitoreoId) }

    LaunchedEffect(state.finalizado) {
        if (state.finalizado) onFinalizado()
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    val monitoreo = state.monitoreo

    LaunchedEffect(mostrarDialogoFinalizar) {
        if (mostrarDialogoFinalizar) notaAlFinalizar = monitoreo?.observaciones.orEmpty()
    }

    Scaffold(
        containerColor = PlagOutColors.Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                monitoreo == null && state.isLoading -> CargandoCentrado()
                monitoreo == null -> EstadoErrorDetalle(
                    mensaje = state.error ?: "No se pudo cargar el monitoreo.",
                    onBack = onBack,
                    onReintentar = { viewModel.cargar(monitoreoId) }
                )
                else -> ContenidoMonitoreoDetalle(
                    monitoreo = monitoreo,
                    datosDesactualizados = state.datosDesactualizados,
                    onBack = onBack,
                    onVerPlantacion = onVerPlantacion,
                    onVerTerreno = onVerTerreno,
                    onEditarUmbral = { viewModel.abrirEditorUmbral() },
                    onEditarUmbralMl = { viewModel.abrirEditorUmbralMl() },
                    onEditarObservaciones = { viewModel.abrirEditorObservaciones() },
                    onVerInfoNivel = { mostrarInfoNivel = true },
                    onVerInfoUmbral = { mostrarInfoUmbral = true },
                    finalizando = state.finalizando,
                    onRefresh = { viewModel.cargar(monitoreoId) },
                    onFinalizarClick = { mostrarDialogoFinalizar = true }
                )
            }
        }
    }

    if (mostrarInfoNivel) {
        NivelDeAlertaSheet(
            nivelActual = monitoreo?.nivel_alerta,
            onDismiss = { mostrarInfoNivel = false }
        )
    }

    if (mostrarInfoUmbral) {
        UmbralDeRiesgoSheet(
            umbralActual = monitoreo?.umbral_riesgo,
            onDismiss = { mostrarInfoUmbral = false }
        )
    }

    if (state.umbralEditado != null && monitoreo != null) {
        SheetEditarUmbral(
            umbralEditado = state.umbralEditado ?: (monitoreo.umbral_riesgo ?: 80),
            umbralOriginal = monitoreo.umbral_riesgo,
            guardando = state.guardandoUmbral,
            onUmbralChange = { viewModel.actualizarUmbralEditado(it) },
            onCancelar = { viewModel.cancelarEdicionUmbral() },
            onGuardar = {
                viewModel.guardarUmbral {
                    scope.launch { snackbarHostState.showSnackbar("Umbral actualizado") }
                }
            }
        )
    }

    if (state.umbralMlEditado != null && monitoreo != null) {
        SheetEditarUmbralMl(
            umbralEditado = state.umbralMlEditado!!,
            recomendado = monitoreo.umbral_alerta_ml_recomendado ?: 0f,
            tieneOverride = monitoreo.umbral_alerta_ml != null,
            guardando = state.guardandoUmbralMl,
            onUmbralChange = viewModel::actualizarUmbralMlEditado,
            onCancelar = viewModel::cancelarEdicionUmbralMl,
            onGuardar = {
                viewModel.guardarUmbralMl {
                    scope.launch { snackbarHostState.showSnackbar("Threshold ML actualizado") }
                }
            },
            onUsarRecomendado = {
                viewModel.usarUmbralMlRecomendado {
                    scope.launch { snackbarHostState.showSnackbar("Se usará el threshold recomendado") }
                }
            }
        )
    }

    if (state.observacionesEditadas != null && monitoreo != null) {
        SheetEditarObservaciones(
            texto = state.observacionesEditadas ?: "",
            textoOriginal = monitoreo.observaciones.orEmpty(),
            guardando = state.guardandoObservaciones,
            onTextoChange = viewModel::actualizarObservacionesEditadas,
            onCancelar = viewModel::cancelarEdicionObservaciones,
            onGuardar = {
                viewModel.guardarObservaciones {
                    scope.launch { snackbarHostState.showSnackbar("Nota guardada") }
                }
            }
        )
    }

    if (mostrarDialogoFinalizar && monitoreo != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoFinalizar = false },
            modifier = Modifier.testTag("dialogFinalizar"),
            title = { Text("¿Finalizar monitoreo?") },
            text = {
                Column {
                    Text(
                        "¿Finalizar el monitoreo de ${monitoreo.plaga_nombre} en ${monitoreo.cultivo_nombre}? " +
                            "Vas a dejar de recibir alertas de esta plaga en este cultivo."
                    )
                    Spacer(Modifier.height(14.dp))
                    // El cierre de campaña es el momento en que el dato está fresco: se ofrece
                    // escribir la nota acá mismo, sin obligar (el campo puede quedar vacío).
                    CampoDeNota(
                        texto = notaAlFinalizar,
                        onTextoChange = { notaAlFinalizar = it.take(MAX_CARACTERES_OBSERVACIONES) },
                        etiqueta = "Nota para la próxima campaña (opcional)",
                        tag = "txtNotaFinalizar"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoFinalizar = false
                        viewModel.finalizarMonitoreo(observaciones = notaAlFinalizar) {}
                    },
                    modifier = Modifier.testTag("btnConfirmarFinalizar")
                ) {
                    Text("Finalizar", color = PlagOutColors.RiskDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoFinalizar = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun EstadoErrorDetalle(mensaje: String, onBack: () -> Unit, onReintentar: () -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PlagOutColors.TextMain)
        }
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = PlagOutColors.RiskUnknown, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(mensaje, color = PlagOutColors.TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onReintentar,
                colors = ButtonDefaults.buttonColors(containerColor = PlagOutColors.Forest, contentColor = PlagOutColors.TextOnDark)
            ) {
                Text("Reintentar")
            }
        }
    }
}

private const val PAGINA_DETALLE = 0
private const val PAGINA_CICLOS = 1

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ContenidoMonitoreoDetalle(
    monitoreo: MonitoreoResponse,
    datosDesactualizados: Boolean,
    onBack: () -> Unit,
    onVerPlantacion: (Int) -> Unit,
    onVerTerreno: (Int) -> Unit,
    onEditarUmbral: () -> Unit,
    onEditarUmbralMl: () -> Unit,
    onEditarObservaciones: () -> Unit,
    onVerInfoNivel: () -> Unit,
    onVerInfoUmbral: () -> Unit,
    finalizando: Boolean,
    onFinalizarClick: () -> Unit,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    Column(Modifier.fillMaxSize()) {
        val formaHeader = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(listOf(PlagOutColors.Forest, PlagOutColors.Leaf)),
                    shape = formaHeader
                )
                .clip(formaHeader)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PlagOutColors.TextOnDark)
                }
                Column {
                    Text(
                        "Monitoreo de plaga",
                        color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        monitoreo.plaga_nombre,
                        color = PlagOutColors.TextOnDark,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Grass, contentDescription = null, tint = PlagOutColors.TextOnDark.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            monitoreo.cultivo_nombre,
                            color = PlagOutColors.TextOnDark.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.Outlined.Landscape, contentDescription = null, tint = PlagOutColors.TextOnDark.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            monitoreo.terreno_nombre,
                            color = PlagOutColors.TextOnDark.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = PlagOutColors.Cream,
            contentColor = PlagOutColors.Forest
        ) {
            Tab(
                selected = pagerState.currentPage == PAGINA_DETALLE,
                onClick = { scope.launch { pagerState.animateScrollToPage(PAGINA_DETALLE) } },
                text = { Text("Detalle", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("tabDetalle")
            )
            Tab(
                selected = pagerState.currentPage == PAGINA_CICLOS,
                onClick = { scope.launch { pagerState.animateScrollToPage(PAGINA_CICLOS) } },
                text = { Text("Ciclos", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("tabCiclos")
            )
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pagina ->
            when (pagina) {
                PAGINA_DETALLE -> DetalleTab(
                    monitoreo = monitoreo,
                    datosDesactualizados = datosDesactualizados,
                    onVerPlantacion = onVerPlantacion,
                    onVerTerreno = onVerTerreno,
                    onEditarUmbral = onEditarUmbral,
                    onEditarUmbralMl = onEditarUmbralMl,
                    onVerInfoUmbral = onVerInfoUmbral,
                    onEditarObservaciones = onEditarObservaciones,
                    finalizando = finalizando,
                    onFinalizarClick = onFinalizarClick
                )
                else -> CiclosTab(
                    monitoreo = monitoreo,
                    onRefresh = onRefresh,
                    onVerInfoNivel = onVerInfoNivel
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DetalleTab(
    monitoreo: MonitoreoResponse,
    datosDesactualizados: Boolean,
    onVerPlantacion: (Int) -> Unit,
    onVerTerreno: (Int) -> Unit,
    onEditarUmbral: () -> Unit,
    onEditarUmbralMl: () -> Unit,
    onVerInfoUmbral: () -> Unit,
    onEditarObservaciones: () -> Unit,
    finalizando: Boolean,
    onFinalizarClick: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            val eclosionAlcanzada =
                monitoreo.gdd_acumulado >= monitoreo.gdd_objetivo || monitoreo.progreso >= 100f
            val fechaEclosion = monitoreo.fecha_eclosion
            if (eclosionAlcanzada || !monitoreo.activo || fechaEclosion != null) {
                Spacer(Modifier.height(10.dp))
                val huboEclosion = eclosionAlcanzada || fechaEclosion != null
                Surface(
                    color = PlagOutColors.Surface,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (huboEclosion) Icons.Outlined.CalendarMonth else Icons.Outlined.EventBusy,
                            contentDescription = null,
                            tint = if (huboEclosion) PlagOutColors.Forest else PlagOutColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (huboEclosion) "Eclosión alcanzada" else "Eclosión no alcanzada",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlagOutColors.TextMain,
                                modifier = Modifier.testTag("txtEstadoEclosion")
                            )
                            Text(
                                when {
                                    fechaEclosion != null -> fechaEclosion.format(
                                        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es"))
                                    )
                                    huboEclosion -> "Fecha no registrada"
                                    else -> "El monitoreo se finalizó antes de llegar al objetivo"
                                },
                                fontSize = 12.sp,
                                color = PlagOutColors.TextSecondary,
                                modifier = Modifier.testTag("txtFechaEclosion")
                            )
                        }
                    }
                }
            }

            if (datosDesactualizados) {
                Spacer(Modifier.height(10.dp))
                EtiquetaInfo(
                    Icons.Filled.ErrorOutline,
                    "Mostrando datos guardados · sin conexión",
                    PlagOutColors.RiskWarn,
                    Modifier.testTag("chipDatosDesactualizados")
                )
            }

            Spacer(Modifier.height(10.dp))

            Surface(
                color = PlagOutColors.Surface,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        "Contexto",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PlagOutColors.TextMain,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                    FilaContexto(
                        icono = Icons.Outlined.Landscape,
                        titulo = monitoreo.terreno_nombre,
                        subtitulo = "Terreno",
                        onClick = { onVerTerreno(monitoreo.terreno_id) }
                    )
                    FilaContexto(
                        icono = Icons.Outlined.Grass,
                        titulo = monitoreo.cultivo_nombre,
                        subtitulo = "Cultivo",
                        onClick = { onVerPlantacion(monitoreo.plantacion_id) }
                    )
                    FilaContexto(
                        icono = Icons.Outlined.BugReport,
                        titulo = monitoreo.plaga_nombre,
                        subtitulo = monitoreo.plaga_nombre_cientifico,
                        onClick = null
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            TarjetaCampo(
                titulo = "Umbral GDD",
                onInfo = onVerInfoUmbral,
                descripcionInfo = "Qué es el umbral GDD",
                tagInfo = "btnInfoUmbral"
            ) {
                if (monitoreo.umbral_riesgo != null) {
                    // El toque sobre la fila abre el editor; la "i" queda afuera de esa zona.
                    val editable = monitoreo.activo
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .let { if (editable) it.clickable(onClick = onEditarUmbral) else it }
                            .testTag("btnEditarUmbral")
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${monitoreo.umbral_riesgo}%",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PlagOutColors.Forest
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (editable) "Te avisamos al superarlo · tocá para ajustar"
                            else "Umbral con el que se siguió este monitoreo",
                            fontSize = 12.sp,
                            color = PlagOutColors.TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        if (editable) {
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = PlagOutColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        "No disponible",
                        fontSize = 13.sp,
                        color = PlagOutColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            TarjetaCampo(
                titulo = "Alerta predictiva (ML)",
                onInfo = null,
                descripcionInfo = "Threshold del modelo",
                tagInfo = "btnInfoUmbralMl"
            ) {
                val recomendado = monitoreo.umbral_alerta_ml_recomendado
                val compatible = recomendado != null && monitoreo.modelo_alerta_ml_id != null
                if (!compatible) {
                    Text(
                        "No hay un modelo compatible para este monitoreo.",
                        fontSize = 13.sp,
                        color = PlagOutColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                    )
                } else {
                    val editable = monitoreo.activo
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .let { if (editable) it.clickable(onClick = onEditarUmbralMl) else it }
                            .testTag("btnEditarUmbralMl")
                            .padding(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                formatearPorcentajeMl(monitoreo.umbral_alerta_ml_efectivo ?: recomendado),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PlagOutColors.Forest
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (monitoreo.umbral_alerta_ml == null) "Threshold recomendado"
                                else "Override del monitoreo",
                                fontSize = 12.sp,
                                color = PlagOutColors.TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            if (editable) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = PlagOutColors.TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Recomendado ${formatearPorcentajeMl(recomendado)} · horizonte ${monitoreo.horizonte_alerta_ml_dias ?: "—"} días",
                            fontSize = 11.sp,
                            color = PlagOutColors.TextSecondary
                        )
                        Text(
                            "Modelo ${monitoreo.modelo_alerta_ml_id}",
                            fontSize = 11.sp,
                            color = PlagOutColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            TarjetaCampo(
                titulo = "Notas de campaña",
                onInfo = null,
                descripcionInfo = "Para qué sirven las notas",
                tagInfo = "btnInfoNotas"
            ) {
                val nota = monitoreo.observaciones?.takeIf { it.isNotBlank() }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onEditarObservaciones)
                        .testTag("btnEditarObservaciones")
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = PlagOutColors.Forest,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            nota ?: "Todavía no escribiste nada",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = if (nota != null) PlagOutColors.TextMain else PlagOutColors.TextSecondary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("txtObservaciones")
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (nota != null) "Tocá para editar"
                            else "Anotá si la plaga apareció, qué aplicaste y si sirvió",
                            fontSize = 11.sp,
                            color = PlagOutColors.TextSecondary
                        )
                    }
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = PlagOutColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        if (!monitoreo.activo) {
            Surface(
                color = PlagOutColors.RiskUnknown.copy(alpha = 0.14f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Flag, contentDescription = null, tint = PlagOutColors.TextSecondary)
                    Spacer(Modifier.width(10.dp))
                    Text("Monitoreo finalizado", color = PlagOutColors.TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            OutlinedButton(
                onClick = onFinalizarClick,
                enabled = !finalizando,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PlagOutColors.RiskDanger),
                border = BorderStroke(1.dp, PlagOutColors.RiskDanger),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btnFinalizarMonitoreo")
            ) {
                if (finalizando) {
                    CircularProgressIndicator(color = PlagOutColors.RiskDanger, modifier = Modifier.size(20.dp))
                } else {
                    Text("Finalizar monitoreo", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CiclosTab(
    monitoreo: MonitoreoResponse,
    onRefresh: () -> Unit,
    onVerInfoNivel: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Ciclos GDD",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.TextMain,
                modifier = Modifier.weight(1f)
            )
            BotonInfoCampo(
                onClick = onVerInfoNivel,
                contentDescription = "Qué es el nivel de alerta",
                tint = estiloDeNivel(nivelAlertaDeCiclos(monitoreo) ?: -1).color,
                modifier = Modifier.testTag("btnInfoNivelAlerta")
            )
        }

        BiofixManual(monitoreo, onRefresh, Modifier.weight(1f))
    }
}

@Composable
private fun TarjetaCampo(
    titulo: String,
    onInfo: (() -> Unit)?,
    descripcionInfo: String,
    tagInfo: String,
    tintInfo: Color = PlagOutColors.Forest,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    titulo,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = PlagOutColors.TextMain,
                    modifier = Modifier.weight(1f)
                )
                if (onInfo != null) {
                    BotonInfoCampo(
                        onClick = onInfo,
                        contentDescription = descripcionInfo,
                        tint = tintInfo,
                        modifier = Modifier.testTag(tagInfo)
                    )
                }
            }
            contenido()
        }
    }
}

@Composable
private fun FilaContexto(
    icono: ImageVector,
    titulo: String,
    subtitulo: String?,
    onClick: (() -> Unit)?
) {
    val base = Modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(horizontal = 18.dp, vertical = 12.dp)

    Row(base, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).background(PlagOutColors.Forest.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = PlagOutColors.Forest, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PlagOutColors.TextMain)
            if (!subtitulo.isNullOrBlank()) {
                Text(subtitulo, fontSize = 12.sp, color = PlagOutColors.TextSecondary)
            }
        }
        if (onClick != null) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = PlagOutColors.TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetEditarUmbral(
    umbralEditado: Int,
    umbralOriginal: Int?,
    guardando: Boolean,
    onUmbralChange: (Int) -> Unit,
    onCancelar: () -> Unit,
    onGuardar: () -> Unit
) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onCancelar,
        sheetState = estadoHoja,
        modifier = Modifier.testTag("sheetUmbral")
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            UmbralDeRiesgoSlider(umbralActual = umbralEditado, onUmbralChange = onUmbralChange)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancelar,
                    enabled = !guardando,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = onGuardar,
                    enabled = !guardando && umbralEditado != umbralOriginal,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("btnGuardarUmbral"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlagOutColors.Forest,
                        contentColor = PlagOutColors.TextOnDark,
                        disabledContainerColor = PlagOutColors.Forest.copy(alpha = 0.4f),
                        disabledContentColor = PlagOutColors.TextOnDark.copy(alpha = 0.6f)
                    )
                ) {
                    if (guardando) {
                        CircularProgressIndicator(color = PlagOutColors.TextOnDark, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Guardar")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetEditarUmbralMl(
    umbralEditado: Int,
    recomendado: Float,
    tieneOverride: Boolean,
    guardando: Boolean,
    onUmbralChange: (Int) -> Unit,
    onCancelar: () -> Unit,
    onGuardar: () -> Unit,
    onUsarRecomendado: () -> Unit
) {
    val minimo = ceil(recomendado.toDouble()).toInt().coerceIn(0, 100)
    ModalBottomSheet(
        onDismissRequest = onCancelar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PlagOutColors.Surface,
        modifier = Modifier.testTag("sheetUmbralMl")
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Threshold de alerta ML", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = PlagOutColors.TextMain)
            Spacer(Modifier.height(6.dp))
            Text(
                "El modelo recomienda ${formatearPorcentajeMl(recomendado)}. Podés usar un porcentaje entero desde $minimo%.",
                fontSize = 13.sp,
                color = PlagOutColors.TextSecondary
            )
            Spacer(Modifier.height(18.dp))
            Text("$umbralEditado%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.Forest)
            Slider(
                value = umbralEditado.toFloat(),
                onValueChange = { onUmbralChange(it.roundToInt()) },
                valueRange = minimo.toFloat()..100f,
                steps = (100 - minimo - 1).coerceAtLeast(0),
                colors = SliderDefaults.colors(
                    thumbColor = PlagOutColors.Forest,
                    activeTrackColor = PlagOutColors.Forest
                ),
                modifier = Modifier.testTag("sliderUmbralMl")
            )
            if (tieneOverride) {
                TextButton(
                    onClick = onUsarRecomendado,
                    enabled = !guardando,
                    modifier = Modifier.align(Alignment.End).testTag("btnUsarRecomendadoMl")
                ) { Text("Usar recomendado", color = PlagOutColors.Forest) }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancelar, enabled = !guardando, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(
                    onClick = onGuardar,
                    enabled = !guardando,
                    colors = ButtonDefaults.buttonColors(containerColor = PlagOutColors.Forest),
                    modifier = Modifier.weight(1f).testTag("btnGuardarUmbralMl")
                ) {
                    if (guardando) CircularProgressIndicator(Modifier.size(18.dp), color = PlagOutColors.TextOnDark)
                    else Text("Guardar")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CampoDeNota(
    texto: String,
    onTextoChange: (String) -> Unit,
    etiqueta: String,
    tag: String,
    habilitado: Boolean = true
) {
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = texto,
            onValueChange = onTextoChange,
            enabled = habilitado,
            label = { Text(etiqueta) },
            placeholder = { Text("Ej.: no apareció la plaga; apliqué X el 12/2 y funcionó") },
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PlagOutColors.Forest,
                cursorColor = PlagOutColors.Forest,
                focusedLabelColor = PlagOutColors.Forest
            ),
            modifier = Modifier.fillMaxWidth().testTag(tag)
        )
        Text(
            "${texto.length}/$MAX_CARACTERES_OBSERVACIONES",
            fontSize = 11.sp,
            color = if (texto.length >= MAX_CARACTERES_OBSERVACIONES) PlagOutColors.RiskWarn
            else PlagOutColors.TextSecondary,
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp, end = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetEditarObservaciones(
    texto: String,
    textoOriginal: String,
    guardando: Boolean,
    onTextoChange: (String) -> Unit,
    onCancelar: () -> Unit,
    onGuardar: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onCancelar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PlagOutColors.Surface,
        modifier = Modifier.testTag("sheetObservaciones")
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Notas de campaña", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = PlagOutColors.TextMain)
            Spacer(Modifier.height(6.dp))
            Text(
                "Lo que escribas acá te va a servir el año que viene: si la plaga apareció, qué " +
                    "tratamiento hiciste y si dio resultado.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = PlagOutColors.TextSecondary
            )
            Spacer(Modifier.height(16.dp))
            CampoDeNota(
                texto = texto,
                onTextoChange = onTextoChange,
                etiqueta = "Tu nota",
                tag = "txtEditarObservaciones",
                habilitado = !guardando
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancelar, enabled = !guardando, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(
                    onClick = onGuardar,
                    enabled = !guardando && texto.trim() != textoOriginal,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlagOutColors.Forest,
                        contentColor = PlagOutColors.TextOnDark,
                        disabledContainerColor = PlagOutColors.Forest.copy(alpha = 0.4f),
                        disabledContentColor = PlagOutColors.TextOnDark.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.weight(1f).testTag("btnGuardarObservaciones")
                ) {
                    if (guardando) CircularProgressIndicator(Modifier.size(18.dp), color = PlagOutColors.TextOnDark)
                    else Text("Guardar")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun formatearPorcentajeMl(valor: Float): String =
    "${BigDecimal(valor.toString()).stripTrailingZeros().toPlainString()}%"
