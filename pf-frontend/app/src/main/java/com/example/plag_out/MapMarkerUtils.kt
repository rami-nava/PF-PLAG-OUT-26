package com.example.plag_out

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import org.osmdroid.tileprovider.tilesource.XYTileSource

/**
 * Teselas usadas por todas las pantallas con mapa (detalle de reporte, mapa de reportes,
 * selección de ubicación).
 */
val OsmTileSource: XYTileSource = XYTileSource(
    "OSM_FR",
    0, 19, 256, ".png",
    arrayOf(
        "https://a.tile.openstreetmap.fr/osmfr/",
        "https://b.tile.openstreetmap.fr/osmfr/",
        "https://c.tile.openstreetmap.fr/osmfr/"
    )
)

object MapMarkerUtils {

    /** Verde para lo propio, rojo para lo ajeno. */
    fun getMarkerIcon(context: Context, isGreen: Boolean): Drawable =
        getMarkerIcon(context, if (isGreen) 125f else 0f)

    /**
     * El pin por defecto de osmdroid retintado al [hue] pedido (0 = rojo, 28 = naranja,
     * 125 = verde), para poder pintar un marcador según la severidad del reporte.
     */
    fun getMarkerIcon(context: Context, hue: Float): Drawable {
        val original = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default)
            ?: return ColorDrawable(AndroidColor.TRANSPARENT)

        val width = original.intrinsicWidth.coerceAtLeast(1)
        val height = original.intrinsicHeight.coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        original.setBounds(0, 0, width, height)
        original.draw(canvas)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val hsv = FloatArray(3)
        for (i in pixels.indices) {
            val color = pixels[i]
            val alpha = (color ushr 24) and 0xFF
            if (alpha == 0) continue

            AndroidColor.colorToHSV(color, hsv)
            // Saturation >= 0.20f identifica el cuerpo cromático del pin,
            // preservando el ícono de la manito (blanco, S ~ 0) y sombras (grises/negro, S ~ 0).
            if (hsv[1] >= 0.20f && hsv[2] >= 0.15f) {
                hsv[0] = hue
                pixels[i] = (alpha shl 24) or (AndroidColor.HSVToColor(hsv) and 0x00FFFFFF)
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return BitmapDrawable(context.resources, bitmap)
    }
}
