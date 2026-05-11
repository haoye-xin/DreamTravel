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

    private val _selectedRegion = MutableStateFlow<SavedRegion?>(null)
    val selectedRegion: StateFlow<SavedRegion?> = _selectedRegion.asStateFlow()

    init {
        if (isEditing) {
            loadTodo()
        }
    }

    private fun loadTodo() {
        viewModelScope.launch {
            val todo = repository.getTodoById(todoId)
            _existingTodo.value = todo
            todo?.let { t ->
                if (t.provinceCode != null && t.formattedAddress != null) {
                    _selectedRegion.value = SavedRegion(
                        provinceCode = t.provinceCode,
                        provinceName = t.provinceName,
                        cityCode = t.cityCode,
                        cityName = t.cityName,
                        districtCode = t.districtCode,
                        districtName = t.districtName,
                        formattedAddress = t.formattedAddress
                    )
                }
            }
        }
    }

    fun onRegionSelected(
        provinceCode: String?,
        provinceName: String?,
        cityCode: String?,
        cityName: String?,
        districtCode: String?,
        districtName: String?,
        formattedAddress: String?
    ) {
        if (provinceCode == null || formattedAddress == null) return
        _selectedRegion.value = SavedRegion(
            provinceCode = provinceCode,
            provinceName = provinceName,
            cityCode = cityCode,
            cityName = cityName,
            districtCode = districtCode,
            districtName = districtName,
            formattedAddress = formattedAddress
        )
    }

    suspend fun addTodoSuspend(title: String, notes: String, remindIntervalMinutes: Int, color: String? = null) {
        saveDefaultRemindInterval(remindIntervalMinutes)
        val region = _selectedRegion.value
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
                updatedAt = System.currentTimeMillis(),
                provinceCode = region?.provinceCode,
                provinceName = region?.provinceName,
                cityCode = region?.cityCode,
                cityName = region?.cityName,
                districtCode = region?.districtCode,
                districtName = region?.districtName,
                formattedAddress = region?.formattedAddress,
                color = color
            )
        )
        analytics.logEvent(AnalyticsEvent.TODO_ADDED, mapOf(AnalyticsEvent.Param.PLACE_ID to placeId))
    }

    suspend fun updateTodoSuspend(title: String, notes: String, remindIntervalMinutes: Int, color: String? = null) {
        saveDefaultRemindInterval(remindIntervalMinutes)
        val existing = _existingTodo.value ?: return
        val region = _selectedRegion.value
        repository.updateTodo(
            existing.copy(
                title = title,
                notes = notes,
                remindIntervalMinutes = remindIntervalMinutes,
                updatedAt = System.currentTimeMillis(),
                provinceCode = region?.provinceCode ?: existing.provinceCode,
                provinceName = region?.provinceName ?: existing.provinceName,
                cityCode = region?.cityCode ?: existing.cityCode,
                cityName = region?.cityName ?: existing.cityName,
                districtCode = region?.districtCode ?: existing.districtCode,
                districtName = region?.districtName ?: existing.districtName,
                formattedAddress = region?.formattedAddress ?: existing.formattedAddress,
                color = color
            )
        )
        analytics.logEvent(AnalyticsEvent.TODO_EDITED, mapOf(AnalyticsEvent.Param.PLACE_ID to placeId))
    }

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

data class SavedRegion(
    val provinceCode: String?,
    val provinceName: String?,
    val cityCode: String?,
    val cityName: String?,
    val districtCode: String?,
    val districtName: String?,
    val formattedAddress: String?
)
