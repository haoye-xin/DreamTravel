package com.dreamtravel.ui.todos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamtravel.data.model.Todo
import com.dreamtravel.data.model.TodoStatus
import com.dreamtravel.data.repository.DreamRepository
import com.dreamtravel.analytics.AnalyticsEvent
import com.dreamtravel.analytics.AnalyticsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TodoFilter { ALL, ACTIVE, HISTORY }

@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val repository: DreamRepository,
    private val savedStateHandle: SavedStateHandle,
    private val analytics: AnalyticsManager
) : ViewModel() {

    val placeId: String = savedStateHandle["placeId"] ?: ""
    val placeName: String = savedStateHandle["placeName"] ?: ""

    private val _filter = MutableStateFlow(TodoFilter.ALL)
    val filter: StateFlow<TodoFilter> = _filter.asStateFlow()

    val todos: StateFlow<List<Todo>> = _filter
        .flatMapLatest { currentFilter ->
            when (currentFilter) {
                TodoFilter.ALL -> repository.getTodos(placeId)
                TodoFilter.ACTIVE -> repository.getActiveTodos(placeId)
                TodoFilter.HISTORY -> repository.getTodoHistory(placeId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: TodoFilter) {
        _filter.value = filter
    }

    fun markCompleted(todoId: String) {
        viewModelScope.launch {
            repository.updateTodoStatus(todoId, TodoStatus.COMPLETED)
            analytics.logEvent(AnalyticsEvent.TODO_COMPLETED, mapOf(AnalyticsEvent.Param.PLACE_ID to placeId))
        }
    }

    fun markInProgress(todoId: String) {
        viewModelScope.launch {
            repository.updateTodoStatus(todoId, TodoStatus.IN_PROGRESS)
            analytics.logEvent(AnalyticsEvent.TODO_IN_PROGRESS, mapOf(AnalyticsEvent.Param.PLACE_ID to placeId))
        }
    }

    fun markSkipped(todoId: String) {
        viewModelScope.launch {
            repository.updateTodoStatus(todoId, TodoStatus.SKIPPED)
            analytics.logEvent(AnalyticsEvent.TODO_SKIPPED, mapOf(AnalyticsEvent.Param.PLACE_ID to placeId))
        }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            repository.deleteTodo(todoId)
            analytics.logEvent(AnalyticsEvent.TODO_DELETED, mapOf(AnalyticsEvent.Param.PLACE_ID to placeId))
        }
    }
}
