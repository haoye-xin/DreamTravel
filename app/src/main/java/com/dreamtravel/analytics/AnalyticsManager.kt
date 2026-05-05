package com.dreamtravel.analytics

/**
 * Lightweight analytics abstraction.
 * Swap the Hilt binding to FirebaseAnalytics, custom backend,
 * or keep the default log-only implementation.
 */
interface AnalyticsManager {

    /**
     * Log an analytics event with optional parameters.
     * Parameters should be primitive types (String, Int, Long, Boolean, Double)
     * or arrays of String — keep values short and free of PII.
     */
    fun logEvent(eventName: String, params: Map<String, Any> = emptyMap())

    /**
     * Convenience: log a simple event with no parameters.
     */
    fun logEvent(eventName: String)

    /**
     * Log a structured user action with category, action, and label.
     * Follows the classic Event-Category-Action-Label model.
     */
    fun logUserAction(category: String, action: String, label: String? = null)
}
