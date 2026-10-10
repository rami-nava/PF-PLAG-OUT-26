package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.EstadoSinResultados
import com.example.plag_out.ui.theme.EstadoVacioFlotante
import com.example.plag_out.ui.theme.EtiquetaInfo
import com.example.plag_out.ui.theme.FiltroChipsRow
import com.example.plag_out.ui.theme.OpcionFiltro
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SkeletonCargando
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.rememberPressScale

// ── Lista ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPlagasScreen(
    viewModel: AdminPlagasViewModel,
    onNuevaPlaga: () -> Unit,
    onEditarPlaga: (Int) -> Unit,
    onSinPermiso: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(state.sinPermiso) { if (state.sinPermiso) onSinPermiso() }

    val visibles = remember(state.plagas, state.busqueda, state.filtro) {
        filtrarPlagas(state.plagas, state.busqueda, state.filtro)
    }
    val activas = state.plagas.count { it.estaActiva }

    Scaffold(
        containerColor = PlagOutColors.Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            var mostrado by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { mostrado = true }
            val escala by animateFloatAsState(
                targetValue = if (mostrado) 1f else 0f,
                animationSpec = tween(360, easing = FastOutSlowInEasing),
                label = "escalaFabPlaga"
            )
            if (state.cargado) {
                ExtendedFloatingActionButton(
                    onClick = onNuevaPlaga,
                    containerColor = PlagOutColors.Sun,
                    contentColor = PlagOutColors.ForestDark,
                    shape = RoundedCornerShape(18.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nueva plaga", fontWeight = FontWeight.Bold) },
                    modifier = Modifier
                        .graphicsLayer { scaleX = escala; scaleY = escala }
                        .testTag("btnNuevaPlaga")
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            EncabezadoSeccionAdmin(
                titulo = "Catálogo de plagas",
                subtitulo = if (state.cargado) {
                    "$activas activas · ${state.plagas.size - activas} dadas de baja"
                } else {
                    "Parámetros que usa el cálculo de GDD"
                }
            )
            BuscadorAdmin(
                valor = state.busqueda,
                onCambio = viewModel::buscar,
                placeholder = "Buscar por nombre o nombre científico",
                tag = "txtBuscarPlaga"
            )
            FiltroChipsRow(
                opciones = listOf(
                    OpcionFiltro(FiltroPlagas.TODAS.ordinal, "Todas", state.plagas.size),
                    OpcionFiltro(FiltroPlagas.ACTIVAS.ordinal, "Activas", activas),
                    OpcionFiltro(FiltroPlagas.INACTIVAS.ordinal, "De baja", state.plagas.size - activas)
                ),
                seleccionado = state.filtro.ordinal,
                onSeleccion = { viewModel.cambiarFiltro(FiltroPlagas.entries[it]) },
                modifier = Modifier.padding(top = 0.dp)
            )

            BannerErrorAdmin(
                mensaje = state.errorAccion,
                onCerrar = viewModel::descartarErrorAccion,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            when {
                state.isLoading && !state.cargado -> SkeletonCargando(alturaTarjeta = 112.dp, cantidad = 4)
                state.error != null && !state.cargado -> EstadoErrorAdmin(state.error!!) { viewModel.cargar() }
                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refrescar,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        state.plagas.isEmpty() -> EstadoVacioFlotante(
                            icono = Icons.Outlined.BugReport,
                            titulo = "Todavía no hay plagas",
                            subtitulo = "Agregá la primera para que los productores puedan monitorearla."
                        )
                        visibles.isEmpty() -> EstadoSinResultados(
                            subtitulo = "Ninguna plaga coincide con la búsqueda o el filtro."
                        )
                        else -> LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("listaPlagasAdmin")
                        ) {
                            itemsIndexed(visibles, key = { _, plaga -> plaga.id }) { index, plaga ->
                                StaggeredAppear(index = index, modifier = Modifier.animateItem()) {
                                    TarjetaPlagaAdmin(plaga, cultivos = state.cultivos) { onEditarPlaga(plaga.id) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaPlagaAdmin(plaga: PlagaAdmin, cultivos: List<CultivoResponse>, onClick: () -> Unit) {
    val interaccion = remember { MutableInteractionSource() }
    val escala = rememberPressScale(interaccion)
    val acento = if (plaga.estaActiva) PlagOutColors.Leaf else PlagOutColors.RiskUnknown
    val nombresCultivos = plaga.cultivos_afectados.orEmpty()
        .mapNotNull { id -> cultivos.firstOrNull { it.id == id }?.nombre }

    Surface(
        onClick = onClick,
        interactionSource = interaccion,
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .testTag("tarjetaPlaga_${plaga.id}")
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .background(acento.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.BugReport, contentDescription = null, tint = acento, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(
                Modifier
                    .weight(1f)
                    .alpha(if (plaga.estaActiva) 1f else 0.7f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        plaga.nombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!plaga.estaActiva) {
                        Spacer(Modifier.width(8.dp))
                        PastillaEstado("DE BAJA", PlagOutColors.RiskUnknown)
                    }
                }
                Text(
                    plaga.nombre_cientifico,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = PlagOutColors.TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    EtiquetaInfo(Icons.Outlined.Thermostat, rangoTermico(plaga), PlagOutColors.Terracotta)
                    plaga.gdd_eclosion?.let {
                        EtiquetaInfo(Icons.Outlined.Egg, "Eclosión ${formatearNumero(it)} GDD", PlagOutColors.Bark)
                    }
                    plaga.gdd_generacion?.let {
                        EtiquetaInfo(Icons.Outlined.Loop, "Generación ${formatearNumero(it)} GDD", PlagOutColors.Bark)
                    }
                    if (nombresCultivos.isNotEmpty()) {
                        EtiquetaInfo(Icons.Outlined.Grass, nombresCultivos.joinToString(", "), PlagOutColors.Leaf)
                    }
                }
                val monitoreos = plaga.monitoreos_activos
                if (monitoreos != null && monitoreos > 0) {
                    Text(
                        "$monitoreos ${if (monitoreos == 1) "monitoreo activo" else "monitoreos activos"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlagOutColors.Forest,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = PlagOutColors.TextSecondary
            )
        }
    }
}

private fun rangoTermico(plaga: PlagaAdmin): String {
    val base = plaga.temp_base?.let(::formatearNumero) ?: "?"
    val max = plaga.temp_max?.let(::formatearNumero) ?: "?"
    return "$base – $max °C"
}

// ── Formulario de alta / edición ────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlagaFormScreen(
    viewModel: AdminPlagasViewModel,
    plagaId: Int?,
    onBack: () -> Unit,
    onGuardado: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val original = remember(plagaId, state.plagas) { plagaId?.let { viewModel.plaga(it) } }

    // Se llegó a una plaga que no está en la lista (p. ej. el proceso se recreó): volver.
    if (plagaId != null && original == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var form by remember(plagaId) { mutableStateOf(original?.let { FormularioPlaga.desde(it) } ?: FormularioPlaga()) }
    var intentoGuardar by remember { mutableStateOf(false) }
    var confirmarBaja by remember { mutableStateOf(false) }
    val errores = remember(form) { validarPlaga(form) }
    fun errorDe(campo: CampoPlaga) = if (intentoGuardar) errores[campo] else null

    LaunchedEffect(Unit) { viewModel.descartarErrorAccion() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlagOutColors.Cream)
            .imePadding()
    ) {
        HeaderDetalleAdmin(
            etiqueta = if (original == null) "Catálogo de plagas" else "Editar plaga",
            titulo = if (original == null) "Nueva plaga" else original.nombre,
            onBack = onBack
        ) {
            if (original != null && !original.estaActiva) {
                Spacer(Modifier.height(6.dp))
                PastillaEstado("DADA DE BAJA", PlagOutColors.Sun)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val monitoreos = original?.monitoreos_activos ?: 0
            if (monitoreos > 0) {
                AvisoImpacto(
                    "Esta plaga tiene $monitoreos ${if (monitoreos == 1) "monitoreo activo" else "monitoreos activos"}. " +
                        "Los cambios en los parámetros se aplican desde el próximo cálculo diario, sin recalcular lo acumulado."
                )
            }

            SeccionFormulario(titulo = "Identificación", subtitulo = "Cómo la ven los productores al elegirla.") {
                CampoFormularioAdmin(
                    valor = form.nombre,
                    onCambio = { form = form.copy(nombre = it) },
                    etiqueta = "Nombre común",
                    placeholder = "Ej: Carpocapsa",
                    error = errorDe(CampoPlaga.NOMBRE),
                    icono = Icons.Outlined.BugReport,
                    tag = "txtNombrePlaga"
                )
                Spacer(Modifier.height(14.dp))
                CampoFormularioAdmin(
                    valor = form.nombreCientifico,
                    onCambio = { form = form.copy(nombreCientifico = it) },
                    etiqueta = "Nombre científico",
                    placeholder = "Ej: Cydia pomonella",
                    error = errorDe(CampoPlaga.NOMBRE_CIENTIFICO),
                    icono = Icons.Outlined.Science,
                    tag = "txtNombreCientifico"
                )
            }

            SeccionFormulario(
                titulo = "Parámetros térmicos",
                subtitulo = "Umbrales de temperatura entre los que la plaga acumula grados-día."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    CampoFormularioAdmin(
                        valor = form.tempBase,
                        onCambio = { nuevo -> if (esNumeroPlaga(nuevo)) form = form.copy(tempBase = nuevo) },
                        etiqueta = "Temp. base",
                        unidad = "°C",
                        error = errorDe(CampoPlaga.TEMP_BASE),
                        tag = "txtTempBase",
                        placeholder = "0",
                        teclado = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    CampoFormularioAdmin(
                        valor = form.tempMax,
                        onCambio = { nuevo -> if (esNumeroPlaga(nuevo)) form = form.copy(tempMax = nuevo) },
                        etiqueta = "Temp. máxima",
                        unidad = "°C",
                        error = errorDe(CampoPlaga.TEMP_MAX),
                        tag = "txtTempMax",
                        placeholder = "0",
                        teclado = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SeccionFormulario(
                titulo = "Grados-día del ciclo",
                subtitulo = "GDD acumulados desde el biofix hasta cada evento biológico."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    CampoFormularioAdmin(
                        valor = form.gddEclosion,
                        onCambio = { nuevo -> if (esNumeroPlaga(nuevo)) form = form.copy(gddEclosion = nuevo) },
                        etiqueta = "Eclosión",
                        unidad = "GDD",
                        error = errorDe(CampoPlaga.GDD_ECLOSION),
                        tag = "txtGddEclosion",
                        placeholder = "0",
                        teclado = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    CampoFormularioAdmin(
                        valor = form.gddGeneracion,
                        onCambio = { nuevo -> if (esNumeroPlaga(nuevo)) form = form.copy(gddGeneracion = nuevo) },
                        etiqueta = "Generación",
                        unidad = "GDD",
                        error = errorDe(CampoPlaga.GDD_GENERACION),
                        tag = "txtGddGeneracion",
                        placeholder = "0",
                        teclado = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SeccionFormulario(
                titulo = "Cultivos afectados",
                subtitulo = "Solo se ofrece para monitorear en estos cultivos."
            ) {
                if (state.cultivos.isEmpty()) {
                    Text("No se pudieron cargar los cultivos.", fontSize = 13.sp, color = PlagOutColors.TextSecondary)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.cultivos.forEach { cultivo ->
                            val elegido = cultivo.id in form.cultivos
                            FilterChip(
                                selected = elegido,
                                onClick = {
                                    form = form.copy(
                                        cultivos = if (elegido) form.cultivos - cultivo.id else form.cultivos + cultivo.id
                                    )
                                },
                                label = { Text(cultivo.nombre) },
                                leadingIcon = if (elegido) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PlagOutColors.Forest,
                                    selectedLabelColor = PlagOutColors.TextOnDark,
                                    selectedLeadingIconColor = PlagOutColors.TextOnDark,
                                    labelColor = PlagOutColors.TextMain
                                ),
                                modifier = Modifier.testTag("chipCultivo_${cultivo.id}")
                            )
                        }
                    }
                }
                errorDe(CampoPlaga.CULTIVOS)?.let {
                    Text(it, color = PlagOutColors.RiskDanger, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }

            if (original != null) {
                AccionBajaPlaga(
                    activa = original.estaActiva,
                    habilitada = !state.guardando,
                    onClick = {
                        if (original.estaActiva) confirmarBaja = true
                        else viewModel.reactivar(original.id)
                    }
                )
            }
        }

        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
            BannerErrorAdmin(
                mensaje = state.errorAccion,
                onCerrar = viewModel::descartarErrorAccion,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Button(
                onClick = {
                    intentoGuardar = true
                    viewModel.guardar(original, form, onGuardado)
                },
                enabled = !state.guardando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btnGuardarPlaga"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlagOutColors.Forest,
                    disabledContainerColor = PlagOutColors.Forest.copy(alpha = 0.4f),
                    contentColor = PlagOutColors.TextOnDark,
                    disabledContentColor = PlagOutColors.TextOnDark.copy(alpha = 0.6f)
                )
            ) {
                if (state.guardando) {
                    CircularProgressIndicator(color = PlagOutColors.TextOnDark, modifier = Modifier.size(22.dp))
                } else {
                    Text(if (original == null) "Crear plaga" else "Guardar cambios", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (confirmarBaja && original != null) {
        val monitoreos = original.monitoreos_activos ?: 0
        DialogoConfirmacionAdmin(
            icono = Icons.Outlined.Archive,
            titulo = "¿Dar de baja la plaga?",
            iconoFicha = Icons.Outlined.BugReport,
            nombre = original.nombre,
            detalle = original.nombre_cientifico,
            consecuencias = listOf(
                "Deja de ofrecerse para monitoreos nuevos",
                "Deja de ofrecerse para reportes nuevos"
            ),
            tranquilidad = (if (monitoreos > 0) {
                "${if (monitoreos == 1) "El monitoreo activo sigue" else "Los ${monitoreos} monitoreos activos siguen"} funcionando. "
            } else "") + "Podés reactivarla cuando quieras.",
            textoConfirmar = "Dar de baja",
            peligro = true,
            onConfirmar = {
                confirmarBaja = false
                viewModel.desactivar(original.id)
            },
            onDismiss = { confirmarBaja = false }
        )
    }
}

@Composable
private fun SeccionFormulario(titulo: String, subtitulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
            Text(
                subtitulo,
                fontSize = 12.sp,
                color = PlagOutColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            contenido()
        }
    }
}

// Solo dígitos, signo y un separador decimal: la temperatura base puede ser negativa.
private fun esNumeroPlaga(texto: String) = texto.all { it.isDigit() || it in "-.," }

@Composable
private fun AvisoImpacto(texto: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlagOutColors.Sun.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("avisoImpactoPlaga"),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = PlagOutColors.Bark, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(texto, fontSize = 13.sp, color = PlagOutColors.TextMain)
    }
}

@Composable
private fun AccionBajaPlaga(activa: Boolean, habilitada: Boolean, onClick: () -> Unit) {
    val color = if (activa) PlagOutColors.RiskDanger else PlagOutColors.Forest
    OutlinedButton(
        onClick = onClick,
        enabled = habilitada,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag(if (activa) "btnDarDeBajaPlaga" else "btnReactivarPlaga")
    ) {
        Icon(if (activa) Icons.Outlined.Archive else Icons.Outlined.Unarchive, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (activa) "Dar de baja" else "Reactivar plaga", fontWeight = FontWeight.Bold)
    }
}
