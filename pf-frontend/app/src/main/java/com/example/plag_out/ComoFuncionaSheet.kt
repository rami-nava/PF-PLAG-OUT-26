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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
                "Plag-Out te ayuda a anticipar el riesgo de plagas en tus cultivos: cargás tus " +
                    "terrenos y lo que sembraste en cada uno, monitoreás las plagas que te " +
                    "importan y reportás lo que ves a campo. Con el clima de tu ubicación la app " +
                    "calcula día a día qué tan favorable fue el ambiente para cada plaga y te " +
                    "avisa cuando el riesgo sube o cuando se viene un nuevo biofix.",
                fontSize = 14.sp,
                color = PlagOutColors.TextSecondary
            )

            Spacer(Modifier.height(24.dp))

            PasoAyuda(
                1,
                "Cargá tus terrenos",
                "Nombre, hectáreas y ubicación en el mapa: de ahí sale el clima con el que se " +
                    "calcula todo lo demás."
            )
            PasoAyuda(
                2,
                "Agregá tus cultivos",
                "Qué cultivo sembraste en ese terreno y en qué fecha."
            )
            PasoAyuda(
                3,
                "Iniciá monitoreos",
                "Elegí la plaga que querés seguir y el umbral de riesgo a partir del cual querés " +
                    "que te avisemos."
            )
            PasoAyuda(
                4,
                "Realizá reportes",
                "Con el botón + de la barra registrás lo que ves a campo: plaga, etapa " +
                    "biológica, severidad, ubicación y fecha. Queda en Reportes como historial " +
                    "de lo que pasó en cada terreno."
            )
            PasoAyuda(
                5,
                "Recibí alertas",
                "Cada monitoreo muestra un nivel de alerta —Bajo, Moderado o Alto— y te " +
                    "notificamos cuando el riesgo supera tu umbral y cuando detectamos un " +
                    "posible biofix, para que llegues antes que la plaga.",
                ultimo = true
            )

            Spacer(Modifier.height(20.dp))

            // Mismo bloque de GDD que muestra la hoja del umbral de riesgo.
            TarjetaGDD()

            Spacer(Modifier.height(12.dp))

            TarjetaConcepto(Icons.Outlined.PlayCircleOutline, "¿Qué es el biofix?") {
                Text(
                    "El biofix es el punto de partida del conteo: la fecha desde la que empezamos a " +
                        "acumular GDD para una plaga, marcada por un hecho biológico concreto " +
                        "—típicamente la primera aparición de adultos en la zona—. Todo el riesgo que " +
                        "ves se cuenta desde ahí: si el biofix se corre, se corre también el pronóstico.",
                    fontSize = 13.sp,
                    color = PlagOutColors.TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                NotaBiofix(
                    "Chicharrita del maíz: hoy podemos avisarte con unos 14 días de margen antes de " +
                        "que aparezcan los adultos. Es una estimación, no una garantía: el clima y la " +
                        "población de la zona pueden adelantarla o atrasarla."
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "En el monitoreo vas a ver los GDD acumulados avanzar hasta el objetivo de " +
                        "eclosión. Y si llega a los GDD de una generación completa, te avisamos de un " +
                        "nuevo biofix: los adultos alcanzan a poner huevos antes de migrar, así que " +
                        "el ciclo vuelve a empezar.",
                    fontSize = 13.sp,
                    color = PlagOutColors.TextSecondary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Mismo bloque de IRA que muestra el detalle de monitoreo.
            TarjetaIRA()

            Spacer(Modifier.height(24.dp))

            Text(
                "Niveles de alerta",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.TextMain
            )
            Text(
                "El nivel de alerta (0, 1 y 2 en los datos) dice qué tan favorables fueron las " +
                    "condiciones para la plaga y, con eso, qué densidad poblacional esperar.",
                fontSize = 13.sp,
                color = PlagOutColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(10.dp))

            // Las filas salen de estiloDeNivel(), el mismo que usan los dashboards: así la ayuda
            // no se puede desincronizar de los colores, etiquetas y textos reales.
            FilaNivelDeAlerta(0)
            FilaNivelDeAlerta(1)
            FilaNivelDeAlerta(2)
            FilaSinDatos()

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
private fun NotaBiofix(texto: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(PlagOutColors.RiskWarn.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Icon(
            Icons.Outlined.Schedule,
            contentDescription = null,
            tint = PlagOutColors.RiskWarn,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            texto,
            fontSize = 12.sp,
            color = PlagOutColors.TextMain,
            modifier = Modifier.weight(1f)
        )
    }
}

/** "Sin datos" no es un nivel de alerta más, así que va en una fila simple debajo de los tres. */
@Composable
private fun FilaSinDatos() {
    val estilo = estiloDeNivel(-1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp, start = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelloDeNivel(estilo = estilo, pulsante = false)
        Spacer(Modifier.width(12.dp))
        Text(
            estilo.descripcion,
            fontSize = 12.sp,
            color = PlagOutColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}
