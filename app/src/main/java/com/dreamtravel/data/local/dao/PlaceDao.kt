package com.dreamtravel.data.local.dao

import androidx.room.*
import com.dreamtravel.data.local.entity.PlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {

    @Query("SELECT * FROM places ORDER BY createdAt DESC")
    fun getAllPlaces(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places ORDER BY createdAt DESC")
    suspend fun getAllPlacesList(): List<PlaceEntity>

    @Query("SELECT * FROM places WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActivePlaces(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE id = :placeId")
    suspend fun getPlaceById(placeId: String): PlaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: PlaceEntity)

    @Update
    suspend fun updatePlace(place: PlaceEntity)

    @Delete
    suspend fun deletePlace(place: PlaceEntity)

    @Query("DELETE FROM places WHERE id = :placeId")
    suspend fun deletePlaceById(placeId: String)

    @Query("UPDATE places SET isActive = :isActive WHERE id = :placeId")
    suspend fun setPlaceActive(placeId: String, isActive: Boolean)

    @Transaction
    suspend fun mergePlaces(places: List<PlaceEntity>) {
        for (place in places) {
            insertPlace(place)
        }
    }
}
