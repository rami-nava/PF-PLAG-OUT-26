package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.AnilloRiesgoGrande
import com.example.plag_out.ui.theme.CargandoCentrado
import com.example.plag_out.ui.theme.EstadisticaCompacta
import com.example.plag_out.ui.theme.EtiquetaInfo
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SelloDeNivel
import com.example.plag_out.ui.theme.SeparadorVertical
import com.example.plag_out.ui.theme.estiloDeNivel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Tamaño del anillo de riesgo en esta pantalla; más chico que el de antes para que todo el
 *  contenido entre sin necesitar scroll. */
private val TAMANO_ANILLO = 132.dp

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

    LaunchedEffect(monitoreoId) { viewModel.cargar(monitoreoId) }

    LaunchedEffect(state.finalizado) {
        if (state.finalizado) onFinalizado()
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    val monitoreo = state.monitoreo

    Scaffold(
        containerColor = PlagOutColors.Cream,
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
                    finalizando = state.finalizando,
                    onFinalizarClick = { mostrarDialogoFinalizar = true }
                )
            }
        }
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

    if (mostrarDialogoFinalizar && monitoreo != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoFinalizar = false },
            modifier = Modifier.testTag("dialogFinalizar"),
            title = { Text("¿Finalizar monitoreo?") },
            text = {
                Text(
                    "¿Finalizar el monitoreo de ${monitoreo.plaga_nombre} en ${monitoreo.cultivo_nombre}? " +
                        "Vas a dejar de recibir alertas de esta plaga en esta plantación."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoFinalizar = false
                        viewModel.finalizarMonitoreo {}
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
    Column(Modifier.fillMaxSize()) {
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ContenidoMonitoreoDetalle(
    monitoreo: MonitoreoResponse,
    datosDesactualizados: Boolean,
    onBack: () -> Unit,
    onVerPlantacion: (Int) -> Unit,
    onVerTerreno: (Int) -> Unit,
    onEditarUmbral: () -> Unit,
    finalizando: Boolean,
    onFinalizarClick: () -> Unit
) {
    val estilo = estiloDeNivel(monitoreo.nivel_alerta)
    // El umbral solo se considera alcanzado por el estado real del monitoreo, no por si se pudo
    // proyectar una fecha: `diasEstimadosAlUmbral` también devuelve null cuando falta ritmo diario
    // para proyectar, y eso no significa que ya se haya llegado al objetivo.
    val umbralAlcanzado = monitoreo.gdd_acumulado >= monitoreo.gdd_objetivo || monitoreo.progreso >= 100f

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.padding(end = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PlagOutColors.TextMain)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    monitoreo.plaga_nombre,
                    color = PlagOutColors.TextMain,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Grass, contentDescription = null, tint = PlagOutColors.TextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(monitoreo.cultivo_nombre, color = PlagOutColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.Landscape, contentDescription = null, tint = PlagOutColors.TextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(monitoreo.terreno_nombre, color = PlagOutColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            AnilloRiesgoGrande(
                progreso = monitoreo.progreso,
                nivelAlerta = monitoreo.nivel_alerta,
                umbralRiesgo = monitoreo.umbral_riesgo,
                tamano = TAMANO_ANILLO,
                grosor = 13.dp,
                modifier = Modifier.testTag("anilloRiesgo")
            )
            Spacer(Modifier.height(8.dp))
            SelloDeNivel(estilo, pulsante = monitoreo.nivel_alerta >= 2)
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            color = PlagOutColors.Surface,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                EstadisticaCompacta("Acumulado", "${monitoreo.gdd_acumulado.toInt()}", Modifier.weight(1f))
                SeparadorVertical()
                EstadisticaCompacta("Objetivo", "${monitoreo.gdd_objetivo.toInt()}", Modifier.weight(1f))
                SeparadorVertical()
                EstadisticaCompacta("GDD hoy", "+${monitoreo.gdd_diario.toInt()}", Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            color = PlagOutColors.Surface,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                EstadisticaCompacta(
                    "Inicio",
                    monitoreo.fecha_inicio?.format(DateTimeFormatter.ofPattern("dd MMM", Locale("es"))) ?: "—",
                    Modifier.weight(1f)
                )
                SeparadorVertical()
                EstadisticaCompacta(
                    "Actualizado",
                    monitoreo.fecha_actualizacion.format(DateTimeFormatter.ofPattern("dd MMM", Locale("es"))),
                    Modifier.weight(1f)
                )
                // El objetivo ya no aplica a un monitoreo finalizado: no tiene sentido proyectar
                // una fecha de alcance para algo que ya terminó.
                if (monitoreo.activo) {
                    SeparadorVertical()
                    val dias = diasEstimadosAlUmbral(monitoreo)
                    val estimado = when {
                        umbralAlcanzado -> "Alcanzado"
                        dias != null -> "$dias días"
                        else -> "Sin datos"
                    }
                    EstadisticaCompacta("Al objetivo", estimado, Modifier.weight(1f))
                }
            }
        }

        if (datosDesactualizados) {
            Spacer(Modifier.height(8.dp))
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
                    subtitulo = "Plantación",
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

        Surface(
            onClick = { if (monitoreo.umbral_riesgo != null && monitoreo.activo) onEditarUmbral() },
            color = PlagOutColors.Surface,
            shape = RoundedCornerShape(18.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().testTag("btnEditarUmbral")
        ) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Umbral de riesgo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PlagOutColors.TextMain, modifier = Modifier.weight(1f))
                if (monitoreo.umbral_riesgo != null) {
                    Text("${monitoreo.umbral_riesgo}%", fontWeight = FontWeight.Bold, color = PlagOutColors.Forest)
                    if (monitoreo.activo) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = PlagOutColors.TextSecondary, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Text("No disponible", fontSize = 13.sp, color = PlagOutColors.TextSecondary)
                }
            }
        }

        Spacer(Modifier.weight(1f))

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
