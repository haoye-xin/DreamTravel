package com.dreamtravel.ui.todos

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dreamtravel.R
import com.dreamtravel.databinding.FragmentTodoListBinding
import com.dreamtravel.data.model.Todo
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TodoListFragment : Fragment(R.layout.fragment_todo_list) {

    private var _binding: FragmentTodoListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TodoListViewModel by viewModels()
    private var todoAdapter: TodoAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTodoListBinding.bind(view)

        requireActivity().title = viewModel.placeName

        setupRecyclerView()
        setupFilterChips()
        observeViewModel()

        binding.fabAddTodo.setOnClickListener {
            val bundle = Bundle().apply { putString("placeId", viewModel.placeId) }
            findNavController().navigate(R.id.action_todoList_to_addTodo, bundle)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerTodos.layoutManager = LinearLayoutManager(requireContext())

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val todo = todoAdapter?.getTodoAt(position) ?: return
                viewModel.deleteTodo(todo.id)
                Snackbar.make(
                    binding.root,
                    getString(R.string.todo_deleted, todo.title),
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).setAction(getString(R.string.undo)) {
                    viewModel.restoreTodo(todo)
                }.show()
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerTodos)
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter(TodoFilter.ALL)
        }
        binding.chipActive.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter(TodoFilter.ACTIVE)
        }
        binding.chipHistory.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter(TodoFilter.HISTORY)
        }
    }

    private fun observeViewModel() {
        // Adapter 只创建一次，后续通过 updateData 更新数据
        todoAdapter = TodoAdapter(
            emptyList(),
            onComplete = { viewModel.markCompleted(it.id) },
            onProgress = { viewModel.markInProgress(it.id) },
            onSkip = { viewModel.markSkipped(it.id) },
            onEdit = { todo -> navigateToEdit(todo) },
            onDelete = { todo -> confirmDelete(todo) }
        )
        binding.recyclerTodos.adapter = todoAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.todos.collect { todos ->
                    binding.textEmpty.visibility = if (todos.isEmpty()) View.VISIBLE else View.GONE
                    todoAdapter?.updateData(todos)
                }
            }
        }
    }

    private fun navigateToEdit(todo: Todo) {
        val bundle = Bundle().apply {
            putString("placeId", viewModel.placeId)
            putString("todoId", todo.id)
        }
        findNavController().navigate(R.id.action_todoList_to_addTodo, bundle)
    }

    private fun confirmDelete(todo: Todo) {
        AlertDialog.Builder(requireContext())
            .setTitle(todo.title)
            .setMessage(R.string.delete_todo_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.deleteTodo(todo.id)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
