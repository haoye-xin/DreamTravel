package com.dreamtravel.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val cityCode: String? = null,
    val latitude: Double,
    val longitude: Double,
    val dwellMinutes: Int = 30,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val placeId: String,
    val title: String,
    val notes: String = "",
    val status: String = "PENDING",
    val remindIntervalMinutes: Int = 1440,
    val remindCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val provinceCode: String? = null,
    val provinceName: String? = null,
    val cityCode: String? = null,
    val cityName: String? = null,
    val districtCode: String? = null,
    val districtName: String? = null,
    val formattedAddress: String? = null,
    val color: String? = null
)

@Entity(tableName = "dwell_events")
data class DwellEventEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val placeId: String,
    val enteredAt: Long,
    val exitedAt: Long? = null,
    val triggeredAt: Long? = null,
    val status: String = "ENTERED"
)
