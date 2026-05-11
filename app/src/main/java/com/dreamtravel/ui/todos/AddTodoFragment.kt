package com.dreamtravel.ui.todos

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dreamtravel.R
import com.dreamtravel.databinding.FragmentAddTodoBinding
import com.dreamtravel.util.PermissionUtils
import android.util.Log
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

data class RemindIntervalOption(val label: String, val minutes: Int)

@AndroidEntryPoint
class AddTodoFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddTodoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddTodoViewModel by viewModels()
    private var notificationRequested = false
    private var selectedColor: String? = null

    private val remindOptions = listOf(
        RemindIntervalOption("30分钟", 30),
        RemindIntervalOption("1小时", 60),
        RemindIntervalOption("6小时", 360),
        RemindIntervalOption("24小时", 1440),
        RemindIntervalOption("48小时", 2880)
    )

    private val colorMap = mapOf(
        R.id.colorRed to "#ff7373",
        R.id.colorOrange to "#ffbc7c",
        R.id.colorYellow to "#fff198",
        R.id.colorGreen to "#b3ff99",
        R.id.colorBlue to "#a6d1ff",
        R.id.colorPurple to "#eaccff"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTodoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (_binding == null) return

        try {
            setupRemindIntervalSpinner()
            setupColorPicker()
            observeEditMode()
            observeRegion()

            binding.layoutRegionSelector.setOnClickListener {
                openRegionPicker()
            }

            binding.btnSaveTodo.setOnClickListener {
                val title = binding.editTodoTitle.text.toString().trim()
                if (title.isBlank()) {
                    binding.editTodoTitle.error = "请输入待办事项"
                    return@setOnClickListener
                }

                val notes = binding.editTodoNotes.text.toString().trim()
                val selectedOption = remindOptions[binding.spinnerRemindInterval.selectedItemPosition]
                val remindIntervalMinutes = selectedOption.minutes

                requestNotificationIfNeeded()

                // 防抖：禁用按钮防止重复点击
                binding.btnSaveTodo.isEnabled = false

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        if (viewModel.isEditing) {
                            viewModel.updateTodoSuspend(title, notes, remindIntervalMinutes, selectedColor)
                        } else {
                            viewModel.addTodoSuspend(title, notes, remindIntervalMinutes, selectedColor)
                        }
                        dismiss()
                    } catch (e: Exception) {
                        Log.e("AddTodoFragment", "保存待办失败", e)
                        binding.btnSaveTodo.isEnabled = true
                        context?.let { ctx ->
                            Toast.makeText(ctx, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            parentFragmentManager.setFragmentResultListener(
                RegionPickerBottomSheetFragment.REQUEST_KEY,
                viewLifecycleOwner
            ) { _, bundle ->
                viewModel.onRegionSelected(
                    provinceCode = bundle.getString("provinceCode"),
                    provinceName = bundle.getString("provinceName"),
                    cityCode = bundle.getString("cityCode"),
                    cityName = bundle.getString("cityName"),
                    districtCode = bundle.getString("districtCode"),
                    districtName = bundle.getString("districtName"),
                    formattedAddress = bundle.getString("formattedAddress")
                )
            }
        } catch (e: Exception) {
            Log.e("AddTodoFragment", "初始化待办界面失败", e)
            Toast.makeText(requireContext(), "打开失败: ${e.message}", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun setupColorPicker() {
        val colorViews = listOf(
            binding.colorRed,
            binding.colorOrange,
            binding.colorYellow,
            binding.colorGreen,
            binding.colorBlue,
            binding.colorPurple
        )

        colorViews.forEach { colorView ->
            colorView.setOnClickListener {
                // Reset all borders
                colorViews.forEach { it.setBackgroundResource(0) }

                // Set selected border
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setStroke(4, Color.BLACK)
                    setColor(Color.TRANSPARENT)
                }
                colorView.background = drawable

                // Store selected color
                selectedColor = colorMap[colorView.id]
            }
        }
    }

    private fun openRegionPicker() {
        val currentRegion = viewModel.selectedRegion.value
        val picker = RegionPickerBottomSheetFragment.newInstance(
            provinceCode = currentRegion?.provinceCode,
            cityCode = currentRegion?.cityCode,
            districtCode = currentRegion?.districtCode
        )
        picker.show(parentFragmentManager, "region_picker")
    }

    private fun observeRegion() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedRegion.collect { region ->
                    if (region?.formattedAddress != null) {
                        binding.tvSelectedRegion.text = region.formattedAddress
                        binding.tvSelectedRegion.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.black)
                        )
                    } else {
                        binding.tvSelectedRegion.text = getString(R.string.hint_select_city)
                        binding.tvSelectedRegion.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                        )
                    }
                }
            }
        }
    }

    private fun setupRemindIntervalSpinner() {
        val labels = remindOptions.map { it.label }.toTypedArray()
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            labels
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as TextView
                tv.text = labels[position]
                return tv
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getDropDownView(position, convertView, parent) as TextView
                tv.text = labels[position]
                return tv
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRemindInterval.adapter = adapter
    }

    private fun observeEditMode() {
        if (!viewModel.isEditing) {
            binding.btnSaveTodo.text = getString(R.string.btn_save)
            selectDefaultRemindInterval()
            return
        }

        binding.btnSaveTodo.text = getString(R.string.btn_update)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.existingTodo.collect { todo ->
                    todo?.let {
                        binding.editTodoTitle.setText(it.title)
                        if (it.notes.isNotBlank()) {
                            binding.editTodoNotes.setText(it.notes)
                        }
                        val minutes = it.remindIntervalMinutes
                        val index = remindOptions.indexOfFirst { opt -> opt.minutes == minutes }
                        if (index >= 0) {
                            binding.spinnerRemindInterval.setSelection(index)
                        }

                        // Set selected color
                        if (it.color != null) {
                            selectedColor = it.color
                            val colorEntry = colorMap.entries.find { entry -> entry.value == it.color }
                            if (colorEntry != null) {
                                val colorView = binding.root.findViewById<View>(colorEntry.key)
                                colorView?.performClick()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun selectDefaultRemindInterval() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.defaultRemindInterval.collect { minutes ->
                    val index = remindOptions.indexOfFirst { opt -> opt.minutes == minutes }
                    if (index >= 0) {
                        binding.spinnerRemindInterval.setSelection(index)
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun requestNotificationIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (PermissionUtils.hasNotificationPermission(requireContext())) return
        if (notificationRequested) return

        val perms = PermissionUtils.getNotificationPermission()
        if (perms.isNotEmpty()) {
            notificationRequested = true
            requestPermissions(perms, 200)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
