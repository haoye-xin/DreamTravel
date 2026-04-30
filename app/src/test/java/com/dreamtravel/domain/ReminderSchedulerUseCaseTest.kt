package com.dreamtravel.domain

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.dreamtravel.data.model.Todo
import com.dreamtravel.data.model.TodoStatus
import com.dreamtravel.data.repository.DreamRepository
import com.dreamtravel.notification.NotificationHelper
import com.dreamtravel.util.Constants
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class ReminderSchedulerUseCaseTest {

    private lateinit var context: Context
    private lateinit var repository: DreamRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var useCase: ReminderSchedulerUseCase

    private val testPlaceId = "place-1"
    private val testPlaceName = "大理"

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        notificationHelper = mockk(relaxed = true)

        // Mock PendingIntent to avoid real Android calls
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockk(relaxed = true)

        useCase = ReminderSchedulerUseCase(context, repository, notificationHelper)
    }

    @After
    fun tearDown() {
        unmockkStatic(PendingIntent::class)
    }

    // ─── triggerReminder ─────────────────────────────────────

    @Test
    fun `triggerReminder sends notification for pending todos`() = runTest {
        val todos = listOf(
            Todo("t1", testPlaceId, "逛古城", "", TodoStatus.PENDING, 1440, 0, 0, null),
            Todo("t2", testPlaceId, "看日出", "", TodoStatus.PENDING, 1440, 0, 0, null)
        )
        coEvery { repository.getTodos(testPlaceId) } returns flowOf(todos)

        useCase.triggerReminder(testPlaceId, testPlaceName)

        coVerify {
            notificationHelper.showReminder(
                placeId = testPlaceId,
                placeName = testPlaceName,
                todos = listOf("逛古城", "看日出"),
                completedPendingIntent = any(),
                inProgressPendingIntent = any(),
                passingPendingIntent = any()
            )
        }
    }

    @Test
    fun `triggerReminder sends notification for in-progress todos`() = runTest {
        val todos = listOf(
            Todo("t1", testPlaceId, "逛古城", "", TodoStatus.IN_PROGRESS, 1440, 1, 0, null)
        )
        coEvery { repository.getTodos(testPlaceId) } returns flowOf(todos)

        useCase.triggerReminder(testPlaceId, testPlaceName)

        coVerify {
            notificationHelper.showReminder(
                placeId = testPlaceId,
                placeName = testPlaceName,
                todos = listOf("逛古城"),
                completedPendingIntent = any(),
                inProgressPendingIntent = any(),
                passingPendingIntent = any()
            )
        }
    }

    @Test
    fun `triggerReminder skips completed todos`() = runTest {
        val todos = listOf(
            Todo("t1", testPlaceId, "逛古城", "", TodoStatus.COMPLETED, 1440, 0, 0, 0),
            Todo("t2", testPlaceId, "看日出", "", TodoStatus.PENDING, 1440, 0, 0, null)
        )
        coEvery { repository.getTodos(testPlaceId) } returns flowOf(todos)

        useCase.triggerReminder(testPlaceId, testPlaceName)

        coVerify {
            notificationHelper.showReminder(
                placeId = testPlaceId,
                placeName = testPlaceName,
                todos = listOf("看日出"), // Only PENDING todo
                completedPendingIntent = any(),
                inProgressPendingIntent = any(),
                passingPendingIntent = any()
            )
        }
    }

    @Test
    fun `triggerReminder skips skipped todos`() = runTest {
        val todos = listOf(
            Todo("t1", testPlaceId, "路过过的", "", TodoStatus.SKIPPED, 1440, 0, 0, 0)
        )
        coEvery { repository.getTodos(testPlaceId) } returns flowOf(todos)

        useCase.triggerReminder(testPlaceId, testPlaceName)

        coVerify(exactly = 0) {
            notificationHelper.showReminder(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `triggerReminder no todos does nothing`() = runTest {
        coEvery { repository.getTodos(testPlaceId) } returns flowOf(emptyList())

        useCase.triggerReminder(testPlaceId, testPlaceName)

        coVerify(exactly = 0) {
            notificationHelper.showReminder(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `triggerReminder all completed does nothing`() = runTest {
        val todos = listOf(
            Todo("t1", testPlaceId, "已完成", "", TodoStatus.COMPLETED, 1440, 0, 0, 0)
        )
        coEvery { repository.getTodos(testPlaceId) } returns flowOf(todos)

        useCase.triggerReminder(testPlaceId, testPlaceName)

        coVerify(exactly = 0) {
            notificationHelper.showReminder(any(), any(), any(), any(), any(), any())
        }
    }

    // ─── handleUserAction ────────────────────────────────────

    @Test
    fun `handleUserAction COMPLETED updates status and cancels notification`() = runTest {
        mockkStatic(NotificationManagerCompat::class)
        val mockManager = mockk<NotificationManagerCompat>(relaxed = true)
        every { NotificationManagerCompat.from(any<Context>()) } returns mockManager

        useCase.handleUserAction(testPlaceId, Constants.ACTION_COMPLETED)

        coVerify { repository.updateAllTodosStatus(testPlaceId, TodoStatus.COMPLETED) }
        verify { mockManager.cancel(testPlaceId.hashCode()) }
        unmockkStatic(NotificationManagerCompat::class)
    }

    @Test
    fun `handleUserAction IN_PROGRESS updates status to in progress`() = runTest {
        useCase.handleUserAction(testPlaceId, Constants.ACTION_IN_PROGRESS)

        coVerify { repository.updateAllTodosStatus(testPlaceId, TodoStatus.IN_PROGRESS) }
    }

    @Test
    fun `handleUserAction PASSING updates status and cancels notification`() = runTest {
        mockkStatic(NotificationManagerCompat::class)
        val mockManager = mockk<NotificationManagerCompat>(relaxed = true)
        every { NotificationManagerCompat.from(any<Context>()) } returns mockManager

        useCase.handleUserAction(testPlaceId, Constants.ACTION_PASSING)

        coVerify { repository.updateAllTodosStatus(testPlaceId, TodoStatus.SKIPPED) }
        verify { mockManager.cancel(testPlaceId.hashCode()) }
        unmockkStatic(NotificationManagerCompat::class)
    }
}
