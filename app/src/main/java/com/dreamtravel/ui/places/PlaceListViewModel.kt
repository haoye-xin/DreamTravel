package com.dreamtravel.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamtravel.data.model.Place
import com.dreamtravel.data.repository.DreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaceListViewModel @Inject constructor(
    private val repository: DreamRepository
) : ViewModel() {

    val places: StateFlow<List<Place>> = repository.getPlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deletePlace(placeId: String) {
        viewModelScope.launch { repository.deletePlace(placeId) }
    }

    fun toggleActive(placeId: String, isActive: Boolean) {
        viewModelScope.launch { repository.setPlaceActive(placeId, isActive) }
    }
}
