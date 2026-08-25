package com.example.plag_out

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmbralDeRiesgoSheet(umbralActual: Int?, onDismiss: () -> Unit) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ejemplo = umbralActual ?: 80

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = estadoHoja,
        containerColor = PlagOutColors.Surface,
        contentWindowInsets = { WindowInsets.systemBars },
        dragHandle = { BottomSheetDefaults.DragHandle(color = PlagOutColors.Divider) },
        modifier = Modifier.testTag("hojaUmbralRiesgo")
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp)
        ) {
            Text(
                "¿Qué es el umbral de riesgo?",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PlagOutColors.TextMain
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Es el porcentaje de los GDD que la plaga necesita para eclosionar a partir del " +
                    "cual querés que te avisemos. Con un umbral del $ejemplo%, la alerta te llega " +
                    "cuando el monitoreo alcanza el $ejemplo% de ese objetivo.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = PlagOutColors.TextSecondary
            )

            Spacer(Modifier.height(16.dp))

            TarjetaGDD()

            Spacer(Modifier.height(14.dp))

            Text(
                "Más bajo, te avisamos antes y tenés más margen para actuar. Más alto, el aviso " +
                    "llega más cerca de la eclosión. Podés cambiarlo cuando quieras.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = PlagOutColors.TextSecondary
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btnEntendidoUmbral"),
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
