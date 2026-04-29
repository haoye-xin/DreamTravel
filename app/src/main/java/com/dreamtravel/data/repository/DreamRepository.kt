package com.dreamtravel.data.repository

import com.dreamtravel.data.model.Place
import com.dreamtravel.data.model.Todo
import com.dreamtravel.data.model.TodoStatus
import kotlinx.coroutines.flow.Flow

interface DreamRepository {

    /** Places */
    fun getPlaces(): Flow<List<Place>>
    fun getActivePlaces(): Flow<List<Place>>
    suspend fun getPlaceById(placeId: String): Place?
    suspend fun addPlace(place: Place)
    suspend fun updatePlace(place: Place)
    suspend fun deletePlace(placeId: String)
    suspend fun setPlaceActive(placeId: String, isActive: Boolean)

    /** Todos */
    fun getTodos(placeId: String): Flow<List<Todo>>
    suspend fun addTodo(todo: Todo)
    suspend fun updateTodoStatus(todoId: String, newStatus: TodoStatus)
    suspend fun updateAllTodosStatus(placeId: String, newStatus: TodoStatus)
    suspend fun incrementRemindCount(todoId: String)
    suspend fun countPendingTodos(placeId: String): Int
}
