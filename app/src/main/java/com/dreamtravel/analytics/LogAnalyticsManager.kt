package com.dreamtravel.analytics

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default analytics implementation that logs events to Logcat
 * and attaches custom keys to Crashlytics for crash correlation.
 * Tag: "DreamAnalytics" — filterable in logcat.
 *
 * Replace with FirebaseAnalytics or custom backend by providing
 * a different @Binds in AnalyticsModule.
 */
@Singleton
class LogAnalyticsManager @Inject constructor() : AnalyticsManager {

    companion object {
        private const val TAG = "DreamAnalytics"
    }

    override fun logEvent(eventName: String, params: Map<String, Any>) {
        val paramString = if (params.isEmpty()) {
            ""
        } else {
            params.entries.joinToString(", ") { (k, v) -> "$k=$v" }
        }

        if (paramString.isNotEmpty()) {
            Log.d(TAG, "[$eventName] {$paramString}")
        } else {
            Log.d(TAG, "[$eventName]")
        }

        // Crashlytics custom key for crash correlation
        logToCrashlytics(eventName, params)
    }

    override fun logEvent(eventName: String) {
        Log.d(TAG, "[$eventName]")
        logToCrashlytics(eventName, emptyMap())
    }

    override fun logUserAction(category: String, action: String, label: String?) {
        val params = mutableMapOf<String, Any>(
            "category" to category,
            "action" to action
        )
        if (!label.isNullOrBlank()) {
            params["label"] = label
        }
        Log.d(TAG, "[user_action] category=$category, action=$action, label=${label ?: "null"}")
        logToCrashlytics("user_action", params)
    }

    private fun logToCrashlytics(eventName: String, params: Map<String, Any>) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.log("[$eventName]")
            params.forEach { (key, value) ->
                crashlytics.setCustomKey("${eventName}_$key", value.toString())
            }
        } catch (_: Exception) {
            // Crashlytics may not be available on all devices
        }
    }
}
