package com.dreamtravel.util

/**
 * Severity of a status message, used for prioritization:
 * ERROR > WARNING > INFO.
 */
enum class Severity {
    ERROR, WARNING, INFO
}

/**
 * Logical category of the status message.
 */
enum class StatusType {
    NETWORK,
    LOCATION_GPS,
    LOCATION_PERMISSION,
    BACKGROUND_LOCATION,
    NOTIFICATION,
    BATTERY_OPTIMIZATION,
    FOREGROUND_SERVICE,
    SYNC
}

/**
 * Encapsulates the action a user can take from a status banner.
 * The [actionType] enum is resolved to an actual Intent in the UI layer
 * so that no Context reference is captured here.
 */
enum class StatusActionType {
    OPEN_LOCATION_SETTINGS,
    OPEN_APP_SETTINGS,
    OPEN_BATTERY_OPTIMIZATION
}

data class StatusAction(
    val labelResId: Int,
    val actionType: StatusActionType
)

/**
 * A single status banner/toast message emitted by [StatusManager].
 */
data class StatusMessage(
    val type: StatusType,
    val messageResId: Int,
    val severity: Severity,
    val action: StatusAction? = null
)

/**
 * Result of a single health diagnostic check, used by diagnostics panels.
 * When [passed] is false, [issue] contains the corresponding [StatusMessage]
 * with severity, message, and optional repair action.
 */
data class HealthCheckItem(
    val type: StatusType,
    val passed: Boolean,
    val issue: StatusMessage?
)
