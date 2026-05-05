package com.dreamtravel.analytics

/**
 * Structured analytics event names and parameter keys.
 * Backend-agnostic: compatible with Firebase Analytics, custom backends, or log-only.
 */
object AnalyticsEvent {

    // ── Onboarding ────────────────────────────────────────────────
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    const val ONBOARDING_SKIPPED = "onboarding_skipped"
    const val PERMISSION_FOREGROUND = "permission_foreground"
    const val PERMISSION_BACKGROUND = "permission_background"
    const val PERMISSION_NOTIFICATION = "permission_notification"

    // ── Places ────────────────────────────────────────────────────
    const val PLACE_ADDED = "place_added"
    const val PLACE_DELETED = "place_deleted"
    const val PLACE_TOGGLED = "place_toggled"

    // ── Todos ─────────────────────────────────────────────────────
    const val TODO_ADDED = "todo_added"
    const val TODO_EDITED = "todo_edited"
    const val TODO_COMPLETED = "todo_completed"
    const val TODO_DELETED = "todo_deleted"
    const val TODO_IN_PROGRESS = "todo_in_progress"
    const val TODO_SKIPPED = "todo_skipped"

    // ── Notifications ─────────────────────────────────────────────
    const val NOTIFICATION_REMINDER_SHOWN = "notification_reminder_shown"
    const val NOTIFICATION_ACTION_COMPLETED = "notification_action_completed"
    const val NOTIFICATION_ACTION_IN_PROGRESS = "notification_action_in_progress"
    const val NOTIFICATION_ACTION_PASSING = "notification_action_passing"

    // ── Screen Views ───────────────────────────────────────────────
    const val SETTINGS_OPENED = "settings_opened"

    // ── Account ───────────────────────────────────────────────────
    const val ACCOUNT_SIGN_UP = "account_sign_up"
    const val ACCOUNT_SIGN_IN = "account_sign_in"
    const val ACCOUNT_SIGN_OUT = "account_sign_out"
    const val ACCOUNT_UPGRADE = "account_upgrade"

    // ── Parameter keys ────────────────────────────────────────────
    object Param {
        const val CITY_NAME = "city_name"
        const val DWELL_MINUTES = "dwell_minutes"
        const val PLACE_ID = "place_id"
        const val IS_ACTIVE = "is_active"
        const val TODO_COUNT = "todo_count"
        const val GRANTED = "granted"
        const val FROM_NOTIFICATION = "from_notification"
        const val SCREEN = "screen"
        const val AUTH_TYPE = "auth_type"  // "anonymous" | "email"
    }
}
