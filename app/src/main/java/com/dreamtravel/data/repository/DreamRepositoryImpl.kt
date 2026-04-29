package com.dreamtravel.data.repository

import com.dreamtravel.data.local.dao.DwellEventDao
import com.dreamtravel.data.local.dao.PlaceDao
import com.dreamtravel.data.local.dao.TodoDao
import com.dreamtravel.data.model.*
import com.dreamtravel.data.remote.FirestoreSyncService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DreamRepositoryImpl @Inject constructor(
    private val placeDao: PlaceDao,
    private val todoDao: TodoDao,
    private val firestoreSync: FirestoreSyncService
) : DreamRepository {

    // ─── Places ──────────────────────────────────────────────

    override fun getPlaces(): Flow<List<Place>> {
        return placeDao.getAllPlaces().map { entities ->
            entities.map { entity ->
                val pendingCount = runCatching {
                    todoDao.countPendingTodos(entity.id)
                }.getOrDefault(0)
                entity.toDomain(pendingCount = pendingCount, totalCount = 0)
            }
        }
    }

    override fun getActivePlaces(): Flow<List<Place>> {
        return placeDao.getActivePlaces().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPlaceById(placeId: String): Place? {
        return placeDao.getPlaceById(placeId)?.toDomain()
    }

    override suspend fun addPlace(place: Place) {
        val entity = place.toEntity()
        placeDao.insertPlace(entity)
        firestoreSync.syncPlaceToFirestore(place)
    }

    override suspend fun updatePlace(place: Place) {
        val entity = place.toEntity()
        placeDao.updatePlace(entity)
        firestoreSync.syncPlaceToFirestore(place)
    }

    override suspend fun deletePlace(placeId: String) {
        placeDao.deletePlaceById(placeId)
        firestoreSync.deletePlaceFromFirestore(placeId)
    }

    override suspend fun setPlaceActive(placeId: String, isActive: Boolean) {
        placeDao.setPlaceActive(placeId, isActive)
        // Firestore sync via update
        getPlaceById(placeId)?.let { firestoreSync.syncPlaceToFirestore(it) }
    }

    // ─── Todos ───────────────────────────────────────────────

    override fun getTodos(placeId: String): Flow<List<Todo>> {
        return todoDao.getTodosByPlace(placeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addTodo(todo: Todo) {
        todoDao.insertTodo(todo.toEntity())
        firestoreSync.syncTodoToFirestore(todo)
    }

    override suspend fun updateTodoStatus(todoId: String, newStatus: TodoStatus) {
        val completedAt = if (newStatus == TodoStatus.COMPLETED) System.currentTimeMillis() else null
        todoDao.updateTodoStatus(todoId, newStatus.value, completedAt)
    }

    override suspend fun updateAllTodosStatus(placeId: String, newStatus: TodoStatus) {
        todoDao.updateAllTodosStatus(placeId, newStatus.value)
    }

    override suspend fun incrementRemindCount(todoId: String) {
        todoDao.incrementRemindCount(todoId)
    }

    override suspend fun countPendingTodos(placeId: String): Int {
        return todoDao.countPendingTodos(placeId)
    }
}
