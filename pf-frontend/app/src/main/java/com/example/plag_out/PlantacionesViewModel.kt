package com.example.plag_out

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlantacionUIState(
    val isLoading: Boolean = false,
    val plantaciones: List<PlantacionesResponse> = emptyList()
)

class PlantacionesViewModel : ViewModel() {

    private val _state = MutableStateFlow(PlantacionUIState())
    val state: StateFlow<PlantacionUIState> = _state.asStateFlow()


    @RequiresApi(Build.VERSION_CODES.O)
    fun getPlantaciones() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.gddService.getPlantaciones()
                if (response.isSuccessful) {
                    val plantacionResponse = response.body() ?: emptyList()
                    _state.value = _state.value.copy(
                        plantaciones = plantacionResponse,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
                Log.e("PLANTACIONES", "Error: ${e.message}")
            }

        }

    }
}