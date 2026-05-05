package com.dreamtravel.ui.todos

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamtravel.data.model.Todo
import com.dreamtravel.data.model.TodoStatus
import com.dreamtravel.data.repository.DreamRepository
import com.dreamtravel.analytics.AnalyticsEvent
import com.dreamtravel.analytics.AnalyticsManager
import com.dreamtravel.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddTodoViewModel @Inject constructor(
    private val repository: DreamRepository,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val analytics: AnalyticsManager
) : ViewModel() {

    val placeId: String = savedStateHandle["placeId"] ?: ""
    val todoId: String = savedStateHandle["todoId"] ?: ""

    val isEditing: Boolean get() = todoId.isNotBlank()

    private val _existingTodo = MutableStateFlow<Todo?>(null)
    val existingTodo: StateFlow<Todo?> = _existingTodo.asStateFlow()

    private val _defaultRemindInterval = MutableStateFlow(defaultRemindInterval())
    val defaultRemindInterval: StateFlow<Int> = _defaultRemindInterval.asStateFlow()

    init {
        if (isEditing) {
            loadTodo()
        }
    }

    private fun loadTodo() {
        viewModelScope.launch {
            val todo = repository.getTodoById(todoId)
            _existingTodo.value = todo
        }
    }

    fun addTodo(title: String, notes: String, remindIntervalMinutes: Int) {
        viewModelScope.launch {
            saveDefaultRemindInterval(remindIntervalMinutes)
            repository.addTodo(
                Todo(
                    id = UUID.randomUUID().toString(),
                    placeId = placeId,
                    title = title,
                    notes = notes,
                    status = TodoStatus.PENDING,
                    remindIntervalMinutes = remindIntervalMinutes,
                    remindCount = 0,
                    createdAt = System.currentTimeMillis(),
                    completedAt = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
            analytics.logEvent(AnalyticsEvent.TODO_ADDED, mapOf(AnalyticsEvent.Param.PLACE_ID to placeId))
        }
    }

    fun updateTodo(title: String, notes: String, remindIntervalMinutes: Int) {
        viewModelScope.launch {
            saveDefaultRemindInterval(remindIntervalMinutes)
            val existing = _existingTodo.value ?: return@launch
            repository.updateTodo(
                existing.copy(
                    title = title,
                    notes = notes,
                    remindIntervalMinutes = remindIntervalMinutes,
                    updatedAt = System.currentTimeMillis()
                )
            )
            analytics.logEvent(AnalyticsEvent.TODO_EDITED, mapOf(AnalyticsEvent.Param.PLACE_ID to placeId))
        }
    }

    // ─── Preferences ─────────────────────────────────────────

    private fun defaultRemindInterval(): Int {
        return prefs().getInt(PREF_KEY_REMIND_INTERVAL, Constants.DEFAULT_REMIND_INTERVAL_MINUTES)
    }

    private fun saveDefaultRemindInterval(minutes: Int) {
        prefs().edit().putInt(PREF_KEY_REMIND_INTERVAL, minutes).apply()
    }

    private fun prefs(): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_NAME = "dream_travel_prefs"
        private const val PREF_KEY_REMIND_INTERVAL = "default_remind_interval_minutes"
    }
}
