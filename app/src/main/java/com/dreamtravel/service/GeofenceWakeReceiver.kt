package com.dreamtravel.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint

/**
 * 接收 Geofence 事件（ENTER / EXIT）并转发给 LocationService 处理
 */
@AndroidEntryPoint
class GeofenceWakeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        LocationService.handleGeofenceEvent(context, intent)
    }
}
