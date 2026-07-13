package com.example.plag_out.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta compartida "Plag-Out" — inspirada en tonos de campo: verdes de cultivo,
 * tierras/maderas y cremas cálidos. Todas las pantallas principales (Monitoreos,
 * Plantaciones, Terrenos) y los componentes de navegación consumen estos tokens
 * para mantener una identidad visual consistente.
 */
object PlagOutColors {
    // Verdes — follaje / marca
    val Forest = Color(0xFF264A2B)
    val ForestDark = Color(0xFF17301B)
    val Leaf = Color(0xFF4C7A3D)
    val LeafLight = Color(0xFF8FBF74)
    val Moss = Color(0xFFA8C68F)

    // Tierra / madera — acentos cálidos
    val Sun = Color(0xFFDCA23A)
    val Terracotta = Color(0xFFC1712F)
    val Bark = Color(0xFF8A6B4F)
    val BarkLight = Color(0xFFC9AE8C)

    // Neutros cremosos — fondos y superficies
    val Cream = Color(0xFFF6F1E4)
    val CreamDeep = Color(0xFFECE3CC)
    val Surface = Color(0xFFFFFDF7)

    // Texto
    val TextMain = Color(0xFF2A2318)
    val TextSecondary = Color(0xFF7A705F)
    val TextOnDark = Color(0xFFF6F1E4)

    // Estados / riesgo
    val RiskOk = Color(0xFF4C8C4A)
    val RiskWarn = Color(0xFFC1861F)
    val RiskDanger = Color(0xFFAD4531)
    val RiskUnknown = Color(0xFFA79A85)

    val Divider = Color(0xFFE4D9BE)
}
