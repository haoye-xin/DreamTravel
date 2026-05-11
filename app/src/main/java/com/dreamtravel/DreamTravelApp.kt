package com.dreamtravel

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.amap.api.services.core.ServiceSettings
import com.dreamtravel.data.repository.DreamRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject

@HiltAndroidApp
class DreamTravelApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var dreamRepository: DreamRepository

    companion object {
        private const val TAG = "DreamTravelApp"
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        // 高德地图 SDK 隐私合规（必须在 super.onCreate() 之前，否则 Hilt 初始化 DI 图时 GeocodeSearch 会崩溃）
        ServiceSettings.updatePrivacyShow(this, true, true)
        ServiceSettings.updatePrivacyAgree(this, true)

        super.onCreate()

        // 全局崩溃捕获：写崩溃日志到文件，便于排查
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            writeCrashLog(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // 初始化 Firebase（如果设备没有 Google Play Services 则降级，不崩溃）
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // 降级：Google Play Services 不可用（常见于国内手机），仅使用本地 Room 功能
            writeCrashLog(e)
            return
        }

        // 匿名登录（静默，不阻塞 App 启动）
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseAuth.getInstance().signInAnonymously().await()
                // 登录成功后从云端拉取数据
                dreamRepository.syncFromCloud()
                Log.d(TAG, "Cloud sync completed after startup")
            } catch (e: Exception) {
                // 降级：允许只使用本地功能
                Log.w(TAG, "Anonymous sign-in failed, using local data only", e)
            }
        }
    }

    private fun writeCrashLog(throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val crashLog = "Time: ${System.currentTimeMillis()}\n${sw}\n---\n"

            // 写到内部存储（可通过 adb shell cat 查看）
            val logFile = File(filesDir, "crash.log")
            logFile.appendText(crashLog)

            // 也尝试写到下载目录（用户可直接访问）
            runCatching {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val publicLogFile = File(downloadDir, "dreamtravel_crash.log")
                publicLogFile.appendText(crashLog)
            }
        } catch (_: Exception) {
            // 静默，写日志失败了不影响崩溃处理
        }
    }
}
