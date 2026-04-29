package com.dreamtravel.ui.places

import android.os.Bundle
import android.view.View
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dreamtravel.R
import com.dreamtravel.databinding.FragmentAddPlaceBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class AddPlaceFragment : Fragment(R.layout.fragment_add_place) {

    private var _binding: FragmentAddPlaceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddPlaceViewModel by viewModels()

    private val dwellOptions = intArrayOf(15, 30, 45, 60, 90, 120)
    private var selectedDwell = 30

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddPlaceBinding.bind(view)

        setupNumberPicker()

        binding.btnSave.setOnClickListener {
            val cityName = binding.editCityName.text.toString().trim()
            if (cityName.isBlank()) {
                binding.editCityName.error = "请输入城市名"
                return@setOnClickListener
            }

            // Demo: use default coordinates (大理)
            // In production, this would use AMap search API
            viewModel.addPlace(
                name = cityName,
                cityCode = null,
                lat = 25.6,
                lng = 100.2,
                dwellMinutes = selectedDwell
            )
            findNavController().popBackStack()
        }
    }

    private fun setupNumberPicker() {
        binding.pickerDwell.apply {
            minValue = 0
            maxValue = dwellOptions.size - 1
            displayedValues = dwellOptions.map { "${it} 分钟" }.toTypedArray()
            value = 1 // default 30 min
            setOnValueChangedListener { _, _, newVal ->
                selectedDwell = dwellOptions[newVal]
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
