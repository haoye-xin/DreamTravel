package com.dreamtravel.ui.todos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dreamtravel.data.model.Todo
import com.dreamtravel.data.model.TodoStatus
import java.text.SimpleDateFormat
import java.util.*

class TodoAdapter(
    private var todos: List<Todo>,
    private val onComplete: (Todo) -> Unit,
    private val onProgress: (Todo) -> Unit,
    private val onSkip: (Todo) -> Unit
) : RecyclerView.Adapter<TodoAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(todos[position])
    }

    override fun getItemCount() = todos.size

    fun updateData(newTodos: List<Todo>) {
        todos = newTodos
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val view: android.view.View
    ) : RecyclerView.ViewHolder(view) {

        fun bind(todo: Todo) {
            val text1 = view.findViewById<android.widget.TextView>(android.R.id.text1)
            val text2 = view.findViewById<android.widget.TextView>(android.R.id.text2)

            val statusIcon = when (todo.status) {
                TodoStatus.COMPLETED -> "✅"
                TodoStatus.IN_PROGRESS -> "🔄"
                TodoStatus.SKIPPED -> "📍"
                TodoStatus.PENDING -> "⏳"
            }

            text1.text = "$statusIcon ${todo.title}"
            text2.text = "添加于 ${dateFormat.format(Date(todo.createdAt))}"

            view.setOnClickListener {
                when (todo.status) {
                    TodoStatus.PENDING -> onProgress(todo)
                    TodoStatus.IN_PROGRESS -> onComplete(todo)
                    else -> {}
                }
            }
        }
    }
}
