package com.dreamtravel.data.local.dao

import androidx.room.*
import com.dreamtravel.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos WHERE placeId = :placeId ORDER BY createdAt DESC")
    fun getTodosByPlace(placeId: String): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE placeId = :placeId AND status = :status ORDER BY createdAt DESC")
    fun getTodosByPlaceAndStatus(placeId: String, status: String): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :todoId")
    suspend fun getTodoById(todoId: String): TodoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity)

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Delete
    suspend fun deleteTodo(todo: TodoEntity)

    @Query("DELETE FROM todos WHERE id = :todoId")
    suspend fun deleteTodoById(todoId: String)

    @Query("UPDATE todos SET status = :status, completedAt = :completedAt WHERE id = :todoId")
    suspend fun updateTodoStatus(todoId: String, status: String, completedAt: Long? = null)

    @Query("UPDATE todos SET status = :status WHERE placeId = :placeId")
    suspend fun updateAllTodosStatus(placeId: String, status: String)

    @Query("UPDATE todos SET remindCount = remindCount + 1 WHERE id = :todoId")
    suspend fun incrementRemindCount(todoId: String)

    @Query("SELECT COUNT(*) FROM todos WHERE placeId = :placeId AND status != 'COMPLETED' AND status != 'SKIPPED'")
    suspend fun countPendingTodos(placeId: String): Int

    @Query("SELECT * FROM todos WHERE placeId = :placeId AND status IN ('PENDING', 'IN_PROGRESS') ORDER BY createdAt DESC")
    fun getActiveTodosByPlace(placeId: String): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE placeId = :placeId AND status IN ('COMPLETED', 'SKIPPED') ORDER BY createdAt DESC")
    fun getTodoHistoryByPlace(placeId: String): Flow<List<TodoEntity>>

    @Transaction
    suspend fun mergeTodos(todos: List<TodoEntity>) {
        for (todo in todos) {
            insertTodo(todo)
        }
    }
}
