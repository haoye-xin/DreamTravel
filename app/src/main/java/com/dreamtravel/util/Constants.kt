package com.dreamtravel.util

object Constants {
    // Geofence
    const val GEOFENCE_WAKE_RADIUS_METERS = 20_000f  // 20km
    const val GEOFENCE_EXPIRATION_HOURS = 720  // 30 days
    const val GEOFENCE_REQUEST_ID_PREFIX = "dream_geofence_"

    // Location
    const val DWELL_CHECK_INTERVAL_MS = 2 * 60 * 1000L  // 2 minutes
    const val GPS_ACCURACY_THRESHOLD_M = 100f
    const val DWELL_TOLERANCE_MS = 5 * 60 * 1000L  // 5 minutes
    const val GPS_UNAVAILABLE_TIMEOUT_MS = 5 * 60 * 1000L

    // Notification
    const val CHANNEL_DREAM_REMINDER = "dream_reminder"
    const val CHANNEL_LOCATION_SERVICE = "location_service"
    const val MIN_REMIND_INTERVAL_MS = 30 * 60 * 1000L  // 30 min minimum

    // Notification Action IDs
    const val ACTION_COMPLETED = "com.dreamtravel.ACTION_COMPLETED"
    const val ACTION_IN_PROGRESS = "com.dreamtravel.ACTION_IN_PROGRESS"
    const val ACTION_PASSING = "com.dreamtravel.ACTION_PASSING"

    // Extras
    const val EXTRA_PLACE_ID = "extra_place_id"
    const val EXTRA_PLACE_NAME = "extra_place_name"
    const val EXTRA_TODO_ID = "todo_id"

    // Defaults
    const val DEFAULT_DWELL_MINUTES = 30
    const val DEFAULT_REMIND_INTERVAL_MINUTES = 1440  // 24 hours
}
