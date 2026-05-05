package com.dreamtravel.data.model

import com.dreamtravel.data.local.entity.TodoEntity

enum class TodoStatus(val value: String) {
    PENDING("PENDING"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    SKIPPED("SKIPPED");

    companion object {
        fun from(value: String): TodoStatus = entries.firstOrNull { it.value == value } ?: PENDING
    }
}

data class Todo(
    val id: String,
    val placeId: String,
    val title: String,
    val notes: String,
    val status: TodoStatus,
    val remindIntervalMinutes: Int,
    val remindCount: Int,
    val createdAt: Long,
    val completedAt: Long?,
    val updatedAt: Long
)

fun TodoEntity.toDomain(): Todo = Todo(
    id = id,
    placeId = placeId,
    title = title,
    notes = notes,
    status = TodoStatus.from(status),
    remindIntervalMinutes = remindIntervalMinutes,
    remindCount = remindCount,
    createdAt = createdAt,
    completedAt = completedAt,
    updatedAt = updatedAt
)

fun Todo.toEntity(): TodoEntity = TodoEntity(
    id = id,
    placeId = placeId,
    title = title,
    notes = notes,
    status = status.value,
    remindIntervalMinutes = remindIntervalMinutes,
    remindCount = remindCount,
    createdAt = createdAt,
    completedAt = completedAt,
    updatedAt = updatedAt
)
