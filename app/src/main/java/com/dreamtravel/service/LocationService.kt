package com.dreamtravel.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.dreamtravel.R
import com.dreamtravel.data.local.dao.DwellEventDao
import com.dreamtravel.data.local.dao.PlaceDao
import com.dreamtravel.data.local.entity.DwellEventEntity
import com.dreamtravel.data.local.entity.PlaceEntity
import com.dreamtravel.data.model.TodoStatus
import com.dreamtravel.data.repository.DreamRepository
import com.dreamtravel.domain.CityDetectionUseCase
import com.dreamtravel.domain.DwellState
import com.dreamtravel.domain.DwellTimerUseCase
import com.dreamtravel.domain.ReminderSchedulerUseCase
import com.dreamtravel.notification.NotificationHelper
import com.dreamtravel.util.Constants
import com.dreamtravel.util.PermissionUtils
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    @Inject lateinit var placeDao: PlaceDao
    @Inject lateinit var dwellEventDao: DwellEventDao
    @Inject lateinit var dwellTimer: DwellTimerUseCase
    @Inject lateinit var cityDetection: CityDetectionUseCase
    @Inject lateinit var reminderScheduler: ReminderSchedulerUseCase
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var repository: DreamRepository
    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var geofencingClient: GeofencingClient
    private var dwellCheckJob: Job? = null
    private var amapLocationClient: AMapLocationClient? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Per-place dwell state
    private val placeDwellState = mutableMapOf<String, DwellInfo>()

    data class DwellInfo(
        var accumulatedMs: Long = 0,
        var lastCheckTime: Long = 0,
        var lastKnownCity: String = "",
        var isActive: Boolean = false
    )

    companion object {
        private var instance: LocationService? = null

        fun startForBoot(context: Context) {
            if (PermissionUtils.hasBackgroundLocationPermission(context)) {
                val intent = Intent(context, LocationService::class.java)
                context.startForegroundService(intent)
            }
        }

        fun handleGeofenceEvent(context: Context, intent: Intent) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
            if (geofencingEvent.hasError()) return

            val transition = geofencingEvent.geofenceTransition
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                transition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                instance?.onGeofenceEnter(triggeringGeofences ?: emptyList())
            } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                instance?.onGeofenceExit(triggeringGeofences ?: emptyList())
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        geofencingClient = LocationServices.getGeofencingClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notificationHelper.buildLocationServiceNotification()
        startForeground(1001, notification)

        // 注册所有活跃地点的 Geofence
        serviceScope.launch {
            registerAllGeofences()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        instance = null
        stopDwellCheck()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ─── Geofence 管理 ───────────────────────────────────────

    private suspend fun registerAllGeofences() {
        if (!PermissionUtils.hasLocationPermission(this)) return

        val activePlaces = placeDao.getActivePlaces()
        activePlaces.collect { places ->
            val geofenceList = mutableListOf<Geofence>()
            for (place in places) {
                geofenceList.add(
                    Geofence.Builder()
                        .setRequestId(Constants.GEOFENCE_REQUEST_ID_PREFIX + place.id)
                        .setCircularRegion(place.latitude, place.longitude, Constants.GEOFENCE_WAKE_RADIUS_METERS)
                        .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_HOURS.toLong() * 3600 * 1000)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build()
                )
            }

            if (geofenceList.isEmpty()) {
                // 无活跃地点，移除所有 geofence
                geofencingClient.removeGeofences(
                    PendingIntent.getBroadcast(
                        this, 0,
                        Intent(this, GeofenceWakeReceiver::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                return@collect
            }

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofenceList)
                .build()

            val pendingIntent = PendingIntent.getBroadcast(
                this, 0,
                Intent(this, GeofenceWakeReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                geofencingClient.removeGeofences(pendingIntent)
                geofencingClient.addGeofences(request, pendingIntent)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    // ─── Geofence 事件处理 ────────────────────────────────────

    private fun onGeofenceEnter(triggeringGeofences: List<Geofence>) {
        serviceScope.launch {
            for (geofence in triggeringGeofences) {
                val placeId = geofence.requestId.removePrefix(Constants.GEOFENCE_REQUEST_ID_PREFIX)
                val place = placeDao.getPlaceById(placeId) ?: continue

                // 启动驻留检查
                placeDwellState[placeId] = DwellInfo(isActive = true)

                // 记录进入事件
                dwellTimer.onEnterCity(
                    com.dreamtravel.data.model.Place(
                        id = place.id,
                        name = place.name,
                        cityCode = place.cityCode,
                        latitude = place.latitude,
                        longitude = place.longitude,
                        dwellMinutes = place.dwellMinutes,
                        isActive = place.isActive,
                        createdAt = place.createdAt,
                        updatedAt = place.updatedAt
                    )
                )

                // 启动驻留检查协程
                startDwellCheck()
            }
        }
    }

    private fun onGeofenceExit(triggeringGeofences: List<Geofence>) {
        serviceScope.launch {
            for (geofence in triggeringGeofences) {
                val placeId = geofence.requestId.removePrefix(Constants.GEOFENCE_REQUEST_ID_PREFIX)
                placeDwellState.remove(placeId)
                dwellTimer.onExitCity(placeId)
            }

            // 如果没有任何活跃地点，停止检查
            if (placeDwellState.isEmpty()) {
                stopDwellCheck()
            }
        }
    }

    // ─── 驻留检查协程 ─────────────────────────────────────────

    private fun startDwellCheck() {
        if (dwellCheckJob?.isActive == true) return

        dwellCheckJob = serviceScope.launch {
            while (isActive) {
                checkCurrentLocation()

                if (placeDwellState.isEmpty()) {
                    stopDwellCheck()
                    break
                }

                delay(Constants.DWELL_CHECK_INTERVAL_MS)
            }
        }
    }

    private fun stopDwellCheck() {
        dwellCheckJob?.cancel()
        dwellCheckJob = null
    }

    private suspend fun checkCurrentLocation() {
        if (!PermissionUtils.hasLocationPermission(this@LocationService)) return

        try {
            val location = suspendCancellableCoroutine<Location?> { cont ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { loc -> cont.resume(loc) {} }
                    .addOnFailureListener { cont.resume(null) {} }
            } ?: return

            // 精度检查
            if (!com.dreamtravel.util.LocationUtils.hasSufficientAccuracy(
                    location,
                    Constants.GPS_ACCURACY_THRESHOLD_M
                )
            ) return

            // 逆地理编码获取城市名
            val cityName = reverseGeocode(location.latitude, location.longitude) ?: return

            // 检查每个活跃的驻留
            for ((placeId, dwellInfo) in placeDwellState.toMap()) {
                if (!dwellInfo.isActive) continue

                val placeEntity = placeDao.getPlaceById(placeId) ?: continue
                val place = com.dreamtravel.data.model.Place(
                    id = placeEntity.id,
                    name = placeEntity.name,
                    cityCode = placeEntity.cityCode,
                    latitude = placeEntity.latitude,
                    longitude = placeEntity.longitude,
                    dwellMinutes = placeEntity.dwellMinutes,
                    isActive = placeEntity.isActive,
                    createdAt = placeEntity.createdAt,
                    updatedAt = placeEntity.updatedAt
                )

                if (cityDetection.isCityMatch(cityName, place)) {
                    // 在梦想城市内 → 累计时间
                    val elapsed = System.currentTimeMillis() - (dwellInfo.lastCheckTime.let {
                        if (it == 0L) System.currentTimeMillis() else it
                    })

                    // 更新上次检查时间
                    placeDwellState[placeId] = dwellInfo.copy(
                        lastCheckTime = System.currentTimeMillis()
                    )

                    // 通过 DwellTimer 累计
                    val result = dwellTimer.tickDwell(placeId, place.dwellMinutes)
                    when (result) {
                        is com.dreamtravel.domain.DwellTickResult.ShouldTrigger -> {
                            reminderScheduler.triggerReminder(placeId, place.name)
                            placeDwellState[placeId] = dwellInfo.copy(isActive = false)
                        }
                        is com.dreamtravel.domain.DwellTickResult.NotDwelling -> {
                            // 需要重新进入
                        }
                        is com.dreamtravel.domain.DwellTickResult.Ticking -> {
                            // 继续等待
                        }
                    }
                } else {
                    // 不在梦想城市 → 检查是否在容差时间内
                    val timeSinceLastCheck = System.currentTimeMillis() - dwellInfo.lastCheckTime
                    if (dwellInfo.lastCheckTime > 0 && timeSinceLastCheck > Constants.DWELL_TOLERANCE_MS) {
                        // 超过容差，重置
                        dwellTimer.onExitCity(placeId)
                        placeDwellState[placeId] = DwellInfo(isActive = true)
                        dwellTimer.onEnterCity(place)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ─── 逆地理编码 ───────────────────────────────────────────

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String? {
        return suspendCancellableCoroutine { cont ->
            try {
                val client = AMapLocationClient(applicationContext)
                val option = AMapLocationClientOption().apply {
                    locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                    isNeedAddress = true
                    isOnceLocation = true
                }
                client.setLocationOption(option)

                client.setLocationListener(object : AMapLocationListener {
                    override fun onLocationChanged(location: com.amap.api.location.AMapLocation?) {
                        val city = location?.city ?: location?.district ?: ""
                        cont.resume(city.ifBlank { null }) {}
                        client.onDestroy()
                    }
                })

                client.startLocation()
            } catch (e: Exception) {
                cont.resume(null) {}
            }
        }
    }
}
