package com.example.plag_out.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2d5016),
    secondary = Color(0xFFe8941a),
    tertiary = Color(0xFF38a169),
    background = Color(0xFFF8F7F4),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFe53e3e)
)

@Composable
fun PlagasGDDTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}