package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


@Composable
fun EncabezadoSeccionAdmin(titulo: String, subtitulo: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp)) {
        Text(titulo, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = PlagOutColors.TextMain)
        Text(
            subtitulo,
            fontSize = 13.sp,
            color = PlagOutColors.TextSecondary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun BuscadorAdmin(valor: String, onCambio: (String) -> Unit, placeholder: String, tag: String) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        placeholder = { Text(placeholder, color = PlagOutColors.TextSecondary) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PlagOutColors.Forest) },
        trailingIcon = {
            if (valor.isNotEmpty()) {
                IconButton(onClick = { onCambio("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda", tint = PlagOutColors.TextSecondary)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = camposColors(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(tag)
    )
}

@Composable
fun HeaderDetalleAdmin(
    etiqueta: String,
    titulo: String,
    onBack: () -> Unit,
    contenidoExtra: @Composable ColumnScope.() -> Unit = {}
) {
    val respiracion = rememberInfiniteTransition(label = "respiracionHeaderAdmin")
    val escalaDecorativa by respiracion.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escalaDecorativaAdmin"
    )

    val forma = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(listOf(PlagOutColors.Forest, PlagOutColors.Leaf)),
                shape = forma
            )
            .clip(forma)
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-40).dp)
                .size(150.dp)
                .graphicsLayer { scaleX = escalaDecorativa; scaleY = escalaDecorativa }
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.06f), CircleShape)
        )

        Column(Modifier.padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("btnVolverAdmin")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PlagOutColors.TextOnDark)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        etiqueta,
                        color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        titulo,
                        color = PlagOutColors.TextOnDark,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(Modifier.padding(start = 12.dp), content = contenidoExtra)
        }
    }
}

@Composable
fun BannerErrorAdmin(mensaje: String?, onCerrar: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    // Se recuerda el último mensaje para que no desaparezca el texto durante la animación de salida
    var ultimo by remember { mutableStateOf(mensaje.orEmpty()) }
    if (mensaje != null) ultimo = mensaje

    AnimatedVisibility(
        visible = mensaje != null,
        enter = fadeIn(tween(220)) + expandVertically(tween(220)),
        exit = fadeOut(tween(160)) + shrinkVertically(tween(160)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PlagOutColors.RiskDanger.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)
                .testTag("bannerErrorAdmin"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = PlagOutColors.RiskDanger, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                ultimo,
                color = PlagOutColors.RiskDanger,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            )
            if (onCerrar != null) {
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar aviso", tint = PlagOutColors.RiskDanger, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun EstadoErrorAdmin(mensaje: String, onReintentar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(color = PlagOutColors.RiskDanger.copy(alpha = 0.10f), shape = CircleShape, modifier = Modifier.size(96.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = PlagOutColors.RiskDanger, modifier = Modifier.size(42.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            mensaje,
            textAlign = TextAlign.Center,
            color = PlagOutColors.TextMain,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("txtErrorAdmin")
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onReintentar,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PlagOutColors.Forest, contentColor = PlagOutColors.TextOnDark),
            modifier = Modifier.testTag("btnReintentarAdmin")
        ) {
            Text("Reintentar", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PastillaEstado(texto: String, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.14f), modifier = modifier) {
        Text(
            texto,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun AvatarIniciales(nombre: String, tamano: Int = 44, fondo: Color = PlagOutColors.Leaf, texto: Color = PlagOutColors.TextOnDark) {
    val iniciales = nombre.split(" ", "@", ".")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }
    Box(
        modifier = Modifier
            .size(tamano.dp)
            .background(fondo, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(iniciales, color = texto, fontWeight = FontWeight.ExtraBold, fontSize = (tamano * 0.36f).sp)
    }
}

@Composable
fun FilaInfoAdmin(icono: ImageVector, etiqueta: String, valor: String, tag: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoFila(icono)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(etiqueta, fontSize = 11.sp, color = PlagOutColors.TextSecondary, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
            Text(
                valor,
                fontSize = 14.sp,
                color = PlagOutColors.TextMain,
                fontWeight = FontWeight.SemiBold,
                modifier = if (tag != null) Modifier.testTag(tag) else Modifier
            )
        }
    }
}

@Composable
fun DialogoConfirmacionAdmin(
    icono: ImageVector,
    titulo: String,
    texto: String,
    textoConfirmar: String,
    peligro: Boolean,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit,
    confirmarHabilitado: Boolean = true,
    contenidoExtra: @Composable ColumnScope.() -> Unit = {}
) {
    val acento = if (peligro) PlagOutColors.RiskDanger else PlagOutColors.Forest
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PlagOutColors.Surface,
        icon = { Icon(icono, contentDescription = null, tint = acento) },
        title = { Text(titulo, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain) },
        text = {
            Column {
                Text(texto, color = PlagOutColors.TextSecondary)
                contenidoExtra()
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                enabled = confirmarHabilitado,
                modifier = Modifier.testTag("btnConfirmarAdmin")
            ) {
                Text(
                    textoConfirmar,
                    color = if (confirmarHabilitado) acento else PlagOutColors.RiskUnknown,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = PlagOutColors.TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun formatearFechaHora(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    val formato = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-AR"))
    return runCatching {
        OffsetDateTime.parse(iso)
            .atZoneSameInstant(java.time.ZoneId.systemDefault())
            .format(formato)
    }.getOrNull()
}
