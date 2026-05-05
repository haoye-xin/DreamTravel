package com.dreamtravel.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dreamtravel.domain.ReminderSchedulerUseCase
import com.dreamtravel.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 处理通知上的三个 Action 按钮点击。
 * 支持单待办粒度（EXTRA_TODO_ID）和地点级粒度。
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderScheduler: ReminderSchedulerUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val placeId = intent.getStringExtra(Constants.EXTRA_PLACE_ID) ?: return
        val action = intent.action ?: return
        val todoId = intent.getStringExtra(Constants.EXTRA_TODO_ID)

        CoroutineScope(Dispatchers.IO).launch {
            reminderScheduler.handleUserAction(placeId, action, todoId)
        }
    }
}
