package com.dreamtravel.ui.places

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dreamtravel.R
import com.dreamtravel.databinding.FragmentPlaceListBinding
import com.dreamtravel.data.model.Place
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaceListFragment : Fragment(R.layout.fragment_place_list) {

    private var _binding: FragmentPlaceListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlaceListViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaceListBinding.bind(view)

        setupRecyclerView()
        observeViewModel()

        binding.fabAddPlace.setOnClickListener {
            findNavController().navigate(R.id.action_placeList_to_addPlace)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerPlaces.layoutManager = LinearLayoutManager(requireContext())
        // Adapter will be set in observeViewModel
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.places.collect { places ->
                    binding.textEmpty.visibility = if (places.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerPlaces.adapter = PlaceAdapter(
                        places,
                        onItemClick = { place ->
                            val bundle = Bundle().apply {
                                putString("placeId", place.id)
                                putString("placeName", place.name)
                            }
                            findNavController().navigate(
                                R.id.action_placeList_to_todoList,
                                bundle
                            )
                        },
                        onDeleteClick = { place -> showDeleteDialog(place) },
                        onToggleActive = { place, isActive ->
                            viewModel.toggleActive(place.id, isActive)
                        }
                    )
                }
            }
        }
    }

    private fun showDeleteDialog(place: Place) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除")
            .setMessage(R.string.delete_confirm)
            .setPositiveButton("删除") { _, _ -> viewModel.deletePlace(place.id) }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
