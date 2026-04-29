package com.dreamtravel.ui.map

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.dreamtravel.R
import com.dreamtravel.databinding.FragmentMapBinding
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map) {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()
    private var aMap: AMap? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMapBinding.bind(view)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { map ->
            aMap = map
            map.uiSettings.isZoomControlsEnabled = true
            loadPlacesOnMap()
        }

        binding.fabMyLocation.setOnClickListener {
            aMap?.let { map ->
                // Default to Dali for demo
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(25.6, 100.2), 12f)
                )
            }
        }
    }

    private fun loadPlacesOnMap() {
        viewModel.places.observe(viewLifecycleOwner) { places ->
            aMap?.let { map ->
                map.clear()
                for (place in places) {
                    map.addMarker(
                        MarkerOptions()
                            .position(LatLng(place.latitude, place.longitude))
                            .title(place.name)
                            .snippet("${place.dwellMinutes}分钟驻留")
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }
}
