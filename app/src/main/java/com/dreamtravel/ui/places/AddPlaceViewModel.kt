package com.dreamtravel.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamtravel.data.model.Place
import com.dreamtravel.data.model.SearchResult
import com.dreamtravel.data.remote.PlaceSearchService
import com.dreamtravel.data.repository.DreamRepository
import com.dreamtravel.analytics.AnalyticsEvent
import com.dreamtravel.analytics.AnalyticsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddPlaceViewModel @Inject constructor(
    private val repository: DreamRepository,
    private val searchService: PlaceSearchService,
    private val analytics: AnalyticsManager
) : ViewModel() {

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _saveComplete = MutableSharedFlow<Unit>()
    val saveComplete: SharedFlow<Unit> = _saveComplete.asSharedFlow()

    fun addPlace(name: String, cityCode: String?, lat: Double, lng: Double, dwellMinutes: Int) {
        viewModelScope.launch {
            repository.addPlace(
                Place(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    cityCode = cityCode,
                    latitude = lat,
                    longitude = lng,
                    dwellMinutes = dwellMinutes,
                    isActive = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun searchAndAddPlace(cityName: String, dwellMinutes: Int) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null

            try {
                val result: SearchResult? = searchService.searchCity(cityName)

                if (result != null) {
                    addPlace(
                        name = cityName,
                        cityCode = result.cityCode,
                        lat = result.latitude,
                        lng = result.longitude,
                        dwellMinutes = dwellMinutes
                    )
                    analytics.logEvent(
                        AnalyticsEvent.PLACE_ADDED,
                        mapOf(
                            AnalyticsEvent.Param.CITY_NAME to cityName,
                            AnalyticsEvent.Param.DWELL_MINUTES to dwellMinutes
                        )
                    )
                    _saveComplete.emit(Unit)
                } else {
                    _searchError.value = "搜索失败，请检查网络"
                }
            } catch (e: Exception) {
                _searchError.value = "搜索出错: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearchError() {
        _searchError.value = null
    }
}
