package com.example.campingapp.database

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers // IMPORTANTE: Importamos los hilos
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoriteViewModel(private val dao: FavoriteDao) : ViewModel() {

    val allFavorites: StateFlow<List<FavoriteEntity>> = dao.getAllFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addFavorite(campingId: Int) {
        // Le decimos que use el hilo IO (Input/Output) para la base de datos
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertFavorite(FavoriteEntity(campingId))
        }
    }

    fun removeFavorite(campingId: Int) {
        // Igual aquí, usamos Dispatchers.IO
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteFavorite(FavoriteEntity(campingId))
        }
    }
}

class FavoriteViewModelFactory(private val dao: FavoriteDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoriteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavoriteViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}