package com.example.plag_out

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.CacheTracker
import com.example.plag_out.AlmacenamientoLocal.PreferenciasUsuario
import com.example.plag_out.AlmacenamientoLocal.MonitoreoRepository
import com.example.plag_out.AlmacenamientoLocal.PlantacionRepository
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import com.example.plag_out.AlmacenamientoLocal.UsuarioRepository
import com.example.plag_out.Service.FcmTokenRegistrar
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class LoginState(
    val email: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String? = null
)

data class CrearCuentaState(
    val nombre: String = "",
    val apellido: String = "",
    val cargosDisponibles: List<CargoResponse> = emptyList(),
    val cargo: CargoResponse? = null,
    val email: String = "",
    val password: String = "",
    val repetirPassword: String = "",
    val cargando: Boolean = false,
    val error: String? = null
)

/** Tope para el logout remoto de Supabase */
private const val TOPE_SIGN_OUT_MS = 5_000L

class AuthViewModel(
    private val supabaseClient: SupabaseClient,
    private val context: Context,
    private val monitoreoRepository: MonitoreoRepository,
    private val terrenoRepository: TerrenoRepository,
    private val plantacionRepository: PlantacionRepository,
    private val usuarioRepository: UsuarioRepository,
    private val gddService: GDDService = RetrofitClient.gddService
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun iniciarSesion(onSuccess: (rol: String) -> Unit) {
        val email = _loginState.value.email
        val password = _loginState.value.password

        _loginState.value = _loginState.value.copy(cargando = true, error = null)

        viewModelScope.launch {
            try {
                supabaseClient.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                val token = supabaseClient.auth.currentAccessTokenOrNull()
                if(token != null) Log.d("AuthViewModel", "Login exitoso.")

                // Sincronizar con el backend de Plag-Out después de un login exitoso
                val user = supabaseClient.auth.currentUserOrNull()
                val metadata = user?.userMetadata
                val nombre = metadata?.get("nombre")?.jsonPrimitive?.contentOrNull ?: ""
                val apellido = metadata?.get("apellido")?.jsonPrimitive?.contentOrNull ?: ""
                val cargoId = metadata?.get("cargo")?.jsonPrimitive?.contentOrNull ?: ""

                if (user != null) {
                    try {
                        val response = gddService.createUser(
                            CreateUserRequest(
                                email = user.email ?: "",
                                nombre = nombre,
                                apellido = apellido,
                                cargo_id = cargoId.toIntOrNull() ?: 1
                            )
                        )
                        if (response.isSuccessful) {
                            Log.d(
                                "AuthViewModel",
                                if(response.body()?.created == false) "El usuario ya existe en el backend"
                                        else "Usuario creado exitosamente en el backend: ${response.body()?.usuario_id}"
                            )
                        } else {
                            Log.e(
                                "AuthViewModel",
                                "Error al crear usuario en backend: ${response.code()} ${
                                    response.errorBody()?.string()
                                }"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Excepción al sincronizar usuario con el backend", e)
                    }
                } else {
                    Log.w(
                        "AuthViewModel",
                        "No se encontró el usuario en Supabase después del login"
                    )
                }

                // El rol decide a qué lado de la app se entra, así que se pide antes de navegar.
                // Si el backend no responde se entra como usuario base: lo que el usuario no
                // puede hacer igual lo rechaza el backend.
                val rol = when (val resultado = obtenerRol()) {
                    is ResultadoRol.Suspendida -> {
                        runCatching { supabaseClient.auth.signOut() }
                        _loginState.value = _loginState.value.copy(
                            cargando = false,
                            error = CuentaSuspendidaEventBus.MENSAJE
                        )
                        return@launch
                    }
                    is ResultadoRol.Ok -> resultado.rol
                }
                PreferenciasUsuario.guardarRol(context, rol)

                FcmTokenRegistrar.iniciarSesion()
                // Registrar el token FCM del dispositivo para poder recibir alertas, salvo que el
                // usuario haya apagado las notificaciones desde su perfil. Las cuentas admin no reciben alertas.
                if (rol != ROL_ADMIN && PreferenciasUsuario.notificacionesActivadas(context)) {
                    viewModelScope.launch(Dispatchers.IO) {
                        FcmTokenRegistrar.registrar()
                    }
                }

                _loginState.value = _loginState.value.copy(cargando = false)
                onSuccess(rol)
            } catch (e: RestException) {
                _loginState.value = _loginState.value.copy(
                    cargando = false,
                    error = mapearErrorLogin(e)
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error inesperado al iniciar sesión", e)
                _loginState.value = _loginState.value.copy(
                    cargando = false,
                    error = "Ocurrió un error inesperado. Intentá de nuevo."
                )
            }
        }
    }

    private sealed interface ResultadoRol {
        data class Ok(val rol: String) : ResultadoRol
        data object Suspendida : ResultadoRol
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun obtenerRol(): ResultadoRol {
        return try {
            val response = withContext(Dispatchers.IO) { gddService.getUsuarioActual() }
            val usuario = response.body()
            when {
                response.isSuccessful && usuario != null -> {
                    withContext(Dispatchers.IO) { usuarioRepository.guardarUsuario(usuario) }
                    ResultadoRol.Ok(usuario.rol ?: ROL_USUARIO)
                }
                response.code() == 403 &&
                    response.errorBody()?.string()?.contains(CuentaSuspendidaEventBus.DETALLE) == true ->
                    ResultadoRol.Suspendida
                else -> {
                    Log.e("AuthViewModel", "No se pudo leer el rol: ${response.code()}")
                    ResultadoRol.Ok(ROL_USUARIO)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AuthViewModel", "No se pudo leer el rol", e)
            ResultadoRol.Ok(ROL_USUARIO)
        }
    }

    fun avisarCuentaSuspendida() {
        _loginState.value = _loginState.value.copy(error = CuentaSuspendidaEventBus.MENSAJE)
    }

    private fun mapearErrorLogin(e: RestException): String {
        return when {
            // Al suspender una cuenta el backend la banea en Supabase Auth
            e.message?.contains("banned", ignoreCase = true) == true ->
                CuentaSuspendidaEventBus.MENSAJE

            e.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
                "Correo o contraseña incorrectos"

            e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                "Confirmá tu correo antes de iniciar sesión"

            else -> "No se pudo iniciar sesión. Intentá de nuevo."
        }
    }

    // ---------- CERRAR SESIÓN ----------

    /**
     * Cierra la sesión de Supabase y borra todo el almacenamiento local del
     * dispositivo (token JWT, caché de Room y marcas de consulta), para que
     * el próximo usuario que inicie sesión no vea datos ajenos.
     *
     * [desregistrarDispositivo] va en false cuando se viene de la baja de cuenta: ahí el backend
     * ya borró los dispositivos junto con el usuario y el JWT quedó sin cuenta detrás, así que
     * pegarle a /usuarios/fcm-token solo devolvería un error.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun cerrarSesion(desregistrarDispositivo: Boolean = true, onComplete: () -> Unit) {
        FcmTokenRegistrar.invalidarSesion()
        viewModelScope.launch {
            // Desregistrar el token FCM antes del signOut, mientras el JWT sigue válido
            FcmTokenRegistrar.desregistrar(desregistrarDispositivo)
            val cerroRemoto = try {
                withTimeoutOrNull(TOPE_SIGN_OUT_MS) { supabaseClient.auth.signOut() } != null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error al cerrar sesión en Supabase", e)
                false
            }
            if (!cerroRemoto) {
                //se borra la sesion en el celular sin contactar a supabase
                runCatching { supabaseClient.auth.clearSession() }
                    .onFailure { Log.e("AuthViewModel", "No se pudo limpiar la sesión local", it) }
            }
            withContext(Dispatchers.IO) {
                monitoreoRepository.borrarTodos()
                plantacionRepository.borrarTodos()
                terrenoRepository.borrarTodos()
                usuarioRepository.borrarTodos()
            }
            CacheTracker.limpiarTodo(context)
            PreferenciasUsuario.limpiarRol(context)
            _loginState.value = LoginState()
            _crearCuentaState.value = CrearCuentaState()
            onComplete()
        }
    }

    // ---------- CAMBIAR CONTRASEÑA ----------

    /**
     * Cambia la contraseña del usuario logueado. La sesión sigue siendo válida después del cambio,
     * así que no hace falta volver a iniciar sesión.
     */
    fun cambiarPassword(nueva: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                supabaseClient.auth.updateUser { password = nueva }
                onSuccess()
            } catch (e: RestException) {
                onError(mapearErrorPassword(e))
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error al cambiar la contraseña", e)
                onError("No se pudo cambiar la contraseña. Revisá tu conexión.")
            }
        }
    }

    private fun mapearErrorPassword(e: RestException): String {
        return when {
            e.message?.contains("Password should be", ignoreCase = true) == true ->
                "La contraseña no cumple los requisitos mínimos"

            e.message?.contains("should be different", ignoreCase = true) == true ->
                "La contraseña nueva tiene que ser distinta de la actual"

            e.message?.contains("reauthentication", ignoreCase = true) == true ->
                "Por seguridad, volvé a iniciar sesión antes de cambiar la contraseña"

            else -> "No se pudo cambiar la contraseña. Intentá de nuevo."
        }
    }

    // ---------- CREAR CUENTA ----------

    fun actualizarNombre(valor: String) {
        _crearCuentaState.value = _crearCuentaState.value.copy(nombre = valor, error = null)
    }

    fun actualizarApellido(valor: String) {
        _crearCuentaState.value = _crearCuentaState.value.copy(apellido = valor, error = null)
    }

    fun actualizarCargo(valor: CargoResponse) {
        _crearCuentaState.value = _crearCuentaState.value.copy(cargo = valor, error = null)
    }

    fun actualizarEmailRegistro(valor: String) {
        _crearCuentaState.value = _crearCuentaState.value.copy(email = valor, error = null)
    }

    fun actualizarPasswordRegistro(valor: String) {
        _crearCuentaState.value = _crearCuentaState.value.copy(password = valor, error = null)
    }

    fun actualizarRepetirPassword(valor: String) {
        _crearCuentaState.value =
            _crearCuentaState.value.copy(repetirPassword = valor, error = null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun cargarCargos() {
        viewModelScope.launch {
            try {
                val response = gddService.getCargos()
                if (response.isSuccessful()) {
                    val cargos = response.body() ?: emptyList()
                    _crearCuentaState.value =
                        _crearCuentaState.value.copy(cargosDisponibles = cargos)
                }
            } catch (e: Exception) {
                _crearCuentaState.value = _crearCuentaState.value.copy(
                    cargando = false,
                    error = "No se pudieron cargar los cargos"
                )
                Log.e("CARGOS", "Error: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun crearCuenta(onSuccess: () -> Unit) {
        val state = _crearCuentaState.value

        if (state.password != state.repetirPassword) {
            _crearCuentaState.value = state.copy(error = "Las contraseñas no coinciden")
            return
        }

        _crearCuentaState.value = state.copy(cargando = true, error = null)

        viewModelScope.launch {
            try {
                // 1. Crear usuario en Supabase (esto enviará el mail de confirmación)
                supabaseClient.auth.signUpWith(Email) {
                    email = state.email
                    password = state.password
                    data = buildJsonObject {
                        put("nombre", state.nombre)
                        put("apellido", state.apellido)
                        put("cargo", state.cargo?.id.toString())
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
                Log.e("Supabase", "Error creando cuenta", e)
                _crearCuentaState.value = _crearCuentaState.value.copy(
                    cargando = false,
                    error = "No se pudo crear la cuenta. Intentá de nuevo."
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
    }

    class AuthViewModelFactory(
        private val client: SupabaseClient,
        private val context: Context,
        private val monitoreoRepository: MonitoreoRepository,
        private val terrenoRepository: TerrenoRepository,
        private val plantacionRepository: PlantacionRepository,
        private val usuarioRepository: UsuarioRepository,
        private val gddService: GDDService = RetrofitClient.gddService
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(
                client, context,
                monitoreoRepository, terrenoRepository, plantacionRepository, usuarioRepository, gddService
            ) as T
        }
    }
}

