package com.example.plag_out

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SelloDeNivel
import com.example.plag_out.ui.theme.estiloDeNivel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComoFuncionaSheet(onDismiss: () -> Unit) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = estadoHoja,
        containerColor = PlagOutColors.Surface,
        // La hoja se abre a pantalla completa: sin esto el contenido se dibuja debajo de la
        // status bar y de la barra de navegación.
        contentWindowInsets = { WindowInsets.systemBars },
        dragHandle = { BottomSheetDefaults.DragHandle(color = PlagOutColors.Divider) }
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp)
                .testTag("hojaComoFunciona")
        ) {
            Text(
                "¿Cómo funciona Plag-Out?",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PlagOutColors.TextMain
            )
            Text(
                "Gestión Inteligente de Cultivos",
                fontSize = 12.sp,
                color = PlagOutColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Plag-Out te ayuda a anticipar el riesgo de plagas en tus cultivos: seguís tus " +
                    "terrenos y plantaciones, y la app te avisa cuando el riesgo de una plaga sube.",
                fontSize = 14.sp,
                color = PlagOutColors.TextSecondary
            )

            Spacer(Modifier.height(24.dp))

            PasoAyuda(
                1,
                "Registrá tu terreno",
                "Nombre, hectáreas y ubicación en el mapa."
            )
            PasoAyuda(
                2,
                "Agregá tus plantaciones",
                "Qué cultivo sembraste en ese terreno y en qué fecha."
            )
            PasoAyuda(
                3,
                "Creá un monitoreo",
                "Elegí la plaga que querés seguir y el umbral de riesgo a partir del cual querés " +
                    "que te avisemos."
            )
            PasoAyuda(
                4,
                "Recibí alertas",
                "Plag-Out sigue tus monitoreos y te notifica cuando el riesgo supera tu umbral.",
                ultimo = true
            )

            Spacer(Modifier.height(20.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(PlagOutColors.Cream, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(
                    "¿Qué son los GDD?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlagOutColors.TextMain
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Grados Día de Crecimiento: una medida del calor acumulado desde la siembra. " +
                        "Plag-Out compara los GDD acumulados de tu plantación con el objetivo de la " +
                        "plaga que estás monitoreando: cuanto más cerca del objetivo, más alto el riesgo.",
                    fontSize = 13.sp,
                    color = PlagOutColors.TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Los valores se recalculan una vez por día.",
                    fontSize = 11.sp,
                    color = PlagOutColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Niveles de riesgo",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.TextMain
            )
            Spacer(Modifier.height(12.dp))

            // Los chips salen de estiloDeNivel(), el mismo que usan los dashboards: así la ayuda
            // no se puede desincronizar de los colores y etiquetas reales.
            FilaNivel(0, "El riesgo está lejos de tu umbral.")
            FilaNivel(1, "El riesgo se está acercando a tu umbral.")
            FilaNivel(2, "Se superó tu umbral: conviene revisar la plantación.")
            FilaNivel(-1, "Todavía no hay monitoreos con datos para mostrar.")

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btnEntendidoComoFunciona"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlagOutColors.Forest,
                    contentColor = PlagOutColors.TextOnDark
                )
            ) {
                Text("Entendido", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PasoAyuda(numero: Int, titulo: String, descripcion: String, ultimo: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(PlagOutColors.Forest),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$numero",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.TextOnDark
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PlagOutColors.TextMain
            )
            Text(
                descripcion,
                fontSize = 13.sp,
                color = PlagOutColors.TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
    if (!ultimo) Spacer(Modifier.height(16.dp))
}

@Composable
private fun FilaNivel(nivel: Int, descripcion: String) {
    val estilo = estiloDeNivel(nivel)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelloDeNivel(estilo = estilo, pulsante = false)
        Spacer(Modifier.width(12.dp))
        Text(
            descripcion,
            fontSize = 12.sp,
            color = PlagOutColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}
