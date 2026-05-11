package com.dreamtravel.ui.places

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
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
import com.dreamtravel.util.StatusManager
import com.dreamtravel.util.StatusMessage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaceListFragment : Fragment(R.layout.fragment_place_list) {

    private var _binding: FragmentPlaceListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlaceListViewModel by viewModels()

    @Inject lateinit var statusManager: StatusManager

    private var currentSnackbar: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaceListBinding.bind(view)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        observeStatuses()

        binding.fabAddPlace.setOnClickListener {
            findNavController().navigate(R.id.action_placeList_to_addPlace)
        }

        showBatteryOptimizationDialogIfNeeded()
    }

    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.toolbar_place_list)
        binding.toolbar.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    findNavController().navigate(R.id.action_placeList_to_settings)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerPlaces.layoutManager = LinearLayoutManager(requireContext())
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

    private fun observeStatuses() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Periodic checks every 30 seconds
                launch {
                    while (true) {
                        statusManager.checkAllStatuses()
                        delay(30_000L)
                    }
                }

                // Collect and display status messages
                launch {
                    statusManager.statusFlow.collect { status ->
                        showStatusSnackbar(status)
                    }
                }
            }
        }
    }

    private fun showStatusSnackbar(status: StatusMessage) {
        // Dismiss any previously shown Snackbar (one at a time)
        currentSnackbar?.dismiss()

        val rootView = binding.root
        val message = getString(status.messageResId)
        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)

        // Add action button if present
        status.action?.let { action ->
            val actionLabel = getString(action.labelResId)
            val actionIntent = statusManager.resolveAction(action.actionType)
            if (actionIntent != null) {
                snackbar.setAction(actionLabel) {
                    try {
                        startActivity(actionIntent)
                    } catch (_: Exception) {
                        // Settings screen unavailable — ignore
                    }
                }
            }
        }

        snackbar.show()
        currentSnackbar = snackbar
    }

    private fun showDeleteDialog(place: Place) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除")
            .setMessage(R.string.delete_confirm)
            .setPositiveButton("删除") { _, _ -> viewModel.deletePlace(place.id) }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showBatteryOptimizationDialogIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)) return

        val prefs = requireContext().getSharedPreferences("dream_travel_prefs", Context.MODE_PRIVATE)
        val lastShown = prefs.getLong("battery_opt_dialog_last_shown", 0)
        val oneWeek = 7 * 24 * 60 * 60 * 1000L
        if (System.currentTimeMillis() - lastShown < oneWeek) return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.battery_optimization_dialog_title)
            .setMessage(R.string.battery_optimization_dialog_message)
            .setPositiveButton(R.string.go_to_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:" + requireContext().packageName)
                }
                startActivity(intent)
                prefs.edit().putLong("battery_opt_dialog_last_shown", System.currentTimeMillis()).apply()
            }
            .setNegativeButton(R.string.btn_later) { _, _ ->
                prefs.edit().putLong("battery_opt_dialog_last_shown", System.currentTimeMillis()).apply()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentSnackbar?.dismiss()
        currentSnackbar = null
        _binding = null
    }
}
