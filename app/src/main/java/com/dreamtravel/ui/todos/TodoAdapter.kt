package com.dreamtravel.ui.todos

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.dreamtravel.data.model.Todo
import com.dreamtravel.data.model.TodoStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodoAdapter(
    private var todos: List<Todo>,
    private val onComplete: (Todo) -> Unit,
    private val onProgress: (Todo) -> Unit,
    private val onSkip: (Todo) -> Unit,
    private val onEdit: (Todo) -> Unit,
    private val onDelete: (Todo) -> Unit
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
                TodoStatus.COMPLETED -> "\u2705"
                TodoStatus.IN_PROGRESS -> "\uD83D\uDD04"
                TodoStatus.SKIPPED -> "\uD83D\uDCCD"
                TodoStatus.PENDING -> "\u23F3"
            }

            text1.text = "$statusIcon ${todo.title}"
            val intervalText = if (todo.remindIntervalMinutes >= 60) {
                "${todo.remindIntervalMinutes / 60}h"
            } else {
                "${todo.remindIntervalMinutes}min"
            }
            text2.text = "提醒间隔: $intervalText  | 添加于 ${dateFormat.format(Date(todo.createdAt))}"

            // Tap: cycle status
            view.setOnClickListener {
                when (todo.status) {
                    TodoStatus.PENDING -> onProgress(todo)
                    TodoStatus.IN_PROGRESS -> onComplete(todo)
                    else -> onEdit(todo) // completed/skipped → tap to edit
                }
            }

            // Long-press: show edit/delete context actions via callbacks
            view.setOnLongClickListener {
                showContextMenu(todo)
                true
            }
        }

        private fun showContextMenu(todo: Todo) {
            val context = view.context
            val items = arrayOf(
                context.getString(android.R.string.ok) + " 编辑",
                context.getString(android.R.string.cancel) + " 删除"
            )
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(todo.title)
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> onEdit(todo)
                        1 -> onDelete(todo)
                    }
                }
                .show()
        }
    }
}
