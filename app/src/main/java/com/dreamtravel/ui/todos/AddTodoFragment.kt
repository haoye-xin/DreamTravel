package com.dreamtravel.ui.todos

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dreamtravel.R
import com.dreamtravel.databinding.FragmentAddTodoBinding
import com.dreamtravel.util.PermissionUtils
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

    private val remindOptions = listOf(
        RemindIntervalOption("30分钟", 30),
        RemindIntervalOption("1小时", 60),
        RemindIntervalOption("6小时", 360),
        RemindIntervalOption("24小时", 1440),
        RemindIntervalOption("48小时", 2880)
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

        setupRemindIntervalSpinner()
        observeEditMode()

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

            if (viewModel.isEditing) {
                viewModel.updateTodo(title, notes, remindIntervalMinutes)
            } else {
                viewModel.addTodo(title, notes, remindIntervalMinutes)
            }
            dismiss()
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
