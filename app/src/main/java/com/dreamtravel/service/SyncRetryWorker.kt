package com.dreamtravel.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dreamtravel.data.local.dao.PlaceDao
import com.dreamtravel.data.local.dao.TodoDao
import com.dreamtravel.data.model.toDomain
import com.dreamtravel.data.remote.FirestoreSyncService
import com.dreamtravel.util.StatusManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncRetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val placeDao: PlaceDao,
    private val todoDao: TodoDao,
    private val firestoreSync: FirestoreSyncService,
    private val statusManager: StatusManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        var allOk = true

        val places = placeDao.getAllPlacesList()
        for (place in places) {
            val ok = firestoreSync.syncPlaceToFirestore(place.toDomain())
            if (!ok) allOk = false
        }

        val todos = todoDao.getAllTodos()
        for (todo in todos) {
            val ok = firestoreSync.syncTodoToFirestore(todo.toDomain())
            if (!ok) allOk = false
        }

        if (allOk) {
            statusManager.reportSyncResult(true)
            return Result.success()
        }
        statusManager.reportSyncResult(false)
        return Result.retry()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "dream_sync_retry"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<SyncRetryWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
                .addTag(UNIQUE_WORK_NAME)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}