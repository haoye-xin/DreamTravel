package com.dreamtravel.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamtravel.data.model.Place
import com.dreamtravel.data.repository.DreamRepository
import com.dreamtravel.analytics.AnalyticsEvent
import com.dreamtravel.analytics.AnalyticsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaceListViewModel @Inject constructor(
    private val repository: DreamRepository,
    private val analytics: AnalyticsManager
) : ViewModel() {

    val places: StateFlow<List<Place>> = repository.getPlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deletePlace(placeId: String) {
        viewModelScope.launch {
            repository.deletePlace(placeId)
            analytics.logEvent(AnalyticsEvent.PLACE_DELETED, mapOf(AnalyticsEvent.Param.PLACE_ID to placeId))
        }
    }

    fun toggleActive(placeId: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.setPlaceActive(placeId, isActive)
            analytics.logEvent(
                AnalyticsEvent.PLACE_TOGGLED,
                mapOf(
                    AnalyticsEvent.Param.PLACE_ID to placeId,
                    AnalyticsEvent.Param.IS_ACTIVE to isActive
                )
            )
        }
    }
}
