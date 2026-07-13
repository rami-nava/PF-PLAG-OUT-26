package com.example.plag_out.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PlagOutColors.Forest,
    onPrimary = PlagOutColors.TextOnDark,
    secondary = PlagOutColors.Sun,
    onSecondary = PlagOutColors.ForestDark,
    tertiary = PlagOutColors.Leaf,
    background = PlagOutColors.Cream,
    onBackground = PlagOutColors.TextMain,
    surface = PlagOutColors.Surface,
    onSurface = PlagOutColors.TextMain,
    surfaceVariant = PlagOutColors.CreamDeep,
    error = PlagOutColors.RiskDanger
)

@Composable
fun PlagasGDDTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}