package com.dreamtravel.util

import android.content.Context
import android.location.Location
import android.location.LocationManager

object LocationUtils {

    fun isInCircle(
        lat: Double, lng: Double,
        centerLat: Double, centerLng: Double,
        radiusMeters: Float
    ): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(lat, lng, centerLat, centerLng, results)
        return results[0] <= radiusMeters
    }

    fun hasSufficientAccuracy(location: Location, thresholdMeters: Float): Boolean {
        return location.hasAccuracy() && location.accuracy <= thresholdMeters
    }

    /**
     * Checks whether the GPS provider is enabled on the device.
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}
