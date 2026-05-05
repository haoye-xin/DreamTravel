package com.dreamtravel.domain

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.dreamtravel.MainActivity
import com.dreamtravel.data.model.Todo
import com.dreamtravel.data.model.TodoStatus
import com.dreamtravel.notification.NotificationActionReceiver
import com.dreamtravel.notification.NotificationHelper
import com.dreamtravel.data.repository.DreamRepository
import com.dreamtravel.analytics.AnalyticsEvent
import com.dreamtravel.analytics.AnalyticsManager
import com.dreamtravel.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ReminderSchedulerUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DreamRepository,
    private val notificationHelper: NotificationHelper,
    private val analytics: AnalyticsManager
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

        analytics.logEvent(
            AnalyticsEvent.NOTIFICATION_REMINDER_SHOWN,
            mapOf(
                AnalyticsEvent.Param.PLACE_ID to placeId,
                AnalyticsEvent.Param.TODO_COUNT to pendingTodos.size
            )
        )

        // 如果只有一个待办，使用单待办粒度的 Action；多个时回退到地点级
        val singleTodo: Todo? = if (pendingTodos.size == 1) pendingTodos.first() else null

        val completedIntent = createActionIntent(placeId, Constants.ACTION_COMPLETED, singleTodo?.id)
        val inProgressIntent = createActionIntent(placeId, Constants.ACTION_IN_PROGRESS, singleTodo?.id)
        val passingIntent = createActionIntent(placeId, Constants.ACTION_PASSING, singleTodo?.id)

        // requestCode 基于待办 ID（单待办）或地点 ID（多个待办）
        val requestBase = singleTodo?.id?.hashCode() ?: placeId.hashCode()

        val completedPI = PendingIntent.getBroadcast(
            context, requestBase,
            completedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val inProgressPI = PendingIntent.getBroadcast(
            context, requestBase + 1,
            inProgressIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val passingPI = PendingIntent.getBroadcast(
            context, requestBase + 2,
            passingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 内容 Intent：点击通知打开该地点的待办列表
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(Constants.EXTRA_PLACE_ID, placeId)
            putExtra(Constants.EXTRA_PLACE_NAME, placeName)
        }
        val contentPI = PendingIntent.getActivity(
            context, placeId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationHelper.showReminder(
            placeId = placeId,
            placeName = placeName,
            todos = pendingTodos,
            completedPendingIntent = completedPI,
            inProgressPendingIntent = inProgressPI,
            passingPendingIntent = passingPI,
            contentPendingIntent = contentPI
        )
    }

    /**
     * 处理用户对通知的响应。
     * @param todoId 若不为空，只更新该待办；否则更新该地点所有待办（兼容旧行为）
     */
    suspend fun handleUserAction(placeId: String, action: String, todoId: String? = null) {
        when (action) {
            Constants.ACTION_COMPLETED -> {
                analytics.logEvent(
                    AnalyticsEvent.NOTIFICATION_ACTION_COMPLETED,
                    mapOf(AnalyticsEvent.Param.PLACE_ID to placeId)
                )
                if (!todoId.isNullOrEmpty()) {
                    repository.updateTodoStatus(todoId, TodoStatus.COMPLETED)
                    NotificationManagerCompat.from(context).cancel(todoId.hashCode())
                } else {
                    repository.updateAllTodosStatus(placeId, TodoStatus.COMPLETED)
                    NotificationManagerCompat.from(context).cancel(placeId.hashCode())
                }
            }

            Constants.ACTION_IN_PROGRESS -> {
                analytics.logEvent(
                    AnalyticsEvent.NOTIFICATION_ACTION_IN_PROGRESS,
                    mapOf(AnalyticsEvent.Param.PLACE_ID to placeId)
                )
                if (!todoId.isNullOrEmpty()) {
                    repository.updateTodoStatus(todoId, TodoStatus.IN_PROGRESS)
                } else {
                    repository.updateAllTodosStatus(placeId, TodoStatus.IN_PROGRESS)
                }
            }

            Constants.ACTION_PASSING -> {
                analytics.logEvent(
                    AnalyticsEvent.NOTIFICATION_ACTION_PASSING,
                    mapOf(AnalyticsEvent.Param.PLACE_ID to placeId)
                )
                if (!todoId.isNullOrEmpty()) {
                    repository.updateTodoStatus(todoId, TodoStatus.SKIPPED)
                    NotificationManagerCompat.from(context).cancel(todoId.hashCode())
                } else {
                    repository.updateAllTodosStatus(placeId, TodoStatus.SKIPPED)
                    NotificationManagerCompat.from(context).cancel(placeId.hashCode())
                }
            }
        }
    }

    private fun createActionIntent(placeId: String, action: String, todoId: String? = null): Intent {
        return Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(Constants.EXTRA_PLACE_ID, placeId)
            if (!todoId.isNullOrEmpty()) {
                putExtra(Constants.EXTRA_TODO_ID, todoId)
            }
        }
    }
}
