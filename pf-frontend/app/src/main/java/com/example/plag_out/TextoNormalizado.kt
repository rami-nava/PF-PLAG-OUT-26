package com.example.plag_out

import java.text.Normalizer

/**
 * Normaliza un nombre para detectar duplicados ignorando mayúsculas, tildes y espacios múltiples.
 */
fun normalizarClaveUnica(texto: String): String {
    val sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    return sinTildes.replace(Regex("\\s+"), " ").lowercase()
}
