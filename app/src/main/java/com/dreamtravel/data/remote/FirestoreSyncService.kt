package com.dreamtravel.data.remote

import com.dreamtravel.data.model.Place
import com.dreamtravel.data.model.Todo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private fun userId(): String = auth.currentUser?.uid ?: "anonymous"

    // ─── Places ──────────────────────────────────────────────

    suspend fun syncPlaceToFirestore(place: Place) {
        try {
            val data = mapOf(
                "userId" to userId(),
                "name" to place.name,
                "cityCode" to (place.cityCode ?: ""),
                "lat" to place.latitude,
                "lng" to place.longitude,
                "dwellMinutes" to place.dwellMinutes,
                "isActive" to place.isActive,
                "createdAt" to place.createdAt
            )
            firestore.collection("places")
                .document(place.id)
                .set(data)
                .await()
        } catch (e: Exception) {
            // 静默失败，本地数据不受影响
            e.printStackTrace()
        }
    }

    suspend fun deletePlaceFromFirestore(placeId: String) {
        try {
            firestore.collection("places")
                .document(placeId)
                .delete()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncAllPlaces() {
        try {
            val snapshot = firestore.collection("places")
                .whereEqualTo("userId", userId())
                .get()
                .await()
            // Last-write-wins: Firestore data is reference
            // Room is primary, so we only push, not pull
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ─── Todos ───────────────────────────────────────────────

    suspend fun syncTodoToFirestore(todo: Todo) {
        try {
            val data = mapOf(
                "userId" to userId(),
                "placeId" to todo.placeId,
                "title" to todo.title,
                "notes" to todo.notes,
                "status" to todo.status.value,
                "remindIntervalMinutes" to todo.remindIntervalMinutes,
                "createdAt" to todo.createdAt
            )
            firestore.collection("todos")
                .document(todo.id)
                .set(data)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
