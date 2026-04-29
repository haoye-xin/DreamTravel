package com.dreamtravel

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class DreamTravelApp : Application() {

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
