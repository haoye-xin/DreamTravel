package com.dreamtravel.data.model

import com.dreamtravel.data.local.entity.PlaceEntity

data class Place(
    val id: String,
    val name: String,
    val cityCode: String?,
    val latitude: Double,
    val longitude: Double,
    val dwellMinutes: Int,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val pendingCount: Int = 0,
    val totalCount: Int = 0
)

fun PlaceEntity.toDomain(pendingCount: Int = 0, totalCount: Int = 0): Place = Place(
    id = id,
    name = name,
    cityCode = cityCode,
    latitude = latitude,
    longitude = longitude,
    dwellMinutes = dwellMinutes,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    pendingCount = pendingCount,
    totalCount = totalCount
)

fun Place.toEntity(): PlaceEntity = PlaceEntity(
    id = id,
    name = name,
    cityCode = cityCode,
    latitude = latitude,
    longitude = longitude,
    dwellMinutes = dwellMinutes,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)
