package com.example.plag_out

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.plag_out.ui.theme.AnilloProgreso
import com.example.plag_out.ui.theme.AnilloSegmentado
import com.example.plag_out.ui.theme.EstadisticaCompacta
import com.example.plag_out.ui.theme.EstadoSinResultados
import com.example.plag_out.ui.theme.EstadoVacioFlotante
import com.example.plag_out.ui.theme.EtiquetaInfo
import com.example.plag_out.ui.theme.FiltroChipsRow
import com.example.plag_out.ui.theme.NivelEstilo
import com.example.plag_out.ui.theme.OpcionFiltro
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SelloDeNivel
import com.example.plag_out.ui.theme.SeparadorVertical
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.contadorAnimado
import com.example.plag_out.ui.theme.estiloDeNivel
import com.example.plag_out.ui.theme.estiloEsperandoBiofix
import com.example.plag_out.ui.theme.estiloFinalizado
import com.example.plag_out.ui.theme.rememberPressScale
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val FILTRO_TODAS = -1
private const val FILTRO_ACTIVAS = 1
private const val FILTRO_FINALIZADAS = 0

private const val PAGINA_INFORMACION = 0
private const val PAGINA_PLANTACIONES = 1

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PlantacionesPorTerreno(
    terrenoId: Int,
    plantacionesViewModel: PlantacionesViewModel,
    monitoreosViewModel: MonitoreosViewModel,
    terrenoViewModel: TerrenosViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    val plantacionesState by plantacionesViewModel.state.collectAsState()
    val monitoreosState by monitoreosViewModel.state.collectAsState()
    val terrenosState by terrenoViewModel.state.collectAsState()

    val terreno = terrenosState.terrenos.find { it.terreno_id == terrenoId }
    val plantacionesDelTerreno = plantacionesState.plantaciones.filter { it.terreno_id == terrenoId }
    val monitoreosDelTerreno = monitoreosState.monitoreos.filter { it.terreno_id == terrenoId }

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(terrenosState.error) {
        terrenosState.error?.let {
            snackbarHostState.showSnackbar(it)
            terrenoViewModel.limpiarError()
        }
    }
    LaunchedEffect(plantacionesState.error) {
        plantacionesState.error?.let {
            snackbarHostState.showSnackbar(it)
            plantacionesViewModel.limpiarError()
        }
    }

    var plantacionAEliminar by remember { mutableStateOf<PlantacionesResponse?>(null) }
    var eliminandoId by remember { mutableStateOf<Int?>(null) }
    var mostrarDialogoEditar by remember { mutableStateOf(false) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    var filtro by rememberSaveable { mutableStateOf(FILTRO_TODAS) }

    val ordenadas = remember(plantacionesDelTerreno, monitoreosState.monitoreos) {
        plantacionesDelTerreno.sortedWith(
            compareByDescending<PlantacionesResponse> { it.activa }
                .thenByDescending { p ->
                    monitoreosState.monitoreos.filter { it.plantacion_id == p.plantacion_id && it.activo }.maxOfOrNull { nivelAlertaEfectivo(it) } ?: -1
                }
        )
    }
    val filtradas = remember(ordenadas, filtro) {
        when (filtro) {
            FILTRO_ACTIVAS -> ordenadas.filter { it.activa }
            FILTRO_FINALIZADAS -> ordenadas.filter { !it.activa }
            else -> ordenadas
        }
    }

    Scaffold(
        containerColor = PlagOutColors.Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = pagerState.currentPage == PAGINA_PLANTACIONES,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("agregar_plantacion/$terrenoId") },
                    containerColor = PlagOutColors.Forest,
                    contentColor = PlagOutColors.TextOnDark,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nuevo Cultivo", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(listOf(PlagOutColors.Forest, PlagOutColors.Leaf)),
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    )
                    .padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PlagOutColors.TextOnDark)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Terreno",
                            color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            terreno?.terreno_nombre ?: "Terreno",
                            color = PlagOutColors.TextOnDark,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        val activas = plantacionesDelTerreno.count { it.activa }
                        Text(
                            if (plantacionesDelTerreno.isEmpty()) "Sin cultivos registrados"
                            else "$activas activa${if (activas == 1) "" else "s"} de ${plantacionesDelTerreno.size}",
                            color = PlagOutColors.TextOnDark.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                    if (terreno != null) {
                        MenuAccionesHeader(
                            ocupado = terrenosState.procesando,
                            tag = "btnMenuTerreno",
                            opciones = listOf(
                                OpcionMenuAccion(
                                    icono = Icons.Outlined.Edit,
                                    titulo = "Editar terreno",
                                    detalle = "Cambiá el nombre o las hectáreas",
                                    color = PlagOutColors.TextMain,
                                    tag = "btnEditarTerreno",
                                    onClick = { mostrarDialogoEditar = true }
                                ),
                                OpcionMenuAccion(
                                    icono = Icons.Outlined.DeleteOutline,
                                    titulo = "Eliminar terreno",
                                    detalle = "Borra sus cultivos y monitoreos",
                                    color = PlagOutColors.RiskDanger,
                                    tag = "btnEliminarTerreno",
                                    onClick = { mostrarDialogoEliminar = true }
                                )
                            )
                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = PlagOutColors.Cream,
                contentColor = PlagOutColors.Forest
            ) {
                Tab(
                    selected = pagerState.currentPage == PAGINA_INFORMACION,
                    onClick = { scope.launch { pagerState.animateScrollToPage(PAGINA_INFORMACION) } },
                    text = { Text("Información", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = pagerState.currentPage == PAGINA_PLANTACIONES,
                    onClick = { scope.launch { pagerState.animateScrollToPage(PAGINA_PLANTACIONES) } },
                    text = { Text("Cultivos", fontWeight = FontWeight.SemiBold) }
                )
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pagina ->
                when (pagina) {
                    PAGINA_INFORMACION -> InformacionTerrenoTab(
                        terreno = terreno,
                        plantacionesDelTerreno = plantacionesDelTerreno,
                        monitoreosDelTerreno = monitoreosDelTerreno
                    )
                    else -> PlantacionesTab(
                        isRefreshing = plantacionesState.isRefreshing || monitoreosState.isRefreshing,
                        onRefresh = {
                            plantacionesViewModel.refrescar()
                            monitoreosViewModel.refrescar()
                        },
                        plantacionesDelTerreno = plantacionesDelTerreno,
                        filtradas = filtradas,
                        filtro = filtro,
                        onFiltroChange = { filtro = it },
                        monitoreos = monitoreosState.monitoreos,
                        eliminandoId = eliminandoId.takeIf { plantacionesState.procesando },
                        onPlantacionClick = { plantacionId -> navController.navigate("plantacion/$plantacionId") },
                        onPlantacionEliminar = { plantacionAEliminar = it }
                    )
                }
            }
        }
    }

    if (mostrarDialogoEditar && terreno != null) {
        EditarTerrenoDialog(
            terreno = terreno,
            procesando = terrenosState.procesando,
            onDismiss = { mostrarDialogoEditar = false },
            onGuardar = { nombre, hectareas ->
                terrenoViewModel.editarTerreno(terreno.terreno_id, nombre, hectareas) { actualizado ->
                    // El nombre del terreno viaja desnormalizado en plantaciones y monitoreos:
                    // sin esto, las pantallas hijas seguirían mostrando el nombre viejo.
                    plantacionesViewModel.renombrarTerreno(actualizado.terreno_id, actualizado.terreno_nombre)
                    monitoreosViewModel.renombrarTerreno(actualizado.terreno_id, actualizado.terreno_nombre)
                    mostrarDialogoEditar = false
                }
            }
        )
    }

    if (mostrarDialogoEliminar && terreno != null) {
        DialogoEliminarTerreno(
            terreno = terreno,
            onConfirmar = {
                mostrarDialogoEliminar = false
                eliminarTerrenoEnCascada(
                    terreno.terreno_id, terrenoViewModel, plantacionesViewModel, monitoreosViewModel, onBack
                )
            },
            onCancelar = { mostrarDialogoEliminar = false }
        )
    }

    plantacionAEliminar?.let { plantacion ->
        DialogoEliminarPlantacion(
            plantacion = plantacion,
            onConfirmar = {
                plantacionAEliminar = null
                eliminandoId = plantacion.plantacion_id
                eliminarPlantacionEnCascada(plantacion.plantacion_id, plantacionesViewModel, monitoreosViewModel) {
                    scope.launch { snackbarHostState.showSnackbar("Cultivo eliminado") }
                }
            },
            onCancelar = { plantacionAEliminar = null }
        )
    }
}

// ── Pestaña "Información" ────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun InformacionTerrenoTab(
    terreno: TerrenoResponse?,
    plantacionesDelTerreno: List<PlantacionesResponse>,
    monitoreosDelTerreno: List<MonitoreoResponse>
) {
    val context = LocalContext.current
    // Un monitoreo finalizado conserva congelado su último nivel de alerta: contarlo acá pintaría
    // el terreno con un estado que ya no existe.
    val monitoreosActivos = monitoreosDelTerreno.filter { it.activo }
    val finalizados = monitoreosDelTerreno.size - monitoreosActivos.size
    val sanos = monitoreosActivos.count { nivelAlertaEfectivo(it) == 0 }
    val atencion = monitoreosActivos.count { nivelAlertaEfectivo(it) == 1 }
    val criticos = monitoreosActivos.count { nivelAlertaEfectivo(it) >= 2 }
    val esperando = monitoreosActivos.count { esperandoBiofix(it) }
    val activas = plantacionesDelTerreno.count { it.activa }
    val activosAnimado = contadorAnimado(monitoreosActivos.size)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnilloSegmentado(
                segmentos = listOf(
                    sanos to estiloDeNivel(0).color,
                    atencion to estiloDeNivel(1).color,
                    criticos to estiloDeNivel(2).color,
                    esperando to estiloEsperandoBiofix().color.copy(alpha = 0.45f)
                ),
                total = monitoreosActivos.size,
                modifier = Modifier.size(104.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$activosAnimado", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = PlagOutColors.TextMain)
                    Text(
                        if (monitoreosActivos.size == 1) "monitoreo\nactivo" else "monitoreos\nactivos",
                        fontSize = 10.sp,
                        lineHeight = 11.sp,
                        textAlign = TextAlign.Center,
                        color = PlagOutColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LeyendaEstadoTerreno(estiloDeNivel(0), sanos)
                LeyendaEstadoTerreno(estiloDeNivel(1), atencion)
                LeyendaEstadoTerreno(estiloDeNivel(2), criticos)
                LeyendaEstadoTerreno(estiloEsperandoBiofix(), esperando)
                // Fuera del anillo a propósito: son historial, no estado actual.
                if (finalizados > 0) {
                    HorizontalDivider(color = PlagOutColors.Divider, modifier = Modifier.width(150.dp))
                    LeyendaEstadoTerreno(estiloFinalizado(), finalizados)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Surface(
            color = PlagOutColors.Surface,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                EstadisticaCompacta("Hectáreas", terreno?.let { "${it.terreno_area.toInt()}" } ?: "—", Modifier.weight(1f))
                SeparadorVertical()
                EstadisticaCompacta("Cultivos", "${plantacionesDelTerreno.size}", Modifier.weight(1f))
                SeparadorVertical()
                EstadisticaCompacta("Cultivos activos", "$activas", Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            onClick = { terreno?.let { abrirUbicacionEnMapa(context, it) } },
            color = PlagOutColors.Surface,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).background(PlagOutColors.Forest.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = PlagOutColors.Forest, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ubicación", fontSize = 12.sp, color = PlagOutColors.TextSecondary, fontWeight = FontWeight.Medium)
                    Text(
                        terreno?.let { "${"%.4f".format(it.terreno_latitud)}, ${"%.4f".format(it.terreno_longitud)}" } ?: "—",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain
                    )
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = PlagOutColors.TextSecondary, modifier = Modifier.size(18.dp))
            }
        }

        if (monitoreosDelTerreno.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            EtiquetaInfo(Icons.AutoMirrored.Outlined.HelpOutline, "Todavía no hay monitoreos en este terreno", PlagOutColors.RiskUnknown)
        } else if (monitoreosActivos.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            EtiquetaInfo(
                Icons.Filled.Flag,
                if (finalizados == 1) "El único monitoreo de este terreno está finalizado"
                else "Los $finalizados monitoreos de este terreno están finalizados",
                PlagOutColors.Bark
            )
        }

        Spacer(Modifier.height(16.dp))
    }

}

@Composable
private fun EditarTerrenoDialog(
    terreno: TerrenoResponse,
    procesando: Boolean,
    onDismiss: () -> Unit,
    onGuardar: (nombre: String, hectareas: String) -> Unit
) {
    // Números reales, igual que en el alta del terreno
    val regex = remember { Regex("^\\d*\\.?\\d*$") }

    var nombre by remember(terreno.terreno_id) { mutableStateOf(terreno.terreno_nombre) }
    var hectareas by remember(terreno.terreno_id) { mutableStateOf(formatearHectareas(terreno.terreno_area)) }

    val area = hectareas.trim().toDoubleOrNull()
    val formularioValido = nombre.trim().isNotEmpty() && area != null && area > 0.0
    val huboCambios = nombre.trim() != terreno.terreno_nombre ||
        area?.toFloat() != terreno.terreno_area

    val nombreInvalido = nombre.isNotEmpty() && nombre.isBlank()
    val hectareasInvalidas = hectareas.isNotEmpty() && (area == null || area <= 0.0)
    val puedeGuardar = formularioValido && huboCambios && !procesando

    MarcoDialogo(tag = "dialogEditarTerreno", onDismissRequest = { if (!procesando) onDismiss() }) { rebote ->
        InsigniaAccion(Icons.Outlined.Edit, PlagOutColors.Forest, rebote)
        Spacer(Modifier.height(16.dp))
        Text(
            "Editar terreno",
            style = MaterialTheme.typography.titleLarge,
            color = PlagOutColors.TextMain,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Cambiá el nombre o la superficie de ${terreno.terreno_nombre}.",
            style = MaterialTheme.typography.bodyMedium,
            color = PlagOutColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del terreno") },
            leadingIcon = { Icon(Icons.Outlined.Terrain, contentDescription = null, tint = PlagOutColors.Forest) },
            singleLine = true,
            enabled = !procesando,
            isError = nombreInvalido,
            supportingText = if (nombreInvalido) {
                { Text("Escribí un nombre") }
            } else null,
            shape = RoundedCornerShape(16.dp),
            colors = camposColors(),
            modifier = Modifier.fillMaxWidth().testTag("txtEditarNombre")
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = hectareas,
            onValueChange = { if (it.matches(regex)) hectareas = it },
            label = { Text("Superficie") },
            leadingIcon = { Icon(Icons.Filled.SquareFoot, contentDescription = null, tint = PlagOutColors.Forest) },
            trailingIcon = { Text("ha", color = PlagOutColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(end = 12.dp)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            enabled = !procesando,
            isError = hectareasInvalidas,
            supportingText = if (hectareasInvalidas) {
                { Text("Tiene que ser mayor a 0") }
            } else null,
            shape = RoundedCornerShape(16.dp),
            colors = camposColors(),
            modifier = Modifier.fillMaxWidth().testTag("txtEditarHectareas")
        )

        if (nombre.trim() != terreno.terreno_nombre) {
            Spacer(Modifier.height(12.dp))
            // El nombre viaja desnormalizado: conviene avisar que se actualiza en todos lados.
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PlagOutColors.Leaf.copy(alpha = 0.09f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = PlagOutColors.Leaf, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "El nuevo nombre se verá también en sus cultivos y monitoreos.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = PlagOutColors.TextMain
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !procesando,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, PlagOutColors.Divider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PlagOutColors.TextMain),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("Cancelar", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onGuardar(nombre, hectareas) },
                enabled = puedeGuardar,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlagOutColors.Forest,
                    contentColor = PlagOutColors.TextOnDark
                ),
                modifier = Modifier.weight(1f).height(50.dp).testTag("btnConfirmarEditarTerreno")
            ) {
                if (procesando) {
                    CircularProgressIndicator(color = PlagOutColors.TextOnDark, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatearHectareas(area: Float): String =
    if (area % 1f == 0f) area.toInt().toString() else area.toString()

@Composable
private fun LeyendaEstadoTerreno(estilo: NivelEstilo, cantidad: Int) {
    val valor = contadorAnimado(cantidad)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(estilo.icono, contentDescription = null, tint = estilo.color, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            estilo.etiqueta,
            color = PlagOutColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.width(104.dp)
        )
        Text("$valor", color = PlagOutColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

private fun abrirUbicacionEnMapa(context: Context, terreno: TerrenoResponse) {
    try {
        val uri = Uri.parse(
            "geo:${terreno.terreno_latitud},${terreno.terreno_longitud}" +
                "?q=${terreno.terreno_latitud},${terreno.terreno_longitud}(${Uri.encode(terreno.terreno_nombre)})"
        )
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: Exception) {
        Log.e("TERRENO_DETALLE", "No se pudo abrir el mapa: ${e.message}")
    }
}

// ── Pestaña "Cultivos" ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PlantacionesTab(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    plantacionesDelTerreno: List<PlantacionesResponse>,
    filtradas: List<PlantacionesResponse>,
    filtro: Int,
    onFiltroChange: (Int) -> Unit,
    monitoreos: List<MonitoreoResponse>,
    eliminandoId: Int?,
    onPlantacionClick: (Int) -> Unit,
    onPlantacionEliminar: (PlantacionesResponse) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val opciones = remember(plantacionesDelTerreno) {
                listOf(
                    OpcionFiltro(FILTRO_TODAS, "Todos", plantacionesDelTerreno.size),
                    OpcionFiltro(FILTRO_ACTIVAS, "Activos", plantacionesDelTerreno.count { it.activa }, colorIcono = PlagOutColors.Leaf),
                    OpcionFiltro(FILTRO_FINALIZADAS, "Finalizados", plantacionesDelTerreno.count { !it.activa }, colorIcono = PlagOutColors.Bark)
                )
            }
            FiltroChipsRow(opciones = opciones, seleccionado = filtro, onSeleccion = onFiltroChange)

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = when {
                        plantacionesDelTerreno.isEmpty() -> "vacio"
                        filtradas.isEmpty() -> "sin-resultados"
                        else -> "lista-$filtro"
                    },
                    transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
                    label = "plantacionesContent"
                ) { target ->
                    when (target) {
                        // Box con scroll para que el gesto de pull-to-refresh también funcione sin lista
                        "vacio" -> Box(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            EstadoVacioFlotante(
                                icono = Icons.Outlined.Grass,
                                titulo = "No hay cultivos",
                                subtitulo = "Registrá un cultivo en este terreno para empezar a monitorear plagas."
                            )
                        }
                        "sin-resultados" -> Box(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            EstadoSinResultados(subtitulo = "No hay cultivos en este estado.")
                        }
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            itemsIndexed(filtradas, key = { _, p -> p.plantacion_id }) { index, plantacion ->
                                StaggeredAppear(index = index) {
                                    DeslizableParaEliminar(
                                        procesando = plantacion.plantacion_id == eliminandoId,
                                        onEliminar = { onPlantacionEliminar(plantacion) }
                                    ) {
                                        PlantacionCard(
                                            plantacion = plantacion,
                                            monitoreos = monitoreos.filter { it.plantacion_id == plantacion.plantacion_id && it.activo },
                                            onClick = { onPlantacionClick(plantacion.plantacion_id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Card de cultivo ───────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PlantacionCard(
    plantacion: PlantacionesResponse,
    monitoreos: List<MonitoreoResponse>,
    onClick: () -> Unit = {}
) {
    // Solo los monitoreos en curso definen el estado: uno finalizado no puede seguir tiñendo la card.
    val nivelMax = monitoreos.filter { it.activo }.maxOfOrNull { nivelAlertaEfectivo(it) } ?: -1
    val estadoColor = if (plantacion.activa) estiloDeNivel(nivelMax).color else PlagOutColors.RiskUnknown

    val interactionSource = remember { MutableInteractionSource() }
    val escala = rememberPressScale(interactionSource)

    val diasDesdeSiembra = ChronoUnit.DAYS.between(plantacion.fecha_siembra, LocalDate.now()).toInt()

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = PlagOutColors.Surface,
        shape = FormaTarjeta,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = escala; scaleY = escala }
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(estadoColor)
            )

            Column(Modifier.padding(start = 17.dp, end = 18.dp, top = 16.dp, bottom = 14.dp).fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Surface(
                            color = (if (plantacion.activa) PlagOutColors.Leaf else PlagOutColors.Bark).copy(alpha = 0.12f),
                            shape = CircleShape
                        ) {
                            Text(
                                if (plantacion.activa) "CULTIVO ACTIVO" else "CULTIVO FINALIZADO",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp,
                                color = if (plantacion.activa) PlagOutColors.Leaf else PlagOutColors.Bark
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            plantacion.cultivo_nombre,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlagOutColors.TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            plantacion.cultivo_nombre_cientifico,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = PlagOutColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val progresoMax = monitoreos.flatMap { ciclosActivos(it) }.maxOfOrNull { it.progreso }
                    if (progresoMax != null) {
                        Spacer(Modifier.width(14.dp))
                        AnilloProgreso(
                            progreso = progresoMax,
                            color = estiloDeNivel(nivelMax).color,
                            tamano = 58.dp,
                            grosor = 6.dp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PlagOutColors.Cream, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = PlagOutColors.Bark, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Sembrado el ${plantacion.fecha_siembra.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es")))}",
                        fontSize = 12.sp,
                        color = PlagOutColors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (diasDesdeSiembra >= 0) "$diasDesdeSiembra días" else "—",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.Forest
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (monitoreos.isEmpty()) {
                    EtiquetaInfo(Icons.Outlined.Grass, "Sin plagas bajo seguimiento", PlagOutColors.RiskUnknown)
                } else {
                    Text(
                        "Monitoreos activos (${monitoreos.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        monitoreos.forEach { monitoreo ->
                            PlagaMiniFila(monitoreo)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlagaMiniFila(monitoreo: MonitoreoResponse) {
    val esperando = esperandoBiofix(monitoreo)
    val estilo = if (esperando) estiloEsperandoBiofix() else estiloDeNivel(nivelAlertaEfectivo(monitoreo))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlagOutColors.Cream, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(monitoreo.plaga_nombre, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
            Text(
                when (val ciclos = ciclosActivos(monitoreo).size) {
                    0 -> "Sin ciclos activos"
                    1 -> "1 ciclo activo"
                    else -> "$ciclos ciclos activos"
                },
                fontSize = 11.sp,
                color = PlagOutColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.width(8.dp))
        SelloDeNivel(estilo, pulsante = !esperando && nivelAlertaEfectivo(monitoreo) >= 2)
    }
}
