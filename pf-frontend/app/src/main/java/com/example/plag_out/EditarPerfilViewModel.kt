package com.example.plag_out

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class EditarPerfilState(
    val nombre: String = "",
    val apellido: String = "",
    val cargosDisponibles: List<CargoResponse> = emptyList(),
    val cargo: CargoResponse? = null,
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null
)

/** Largo máximo aceptado para nombre y apellido. */
private const val MAX_LARGO = 60

class EditarPerfilViewModel(
    private val gddService: GDDService = RetrofitClient.gddService,
    /**
     * Espeja los datos en el `user_metadata` de Supabase. Es un parámetro y no una llamada directa
     * para que los tests no necesiten un cliente de Supabase real.
     *
     * Hace falta porque [AuthViewModel] lee ese metadata en cada login para alimentar
     * `POST /usuarios`: si no se actualiza, el metadata queda viejo.
     */
    private val espejarEnSupabase: suspend (nombre: String, apellido: String, cargoId: Int?) -> Unit =
        { nombre, apellido, cargoId ->
            SupabaseProvider.client.auth.updateUser {
                data = buildJsonObject {
                    put("nombre", nombre)
                    put("apellido", apellido)
                    if (cargoId != null) put("cargo", cargoId.toString())
                }
            }
        }
) : ViewModel() {

    private val _state = MutableStateFlow(EditarPerfilState())
    val state: StateFlow<EditarPerfilState> = _state.asStateFlow()

    /** El usuario tal como vino del backend, para detectar si hubo cambios reales. */
    private var original: UsuarioResponse? = null

    /**
     * Carga el formulario con los datos actuales del usuario y trae los cargos disponibles.
     * Idempotente: volver de una rotación no vuelve a pisar lo que el usuario venía tipeando.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun precargar(usuario: UsuarioResponse) {
        if (original?.usuario_id == usuario.usuario_id) return
        original = usuario
        _state.value = _state.value.copy(
            nombre = usuario.nombre,
            apellido = usuario.apellido,
            error = null
        )
        cargarCargos()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun cargarCargos() {
        _state.value = _state.value.copy(cargando = true)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { gddService.getCargos() }
                val cargos = response.body().orEmpty()
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        cargando = false,
                        cargosDisponibles = cargos,
                        // UsuarioResponse trae el cargo como texto y el request espera un id:
                        // hay que resolverlo contra la lista para preseleccionar el dropdown.
                        cargo = cargos.firstOrNull { it.tipo == original?.cargo }
                    )
                } else {
                    _state.value = _state.value.copy(cargando = false)
                    Log.e("EDITAR_PERFIL", "Error al cargar cargos: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(cargando = false)
                Log.e("EDITAR_PERFIL", "Error al cargar cargos: ${e.message}")
            }
        }
    }

    fun actualizarNombre(valor: String) {
        _state.value = _state.value.copy(nombre = valor, error = null)
    }

    fun actualizarApellido(valor: String) {
        _state.value = _state.value.copy(apellido = valor, error = null)
    }

    fun actualizarCargo(valor: CargoResponse) {
        _state.value = _state.value.copy(cargo = valor, error = null)
    }

    /** Nombre y apellido con contenido y dentro del largo permitido. */
    fun formularioValido(): Boolean {
        val s = _state.value
        return s.nombre.isNotBlank() && s.nombre.length <= MAX_LARGO &&
                s.apellido.isNotBlank() && s.apellido.length <= MAX_LARGO
    }

    /** Si algo cambió respecto al usuario cargado; evita mandar un PATCH vacío. */
    fun hayCambios(): Boolean {
        val actual = original ?: return false
        val s = _state.value
        return s.nombre.trim() != actual.nombre ||
                s.apellido.trim() != actual.apellido ||
                (s.cargo != null && s.cargo.tipo != actual.cargo)
    }

    fun puedeGuardar(): Boolean = formularioValido() && hayCambios() && !_state.value.guardando

    @RequiresApi(Build.VERSION_CODES.O)
    fun guardar(onSuccess: (UsuarioResponse) -> Unit) {
        val actual = original ?: return
        if (!formularioValido()) {
            _state.value = _state.value.copy(error = "Completá tu nombre y apellido.")
            return
        }

        val nombre = _state.value.nombre.trim()
        val apellido = _state.value.apellido.trim()
        val cargo = _state.value.cargo

        _state.value = _state.value.copy(guardando = true, error = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.actualizarUsuario(
                        UpdateUserRequest(nombre = nombre, apellido = apellido, cargo_id = cargo?.id)
                    )
                }
                if (!response.isSuccessful) {
                    _state.value = _state.value.copy(
                        guardando = false,
                        error = mensajeDeError(response.code())
                    )
                    Log.e("EDITAR_PERFIL", "Error al guardar: ${response.code()}")
                    return@launch
                }

                // Best-effort: si el espejado falla, el cambio en el backend ya se guardó y no
                // tiene sentido hacer fallar la pantalla por esto.
                runCatching { espejarEnSupabase(nombre, apellido, cargo?.id) }
                    .onFailure { Log.e("EDITAR_PERFIL", "No se pudo espejar en Supabase", it) }

                // Si el backend devuelve el usuario actualizado se usa ese; si no, se arma con lo
                // que ya teníamos más lo editado, para no depender de la forma de la respuesta.
                val actualizado = response.body() ?: actual.copy(
                    nombre = nombre,
                    apellido = apellido,
                    cargo = cargo?.tipo ?: actual.cargo
                )
                original = actualizado
                _state.value = _state.value.copy(guardando = false)
                onSuccess(actualizado)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    guardando = false,
                    error = "No se pudieron guardar los cambios. Revisá tu conexión."
                )
                Log.e("EDITAR_PERFIL", "Error al guardar: ${e.message}")
            }
        }
    }

    private fun mensajeDeError(codigo: Int): String = when (codigo) {
        400, 422 -> "Revisá los datos ingresados."
        401, 403 -> "Tu sesión expiró. Volvé a iniciar sesión."
        else -> "No se pudieron guardar los cambios. Intentá de nuevo."
    }
}
