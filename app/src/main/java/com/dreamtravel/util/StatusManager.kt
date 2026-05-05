package com.dreamtravel.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.dreamtravel.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors system states and exposes them as a [SharedFlow] of [StatusMessage].
 *
 * Checks performed:
 * - Network connectivity (ERROR)
 * - Location permission (ERROR)
 * - GPS provider enabled (WARNING, only when location permission is granted)
 * - Notification permission (WARNING, API 33+)
 * - Background location permission (WARNING, only when foreground granted)
 * - Battery optimization exemption (WARNING)
 * - Foreground service running (INFO)
 * - Sync failures (WARNING, externally reported)
 *
 * Also provides [checkAllDetailed] for diagnostics panels showing
 * pass/fail for every component with repair actions.
 *
 * Emits ONE message at a time: highest severity first (Error > Warning > Info).
 *
 * Consumer should call [checkAllStatuses] periodically and collect [statusFlow].
 */
@Singleton
class StatusManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val powerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private val _statusFlow = MutableSharedFlow<StatusMessage>(replay = 0, extraBufferCapacity = 1)
    val statusFlow: SharedFlow<StatusMessage> = _statusFlow

    private val _statusList = MutableStateFlow<List<StatusMessage>>(emptyList())
    /** All current status checks (not just the highest severity). Useful for diagnostics panels. */
    val statusList: StateFlow<List<StatusMessage>> = _statusList.asStateFlow()

    @Volatile
    private var syncFailed: Boolean = false

    // ── Public API ─────────────────────────────────────────────────

    fun reportSyncResult(success: Boolean) {
        syncFailed = !success
    }

    /**
     * Runs all status checks, emits the highest-severity message to [statusFlow],
     * and updates [statusList] with ALL current statuses for diagnostics panels.
     * Safe to call from any coroutine context.
     */
    suspend fun checkAllStatuses() {
        val messages = buildStatusList()

        // Emit highest severity message for snackbar consumers
        val toEmit = messages.minByOrNull { it.severity.ordinal }
        if (toEmit != null) {
            _statusFlow.tryEmit(toEmit)
        }

        _statusList.value = messages
    }

    /**
     * Returns a snapshot of all status checks. Does NOT emit to [statusFlow].
     * Call this from a background thread/coroutine to avoid blocking the UI.
     */
    fun getStatusSnapshot(): List<StatusMessage> = buildStatusList()

    /**
     * Returns a detailed diagnostic of ALL system checks (passing and failing).
     * Unlike [getStatusSnapshot] which only returns issues, this returns every
     * check so diagnostics panels can show green/pass for healthy components.
     */
    fun checkAllDetailed(): List<HealthCheckItem> {
        val issues = buildStatusList()
        val issueByType = issues.associateBy { it.type }

        // Display order: network → foreground location → background location →
        // gps → notification → battery → foreground service → sync
        val orderedTypes = listOf(
            StatusType.NETWORK,
            StatusType.LOCATION_PERMISSION,
            StatusType.BACKGROUND_LOCATION,
            StatusType.LOCATION_GPS,
            StatusType.NOTIFICATION,
            StatusType.BATTERY_OPTIMIZATION,
            StatusType.FOREGROUND_SERVICE,
            StatusType.SYNC
        )

        return orderedTypes.map { type ->
            HealthCheckItem(
                type = type,
                passed = issueByType[type] == null,
                issue = issueByType[type]
            )
        }
    }

    // ── Internal ─────────────────────────────────────────────

    private fun buildStatusList(): List<StatusMessage> {
        val messages = mutableListOf<StatusMessage>()

        // 1. Network — ERROR
        if (!isNetworkAvailable()) {
            messages.add(
                StatusMessage(
                    type = StatusType.NETWORK,
                    messageResId = R.string.status_network_down,
                    severity = Severity.ERROR
                )
            )
        }

        // 2. Location permission — ERROR
        val hasLocationPerm = PermissionUtils.hasLocationPermission(context)
        if (!hasLocationPerm) {
            messages.add(
                StatusMessage(
                    type = StatusType.LOCATION_PERMISSION,
                    messageResId = R.string.status_location_permission_denied,
                    severity = Severity.ERROR,
                    action = StatusAction(
                        labelResId = R.string.go_to_settings,
                        actionType = StatusActionType.OPEN_APP_SETTINGS
                    )
                )
            )
        }

        // 3. GPS provider — WARNING (only meaningful when location permission granted)
        if (hasLocationPerm && !LocationUtils.isLocationEnabled(context)) {
            messages.add(
                StatusMessage(
                    type = StatusType.LOCATION_GPS,
                    messageResId = R.string.status_gps_disabled,
                    severity = Severity.WARNING,
                    action = StatusAction(
                        labelResId = R.string.go_to_settings,
                        actionType = StatusActionType.OPEN_LOCATION_SETTINGS
                    )
                )
            )
        }

        // 4. Notification permission — WARNING (API 33+)
        if (!PermissionUtils.hasNotificationPermission(context)) {
            messages.add(
                StatusMessage(
                    type = StatusType.NOTIFICATION,
                    messageResId = R.string.status_notification_denied,
                    severity = Severity.WARNING
                )
            )
        }

        // 5. Background location permission — WARNING (only when foreground granted)
        if (hasLocationPerm && !PermissionUtils.hasBackgroundLocationPermission(context)) {
            messages.add(
                StatusMessage(
                    type = StatusType.BACKGROUND_LOCATION,
                    messageResId = R.string.status_background_location_denied,
                    severity = Severity.WARNING,
                    action = StatusAction(
                        labelResId = R.string.go_to_settings,
                        actionType = StatusActionType.OPEN_APP_SETTINGS
                    )
                )
            )
        }

        // 6. Battery optimization exemption — WARNING
        if (!isBatteryOptimizationExempt()) {
            messages.add(
                StatusMessage(
                    type = StatusType.BATTERY_OPTIMIZATION,
                    messageResId = R.string.status_battery_optimization,
                    severity = Severity.WARNING,
                    action = StatusAction(
                        labelResId = R.string.settings_diag_fix,
                        actionType = StatusActionType.OPEN_BATTERY_OPTIMIZATION
                    )
                )
            )
        }

        // 7. Foreground service — INFO
        if (!isForegroundServiceRunning()) {
            messages.add(
                StatusMessage(
                    type = StatusType.FOREGROUND_SERVICE,
                    messageResId = R.string.status_foreground_service,
                    severity = Severity.INFO
                )
            )
        }

        // 8. Sync failure — WARNING
        if (syncFailed) {
            messages.add(
                StatusMessage(
                    type = StatusType.SYNC,
                    messageResId = R.string.status_sync_failed,
                    severity = Severity.WARNING
                )
            )
        }

        return messages
    }

    // ── Helpers ────────────────────────────────────────────────────

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isBatteryOptimizationExempt(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    @Suppress("DEPRECATION")
    private fun isForegroundServiceRunning(): Boolean {
        try {
            for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
                if (service.service.className.contains("LocationService") &&
                    service.foreground
                ) {
                    return true
                }
            }
        } catch (_: Exception) {
            // Fallback: assume not running if we can't check
        }
        return false
    }

    // ── Action resolution (called from UI layer) ───────────────────

    fun resolveAction(actionType: StatusActionType): Intent? {
        return when (actionType) {
            StatusActionType.OPEN_LOCATION_SETTINGS -> Intent(
                Settings.ACTION_LOCATION_SOURCE_SETTINGS
            )
            StatusActionType.OPEN_APP_SETTINGS -> PermissionUtils.openAppSettingsIntent(context)
            StatusActionType.OPEN_BATTERY_OPTIMIZATION -> Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            ).apply {
                data = Uri.parse("package:" + context.packageName)
            }
        }
    }
}
