package com.example.plag_out

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.FeedbackPrediccionPendiente
import com.example.plag_out.AlmacenamientoLocal.FeedbackPrediccionRepository
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class PrediccionDetalleUIState(
    val isLoading: Boolean = false,
    val prediccion: PrediccionDetalleResponse? = null,
    val feedbackPendiente: FeedbackPrediccionPendiente? = null,
    val enviando: Boolean = false,
    val noDisponible: Boolean = false,
    val biofixResultado: BiofixResult? = null,
    val error: String? = null
)

class PrediccionDetalleViewModel(
    private val feedbackRepository: FeedbackPrediccionRepository,
    private val gddService: GDDService = RetrofitClient.gddService,
    private val ownerIdProvider: () -> String? = {
        SupabaseProvider.client.auth.currentUserOrNull()?.id
    }
) : ViewModel() {

    private val _state = MutableStateFlow(PrediccionDetalleUIState())
    val state: StateFlow<PrediccionDetalleUIState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            PresenceRetryEvents.events.collect { event ->
                if (event.kind == "ml" && event.owner == ownerIdProvider() && event.id == _state.value.prediccion?.id) {
                    _state.value = _state.value.copy(biofixResultado = event.result)
                    cargar(event.id)
                }
            }
        }
    }

    fun cargar(prediccionId: Int, aviso: String? = null) {
        val ownerId = ownerIdProvider()
        if (ownerId == null) {
            _state.value = PrediccionDetalleUIState(error = "Tu sesión expiró. Volvé a iniciar sesión.")
            return
        }

        _state.value = _state.value.copy(isLoading = true, error = null, noDisponible = false)
        viewModelScope.launch {
            val pendiente = withContext(Dispatchers.IO) {
                feedbackRepository.obtener(ownerId, prediccionId)
            }
            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.getPrediccion(prediccionId)
                }
                val prediccion = response.body()
                if (response.isSuccessful && prediccion != null) {
                    val terminal = prediccion.confirmacion.estado in setOf("respondida", "vencida", "no_solicitada")
                    if (terminal && pendiente != null) {
                        withContext(Dispatchers.IO) {
                            feedbackRepository.eliminar(ownerId, prediccionId)
                        }
                    }
                    _state.value = PrediccionDetalleUIState(
                        prediccion = prediccion,
                        error = aviso,
                        biofixResultado = _state.value.biofixResultado,
                        feedbackPendiente = if (terminal) null else pendiente
                    )
                } else if (response.code() == 404) {
                    borrarPendiente(ownerId, prediccionId)
                    _state.value = PrediccionDetalleUIState(noDisponible = true)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        feedbackPendiente = pendiente,
                        error = mensajeCarga(response.code())
                    )
                }
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    feedbackPendiente = pendiente,
                    error = "No se pudo cargar la predicción. Revisá tu conexión."
                )
            }
        }
    }

    fun responder(respuesta: String, biofix: BiofixRequest? = null) {
        if (respuesta == "presente" && biofix == null) return
        if (respuesta !in RESPUESTAS || _state.value.enviando) return
        val prediccion = _state.value.prediccion ?: return
        if (prediccion.confirmacion.estado != "pendiente") return
        val ownerId = ownerIdProvider() ?: run {
            _state.value = _state.value.copy(error = "Tu sesión expiró. Volvé a iniciar sesión.")
            return
        }

        _state.value = _state.value.copy(enviando = true)
        viewModelScope.launch {
            val existente = withContext(Dispatchers.IO) {
                feedbackRepository.obtener(ownerId, prediccion.id)
            }
            if (existente != null && existente.estado != "requiere_revision" && existente.respuesta != respuesta) {
                _state.value = _state.value.copy(
                    feedbackPendiente = existente,
                    enviando = false,
                    error = "Ya hay una respuesta pendiente. Reintentá ese envío antes de continuar."
                )
                return@launch
            }
            if (existente?.estado == "requiere_revision") borrarPendiente(ownerId, prediccion.id)
            val feedback = existente?.takeUnless { it.estado == "requiere_revision" } ?: FeedbackPrediccionPendiente(
                owner_id = ownerId,
                prediccion_id = prediccion.id,
                respuesta = respuesta,
                biofix_json = biofix?.let { com.google.gson.Gson().toJson(it) },
                idempotency_key = biofix?.idempotency_key ?: UUID.randomUUID().toString()
            ).also { withContext(Dispatchers.IO) { feedbackRepository.guardar(it) } }

            _state.value = _state.value.copy(feedbackPendiente = feedback)
            enviar(feedback)
        }
    }

    fun reintentar() {
        val feedback = _state.value.feedbackPendiente ?: return
        if (feedback.owner_id != ownerIdProvider() || _state.value.enviando) return
        viewModelScope.launch { enviar(feedback) }
    }

    private suspend fun enviar(feedback: FeedbackPrediccionPendiente) {
        if (feedback.respuesta == "presente" && (feedback.biofix_json == null || feedback.estado == "requiere_revision")) {
            _state.value = _state.value.copy(error = "Revisá la fecha y el ciclo antes de confirmar presencia.")
            return
        }
        _state.value = _state.value.copy(enviando = true, error = null)
        try {
            val response = withContext(Dispatchers.IO) {
                gddService.confirmarPrediccion(
                    feedback.prediccion_id,
                    PrediccionConfirmacionRequest(
                        biofix = feedback.biofix_json?.let { com.google.gson.Gson().fromJson(it, BiofixRequest::class.java) },
                        respuesta = feedback.respuesta,
                        idempotency_key = feedback.idempotency_key
                    )
                )
            }
            when {
                response.isSuccessful -> {
                    borrarPendiente(feedback.owner_id, feedback.prediccion_id)
                    val actual = _state.value.prediccion
                    _state.value = _state.value.copy(
                        prediccion = actual?.copy(
                            confirmacion = actual.confirmacion.copy(
                                estado = "respondida",
                                respuesta = response.body()?.respuesta ?: feedback.respuesta,
                                respondido_en = response.body()?.respondido_en
                            )
                        ),
                        feedbackPendiente = null,
                        biofixResultado = response.body()?.biofix,
                        enviando = false
                    )
                }
                response.code() == 409 -> {
                    borrarPendiente(feedback.owner_id, feedback.prediccion_id)
                    _state.value = _state.value.copy(feedbackPendiente = null, enviando = false)
                    _state.value = _state.value.copy(error = "La solicitud cambió. Actualizá la predicción y revisá la selección del ciclo antes de volver a confirmar.")
                    cargar(feedback.prediccion_id, "La solicitud cambió. Revisá la fecha y el ciclo antes de confirmar de nuevo.")
                }
                response.code() == 410 -> {
                    borrarPendiente(feedback.owner_id, feedback.prediccion_id)
                    val actual = _state.value.prediccion
                    _state.value = _state.value.copy(
                        prediccion = actual?.copy(
                            confirmacion = actual.confirmacion.copy(estado = "vencida")
                        ),
                        feedbackPendiente = null,
                        enviando = false
                    )
                }
                response.code() == 404 -> {
                    borrarPendiente(feedback.owner_id, feedback.prediccion_id)
                    _state.value = PrediccionDetalleUIState(noDisponible = true)
                }
                response.code() == 422 -> {
                    borrarPendiente(feedback.owner_id, feedback.prediccion_id)
                    _state.value = _state.value.copy(
                        feedbackPendiente = null,
                        enviando = false,
                        error = "La aplicación no pudo enviar una respuesta válida. Actualizala e intentá nuevamente."
                    )
                }
                else -> _state.value = _state.value.copy(
                    enviando = false,
                    error = if (response.code() in setOf(401, 403)) {
                        "Tu sesión expiró. Volvé a iniciar sesión."
                    } else {
                        if (feedback.respuesta == "presente")
                            "Envío pendiente. Se reintentará automáticamente con conexión; también podés reintentar ahora."
                        else "No se pudo enviar la respuesta. Podés reintentar sin duplicarla."
                    }
                )
            }
        } catch (_: Exception) {
            _state.value = _state.value.copy(
                enviando = false,
                error = if (feedback.respuesta == "presente")
                            "Envío pendiente. Se reintentará automáticamente con conexión; también podés reintentar ahora."
                        else "No se pudo enviar la respuesta. Podés reintentar sin duplicarla."
            )
        }
    }

    private suspend fun borrarPendiente(ownerId: String, prediccionId: Int) =
        withContext(Dispatchers.IO) { feedbackRepository.eliminar(ownerId, prediccionId) }

    private fun mensajeCarga(codigo: Int) = when (codigo) {
        401, 403 -> "Tu sesión expiró. Volvé a iniciar sesión."
        else -> "No se pudo cargar la predicción. Intentá nuevamente."
    }

    companion object {
        val RESPUESTAS = setOf("presente", "no_observada", "no_verificada")
    }
}

class PrediccionDetalleViewModelFactory(
    private val feedbackRepository: FeedbackPrediccionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PrediccionDetalleViewModel(feedbackRepository) as T
}
