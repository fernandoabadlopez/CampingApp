package com.campingapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.campingapp.model.Camping
import com.campingapp.repository.CampingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CampingRepository(application)

    private val _campings = MutableStateFlow<List<Camping>>(emptyList())
    val campings: StateFlow<List<Camping>> = _campings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadFromJson()
    }

    fun loadFromJson() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _campings.value = repository.getCampingsFromJson()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFromApi() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _campings.value = repository.getCampingsFromApi()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}
