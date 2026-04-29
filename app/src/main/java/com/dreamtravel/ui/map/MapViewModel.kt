package com.dreamtravel.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamtravel.data.model.Place
import com.dreamtravel.data.repository.DreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: DreamRepository
) : ViewModel() {

    val places: StateFlow<List<Place>> = repository.getPlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
