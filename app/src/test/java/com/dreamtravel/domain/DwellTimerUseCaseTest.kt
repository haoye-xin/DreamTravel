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
        "place-1", "大理", null, 25.6, 100.2, 30, true, 10000L, 10000L
    )

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        useCase = DwellTimerUseCase(dao)
    }

    @Test
    fun `onEnterCity no active event returns Started`() = runTest {
        coEvery { dao.getActiveEvent("place-1") } returns null

        val result = useCase.onEnterCity(testPlace)

        assertTrue(result is DwellState.Started)
        coVerify { dao.insertEvent(match { it.placeId == "place-1" }) }
    }

    @Test
    fun `onEnterCity already triggered returns AlreadyTriggered`() = runTest {
        coEvery { dao.getActiveEvent("place-1") } returns DwellEventEntity(
            "evt-1", "place-1", System.currentTimeMillis() - 40 * 60 * 1000, status = "TRIGGERED"
        )

        val result = useCase.onEnterCity(testPlace)

        assertTrue(result is DwellState.AlreadyTriggered)
    }

    @Test
    fun `onEnterCity dwelling not exceeded returns Continuing`() = runTest {
        coEvery { dao.getActiveEvent("place-1") } returns DwellEventEntity(
            "evt-2", "place-1", System.currentTimeMillis() - 1000, status = "DWELLING"
        )

        val result = useCase.onEnterCity(testPlace)

        assertTrue(result is DwellState.Continuing)
    }

    @Test
    fun `onEnterCity dwelling exceeded returns ShouldTrigger`() = runTest {
        coEvery { dao.getActiveEvent("place-1") } returns DwellEventEntity(
            "evt-3", "place-1", System.currentTimeMillis() - 40 * 60 * 1000, status = "DWELLING"
        )

        val result = useCase.onEnterCity(testPlace)

        assertTrue(result is DwellState.ShouldTrigger)
    }

    @Test
    fun `tickDwell no active event returns NotDwelling`() = runTest {
        coEvery { dao.getActiveEvent("place-1") } returns null

        val result = useCase.tickDwell("place-1", 30)

        assertTrue(result is DwellTickResult.NotDwelling)
    }

    @Test
    fun `tickDwell not exceeded returns Ticking`() = runTest {
        coEvery { dao.getActiveEvent("place-1") } returns DwellEventEntity(
            "evt-4", "place-1", System.currentTimeMillis() - 1000, status = "DWELLING"
        )

        val result = useCase.tickDwell("place-1", 30)

        assertTrue(result is DwellTickResult.Ticking)
    }

    @Test
    fun `tickDwell exceeded returns ShouldTrigger`() = runTest {
        coEvery { dao.getActiveEvent("place-1") } returns DwellEventEntity(
            "evt-5", "place-1", System.currentTimeMillis() - 40 * 60 * 1000, status = "DWELLING"
        )

        val result = useCase.tickDwell("place-1", 30)

        assertTrue(result is DwellTickResult.ShouldTrigger)
        coVerify { dao.updateEvent(match { it.status == "TRIGGERED" }) }
    }

    @Test
    fun `onExitCity marks active event as EXITED`() = runTest {
        coEvery { dao.getActiveEvent("place-1") } returns DwellEventEntity(
            "evt-6", "place-1", System.currentTimeMillis() - 1000, status = "DWELLING"
        )

        useCase.onExitCity("place-1")

        coVerify { dao.updateEvent(match { it.status == "EXITED" }) }
    }

    @Test
    fun `onExitCity no active event does nothing`() = runTest {
        coEvery { dao.getActiveEvent("place-1") } returns null

        useCase.onExitCity("place-1")

        coVerify(exactly = 0) { dao.updateEvent(any()) }
    }

    @Test
    fun `clearEvents deletes events by place`() = runTest {
        useCase.clearEvents("place-1")

        coVerify { dao.deleteEventsByPlace("place-1") }
    }
}
