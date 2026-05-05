package com.dreamtravel.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amap.api.maps.model.LatLng
import com.dreamtravel.data.model.Place
import com.dreamtravel.data.repository.DreamRepository
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: DreamRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    val places: StateFlow<List<Place>> = repository.getPlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _locationLoading = MutableStateFlow(false)
    val locationLoading: StateFlow<Boolean> = _locationLoading.asStateFlow()

    fun loadCurrentLocation() {
        viewModelScope.launch {
            _locationLoading.value = true
            try {
                val location = fusedLocationClient.lastLocation.await()
                location?.let {
                    _currentLocation.value = LatLng(it.latitude, it.longitude)
                }
            } catch (_: SecurityException) {
                // Permission denied — handled by Fragment
            } catch (_: Exception) {
                // Location is best-effort; silently ignore failures
            } finally {
                _locationLoading.value = false
            }
        }
    }
}
