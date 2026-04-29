package com.dreamtravel.ui.todos

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dreamtravel.R
import com.dreamtravel.databinding.FragmentTodoListBinding
import com.dreamtravel.data.model.Todo
import com.dreamtravel.data.model.TodoStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TodoListFragment : Fragment(R.layout.fragment_todo_list) {

    private var _binding: FragmentTodoListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TodoListViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTodoListBinding.bind(view)

        // Set title in parent activity
        requireActivity().title = viewModel.placeName

        setupRecyclerView()
        observeViewModel()

        binding.fabAddTodo.setOnClickListener {
            val bundle = Bundle().apply { putString("placeId", viewModel.placeId) }
            findNavController().navigate(R.id.action_todoList_to_addTodo, bundle)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerTodos.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.todos.collect { todos ->
                    binding.textEmpty.visibility = if (todos.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerTodos.adapter = TodoAdapter(
                        todos,
                        onComplete = { viewModel.markCompleted(it.id) },
                        onProgress = { viewModel.markInProgress(it.id) },
                        onSkip = { viewModel.markSkipped(it.id) }
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
