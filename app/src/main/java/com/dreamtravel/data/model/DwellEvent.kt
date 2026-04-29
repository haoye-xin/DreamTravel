package com.dreamtravel.data.model

enum class DwellStatus(val value: String) {
    ENTERED("ENTERED"),
    DWELLING("DWELLING"),
    TRIGGERED("TRIGGERED"),
    EXITED("EXITED");

    companion object {
        fun from(value: String): DwellStatus = entries.firstOrNull { it.value == value } ?: ENTERED
    }
}

data class DwellEvent(
    val id: String,
    val placeId: String,
    val enteredAt: Long,
    val exitedAt: Long?,
    val triggeredAt: Long?,
    val status: DwellStatus
)
