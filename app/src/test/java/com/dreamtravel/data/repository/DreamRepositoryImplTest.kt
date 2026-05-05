package com.dreamtravel.data.repository

import com.dreamtravel.data.local.dao.PlaceDao
import com.dreamtravel.data.local.dao.TodoDao
import com.dreamtravel.data.local.entity.PlaceEntity
import com.dreamtravel.data.local.entity.TodoEntity
import com.dreamtravel.data.model.Place
import com.dreamtravel.data.model.Todo
import com.dreamtravel.data.model.TodoStatus
import com.dreamtravel.data.remote.FirestoreSyncService
import io.mockk.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DreamRepositoryImplTest {

    private lateinit var placeDao: PlaceDao
    private lateinit var todoDao: TodoDao
    private lateinit var firestoreSync: FirestoreSyncService
    private lateinit var statusManager: com.dreamtravel.util.StatusManager
    private lateinit var repository: DreamRepositoryImpl

    private val testPlaceEntity = PlaceEntity(
        id = "place-1",
        name = "大理",
        cityCode = "大理",
        latitude = 25.6,
        longitude = 100.2,
        dwellMinutes = 30,
        isActive = true,
        createdAt = 1000L
    )

    private val testPlace = Place(
        id = "place-1",
        name = "大理",
        cityCode = "大理",
        latitude = 25.6,
        longitude = 100.2,
        dwellMinutes = 30,
        isActive = true,
        createdAt = 1000L,
        updatedAt = 1000L,
        pendingCount = 0,
        totalCount = 0
    )

    @Before
    fun setUp() {
        placeDao = mockk(relaxed = true)
        todoDao = mockk(relaxed = true)
        firestoreSync = mockk(relaxed = true)
        statusManager = mockk(relaxed = true)
        repository = DreamRepositoryImpl(placeDao, todoDao, firestoreSync, statusManager)
    }

    // ─── Places ──────────────────────────────────────────────

    @Test
    fun `getPlaces maps entities to domain with pending count`() = runTest {
        coEvery { placeDao.getAllPlaces() } returns flowOf(listOf(testPlaceEntity))
        coEvery { todoDao.countPendingTodos("place-1") } returns 3

        val result = repository.getPlaces().first()

        assertEquals(1, result.size)
        assertEquals("大理", result[0].name)
        assertEquals(3, result[0].pendingCount)
    }

    @Test
    fun `getPlaces handles empty list`() = runTest {
        coEvery { placeDao.getAllPlaces() } returns flowOf(emptyList())

        val result = repository.getPlaces().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getActivePlaces returns active places`() = runTest {
        coEvery { placeDao.getActivePlaces() } returns flowOf(listOf(testPlaceEntity))

        val result = repository.getActivePlaces().first()

        assertEquals(1, result.size)
        assertEquals("大理", result[0].name)
    }

    @Test
    fun `getPlaceById returns domain place`() = runTest {
        coEvery { placeDao.getPlaceById("place-1") } returns testPlaceEntity

        val result = repository.getPlaceById("place-1")

        assertNotNull(result)
        assertEquals("大理", result?.name)
    }

    @Test
    fun `getPlaceById returns null for missing`() = runTest {
        coEvery { placeDao.getPlaceById("nonexistent") } returns null

        val result = repository.getPlaceById("nonexistent")

        assertNull(result)
    }

    @Test
    fun `addPlace inserts locally and syncs to Firestore`() = runTest {
        repository.addPlace(testPlace)

        coVerify { placeDao.insertPlace(any()) }
        coVerify { firestoreSync.syncPlaceToFirestore(any()) }
    }

    @Test
    fun `updatePlace updates locally and syncs to Firestore`() = runTest {
        repository.updatePlace(testPlace)

        coVerify { placeDao.updatePlace(any()) }
        coVerify { firestoreSync.syncPlaceToFirestore(any()) }
    }

    @Test
    fun `deletePlace deletes locally and from Firestore`() = runTest {
        repository.deletePlace("place-1")

        coVerify { placeDao.deletePlaceById("place-1") }
        coVerify { firestoreSync.deletePlaceFromFirestore("place-1") }
    }

    @Test
    fun `setPlaceActive updates active flag`() = runTest {
        coEvery { placeDao.getPlaceById("place-1") } returns testPlaceEntity

        repository.setPlaceActive("place-1", false)

        coVerify { placeDao.setPlaceActive("place-1", false) }
    }

    // ─── Todos ───────────────────────────────────────────────

    @Test
    fun `getTodos maps entities to domain`() = runTest {
        val todoEntity = TodoEntity(
            id = "todo-1",
            placeId = "place-1",
            title = "逛古城",
            notes = "",
            status = "PENDING",
            remindIntervalMinutes = 1440,
            remindCount = 0,
            createdAt = 1000L
        )
        coEvery { todoDao.getTodosByPlace("place-1") } returns flowOf(listOf(todoEntity))

        val result = repository.getTodos("place-1").first()

        assertEquals(1, result.size)
        assertEquals("逛古城", result[0].title)
        assertEquals(TodoStatus.PENDING, result[0].status)
    }

    @Test
    fun `getTodos handles empty`() = runTest {
        coEvery { todoDao.getTodosByPlace("place-1") } returns flowOf(emptyList())

        val result = repository.getTodos("place-1").first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `addTodo inserts locally and syncs`() = runTest {
        val todo = Todo(
            id = "todo-1", placeId = "place-1", title = "逛古城",
            notes = "", status = TodoStatus.PENDING,
            remindIntervalMinutes = 1440, remindCount = 0,
            createdAt = 1000L, completedAt = null,
            updatedAt = 1000L
        )

        repository.addTodo(todo)

        coVerify { todoDao.insertTodo(any()) }
        coVerify { firestoreSync.syncTodoToFirestore(any()) }
    }

    @Test
    fun `updateTodoStatus sets completedAt when COMPLETED`() = runTest {
        repository.updateTodoStatus("todo-1", TodoStatus.COMPLETED)

        coVerify {
            todoDao.updateTodoStatus(
                todoId = "todo-1",
                status = "COMPLETED",
                completedAt = match { it != null }
            )
        }
    }

    @Test
    fun `updateTodoStatus null completedAt for PENDING`() = runTest {
        repository.updateTodoStatus("todo-1", TodoStatus.PENDING)

        coVerify {
            todoDao.updateTodoStatus(
                todoId = "todo-1",
                status = "PENDING",
                completedAt = null
            )
        }
    }

    @Test
    fun `updateAllTodosStatus delegates`() = runTest {
        coEvery { todoDao.getTodosByPlace("place-1") } returns flowOf(emptyList())
        repository.updateAllTodosStatus("place-1", TodoStatus.COMPLETED)

        coVerify { todoDao.updateAllTodosStatus("place-1", "COMPLETED") }
    }

    @Test
    fun `incrementRemindCount delegates`() = runTest {
        repository.incrementRemindCount("todo-1")

        coVerify { todoDao.incrementRemindCount("todo-1") }
    }

    @Test
    fun `countPendingTodos delegates`() = runTest {
        coEvery { todoDao.countPendingTodos("place-1") } returns 5

        val result = repository.countPendingTodos("place-1")

        assertEquals(5, result)
        coVerify { todoDao.countPendingTodos("place-1") }
    }
}
