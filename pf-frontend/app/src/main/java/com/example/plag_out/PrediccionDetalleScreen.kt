package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PrediccionDetalleScreen(
    prediccionId: Int,
    viewModel: PrediccionDetalleViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(prediccionId) { viewModel.cargar(prediccionId) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = PlagOutColors.TextMain)
            }
            Text(
                "Alerta predictiva",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PlagOutColors.TextMain
            )
        }

        when {
            state.isLoading && state.prediccion == null -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PlagOutColors.Forest)
                }
            }
            state.noDisponible -> EstadoPrediccion(
                "Esta predicción no está disponible.",
                "Puede no existir o pertenecer a otra cuenta."
            )
            state.prediccion == null -> EstadoPrediccion(
                "No pudimos cargar la predicción.",
                state.error ?: "Intentá nuevamente.",
                onReintentar = { viewModel.cargar(prediccionId) }
            )
            else -> ContenidoPrediccion(
                state = state,
                onResponder = viewModel::responder,
                onReintentar = viewModel::reintentar
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ContenidoPrediccion(
    state: PrediccionDetalleUIState,
    onResponder: (String) -> Unit,
    onReintentar: () -> Unit
) {
    val prediccion = state.prediccion ?: return
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("detallePrediccion")
    ) {
        Spacer(Modifier.height(8.dp))
        Surface(
            color = PlagOutColors.Surface,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.BugReport, null, tint = PlagOutColors.Forest)
                    Text(
                        prediccion.plaga_nombre_cientifico,
                        modifier = Modifier.padding(start = 10.dp),
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DatoPrediccion("Probabilidad", porcentaje(prediccion.probabilidad_porcentaje))
                    DatoPrediccion(
                        "Umbral efectivo",
                        prediccion.umbral_efectivo_porcentaje?.let(::porcentaje) ?: "No disponible"
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("Horizonte: ${prediccion.horizon_days} días", color = PlagOutColors.TextSecondary, fontSize = 13.sp)
                Text("Modelo: ${prediccion.model_id}", color = PlagOutColors.TextSecondary, fontSize = 12.sp)
                prediccion.confirmacion.expira_en?.let {
                    Text("Vigente hasta: ${formatearFecha(it)}", color = PlagOutColors.TextSecondary, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        when (prediccion.confirmacion.estado) {
            "pendiente" -> if (state.feedbackPendiente == null) {
                Text("¿Pudiste verificar la presencia de la plaga?", fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                Spacer(Modifier.height(10.dp))
                OpcionFeedback("Sí, está presente", "presente", state.enviando, onResponder)
                OpcionFeedback("Revisé y no la observé", "no_observada", state.enviando, onResponder)
                OpcionFeedback("No pude verificar", "no_verificada", state.enviando, onResponder)
                Text(
                    "No observarla o no verificarla no confirma ausencia ni significa que el modelo sea incorrecto.",
                    color = PlagOutColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                EstadoPrediccion(
                    "Respuesta pendiente",
                    etiquetaRespuesta(state.feedbackPendiente.respuesta),
                    onReintentar = onReintentar,
                    cargando = state.enviando
                )
            }
            "respondida" -> EstadoPrediccion(
                "Respuesta enviada",
                etiquetaRespuesta(prediccion.confirmacion.respuesta)
            )
            "vencida" -> EstadoPrediccion(
                "La solicitud venció",
                "El período para responder esta alerta ya terminó."
            )
            else -> EstadoPrediccion(
                "No requiere respuesta",
                "Esta predicción no tiene una verificación pendiente."
            )
        }

        state.error?.let {
            Text(it, color = PlagOutColors.RiskDanger, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun DatoPrediccion(etiqueta: String, valor: String) {
    Column {
        Text(valor, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = PlagOutColors.Forest)
        Text(etiqueta, fontSize = 11.sp, color = PlagOutColors.TextSecondary)
    }
}

@Composable
private fun OpcionFeedback(
    texto: String,
    respuesta: String,
    enviando: Boolean,
    onResponder: (String) -> Unit
) {
    OutlinedButton(
        onClick = { onResponder(respuesta) },
        enabled = !enviando,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).testTag("respuesta_$respuesta")
    ) { Text(texto, color = PlagOutColors.Forest) }
}

@Composable
private fun EstadoPrediccion(
    titulo: String,
    detalle: String,
    onReintentar: (() -> Unit)? = null,
    cargando: Boolean = false
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = PlagOutColors.RiskUnknown)
        Text(titulo, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain, modifier = Modifier.padding(top = 8.dp))
        Text(detalle, color = PlagOutColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        if (onReintentar != null) {
            Button(
                onClick = onReintentar,
                enabled = !cargando,
                colors = ButtonDefaults.buttonColors(containerColor = PlagOutColors.Forest),
                modifier = Modifier.padding(top = 14.dp).testTag("btnReintentarPrediccion")
            ) {
                if (cargando) CircularProgressIndicator(modifier = Modifier.height(18.dp), color = PlagOutColors.TextOnDark)
                else Text("Reintentar", color = PlagOutColors.TextOnDark)
            }
        }
    }
}

private fun porcentaje(valor: Float): String = String.format(Locale.US, "%.2f%%", valor).replace('.', ',')

@RequiresApi(Build.VERSION_CODES.O)
private fun formatearFecha(valor: String): String = runCatching {
    OffsetDateTime.parse(valor).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}.getOrDefault(valor)

private fun etiquetaRespuesta(respuesta: String?): String = when (respuesta) {
    "presente" -> "Sí, está presente"
    "no_observada" -> "Revisé y no la observé"
    "no_verificada" -> "No pude verificar"
    else -> "Respuesta registrada"
}
