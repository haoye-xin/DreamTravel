package com.dreamtravel.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.dreamtravel.R
import com.dreamtravel.data.model.Place
import com.dreamtravel.databinding.FragmentMapBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map) {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()
    private var aMap: AMap? = null
    private var places: List<Place> = emptyList()
    private var lastZoom = 0f

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            enableMyLocation()
            loadMyLocation()
        } else {
            Toast.makeText(requireContext(), "需要位置权限才能显示当前位置", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMapBinding.bind(view)

        binding.mapView.onCreate(savedInstanceState)
        val map = binding.mapView.map
        if (map != null) {
            aMap = map
            map.uiSettings.apply {
                isZoomControlsEnabled = true
                isCompassEnabled = true
                isMyLocationButtonEnabled = false // use our custom FAB
            }
            setupMapListeners()
            if (hasLocationPermission()) {
                enableMyLocation()
                viewModel.loadCurrentLocation()
            }
            observeCurrentLocation()
            observePlaces()
        }

        binding.fabMyLocation.setOnClickListener { handleMyLocationClick() }
        binding.fabMapType.setOnClickListener { toggleMapType() }
    }

    // ── Map listeners ──────────────────────────────────────────────

    private fun setupMapListeners() {
        val map = aMap ?: return
        map.setOnMarkerClickListener { marker ->
            val obj = marker.`object`
            when (obj) {
                is ClusterInfo -> {
                    val center = LatLng(
                        obj.places.map { it.latitude }.average(),
                        obj.places.map { it.longitude }.average()
                    )
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(center, map.cameraPosition.zoom + 2f)
                    )
                    true
                }
                is Place -> {
                    if (marker.isInfoWindowShown) {
                        marker.hideInfoWindow()
                    } else {
                        marker.showInfoWindow()
                    }
                    true
                }
                else -> false
            }
        }

        map.setOnInfoWindowClickListener { marker ->
            val obj = marker.`object`
            if (obj is Place) {
                val bundle = Bundle().apply {
                    putString("placeId", obj.id)
                    putString("placeName", obj.name)
                }
                findNavController().navigate(R.id.action_map_to_todoList, bundle)
            }
        }

        map.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChange(position: CameraPosition?) {}
            override fun onCameraChangeFinish(position: CameraPosition?) {
                val currentZoom = position?.zoom ?: return
                if (kotlin.math.abs(currentZoom - lastZoom) > 0.5f) {
                    lastZoom = currentZoom
                    renderMarkers()
                }
            }
        })
    }

    // ── Places + markers ───────────────────────────────────────────

    private fun observePlaces() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.places.collect { newPlaces ->
                    val isFirstLoad = places.isEmpty() && newPlaces.isNotEmpty()
                    places = newPlaces
                    renderMarkers()
                    if (isFirstLoad) centerMapInitially()
                }
            }
        }
    }

    private fun renderMarkers() {
        val map = aMap ?: return
        map.clear()
        if (places.isEmpty()) return

        val zoom = map.cameraPosition.zoom
        if (places.size > 5 && zoom < 14f) {
            renderClusteredMarkers(map)
        } else {
            renderIndividualMarkers(map)
        }
    }

    private fun renderIndividualMarkers(map: AMap) {
        for (place in places) {
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(place.latitude, place.longitude))
                    .title(place.name)
                    .snippet("${place.dwellMinutes}分钟驻留")
            )
            marker.`object` = place
        }
    }

    private fun renderClusteredMarkers(map: AMap) {
        val projection = map.projection
        val clustered = BooleanArray(places.size)
        val groups = mutableListOf<MutableList<Int>>()

        for (i in places.indices) {
            if (clustered[i]) continue
            val group = mutableListOf(i)
            val p1 = projection.toScreenLocation(LatLng(places[i].latitude, places[i].longitude))
            for (j in i + 1 until places.size) {
                if (clustered[j]) continue
                val p2 = projection.toScreenLocation(LatLng(places[j].latitude, places[j].longitude))
                val dx = (p1.x - p2.x).toDouble()
                val dy = (p1.y - p2.y).toDouble()
                if (sqrt(dx * dx + dy * dy) < 60.0) {
                    group.add(j)
                    clustered[j] = true
                }
            }
            clustered[i] = true
            groups.add(group)
        }

        for (group in groups) {
            if (group.size == 1) {
                val place = places[group[0]]
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(LatLng(place.latitude, place.longitude))
                        .title(place.name)
                        .snippet("${place.dwellMinutes}分钟驻留")
                )
                marker.`object` = place
            } else {
                val avgLat = group.map { places[it].latitude }.average()
                val avgLng = group.map { places[it].longitude }.average()
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(LatLng(avgLat, avgLng))
                        .title("${group.size} 个地点")
                        .snippet("点击放大地图查看详情")
                )
                marker.`object` = ClusterInfo(group.map { places[it] })
            }
        }
    }

    // ── Initial centering ──────────────────────────────────────────

    private fun centerMapInitially() {
        if (hasLocationPermission()) {
            viewModel.loadCurrentLocation()
        }
        if (places.isNotEmpty()) {
            zoomToFitAllMarkers()
        }
    }

    private fun zoomToFitAllMarkers() {
        if (places.isEmpty()) return
        val builder = LatLngBounds.Builder()
        for (place in places) {
            builder.include(LatLng(place.latitude, place.longitude))
        }
        aMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
    }

    // ── Location ───────────────────────────────────────────────────

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun enableMyLocation() {
        try {
            aMap?.isMyLocationEnabled = true
        } catch (_: SecurityException) {
            // Permission not granted — skip
        }
    }

    private fun handleMyLocationClick() {
        if (hasLocationPermission()) {
            enableMyLocation()
            loadMyLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun loadMyLocation() {
        Toast.makeText(requireContext(), "正在获取位置...", Toast.LENGTH_SHORT).show()
        viewModel.loadCurrentLocation()
    }

    private fun observeCurrentLocation() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentLocation.collect { latLng ->
                    latLng?.let {
                        aMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(it, 15f)
                        )
                    }
                }
            }
        }
    }

    // ── Map type ───────────────────────────────────────────────────

    private fun toggleMapType() {
        aMap?.let { map ->
            map.mapType = if (map.mapType == AMap.MAP_TYPE_NORMAL) {
                AMap.MAP_TYPE_SATELLITE
            } else {
                AMap.MAP_TYPE_NORMAL
            }
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────

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
        binding.mapView.onDestroy()
        _binding = null
    }
}

/** Holds the list of [Place]s represented by a cluster marker. */
private data class ClusterInfo(val places: List<Place>)
