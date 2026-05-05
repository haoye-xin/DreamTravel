package com.dreamtravel.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dreamtravel.R
import com.dreamtravel.data.model.Todo
import com.dreamtravel.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        // 梦想提醒渠道
        val dreamChannel = NotificationChannel(
            Constants.CHANNEL_DREAM_REMINDER,
            context.getString(R.string.channel_dream_reminder),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_dream_reminder_desc)
            enableVibration(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(dreamChannel)

        // 定位服务渠道（低优先级，静默）
        val locationChannel = NotificationChannel(
            Constants.CHANNEL_LOCATION_SERVICE,
            context.getString(R.string.channel_location_service),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.channel_location_service_desc)
            setSound(null, null)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(locationChannel)
    }

    /**
     * 发送梦想提醒通知（含三个 Action 按钮）。
     * 当只有一个待办时，Action 精确到该待办；多个时回退到地点级。
     */
    fun showReminder(
        placeId: String,
        placeName: String,
        todos: List<Todo>,
        completedPendingIntent: PendingIntent,
        inProgressPendingIntent: PendingIntent,
        passingPendingIntent: PendingIntent,
        contentPendingIntent: PendingIntent
    ) {
        val title = String.format(
            context.getString(R.string.notification_title),
            placeName
        )

        val todoTitles = todos.map { it.title }

        // 构建内容文本：最多 3 行
        val contentText = when {
            todoTitles.size <= 3 -> todoTitles.joinToString("\n") { "• $it" }
            else -> todoTitles.take(3).joinToString("\n") { "• $it" } +
                    "\n" + String.format(
                context.getString(R.string.notification_more),
                todoTitles.size - 3
            )
        }

        // BigTextStyle：编号展示所有待办
        val bigText = buildString {
            todoTitles.forEachIndexed { index, title ->
                if (index > 0) append("\n")
                append("${index + 1}. $title")
            }
        }

        val builder = NotificationCompat.Builder(context, Constants.CHANNEL_DREAM_REMINDER)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_edit,
                context.getString(R.string.action_completed),
                completedPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_rotate,
                context.getString(R.string.action_in_progress),
                inProgressPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.action_passing),
                passingPendingIntent
            )

        notificationManager.notify(placeId.hashCode(), builder.build())
    }

    /**
     * 前台 Service 持久通知
     */
    fun buildLocationServiceNotification(): Notification {
        return NotificationCompat.Builder(context, Constants.CHANNEL_LOCATION_SERVICE)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.location_notice))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * 取消指定地点的通知
     */
    fun cancelNotification(placeId: String) {
        notificationManager.cancel(placeId.hashCode())
    }
}
