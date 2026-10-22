package com.example.plag_out

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationIntentTest {
    private val activity = Robolectric.buildActivity(MainActivity::class.java).get()

    private fun read(intent: Intent) {
        MainActivity::class.java.getDeclaredMethod("leerDeepLink", Intent::class.java)
            .apply { isAccessible = true }.invoke(activity, intent)
    }

    private fun pending(name: String): Any? =
        (MainActivity::class.java.getDeclaredField(name).apply { isAccessible = true }
            .get(activity) as State<*>).value

    @Test
    fun `push ML without a URL opens the prediction`() {
        val intent = Intent().putExtra("tipo", "ALERTA_ML_RIESGO").putExtra("prediccion_id", "41")
        assertNull(intent.data)
        read(intent)
        assertEquals("41", pending("deepLinkPrediccionId"))
    }

    @Test
    fun `push GDD without a URL opens its monitoring`() {
        read(Intent().putExtra("tipo", "ALERTA_GDD_CICLO").putExtra("monitoreo_id", "7"))
        assertEquals("7", pending("deepLinkMonitoreoId"))
        assertNull(pending("deepLinkPrediccionId"))
    }

    @Test
    fun `expired password recovery keeps its own flow`() {
        read(Intent(Intent.ACTION_VIEW, Uri.parse("plagout://reset-password/#error=otp_expired"))
            .putExtra("prediccion_id", "41"))
        assertEquals(true, pending("deepLinkRecuperarPassword"))
        assertEquals("El enlace de recuperación ya fue utilizado o ha expirado. Por favor, solicitá uno nuevo.",
            pending("deepLinkRecuperarPasswordError"))
        assertNull(pending("deepLinkPrediccionId"))
    }
}
