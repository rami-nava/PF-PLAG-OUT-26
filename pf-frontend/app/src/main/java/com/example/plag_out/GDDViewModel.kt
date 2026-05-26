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
import java.io.Console
import java.time.LocalDate

data class AppState @RequiresApi(Build.VERSION_CODES.O) constructor(
    val latitude: Double = -34.6037,
    val longitude: Double = -58.3816,
    val startDate: String = LocalDate.now().toString(),
    val currentDate: String = LocalDate.now().toString(),
    val initialGDD: Int = 0,
    val currentGDD: Int = 0,
    val targetGDD: Int = 500,
    val baseTemp: Double = 10.0,
    val cropName: String = "Plaga Desconocida",
    val notes: String = "",

    // Estado de simulación
    val lastResponse: GDDSimulationResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class GDDViewModel : ViewModel() {

    @RequiresApi(Build.VERSION_CODES.O)
    private val _state = MutableStateFlow(AppState())
    @RequiresApi(Build.VERSION_CODES.O)
    val state: StateFlow<AppState> = _state.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateConfig(
        latitude: Double? = null,
        longitude: Double? = null,
        startDate: String? = null,
        initialGDD: Int? = null,
        targetGDD: Int? = null,
        baseTemp: Double? = null,
        cropName: String? = null,
        notes: String? = null
    ) {
        _state.value = _state.value.copy(
            latitude = latitude ?: _state.value.latitude,
            longitude = longitude ?: _state.value.longitude,
            startDate = startDate ?: _state.value.startDate,
            initialGDD = initialGDD ?: _state.value.initialGDD,
            currentGDD = initialGDD ?: _state.value.currentGDD,
            targetGDD = targetGDD ?: _state.value.targetGDD,
            baseTemp = baseTemp ?: _state.value.baseTemp,
            cropName = cropName ?: _state.value.cropName,
            notes = notes ?: _state.value.notes,
            currentDate = startDate ?: _state.value.currentDate
        )
        clearMessages()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun simulateDay() {
        viewModelScope.launch(Dispatchers.IO) {

            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val nextDate = LocalDate.parse(_state.value.currentDate)
                .plusDays(1)
                .toString()

            val request = GDDSimulationRequest(
                latitude = _state.value.latitude,
                longitude = _state.value.longitude,
                startDate = _state.value.startDate,
                currentDate = _state.value.currentDate,
                initialGDD = _state.value.currentGDD,
                targetGDD = _state.value.targetGDD,
                baseTemperature = _state.value.baseTemp,
                cropName = _state.value.cropName,
                notes = _state.value.notes
            )

            try {
                val response = RetrofitClient.gddService.simulateDay(request)

                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        _state.value = _state.value.copy(
                            currentGDD = data.currentGDD,
                            currentDate = nextDate,
                            lastResponse = data,
                            isLoading = false,
                            successMessage = data.message
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Error: ${response.code()} ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                Log.e("GDD_API", "Error: ${e.message}", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun resetSimulation() {
        _state.value = _state.value.copy(
            currentGDD = _state.value.initialGDD,
            currentDate = _state.value.startDate,
            lastResponse = null,
            successMessage = "Simulación reiniciada"
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun clearMessages() {
        _state.value = _state.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

}