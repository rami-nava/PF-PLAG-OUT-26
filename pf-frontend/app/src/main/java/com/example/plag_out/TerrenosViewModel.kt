package com.example.plag_out

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TerrenoUIState(
    var isLoading: Boolean = true,
    val terrenos: List<TerrenoResponse> = emptyList()
)

class TerrenosViewModel(context: Context, repository: TerrenoRepository) : ViewModel() {

    private val _state = MutableStateFlow(TerrenoUIState())
    val state: StateFlow<TerrenoUIState> = _state.asStateFlow()
    val terrenoRepository = repository
    val context = context


    @RequiresApi(Build.VERSION_CODES.O)
    fun getTerrenos() {
        viewModelScope.launch {
            val lastFetch = getFetched()

            // Verificar si nunca se consulto
            if (!lastFetch) {
                _state.value = _state.value.copy(isLoading = true)

                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.gddService.getTerrenos()
                    }

                    if (response.isSuccessful) {
                        val terrenosResponse = response.body() ?: emptyList()

                        _state.value = _state.value.copy(
                            terrenos = terrenosResponse,
                            isLoading = false
                        )

                        terrenoRepository.guardarTerrenos(terrenosResponse)
                        saveFetched()
                    }
                } catch (e: Exception) {
                    _state.value = _state.value.copy(isLoading = false)
                    Log.e("TERRENOS", "Error: ${e.message}")
                }
            } else {
                // Cargar del caché
                val cachedTerrenos = withContext(Dispatchers.IO) {
                    terrenoRepository.obtenerTerrenos()
                }

                _state.value = _state.value.copy(
                    terrenos = cachedTerrenos,
                    isLoading = false
                )
            }
        }
    }

    private fun getFetched(): Boolean {
        val prefs = context.getSharedPreferences("terrenos_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("fetched", false)
    }

    private fun saveFetched() {
        val prefs = context.getSharedPreferences("terrenos_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("fetched", true)
            .apply()
    }
}

class TerrenosViewModelFactory(
    private val context: Context,
    private val repository: TerrenoRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TerrenosViewModel(context,repository) as T
    }
}