package com.example.plag_out

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.PlantacionRepository
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive


enum class Cargo(val label: String) {
    INGENIERO_AGRONOMO("Ingeniero Agrónomo"),
    PRODUCTOR("Productor"),
    OTRO("Otro")
}

data class LoginState(
    val email: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String? = null
)

data class CrearCuentaState(
    val nombre: String = "",
    val apellido: String = "",
    val cargo: Cargo? = null,
    val email: String = "",
    val password: String = "",
    val repetirPassword: String = "",
    val cargando: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    //private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _loginState

    private val _crearCuentaState = MutableStateFlow(CrearCuentaState())
    val crearCuentaState: StateFlow<CrearCuentaState> = _crearCuentaState

    // ---------- LOGIN ----------

    fun actualizarEmail(valor: String) {
        _loginState.value = _loginState.value.copy(email = valor, error = null)
    }

    fun actualizarPassword(valor: String) {
        _loginState.value = _loginState.value.copy(password = valor, error = null)
    }

    /*fun iniciarSesion(onSuccess: () -> Unit) {
        val email = _loginState.value.email
        val password = _loginState.value.password

        _loginState.value = _loginState.value.copy(cargando = true, error = null)

        viewModelScope.launch {
            try {
                //supabaseClient.auth.signInWith(Email) {
                  //  this.email = email
                    //this.password = password
                //}
                _loginState.value = _loginState.value.copy(cargando = false)
                onSuccess()
            } catch (e: RestException) {
                _loginState.value = _loginState.value.copy(
                    cargando = false,
                    error = mapearErrorLogin(e)
                )
            } catch (e: Exception) {
                _loginState.value = _loginState.value.copy(
                    cargando = false,
                    error = "Ocurrió un error inesperado. Intentá de nuevo."
                )
            }
        }
    }*

    private fun mapearErrorLogin(e: RestException): String {
        return when {
            e.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
                "Correo o contraseña incorrectos"
            e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                "Confirmá tu correo antes de iniciar sesión"
            else -> "No se pudo iniciar sesión. Intentá de nuevo."
        }
    }*/

    // ---------- CREAR CUENTA ----------

    fun actualizarNombre(valor: String) {
        _crearCuentaState.value = _crearCuentaState.value.copy(nombre = valor, error = null)
    }

    fun actualizarApellido(valor: String) {
        _crearCuentaState.value = _crearCuentaState.value.copy(apellido = valor, error = null)
    }

    fun actualizarCargo(valor: Cargo) {
        _crearCuentaState.value = _crearCuentaState.value.copy(cargo = valor, error = null)
    }

    fun actualizarEmailRegistro(valor: String) {
        _crearCuentaState.value = _crearCuentaState.value.copy(email = valor, error = null)
    }

    fun actualizarPasswordRegistro(valor: String) {
        _crearCuentaState.value = _crearCuentaState.value.copy(password = valor, error = null)
    }

    fun actualizarRepetirPassword(valor: String) {
        _crearCuentaState.value = _crearCuentaState.value.copy(repetirPassword = valor, error = null)
    }

   /* fun crearCuenta(onSuccess: () -> Unit) {
        val state = _crearCuentaState.value

        if (state.password != state.repetirPassword) {
            _crearCuentaState.value = state.copy(error = "Las contraseñas no coinciden")
            return
        }

        _crearCuentaState.value = state.copy(cargando = true, error = null)

        viewModelScope.launch {
            try {
                supabaseClient.auth.signUpWith(Email) {
                    email = state.email
                    password = state.password
                    data = buildJsonObject {
                        put("nombre", JsonPrimitive(state.nombre))
                        put("apellido", JsonPrimitive(state.apellido))
                        put("cargo", JsonPrimitive(state.cargo?.name ?: ""))
                    }
                }

                _crearCuentaState.value = _crearCuentaState.value.copy(cargando = false)
                onSuccess()
            } catch (e: RestException) {
                _crearCuentaState.value = _crearCuentaState.value.copy(
                    cargando = false,
                    error = mapearErrorRegistro(e)
                )
            } catch (e: Exception) {
                _crearCuentaState.value = _crearCuentaState.value.copy(
                    cargando = false,
                    error = "Ocurrió un error inesperado. Intentá de nuevo."
                )
            }
        }
    }

    private fun mapearErrorRegistro(e: RestException): String {
        return when {
            e.message?.contains("already registered", ignoreCase = true) == true ->
                "Ya existe una cuenta con este correo"
            e.message?.contains("Password should be", ignoreCase = true) == true ->
                "La contraseña no cumple los requisitos mínimos"
            else -> "No se pudo crear la cuenta. Intentá de nuevo."
        }
    }*/

    // ---------- Para tests ----------

    fun setLoginStateParaTest(state: LoginState) {
        _loginState.value = state
    }

    fun setCrearCuentaStateParaTest(state: CrearCuentaState) {
        _crearCuentaState.value = state
    }

    class AuthViewModelFactory(
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel() as T
        }
    }
}