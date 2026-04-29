package com.dreamtravel.domain

import com.dreamtravel.data.local.dao.DwellEventDao
import com.dreamtravel.data.local.entity.DwellEventEntity
import com.dreamtravel.data.model.Place
import javax.inject.Inject

/**
 * 驻留计时器逻辑。
 * 管理 ENTER → DWELLING → TRIGGERED → EXITED 的状态转换。
 */
class DwellTimerUseCase @Inject constructor(
    private val dwellEventDao: DwellEventDao
) {

    /**
     * 记录进入事件。如果已有一个活跃的 ENTERED/DWELLING 事件，
     * 检查是否在容差时间内（5分钟），决定是继续计时还是重置。
     */
    suspend fun onEnterCity(place: Place): DwellState {
        val activeEvent = dwellEventDao.getActiveEvent(place.id)

        if (activeEvent != null) {
            val elapsed = System.currentTimeMillis() - activeEvent.enteredAt

            // 已经触发了通知，不再重新计时
            if (activeEvent.status == "TRIGGERED") {
                return DwellState.AlreadyTriggered
            }

            // 还在计时中，继续
            if (elapsed < place.dwellMinutes * 60_000L) {
                return DwellState.Continuing(elapsed)
            }

            // 已经超过驻留时间
            return DwellState.ShouldTrigger
        }

        // 无活跃事件，创建新事件
        val event = DwellEventEntity(
            placeId = place.id,
            enteredAt = System.currentTimeMillis(),
            status = "DWELLING"
        )
        dwellEventDao.insertEvent(event)
        return DwellState.Started
    }

    /**
     * 累计计时。返回当前已驻留的毫秒数和是否应触发通知。
     */
    suspend fun tickDwell(placeId: String, dwellMinutes: Int): DwellTickResult {
        val event = dwellEventDao.getActiveEvent(placeId) ?: return DwellTickResult.NotDwelling

        val elapsed = System.currentTimeMillis() - event.enteredAt

        return if (elapsed >= dwellMinutes * 60_000L) {
            dwellEventDao.updateEvent(
                event.copy(status = "TRIGGERED", triggeredAt = System.currentTimeMillis())
            )
            DwellTickResult.ShouldTrigger(elapsed)
        } else {
            DwellTickResult.Ticking(elapsed)
        }
    }

    /**
     * 离开城市，标记事件为 EXITED
     */
    suspend fun onExitCity(placeId: String) {
        val event = dwellEventDao.getActiveEvent(placeId) ?: return
        dwellEventDao.updateEvent(
            event.copy(status = "EXITED", exitedAt = System.currentTimeMillis())
        )
    }

    /**
     * 清理指定地点的事件记录
     */
    suspend fun clearEvents(placeId: String) {
        dwellEventDao.deleteEventsByPlace(placeId)
    }
}

sealed class DwellState {
    data object Started : DwellState()
    data class Continuing(val elapsedMs: Long) : DwellState()
    data object ShouldTrigger : DwellState()
    data object AlreadyTriggered : DwellState()
}

sealed class DwellTickResult {
    data class Ticking(val elapsedMs: Long) : DwellTickResult()
    data class ShouldTrigger(val elapsedMs: Long) : DwellTickResult()
    data object NotDwelling : DwellTickResult()
}
