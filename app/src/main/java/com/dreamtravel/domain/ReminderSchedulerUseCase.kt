package com.dreamtravel.domain

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.dreamtravel.data.model.TodoStatus
import com.dreamtravel.notification.NotificationActionReceiver
import com.dreamtravel.notification.NotificationHelper
import com.dreamtravel.data.repository.DreamRepository
import com.dreamtravel.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ReminderSchedulerUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DreamRepository,
    private val notificationHelper: NotificationHelper
) {

    /**
     * 触发梦想提醒通知。
     * 当驻留时间到达阈值时调用。
     */
    suspend fun triggerReminder(placeId: String, placeName: String) {
        val allTodos = repository.getTodos(placeId).first()
        val pendingTodos = allTodos.filter {
            it.status == TodoStatus.PENDING || it.status == TodoStatus.IN_PROGRESS
        }

        if (pendingTodos.isEmpty()) return

        // 创建通知 Action Intents
        val completedIntent = createActionIntent(placeId, Constants.ACTION_COMPLETED)
        val inProgressIntent = createActionIntent(placeId, Constants.ACTION_IN_PROGRESS)
        val passingIntent = createActionIntent(placeId, Constants.ACTION_PASSING)

        val completedPI = PendingIntent.getBroadcast(
            context, placeId.hashCode(),
            completedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val inProgressPI = PendingIntent.getBroadcast(
            context, placeId.hashCode() + 1,
            inProgressIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val passingPI = PendingIntent.getBroadcast(
            context, placeId.hashCode() + 2,
            passingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationHelper.showReminder(
            placeId = placeId,
            placeName = placeName,
            todos = pendingTodos.map { it.title },
            completedPendingIntent = completedPI,
            inProgressPendingIntent = inProgressPI,
            passingPendingIntent = passingPI
        )
    }

    /**
     * 处理用户对通知的响应
     */
    suspend fun handleUserAction(placeId: String, action: String) {
        when (action) {
            Constants.ACTION_COMPLETED -> {
                repository.updateAllTodosStatus(placeId, TodoStatus.COMPLETED)
                NotificationManagerCompat.from(context).cancel(placeId.hashCode())
            }

            Constants.ACTION_IN_PROGRESS -> {
                repository.updateAllTodosStatus(placeId, TodoStatus.IN_PROGRESS)
                // 重新发送通知表示已接受
                // 周期重提醒由 ReminderAlarmWorker 处理
            }

            Constants.ACTION_PASSING -> {
                repository.updateAllTodosStatus(placeId, TodoStatus.SKIPPED)
                NotificationManagerCompat.from(context).cancel(placeId.hashCode())
            }
        }
    }

    private fun createActionIntent(placeId: String, action: String): Intent {
        return Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(Constants.EXTRA_PLACE_ID, placeId)
        }
    }
}
