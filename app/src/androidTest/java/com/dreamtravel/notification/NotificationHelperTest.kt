package com.dreamtravel.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationHelperTest {

    private lateinit var context: Context
    private lateinit var notificationHelper: NotificationHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationHelper = NotificationHelper(context)
    }

    @Test
    fun `buildLocationServiceNotification returns non-null notification`() {
        val notification = notificationHelper.buildLocationServiceNotification()
        assertNotNull(notification)
        // Verify it has the expected properties
        assertTrue(notification.extras.containsKey("android.title"))
    }

    @Test
    fun `cancelNotification does not crash`() {
        // Should not throw for non-existent notification
        notificationHelper.cancelNotification("nonexistent-place")
        // If we reach here without crash, test passes
    }

    @Test
    fun `showReminder does not crash with valid args`() {
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context, 0,
            android.content.Intent(),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        notificationHelper.showReminder(
            placeId = "test-place",
            placeName = "测试城市",
            todos = listOf("待办事项 1", "待办事项 2"),
            completedPendingIntent = pendingIntent,
            inProgressPendingIntent = pendingIntent,
            passingPendingIntent = pendingIntent
        )
        // Should not crash
    }
}
