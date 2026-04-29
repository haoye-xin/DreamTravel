package com.dreamtravel.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 设备开机后自动恢复定位服务
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            LocationService.startForBoot(context)
        }
    }
}
