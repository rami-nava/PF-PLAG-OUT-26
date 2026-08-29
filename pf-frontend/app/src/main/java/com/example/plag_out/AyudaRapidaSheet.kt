package com.example.plag_out

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors

/**
 * Ayuda rápida del signo de pregunta de la barra superior: cinco líneas con el recorrido de la
 * app y nada más. La explicación larga (GDD, biofix, IRA, niveles) vive en [ComoFuncionaSheet],
 * al que se llega desde Perfil → Ajustes; acá solo se apunta para allá.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyudaRapidaSheet(onDismiss: () -> Unit, onVerGuiaCompleta: () -> Unit) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = estadoHoja,
        containerColor = PlagOutColors.Surface,
        contentWindowInsets = { WindowInsets.systemBars },
        dragHandle = { BottomSheetDefaults.DragHandle(color = PlagOutColors.Divider) }
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp)
                .testTag("hojaAyudaRapida")
        ) {
            Text(
                "¿Cómo se usa Plag-Out?",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PlagOutColors.TextMain
            )
            Text(
                "El recorrido completo, en cinco pasos",
                fontSize = 12.sp,
                color = PlagOutColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(20.dp))

            PasoRapido(
                Icons.Default.Terrain,
                "Cargá tus terrenos",
                "Nombre, hectáreas y ubicación en el mapa."
            )
            PasoRapido(
                Icons.Outlined.Grass,
                "Agregá tus cultivos",
                "Qué sembraste en cada terreno y en qué fecha."
            )
            PasoRapido(
                Icons.Default.Science,
                "Iniciá monitoreos",
                "Elegí la plaga a seguir y tu umbral de riesgo."
            )
            PasoRapido(
                Icons.Outlined.BugReport,
                "Realizá reportes",
                "Registrá lo que ves a campo con el botón + de la barra."
            )
            PasoRapido(
                Icons.Default.Notifications,
                "Recibí alertas",
                "Te avisamos ante un posible biofix y cuando el riesgo sube.",
                ultimo = true
            )

            Spacer(Modifier.height(24.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(PlagOutColors.Moss.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.MenuBook,
                    contentDescription = null,
                    tint = PlagOutColors.Forest,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "La explicación en detalle —GDD, biofix y niveles de alerta— está en " +
                        "Perfil → Ajustes → ¿Cómo funciona?",
                    fontSize = 12.sp,
                    color = PlagOutColors.TextMain,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btnEntendidoAyudaRapida"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlagOutColors.Forest,
                    contentColor = PlagOutColors.TextOnDark
                )
            ) {
                Text("Entendido", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onVerGuiaCompleta,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btnVerGuiaCompleta")
            ) {
                Text(
                    "Ver la guía completa",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlagOutColors.Forest
                )
            }
        }
    }
}

/** Fila ícono + título + una línea: la versión corta de `PasoAyuda` de la hoja larga. */
@Composable
private fun PasoRapido(icono: ImageVector, titulo: String, descripcion: String, ultimo: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PlagOutColors.Forest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = PlagOutColors.TextOnDark,
                modifier = Modifier.size(18.dp)
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
