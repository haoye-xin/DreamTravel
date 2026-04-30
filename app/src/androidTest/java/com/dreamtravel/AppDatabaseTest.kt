package com.dreamtravel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dreamtravel.data.local.AppDatabase
import com.dreamtravel.data.local.entity.DwellEventEntity
import com.dreamtravel.data.local.entity.PlaceEntity
import com.dreamtravel.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert and retrieve place`() = runBlocking {
        val place = PlaceEntity(
            id = "p1",
            name = "大理",
            cityCode = "大理",
            latitude = 25.6,
            longitude = 100.2,
            dwellMinutes = 30,
            isActive = true,
            createdAt = 1000L
        )
        db.placeDao().insertPlace(place)

        val result = db.placeDao().getPlaceById("p1")
        assertNotNull(result)
        assertEquals("大理", result?.name)
    }

    @Test
    fun `get active places filters correctly`() = runBlocking {
        db.placeDao().insertPlace(
            PlaceEntity(id = "p1", name = "大理", latitude = 25.6, longitude = 100.2, isActive = true)
        )
        db.placeDao().insertPlace(
            PlaceEntity(id = "p2", name = "昆明", latitude = 25.0, longitude = 102.7, isActive = false)
        )

        val activePlaces = db.placeDao().getActivePlaces().first()
        assertEquals(1, activePlaces.size)
        assertEquals("大理", activePlaces[0].name)
    }

    @Test
    fun `insert and retrieve todo`() = runBlocking {
        db.placeDao().insertPlace(
            PlaceEntity(id = "p1", name = "大理", latitude = 25.6, longitude = 100.2)
        )
        db.todoDao().insertTodo(
            TodoEntity(id = "t1", placeId = "p1", title = "逛古城", notes = "")
        )

        val todos = db.todoDao().getTodosByPlace("p1").first()
        assertEquals(1, todos.size)
        assertEquals("逛古城", todos[0].title)
    }

    @Test
    fun `update todo status`() = runBlocking {
        db.placeDao().insertPlace(
            PlaceEntity(id = "p1", name = "大理", latitude = 25.6, longitude = 100.2)
        )
        db.todoDao().insertTodo(
            TodoEntity(id = "t1", placeId = "p1", title = "逛古城", status = "PENDING")
        )

        db.todoDao().updateTodoStatus("t1", "COMPLETED", 2000L)

        val todos = db.todoDao().getTodosByPlace("p1").first()
        assertEquals("COMPLETED", todos[0].status)
        assertEquals(2000L, todos[0].completedAt)
    }

    @Test
    fun `count pending todos`() = runBlocking {
        db.placeDao().insertPlace(
            PlaceEntity(id = "p1", name = "大理", latitude = 25.6, longitude = 100.2)
        )
        db.todoDao().insertTodo(
            TodoEntity(id = "t1", placeId = "p1", title = "A", status = "PENDING")
        )
        db.todoDao().insertTodo(
            TodoEntity(id = "t2", placeId = "p1", title = "B", status = "COMPLETED")
        )
        db.todoDao().insertTodo(
            TodoEntity(id = "t3", placeId = "p1", title = "C", status = "IN_PROGRESS")
        )

        val count = db.todoDao().countPendingTodos("p1")
        assertEquals(2, count) // PENDING + IN_PROGRESS
    }

    @Test
    fun `update all todos status`() = runBlocking {
        db.placeDao().insertPlace(
            PlaceEntity(id = "p1", name = "大理", latitude = 25.6, longitude = 100.2)
        )
        db.todoDao().insertTodo(
            TodoEntity(id = "t1", placeId = "p1", title = "A", status = "PENDING")
        )
        db.todoDao().insertTodo(
            TodoEntity(id = "t2", placeId = "p1", title = "B", status = "PENDING")
        )

        db.todoDao().updateAllTodosStatus("p1", "COMPLETED")

        val todos = db.todoDao().getTodosByPlace("p1").first()
        assertTrue(todos.all { it.status == "COMPLETED" })
    }

    @Test
    fun `dwell event lifecycle`() = runBlocking {
        val event = DwellEventEntity(
            id = "e1",
            placeId = "p1",
            enteredAt = 1000L,
            status = "DWELLING"
        )
        db.dwellEventDao().insertEvent(event)

        val active = db.dwellEventDao().getActiveEvent("p1")
        assertNotNull(active)
        assertEquals("DWELLING", active?.status)

        db.dwellEventDao().updateEvent(
            event.copy(status = "TRIGGERED", triggeredAt = 2000L)
        )

        val updated = db.dwellEventDao().getActiveEvent("p1")
        assertEquals("TRIGGERED", updated?.status)

        db.dwellEventDao().deleteEventsByPlace("p1")
        val deleted = db.dwellEventDao().getActiveEvent("p1")
        assertNull(deleted)
    }
}
