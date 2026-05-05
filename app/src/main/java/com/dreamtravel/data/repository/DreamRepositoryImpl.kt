package com.dreamtravel.data.repository

import com.dreamtravel.data.local.dao.PlaceDao
import com.dreamtravel.data.local.dao.TodoDao
import com.dreamtravel.data.model.*
import com.dreamtravel.data.remote.FirestoreSyncService
import com.dreamtravel.util.StatusManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DreamRepositoryImpl @Inject constructor(
    private val placeDao: PlaceDao,
    private val todoDao: TodoDao,
    private val firestoreSync: FirestoreSyncService,
    private val statusManager: StatusManager
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
        val updatedPlace = place.copy(updatedAt = System.currentTimeMillis())
        val entity = updatedPlace.toEntity()
        placeDao.insertPlace(entity)
        val syncOk = firestoreSync.syncPlaceToFirestore(updatedPlace)
        statusManager.reportSyncResult(syncOk)
    }

    override suspend fun updatePlace(place: Place) {
        val updatedPlace = place.copy(updatedAt = System.currentTimeMillis())
        val entity = updatedPlace.toEntity()
        placeDao.updatePlace(entity)
        val syncOk = firestoreSync.syncPlaceToFirestore(updatedPlace)
        statusManager.reportSyncResult(syncOk)
    }

    override suspend fun deletePlace(placeId: String) {
        placeDao.deletePlaceById(placeId)
        val syncOk = firestoreSync.deletePlaceFromFirestore(placeId)
        statusManager.reportSyncResult(syncOk)
    }

    override suspend fun setPlaceActive(placeId: String, isActive: Boolean) {
        placeDao.setPlaceActive(placeId, isActive)
        // Sync updated state to Firestore
        getPlaceById(placeId)?.let {
            val syncOk = firestoreSync.syncPlaceToFirestore(it)
            statusManager.reportSyncResult(syncOk)
        }
    }

    // ─── Todos ───────────────────────────────────────────────

    override fun getTodos(placeId: String): Flow<List<Todo>> {
        return todoDao.getTodosByPlace(placeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveTodos(placeId: String): Flow<List<Todo>> {
        return todoDao.getActiveTodosByPlace(placeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTodoHistory(placeId: String): Flow<List<Todo>> {
        return todoDao.getTodoHistoryByPlace(placeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTodoById(todoId: String): Todo? {
        return todoDao.getTodoById(todoId)?.toDomain()
    }

    override suspend fun addTodo(todo: Todo) {
        val updatedTodo = todo.copy(updatedAt = System.currentTimeMillis())
        todoDao.insertTodo(updatedTodo.toEntity())
        val syncOk = firestoreSync.syncTodoToFirestore(updatedTodo)
        statusManager.reportSyncResult(syncOk)
    }

    override suspend fun updateTodo(todo: Todo) {
        val updatedTodo = todo.copy(updatedAt = System.currentTimeMillis())
        todoDao.updateTodo(updatedTodo.toEntity())
        val syncOk = firestoreSync.syncTodoToFirestore(updatedTodo)
        statusManager.reportSyncResult(syncOk)
    }

    override suspend fun updateTodoStatus(todoId: String, newStatus: TodoStatus) {
        val completedAt = if (newStatus == TodoStatus.COMPLETED) System.currentTimeMillis() else null
        todoDao.updateTodoStatus(todoId, newStatus.value, completedAt)
        // Sync status change to Firestore
        todoDao.getTodoById(todoId)?.toDomain()?.let { todo ->
            val updatedTodo = todo.copy(updatedAt = System.currentTimeMillis(), status = newStatus)
            val syncOk = firestoreSync.syncTodoToFirestore(updatedTodo)
            statusManager.reportSyncResult(syncOk)
        }
    }

    override suspend fun updateAllTodosStatus(placeId: String, newStatus: TodoStatus) {
        todoDao.updateAllTodosStatus(placeId, newStatus.value)
        // Sync each todo's status to Firestore
        val todos = todoDao.getTodosByPlace(placeId).first().map { it.toDomain() }
        var allOk = true
        for (todo in todos) {
            val updatedTodo = todo.copy(updatedAt = System.currentTimeMillis(), status = newStatus)
            val syncOk = firestoreSync.syncTodoToFirestore(updatedTodo)
            if (!syncOk) allOk = false
        }
        statusManager.reportSyncResult(allOk)
    }

    override suspend fun deleteTodo(todoId: String) {
        todoDao.deleteTodoById(todoId)
        val syncOk = firestoreSync.deleteTodoFromFirestore(todoId)
        statusManager.reportSyncResult(syncOk)
    }

    override suspend fun incrementRemindCount(todoId: String) {
        todoDao.incrementRemindCount(todoId)
        // Sync updated remindCount to Firestore
        todoDao.getTodoById(todoId)?.toDomain()?.let { todo ->
            val updatedTodo = todo.copy(updatedAt = System.currentTimeMillis())
            val syncOk = firestoreSync.syncTodoToFirestore(updatedTodo)
            statusManager.reportSyncResult(syncOk)
        }
    }

    override suspend fun countPendingTodos(placeId: String): Int {
        return todoDao.countPendingTodos(placeId)
    }

    // ─── Cloud Sync ──────────────────────────────────────────

    override suspend fun syncFromCloud() {
        val snapshot = firestoreSync.pullCloudData()
        if (snapshot == null) {
            statusManager.reportSyncResult(false)
            return
        }

        // Merge places (insert if new or cloud is newer)
        for (cloudPlace in snapshot.places) {
            val local = placeDao.getPlaceById(cloudPlace.id)
            if (local == null || cloudPlace.updatedAt > local.updatedAt) {
                placeDao.insertPlace(cloudPlace.toEntity())
            }
        }

        // Merge todos (insert if new or cloud is newer)
        for (cloudTodo in snapshot.todos) {
            val local = todoDao.getTodoById(cloudTodo.id)
            if (local == null || cloudTodo.updatedAt > local.updatedAt) {
                todoDao.insertTodo(cloudTodo.toEntity())
            }
        }

        statusManager.reportSyncResult(true)
    }
}
