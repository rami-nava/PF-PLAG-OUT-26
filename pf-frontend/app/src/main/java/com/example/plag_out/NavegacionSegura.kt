package com.example.plag_out

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * Evita apilar pantallas duplicadas por doble toque rápido mientras una transición está en curso.
 * Solo navega si el destino actual está en estado RESUMED (o aún no hay backstack entry).
 */
fun NavController.navegarSeguro(
    ruta: String,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    val estado = currentBackStackEntry?.lifecycle?.currentState
    if (estado != null && !estado.isAtLeast(Lifecycle.State.RESUMED)) return
    navigate(ruta) {
        launchSingleTop = true
        builder()
    }
}

/**
 * Evita hacer doble popBackStack por toque rápido o dejar la app en pantalla vacía
 * cuando no hay destino previo en la pila.
 */
fun NavController.volverSeguro(): Boolean {
    val estado = currentBackStackEntry?.lifecycle?.currentState
    if (estado != null && !estado.isAtLeast(Lifecycle.State.RESUMED)) return false
    if (previousBackStackEntry == null) return false
    return popBackStack()
}
