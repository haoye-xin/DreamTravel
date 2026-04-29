package com.dreamtravel.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamtravel.data.model.Place
import com.dreamtravel.data.repository.DreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddPlaceViewModel @Inject constructor(
    private val repository: DreamRepository
) : ViewModel() {

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
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
}
