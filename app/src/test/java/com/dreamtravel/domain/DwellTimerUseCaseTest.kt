package com.dreamtravel.domain

import com.dreamtravel.data.local.dao.DwellEventDao
import com.dreamtravel.data.local.entity.DwellEventEntity
import com.dreamtravel.data.model.Place
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DwellTimerUseCaseTest {

    private lateinit var dao: DwellEventDao
    private lateinit var useCase: DwellTimerUseCase

    private val testPlace = Place(
        id = "place-1",
        name = "大理",
        cityCode = null,
        latitude = 25.6,
        longitude = 100.2,
        dwellMinutes = 30,
        isActive = true,
        createdAt = 10000L
    )

    private val now = 1700000000000L
    private val tenMinutesAgo = now - 10 * 60 * 1000L
    private val fortyMinutesAgo = now - 40 * 60 * 1000L

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        useCase = DwellTimerUseCase(dao)
    }

    // ─── onEnterCity: No active event ────────────────────────

    @Test
    fun `onEnterCity no active event returns Started and inserts event`() = runTest {
        coEvery { dao.getActiveEvent("place-1") } returns null

        val result = useCase.onEnterCity(testPlace)

        assertTrue(result is DwellState.Started)
        coVerify { dao.insertEvent(match { it.placeId == "place-1" && it.status == "DWELLING" }) }
    }

    // ─── onEnterCity: Already TRIGGERED ──────────────────────

    @Test
    fun `onEnterCity already triggered returns AlreadyTriggered`() = runTest {
        val triggeredEvent = DwellEventEntity(
            id = "evt-1",
            placeId = "place-1",
            enteredAt = fortyMinutesAgo,
            status = "TRIGGERED"
        )
        coEvery { dao.getActiveEvent("place-1") } returns triggeredEvent

        val result = useCase.onEnterCity(testPlace)

        assertTrue(result is DwellState.AlreadyTriggered)
    }

    // ─── onEnterCity: DWELLING, not exceeded ─────────────────

    @Test
    fun `onEnterCity dwelling not exceeded returns Continuing`() = runTest {
        // dwellMinutes=30, elapsed=10min → not exceeded
        val dwellingEvent = DwellEventEntity(
            id = "evt-2",
            placeId = "place-1",
            enteredAt = tenMinutesAgo,
            status = "DWELLING"
        )
        coEvery { dao.getActiveEvent("place-1") } returns dwellingEvent

        val result = useCase.onEnterCity(testPlace)

        assertTrue(result is DwellState.Continuing)
        val elapsed = (result as DwellState.Continuing).elapsedMs
        assertTrue(elapsed > 0)
        assertTrue(elapsed < testPlace.dwellMinutes * 60_000L)
    }

    // ─── onEnterCity: DWELLING, exceeded ─────────────────────

    @Test
    fun `onEnterCity dwelling exceeded returns ShouldTrigger`() = runTest {
        // dwellMinutes=30, elapsed=40min → exceeded
        val dwellingEvent = DwellEventEntity(
            id = "evt-3",
            placeId = "place-1",
            enteredAt = fortyMinutesAgo,
            status = "DWELLING"
        )
        coEvery { dao.getActiveEvent("place-1") } returns dwellingEvent

        val result = useCase.onEnterCity(testPlace)

        assertTrue(result is DwellState.ShouldTrigger)
    }

    // ─── tickDwell ───────────────────────────────────────────

    @Test
    fun `tickDwell no active event returns NotDwelling`() = runTest {
        coEvery { dao.getActiveEvent("place-1") } returns null

        val result = useCase.tickDwell("place-1", 30)

        assertTrue(result is DwellTickResult.NotDwelling)
    }

    @Test
    fun `tickDwell not exceeded returns Ticking`() = runTest {
        val event = DwellEventEntity(
            id = "evt-4",
            placeId = "place-1",
            enteredAt = tenMinutesAgo,
            status = "DWELLING"
        )
        coEvery { dao.getActiveEvent("place-1") } returns event

        val result = useCase.tickDwell("place-1", 30)

        assertTrue(result is DwellTickResult.Ticking)
        val elapsed = (result as DwellTickResult.Ticking).elapsedMs
        assertTrue(elapsed > 0)
    }

    @Test
    fun `tickDwell exceeded returns ShouldTrigger and updates event`() = runTest {
        val event = DwellEventEntity(
            id = "evt-5",
            placeId = "place-1",
            enteredAt = fortyMinutesAgo,
            status = "DWELLING"
        )
        coEvery { dao.getActiveEvent("place-1") } returns event

        val result = useCase.tickDwell("place-1", 30)

        assertTrue(result is DwellTickResult.ShouldTrigger)
        coVerify {
            dao.updateEvent(match { it.status == "TRIGGERED" && it.triggeredAt != null })
        }
    }

    // ─── onExitCity ──────────────────────────────────────────

    @Test
    fun `onExitCity marks active event as EXITED`() = runTest {
        val event = DwellEventEntity(
            id = "evt-6",
            placeId = "place-1",
            enteredAt = tenMinutesAgo,
            status = "DWELLING"
        )
        coEvery { dao.getActiveEvent("place-1") } returns event

        useCase.onExitCity("place-1")

        coVerify {
            dao.updateEvent(match { it.status == "EXITED" && it.exitedAt != null })
        }
    }

    @Test
    fun `onExitCity no active event does nothing`() = runTest {
        coEvery { dao.getActiveEvent("place-1") } returns null

        useCase.onExitCity("place-1")

        // Should not crash, no update called
        coVerify(exactly = 0) { dao.updateEvent(any()) }
    }

    // ─── clearEvents ─────────────────────────────────────────

    @Test
    fun `clearEvents deletes events by place`() = runTest {
        useCase.clearEvents("place-1")

        coVerify { dao.deleteEventsByPlace("place-1") }
    }
}
