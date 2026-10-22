package com.example.plag_out

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"))

private fun hoyEnArgentina(): LocalDate = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))

private fun fechaCorta(iso: String?): String =
    iso?.let { runCatching { LocalDate.parse(it).format(FORMATO_FECHA) }.getOrNull() } ?: "—"

/** Pestaña del detalle del monitoreo: los tratamientos y su eficacia. */
@Composable
fun TratamientosTab(monitoreo: MonitoreoResponse, viewModel: TratamientosViewModel) {
    val state by viewModel.state.collectAsState()
    var formulario by remember { mutableStateOf<Pair<ItemTratamiento?, FormularioTratamiento>?>(null) }
    var aEliminar by remember { mutableStateOf<ItemTratamiento?>(null) }
    val hoy = remember { hoyEnArgentina() }

    LaunchedEffect(monitoreo.monitoreo_id) { viewModel.cargar(monitoreo.monitoreo_id) }

    fun avisar(resultado: EnvioTratamiento?) {
        when (resultado) {
            is EnvioTratamiento.Enviado -> Avisos.exito("Tratamiento guardado.")
            EnvioTratamiento.Reintentar -> Avisos.exito("Sin conexión: quedó guardado y se envía solo al volver la señal.")
            is EnvioTratamiento.Rechazado -> Avisos.error(resultado.mensaje)
            null -> Unit
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().testTag("listaTratamientos")
    ) {
        item {
            Column {
                Text("Tratamientos", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                Text(
                    "Contá la plaga antes de aplicar y unos días después. Si dejás una franja sin tratar (testigo) " +
                        "y la contás igual, se calcula la eficacia real; sin testigo, solo la reducción observada.",
                    fontSize = 12.sp,
                    color = PlagOutColors.TextSecondary,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (monitoreo.activo) {
            item {
                Button(
                    onClick = { formulario = null to FormularioTratamiento.nuevo(hoy) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PlagOutColors.Forest, contentColor = PlagOutColors.TextOnDark),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btnRegistrarTratamiento")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Registrar tratamiento", fontWeight = FontWeight.Bold)
                }
            }
        }
        state.error?.takeIf { state.tratamientos.isEmpty() }?.let { error ->
            item { Text(error, fontSize = 12.sp, color = PlagOutColors.TextSecondary) }
        }
        if (state.items.isEmpty() && !state.cargando) {
            item {
                Text(
                    "Todavía no hay tratamientos en este monitoreo.",
                    fontSize = 13.sp,
                    color = PlagOutColors.TextSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
        items(state.items, key = { it.clave }) { item ->
            TarjetaTratamiento(
                item = item,
                onCompletar = {
                    formulario = item to when (item) {
                        is ItemTratamiento.Enviado -> FormularioTratamiento.desde(item.tratamiento)
                        is ItemTratamiento.SinEnviar -> formularioDeAlta(item.alta)
                    }.let { it.copy(conDespues = true, fechaConteoDespues = it.fechaConteoDespues ?: hoy) }
                },
                onEditar = {
                    formulario = item to when (item) {
                        is ItemTratamiento.Enviado -> FormularioTratamiento.desde(item.tratamiento)
                        is ItemTratamiento.SinEnviar -> formularioDeAlta(item.alta)
                    }
                },
                onEliminar = { aEliminar = item }
            )
        }
    }

    formulario?.let { (item, inicial) ->
        DialogoTratamiento(
            inicial = inicial,
            esNuevo = item == null,
            inicioMonitoreo = monitoreo.fecha_inicio,
            hoy = hoy,
            guardando = state.guardando,
            onCancelar = { formulario = null },
            onGuardar = { form ->
                formulario = null
                if (item == null) viewModel.registrar(form, ::avisar) else viewModel.editar(item, form, ::avisar)
            }
        )
    }

    aEliminar?.let { item ->
        val sinEnviar = item is ItemTratamiento.SinEnviar
        DialogoConfirmarAccion(
            titulo = if (sinEnviar) "¿Descartar este tratamiento?" else "¿Eliminar este tratamiento?",
            colorAccion = PlagOutColors.RiskDanger,
            iconoAccion = Icons.Outlined.ErrorOutline,
            textoConfirmar = if (sinEnviar) "Descartar" else "Eliminar",
            icono = Icons.Outlined.Science,
            nombre = when (item) {
                is ItemTratamiento.Enviado -> item.tratamiento.producto
                is ItemTratamiento.SinEnviar -> item.alta.producto
            },
            detalle = null,
            encabezadoConsecuencias = "AL HACERLO",
            consecuencias = listOf(
                if (sinEnviar) "Se borra del teléfono: todavía no se había enviado." else "Se borran sus conteos y su eficacia."
            ),
            aviso = "No se puede deshacer.",
            tagDialogo = "dialogEliminarTratamiento",
            tagConfirmar = "btnConfirmarEliminarTratamiento",
            tagCancelar = "btnCancelarEliminarTratamiento",
            onConfirmar = {
                aEliminar = null
                viewModel.eliminar(item) { error -> if (error != null) Avisos.error(error) else Avisos.exito("Tratamiento eliminado.") }
            },
            onCancelar = { aEliminar = null }
        )
    }
}

internal fun formularioDeAlta(alta: TratamientoRequest) = FormularioTratamiento.desde(
    TratamientoResponse(
        id = 0, monitoreo_id = 0, fecha_aplicacion = alta.fecha_aplicacion, producto = alta.producto,
        unidad_conteo = alta.unidad_conteo, conteo_antes = alta.conteo_antes, fecha_conteo_antes = alta.fecha_conteo_antes,
        conteo_despues = alta.conteo_despues, fecha_conteo_despues = alta.fecha_conteo_despues,
        testigo_antes = alta.testigo_antes, testigo_despues = alta.testigo_despues
    )
)

@Composable
private fun TarjetaTratamiento(
    item: ItemTratamiento,
    onCompletar: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    // Para mostrar, lo que espera envío se lee igual que lo enviado.
    val datos = when (item) {
        is ItemTratamiento.Enviado -> item.tratamiento
        is ItemTratamiento.SinEnviar -> TratamientoResponse(
            id = 0, monitoreo_id = item.fila.monitoreo_id, fecha_aplicacion = item.alta.fecha_aplicacion,
            producto = item.alta.producto, unidad_conteo = item.alta.unidad_conteo, conteo_antes = item.alta.conteo_antes,
            fecha_conteo_antes = item.alta.fecha_conteo_antes, conteo_despues = item.alta.conteo_despues,
            fecha_conteo_despues = item.alta.fecha_conteo_despues, testigo_antes = item.alta.testigo_antes,
            testigo_despues = item.alta.testigo_despues
        )
    }
    val unidad = UnidadConteo.de(datos.unidad_conteo)?.etiqueta.orEmpty()

    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, PlagOutColors.CreamDeep),
        modifier = Modifier.fillMaxWidth().testTag("tarjetaTratamiento")
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(datos.producto, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                    Text("Aplicado el ${fechaCorta(datos.fecha_aplicacion)}", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
                }
            }
            when (item) {
                is ItemTratamiento.SinEnviar -> if (item.requiereRevision) {
                    EstadoEnvio(Icons.Outlined.ErrorOutline, "No se pudo enviar: revisá los datos", PlagOutColors.RiskDanger)
                } else {
                    EstadoEnvio(Icons.Outlined.CloudOff, "Pendiente de envío", PlagOutColors.Bark)
                }
                is ItemTratamiento.Enviado -> if (item.edicionPendiente != null) {
                    EstadoEnvio(Icons.Outlined.CloudOff, "Cambios pendientes de envío", PlagOutColors.Bark)
                }
            }

            Spacer(Modifier.height(10.dp))
            FilaConteo("Antes", datos.conteo_antes, unidad, fechaCorta(datos.fecha_conteo_antes))
            datos.conteo_despues?.let { FilaConteo("Después", it, unidad, fechaCorta(datos.fecha_conteo_despues)) }
            datos.testigo_antes?.let { antes ->
                Text(
                    "Testigo: ${formatearConteo(antes)} antes" + (datos.testigo_despues?.let { " · ${formatearConteo(it)} después" } ?: ""),
                    fontSize = 12.sp,
                    color = PlagOutColors.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(10.dp))
            if (item is ItemTratamiento.Enviado && item.edicionPendiente == null) {
                val resultado = resultadoDeTratamiento(item.tratamiento)
                Text(resultado.titulo, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = PlagOutColors.Forest, modifier = Modifier.testTag("resultadoTratamiento"))
                resultado.detalle?.let { Text(it, fontSize = 11.sp, color = PlagOutColors.TextSecondary, lineHeight = 15.sp) }
            } else {
                Text("La cifra se calcula al enviarse.", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
            }

            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                if (datos.conteo_despues == null) {
                    TextButton(onClick = onCompletar, modifier = Modifier.testTag("btnConteoPosterior")) {
                        Text("Cargar conteo posterior", color = PlagOutColors.Forest, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = onEditar, modifier = Modifier.testTag("btnEditarTratamiento")) {
                        Text("Editar", color = PlagOutColors.Forest, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onEliminar, modifier = Modifier.testTag("btnEliminarTratamiento")) {
                    Text(if (item is ItemTratamiento.SinEnviar) "Descartar" else "Eliminar", color = PlagOutColors.RiskDanger)
                }
            }
        }
    }
}

@Composable
private fun EstadoEnvio(icono: ImageVector, texto: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 8.dp)
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("estadoEnvioTratamiento")
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun FilaConteo(etiqueta: String, valor: Double, unidad: String, fecha: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, fontSize = 13.sp, color = PlagOutColors.TextSecondary, modifier = Modifier.width(64.dp))
        Text("${formatearConteo(valor)} $unidad", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PlagOutColors.TextMain, modifier = Modifier.weight(1f))
        Text(fecha, fontSize = 11.sp, color = PlagOutColors.TextSecondary)
    }
}

// ── Formulario ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DialogoTratamiento(
    inicial: FormularioTratamiento,
    esNuevo: Boolean,
    inicioMonitoreo: LocalDate?,
    hoy: LocalDate,
    guardando: Boolean,
    onCancelar: () -> Unit,
    onGuardar: (FormularioTratamiento) -> Unit
) {
    var form by remember { mutableStateOf(inicial) }
    var intento by remember { mutableStateOf(false) }
    val errores = remember(form) { validarTratamiento(form, inicioMonitoreo, hoy) }
    fun error(campo: CampoTratamiento) = if (intento) errores[campo] else null
    fun numero(texto: String) = texto.all { it.isDigit() || it in ".," }

    MarcoDialogo(tag = "dialogTratamiento", onDismissRequest = onCancelar) { _ ->
        Text(
            if (esNuevo) "Registrar tratamiento" else "Tratamiento",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PlagOutColors.TextMain,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoFecha("Fecha de aplicación", form.fechaAplicacion, inicioMonitoreo, hoy, error(CampoTratamiento.FECHA_APLICACION), "fechaAplicacion") {
                form = form.copy(fechaAplicacion = it)
            }
            CampoFormularioAdmin(
                valor = form.producto,
                onCambio = { form = form.copy(producto = it.take(120)) },
                etiqueta = "Producto",
                placeholder = "Ej: Clorantraniliprole",
                error = error(CampoTratamiento.PRODUCTO),
                tag = "txtProductoTratamiento"
            )

            Text("Cómo contaste", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PlagOutColors.TextMain)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UnidadConteo.entries.forEach { unidad ->
                    FilterChip(
                        selected = form.unidad == unidad,
                        onClick = { form = form.copy(unidad = unidad) },
                        label = { Text("Individuos ${unidad.etiqueta}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PlagOutColors.Forest,
                            selectedLabelColor = PlagOutColors.TextOnDark
                        ),
                        modifier = Modifier.testTag("chipUnidad_${unidad.clave}")
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                CampoFormularioAdmin(
                    valor = form.conteoAntes,
                    onCambio = { if (numero(it)) form = form.copy(conteoAntes = it) },
                    etiqueta = "Conteo antes",
                    error = error(CampoTratamiento.CONTEO_ANTES),
                    tag = "txtConteoAntes",
                    placeholder = "0",
                    teclado = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                CampoFecha("Fecha", form.fechaConteoAntes, null, hoy, error(CampoTratamiento.FECHA_CONTEO_ANTES), "fechaConteoAntes", Modifier.weight(1f)) {
                    form = form.copy(fechaConteoAntes = it)
                }
            }

            FilaInterruptor(
                titulo = "Dejé una franja sin tratar (testigo)",
                detalle = "Contarla igual permite separar el efecto del producto de lo que la plaga cambió sola.",
                activo = form.conTestigo,
                tag = "swTestigo"
            ) { form = form.copy(conTestigo = it) }
            if (form.conTestigo) {
                CampoFormularioAdmin(
                    valor = form.testigoAntes,
                    onCambio = { if (numero(it)) form = form.copy(testigoAntes = it) },
                    etiqueta = "Testigo antes",
                    error = error(CampoTratamiento.TESTIGO_ANTES),
                    tag = "txtTestigoAntes",
                    placeholder = "0",
                    teclado = KeyboardType.Decimal
                )
            }

            FilaInterruptor(
                titulo = "Ya tengo el conteo posterior",
                detalle = "Si todavía no, lo cargás después desde la tarjeta del tratamiento.",
                activo = form.conDespues,
                tag = "swConteoPosterior"
            ) { activo -> form = form.copy(conDespues = activo, fechaConteoDespues = form.fechaConteoDespues ?: hoy) }
            if (form.conDespues) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    CampoFormularioAdmin(
                        valor = form.conteoDespues,
                        onCambio = { if (numero(it)) form = form.copy(conteoDespues = it) },
                        etiqueta = "Conteo después",
                        error = error(CampoTratamiento.CONTEO_DESPUES),
                        tag = "txtConteoDespues",
                        placeholder = "0",
                        teclado = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    CampoFecha("Fecha", form.fechaConteoDespues, form.fechaAplicacion?.plusDays(1), hoy, error(CampoTratamiento.FECHA_CONTEO_DESPUES), "fechaConteoDespues", Modifier.weight(1f)) {
                        form = form.copy(fechaConteoDespues = it)
                    }
                }
                if (form.conTestigo) {
                    CampoFormularioAdmin(
                        valor = form.testigoDespues,
                        onCambio = { if (numero(it)) form = form.copy(testigoDespues = it) },
                        etiqueta = "Testigo después (opcional)",
                        error = error(CampoTratamiento.TESTIGO_DESPUES),
                        tag = "txtTestigoDespues",
                        placeholder = "Vacío: solo reducción observada",
                        teclado = KeyboardType.Decimal
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCancelar, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                Text("Cancelar", color = PlagOutColors.Forest)
            }
            Button(
                onClick = {
                    intento = true
                    if (errores.isEmpty()) onGuardar(form)
                },
                enabled = !guardando,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PlagOutColors.Forest, contentColor = PlagOutColors.TextOnDark),
                modifier = Modifier.weight(1f).testTag("btnGuardarTratamiento")
            ) { Text("Guardar", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun FilaInterruptor(titulo: String, detalle: String, activo: Boolean, tag: String, onCambio: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onCambio(!activo) }.padding(vertical = 2.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PlagOutColors.TextMain)
            Text(detalle, fontSize = 11.sp, color = PlagOutColors.TextSecondary, lineHeight = 14.sp)
        }
        Switch(
            checked = activo,
            onCheckedChange = onCambio,
            colors = SwitchDefaults.colors(checkedTrackColor = PlagOutColors.Forest),
            modifier = Modifier.testTag(tag)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoFecha(
    etiqueta: String,
    fecha: LocalDate?,
    minima: LocalDate?,
    maxima: LocalDate,
    error: String?,
    tag: String,
    modifier: Modifier = Modifier,
    onElegir: (LocalDate) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(
            etiqueta,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (error != null) PlagOutColors.RiskDanger else PlagOutColors.TextMain,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        OutlinedButton(
            onClick = { abierto = true },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, if (error != null) PlagOutColors.RiskDanger else PlagOutColors.CreamDeep),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag(tag)
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = PlagOutColors.Forest, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(fecha?.format(FORMATO_FECHA) ?: "Elegir", color = PlagOutColors.TextMain, fontSize = 14.sp)
        }
        error?.let { Text(it, color = PlagOutColors.RiskDanger, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) }
    }
    if (abierto) {
        val calendario = rememberDatePickerState(
            initialSelectedDateMillis = (fecha ?: maxima).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val dia = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return !dia.isAfter(maxima) && (minima == null || !dia.isBefore(minima))
                }

                override fun isSelectableYear(year: Int): Boolean = year <= maxima.year && (minima == null || year >= minima.year)
            }
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            colors = DatePickerDefaults.colors(containerColor = PlagOutColors.Surface),
            confirmButton = {
                TextButton(onClick = {
                    calendario.selectedDateMillis?.let { onElegir(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    abierto = false
                }) { Text("Aceptar", color = PlagOutColors.Forest, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar", color = PlagOutColors.Forest) } }
        ) {
            DatePicker(
                state = calendario,
                title = null,
                colors = DatePickerDefaults.colors(
                    containerColor = PlagOutColors.Surface,
                    selectedDayContainerColor = PlagOutColors.Forest,
                    todayContentColor = PlagOutColors.Forest,
                    todayDateBorderColor = PlagOutColors.Forest
                )
            )
        }
    }
}
