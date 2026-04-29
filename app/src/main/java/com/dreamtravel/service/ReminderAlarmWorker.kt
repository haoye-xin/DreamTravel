package com.dreamtravel.service

import android.content.Context
import androidx.work.*
import com.dreamtravel.data.model.TodoStatus
import com.dreamtravel.data.repository.DreamRepository
import com.dreamtravel.domain.ReminderSchedulerUseCase
import com.dreamtravel.notification.NotificationHelper
import com.dreamtravel.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * 周期性重提醒 Worker。
 * 当用户选择「进行中」后，按 remindIntervalMinutes 间隔发送重提醒。
 * 使用 WorkManager 因为间隔至少 30 分钟（≥ 15 分钟最小限制）。
 */
class ReminderAlarmWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: DreamRepository,
    private val reminderScheduler: ReminderSchedulerUseCase,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val placeId = inputData.getString(KEY_PLACE_ID) ?: return Result.failure()
        val placeName = inputData.getString(KEY_PLACE_NAME) ?: return Result.failure()

        // 检查是否有仍处于 IN_PROGRESS 的 todo
        val todos = repository.getTodos(placeId)
        val pendingTodos = kotlinx.coroutines.flow.first { true }
            .filter { it.status == TodoStatus.IN_PROGRESS }

        if (pendingTodos.isEmpty()) {
            return Result.success() // 所有已完成，停止
        }

        // 递增提醒计数
        pendingTodos.forEach { todo ->
            repository.incrementRemindCount(todo.id)
        }

        // 重新发送提醒
        reminderScheduler.triggerReminder(placeId, placeName)

        return Result.success()
    }

    companion object {
        private const val KEY_PLACE_ID = "place_id"
        private const val KEY_PLACE_NAME = "place_name"
        private const val UNIQUE_WORK_NAME_PREFIX = "reminder_alarm_"

        fun schedule(
            context: Context,
            placeId: String,
            placeName: String,
            delayMinutes: Long
        ) {
            val workName = UNIQUE_WORK_NAME_PREFIX + placeId

            val inputData = Data.Builder()
                .putString(KEY_PLACE_ID, placeId)
                .putString(KEY_PLACE_NAME, placeName)
                .build()

            val request = OneTimeWorkRequestBuilder<ReminderAlarmWorker>()
                .setInitialDelay(
                    maxOf(delayMinutes, Constants.MIN_REMIND_INTERVAL_MS / 60_000),
                    TimeUnit.MINUTES
                )
                .setInputData(inputData)
                .addTag(workName)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    workName,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }

        fun cancel(context: Context, placeId: String) {
            val workName = UNIQUE_WORK_NAME_PREFIX + placeId
            WorkManager.getInstance(context).cancelUniqueWork(workName)
        }
    }
}
