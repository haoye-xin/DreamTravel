package com.dreamtravel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.dreamtravel.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import java.io.StringWriter
import java.io.PrintWriter

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "dream_travel_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            // 获取 Navigation 控制器
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            val navController = navHostFragment?.navController

            // 根据引导完成状态设置起始目标
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val onboardingDone = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

            if (onboardingDone && navController != null) {
                // 回访用户：跳过引导页，直接进入主页
                val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
                navGraph.setStartDestination(R.id.placeListFragment)
                navController.setGraph(navGraph, null)
            }
            // 首次用户：使用 nav_graph.xml 中默认的 startDestination（onboardingFragment）

            // 处理从通知点击进入的导航
            handleNotificationIntent(intent)
        } catch (e: Exception) {
            // 崩溃诊断：显示错误信息到屏幕上
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val crashMsg = "${e.javaClass.simpleName}: ${e.message}"
            Log.e("DreamTravel", "Startup crash", e)

            // 用极简 UI 显示崩溃信息（不依赖布局资源）
            try {
                val errorView = TextView(this).apply {
                    text = "启动崩溃:\n$crashMsg\n\n请截图发给开发者"
                    textSize = 14f
                    setPadding(40, 80, 40, 40)
                }
                setContentView(errorView)
                Toast.makeText(this, crashMsg, Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                // 连显示错误都失败了，最后尝试
                Toast.makeText(this, crashMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    /**
     * 从通知点击进入时，导航到待办列表页面
     */
    private fun handleNotificationIntent(intent: Intent?) {
        val placeId = intent?.getStringExtra(Constants.EXTRA_PLACE_ID) ?: return
        val placeName = intent.getStringExtra(Constants.EXTRA_PLACE_NAME) ?: return

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment ?: return
        val navController = navHostFragment.navController

        // 导航到待办列表：先确保在主页面，再跳转到待办列表
        val bundle = Bundle().apply {
            putString("placeId", placeId)
            putString("placeName", placeName)
        }
        navController.navigate(
            R.id.action_placeList_to_todoList,
            bundle
        )
    }
}
