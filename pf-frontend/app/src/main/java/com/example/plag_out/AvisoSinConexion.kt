package com.example.plag_out

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors

internal fun hayRed(context: Context): Boolean = runCatching {
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}.getOrDefault(true)

@Composable
fun rememberHayRed(): State<Boolean> {
    val context = LocalContext.current
    val hayRedAhora = remember(context) { mutableStateOf(hayRed(context)) }
    DisposableEffect(context) {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        // Los callbacks llegan en un hilo del sistema; escribir un State desde ahí es seguro.
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capacidades: NetworkCapabilities) {
                hayRedAhora.value = capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }

            override fun onLost(network: Network) {
                hayRedAhora.value = false
            }
        }
        runCatching { connectivity.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { connectivity.unregisterNetworkCallback(callback) } }
    }
    return hayRedAhora
}

@Composable
fun AvisoSinConexion(
    reintentando: Boolean,
    onReintentar: () -> Unit,
    tag: String,
    tagBotonReintentar: String,
    modifier: Modifier = Modifier,
    detalle: String = "Mostrando los datos guardados en tu dispositivo"
) {
    Surface(
        color = PlagOutColors.RiskWarn.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PlagOutColors.RiskWarn.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = PlagOutColors.RiskWarn,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Sin conexión",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PlagOutColors.TextMain
                )
                Text(
                    detalle,
                    fontSize = 12.sp,
                    color = PlagOutColors.TextSecondary
                )
            }
            if (reintentando) {
                CircularProgressIndicator(
                    color = PlagOutColors.RiskWarn,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .size(18.dp)
                )
            } else {
                TextButton(
                    onClick = onReintentar,
                    modifier = Modifier.testTag(tagBotonReintentar)
                ) {
                    Text("Reintentar", color = PlagOutColors.Forest, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
