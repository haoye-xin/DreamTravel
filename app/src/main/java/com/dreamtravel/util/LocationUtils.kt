package com.dreamtravel.util

import android.location.Location

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
}
