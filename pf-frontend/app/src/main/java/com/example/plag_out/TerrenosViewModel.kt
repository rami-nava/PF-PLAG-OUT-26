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

data class TerrenoUIState(
    val isLoading: Boolean = false,
    val terrenos: List<TerrenoResponse> = emptyList()
)

class TerrenosViewModel : ViewModel() {

    private val _state = MutableStateFlow(TerrenoUIState())
    val state: StateFlow<TerrenoUIState> = _state.asStateFlow()


    @RequiresApi(Build.VERSION_CODES.O)
    fun getTerrenos() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.gddService.getTerrenos()
                if (response.isSuccessful) {
                    val terrenosResponse = response.body() ?: emptyList()
                    _state.value = _state.value.copy(
                        terrenos = terrenosResponse,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
                Log.e("TERRENOS", "Error: ${e.message}")
            }

        }

    }
}