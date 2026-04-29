package com.dreamtravel.data.local.dao

import androidx.room.*
import com.dreamtravel.data.local.entity.DwellEventEntity

@Dao
interface DwellEventDao {

    @Insert
    suspend fun insertEvent(event: DwellEventEntity)

    @Update
    suspend fun updateEvent(event: DwellEventEntity)

    @Query("SELECT * FROM dwell_events WHERE placeId = :placeId ORDER BY enteredAt DESC LIMIT 1")
    suspend fun getLatestEvent(placeId: String): DwellEventEntity?

    @Query("SELECT * FROM dwell_events WHERE placeId = :placeId AND status = 'ENTERED' ORDER BY enteredAt DESC LIMIT 1")
    suspend fun getActiveEvent(placeId: String): DwellEventEntity?

    @Query("DELETE FROM dwell_events WHERE placeId = :placeId")
    suspend fun deleteEventsByPlace(placeId: String)
}
