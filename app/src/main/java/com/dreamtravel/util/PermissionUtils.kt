package com.dreamtravel.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermissionUtils {

    private const val PREFS_NAME = "dream_travel_prefs"
    const val KEY_PERMISSION_FOREGROUND = "permission_foreground_granted"
    const val KEY_PERMISSION_BACKGROUND = "permission_background_granted"
    const val KEY_PERMISSION_NOTIFICATION = "permission_notification_granted"

    // ── Check methods ──────────────────────────────────────────────

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBackgroundLocationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return hasLocationPermission(context)
        }
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ── Permission arrays ──────────────────────────────────────────

    /** Foreground location only — first step of staged flow */
    fun getForegroundPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    /** Background location only — second step of staged flow (API 29+) */
    fun getBackgroundPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            emptyArray()
        }
    }

    /** All permissions at once — kept as fallback / get-all method */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    /** POST_NOTIFICATIONS permission for API 33+ */
    fun getNotificationPermission(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }
    }

    // ── Denial detection ───────────────────────────────────────────

    /**
     * Returns true when the permission is permanently denied.
     * This is true when:
     *  - The permission is NOT granted, AND
     *  - shouldShowRequestPermissionRationale returns false on the Fragment
     */
    fun isPermanentlyDenied(fragment: Fragment, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            fragment.requireContext(), permission
        ) != PackageManager.PERMISSION_GRANTED &&
                !fragment.shouldShowRequestPermissionRationale(permission)
    }

    // ── Settings navigation ────────────────────────────────────────

    fun openAppSettings(context: Context) {
        context.startActivity(openAppSettingsIntent(context))
    }

    fun openAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }

    // ── SharedPreferences tracking ─────────────────────────────────

    fun savePermissionResult(context: Context, key: String, granted: Boolean) {
        getPrefs(context).edit().putBoolean(key, granted).apply()
    }

    fun getPermissionResult(context: Context, key: String): Boolean {
        return getPrefs(context).getBoolean(key, false)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
