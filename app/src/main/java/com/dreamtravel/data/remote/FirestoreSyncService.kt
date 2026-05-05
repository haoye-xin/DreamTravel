package com.dreamtravel.data.remote

import com.dreamtravel.data.model.Place
import com.dreamtravel.data.model.Todo
import com.dreamtravel.data.model.TodoStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncService @Inject constructor(
    private val firestore: FirebaseFirestore?,
    private val auth: FirebaseAuth?
) {

    private fun userId(): String = auth?.currentUser?.uid ?: "anonymous"

    // ─── Places ──────────────────────────────────────────────

    /**
     * @return `true` if the sync succeeded, `false` otherwise.
     */
    suspend fun syncPlaceToFirestore(place: Place): Boolean {
        val fs = firestore ?: return false
        return try {
            val data = mapOf(
                "userId" to userId(),
                "name" to place.name,
                "cityCode" to (place.cityCode ?: ""),
                "lat" to place.latitude,
                "lng" to place.longitude,
                "dwellMinutes" to place.dwellMinutes,
                "isActive" to place.isActive,
                "createdAt" to place.createdAt,
                "updatedAt" to place.updatedAt
            )
            fs.collection("places")
                .document(place.id)
                .set(data)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * @return `true` if the sync succeeded, `false` otherwise.
     */
    suspend fun deletePlaceFromFirestore(placeId: String): Boolean {
        val fs = firestore ?: return false
        return try {
            fs.collection("places")
                .document(placeId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    data class CloudSnapshot(
        val places: List<Place>,
        val todos: List<Todo>
    )

    /**
     * Pulls all places and todos for the current user from Firestore.
     * Returns null on failure; caller should merge into Room using updatedAt comparison.
     */
    suspend fun pullCloudData(): CloudSnapshot? {
        val fs = firestore ?: return null
        return try {
            val placesSnapshot = fs.collection("places")
                .whereEqualTo("userId", userId())
                .get()
                .await()

            val places = placesSnapshot.documents.mapNotNull { doc ->
                try {
                    Place(
                        id = doc.id,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        cityCode = doc.getString("cityCode")?.takeIf { it.isNotEmpty() },
                        latitude = doc.getDouble("lat") ?: 0.0,
                        longitude = doc.getDouble("lng") ?: 0.0,
                        dwellMinutes = doc.getLong("dwellMinutes")?.toInt() ?: 30,
                        isActive = doc.getBoolean("isActive") ?: true,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                } catch (_: Exception) { null }
            }

            val todosSnapshot = fs.collection("todos")
                .whereEqualTo("userId", userId())
                .get()
                .await()

            val todos = todosSnapshot.documents.mapNotNull { doc ->
                try {
                    Todo(
                        id = doc.id,
                        placeId = doc.getString("placeId") ?: return@mapNotNull null,
                        title = doc.getString("title") ?: "",
                        notes = doc.getString("notes") ?: "",
                        status = TodoStatus.from(doc.getString("status") ?: "PENDING"),
                        remindIntervalMinutes = doc.getLong("remindIntervalMinutes")?.toInt() ?: 1440,
                        remindCount = doc.getLong("remindCount")?.toInt() ?: 0,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        completedAt = doc.getLong("completedAt"),
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                } catch (_: Exception) { null }
            }

            CloudSnapshot(places, todos)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ─── Todos ───────────────────────────────────────────────

    /**
     * @return `true` if the sync succeeded, `false` otherwise.
     */
    suspend fun syncTodoToFirestore(todo: Todo): Boolean {
        val fs = firestore ?: return false
        return try {
            val data = mapOf(
                "userId" to userId(),
                "placeId" to todo.placeId,
                "title" to todo.title,
                "notes" to todo.notes,
                "status" to todo.status.value,
                "remindIntervalMinutes" to todo.remindIntervalMinutes,
                "remindCount" to todo.remindCount,
                "createdAt" to todo.createdAt,
                "completedAt" to todo.completedAt,
                "updatedAt" to todo.updatedAt
            )
            fs.collection("todos")
                .document(todo.id)
                .set(data)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * @return `true` if the sync succeeded, `false` otherwise.
     */
    suspend fun deleteTodoFromFirestore(todoId: String): Boolean {
        val fs = firestore ?: return false
        return try {
            fs.collection("todos")
                .document(todoId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
