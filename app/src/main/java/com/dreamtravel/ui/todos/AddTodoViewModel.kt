package com.dreamtravel.ui.todos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamtravel.data.model.Todo
import com.dreamtravel.data.model.TodoStatus
import com.dreamtravel.data.repository.DreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddTodoViewModel @Inject constructor(
    private val repository: DreamRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val placeId: String = savedStateHandle["placeId"] ?: ""

    fun addTodo(title: String, notes: String) {
        viewModelScope.launch {
            repository.addTodo(
                Todo(
                    id = UUID.randomUUID().toString(),
                    placeId = placeId,
                    title = title,
                    notes = notes,
                    status = TodoStatus.PENDING,
                    remindIntervalMinutes = 1440,
                    remindCount = 0,
                    createdAt = System.currentTimeMillis(),
                    completedAt = null
                )
            )
        }
    }
}
