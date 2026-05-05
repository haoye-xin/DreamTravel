package com.dreamtravel.ui.places

import android.os.Bundle
import android.view.View
import android.widget.NumberPicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.dreamtravel.R
import com.dreamtravel.databinding.FragmentAddPlaceBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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

        observeViewModel()

        binding.btnSave.setOnClickListener {
            val cityName = binding.editCityName.text.toString().trim()
            if (cityName.isBlank()) {
                binding.editCityName.error = "请输入城市名"
                return@setOnClickListener
            }

            viewModel.searchAndAddPlace(cityName, selectedDwell)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isSearching.collect { searching ->
                    binding.btnSave.isEnabled = !searching
                    binding.btnSave.text = if (searching) getString(R.string.searching_city) else "保存梦想之地"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchError.collect { error ->
                    if (error != null) {
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                        viewModel.clearSearchError()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveComplete.collect {
                    findNavController().popBackStack()
                }
            }
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
