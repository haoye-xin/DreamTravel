package com.dreamtravel

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DreamTravelApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // 初始化 Firebase
        FirebaseApp.initializeApp(this)

        // 匿名登录（静默，不阻塞 App 启动）
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseAuth.getInstance().signInAnonymously().await()
            } catch (e: Exception) {
                // 降级：允许只使用本地功能
                e.printStackTrace()
            }
        }
    }
}
