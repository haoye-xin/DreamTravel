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
                    val statusStr = doc.getString("status") ?: "PENDING"
                    Todo(
                        id = doc.id,
                        placeId = doc.getString("placeId") ?: return@mapNotNull null,
                        title = doc.getString("title") ?: "",
                        notes = doc.getString("notes") ?: "",
                        status = TodoStatus.from(statusStr),
                        remindIntervalMinutes = doc.getLong("remindIntervalMinutes")?.toInt() ?: 1440,
                        remindCount = doc.getLong("remindCount")?.toInt() ?: 0,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        completedAt = doc.getLong("completedAt"),
                        updatedAt = doc.getLong("updatedAt") ?: 0L,
                        provinceCode = doc.getString("provinceCode")?.takeIf { it.isNotEmpty() },
                        provinceName = doc.getString("provinceName")?.takeIf { it.isNotEmpty() },
                        cityCode = doc.getString("cityCode")?.takeIf { it.isNotEmpty() },
                        cityName = doc.getString("cityName")?.takeIf { it.isNotEmpty() },
                        districtCode = doc.getString("districtCode")?.takeIf { it.isNotEmpty() },
                        districtName = doc.getString("districtName")?.takeIf { it.isNotEmpty() },
                        formattedAddress = doc.getString("formattedAddress")?.takeIf { it.isNotEmpty() },
                        color = doc.getString("color")?.takeIf { it.isNotEmpty() }
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

    suspend fun syncTodoToFirestore(todo: Todo): Boolean {
        val fs = firestore ?: return false
        return try {
            val data = mutableMapOf<String, Any>(
                "userId" to userId(),
                "placeId" to todo.placeId,
                "title" to todo.title,
                "notes" to todo.notes,
                "status" to todo.status.value,
                "remindIntervalMinutes" to todo.remindIntervalMinutes,
                "remindCount" to todo.remindCount,
                "createdAt" to todo.createdAt,
                "updatedAt" to todo.updatedAt,
                "provinceCode" to (todo.provinceCode ?: ""),
                "provinceName" to (todo.provinceName ?: ""),
                "cityCode" to (todo.cityCode ?: ""),
                "cityName" to (todo.cityName ?: ""),
                "districtCode" to (todo.districtCode ?: ""),
                "districtName" to (todo.districtName ?: ""),
                "formattedAddress" to (todo.formattedAddress ?: ""),
                "color" to (todo.color ?: "")
            )
            // Firestore SDK 不接受 null 值，completedAt 为 null 时跳过该字段
            todo.completedAt?.let { data["completedAt"] = it }
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