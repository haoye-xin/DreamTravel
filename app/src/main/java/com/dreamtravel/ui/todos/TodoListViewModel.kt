package com.dreamtravel.ui.todos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamtravel.data.model.Todo
import com.dreamtravel.data.model.TodoStatus
import com.dreamtravel.data.repository.DreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val repository: DreamRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val placeId: String = savedStateHandle["placeId"] ?: ""
    val placeName: String = savedStateHandle["placeName"] ?: ""

    val todos: StateFlow<List<Todo>> = repository.getTodos(placeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markCompleted(todoId: String) {
        viewModelScope.launch { repository.updateTodoStatus(todoId, TodoStatus.COMPLETED) }
    }

    fun markInProgress(todoId: String) {
        viewModelScope.launch { repository.updateTodoStatus(todoId, TodoStatus.IN_PROGRESS) }
    }

    fun markSkipped(todoId: String) {
        viewModelScope.launch { repository.updateTodoStatus(todoId, TodoStatus.SKIPPED) }
    }
}
