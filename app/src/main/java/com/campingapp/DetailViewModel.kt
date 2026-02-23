package com.campingapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.campingapp.model.Comment
import com.campingapp.repository.CampingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(
    application: Application,
    private val campingId: String
) : AndroidViewModel(application) {

    private val repository = CampingRepository(application)

    val comments: StateFlow<List<Comment>> = repository.getComments(campingId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addComment(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addComment(campingId, text.trim())
        }
    }

    fun deleteComment(comment: Comment) {
        viewModelScope.launch {
            repository.deleteComment(comment)
        }
    }
}
