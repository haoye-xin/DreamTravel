# DreamTravel 开发环境安装清单

> 生成时间: 2026-04-29 | 自动安装记录

## 已自动安装

| 软件 | 版本 | 用途 | 安装方式 |
|------|------|------|----------|
| Git for Windows | 2.47.1 | 版本控制 | 已预装 (E:\Git) |
| GitHub CLI | 2.92.0 | 远程仓库操作 | winget |
| Microsoft OpenJDK 17 | 17.0.18.8 | Java 编译 | winget (需确认) |

## 需手动安装

| 软件 | 最低版本 | 用途 | 下载链接 |
|------|----------|------|----------|
| JDK 17 | 17.0.x | Kotlin/Android 编译 | https://adoptium.net/download/ |
| Android Studio | Hedgehog 2023.1+ | IDE + Android SDK + 模拟器 | https://developer.android.com/studio |
| **或** Android SDK 命令行工具 | - | 最小开发环境(无 IDE) | https://developer.android.com/studio#command-line-tools |

## 需注册获取的 Key

| 服务 | 用途 | 获取地址 |
|------|------|----------|
| Firebase 项目 (google-services.json) | 后端/认证/推送 | https://console.firebase.google.com/ |
| 高德地图 API Key | 地图/定位/搜索 | https://console.amap.com/ |

## 安装后配置

1. 设置 `ANDROID_HOME` 环境变量指向 SDK 目录
2. 将 `google-services.json` 放入 `app/` 目录
3. 在 `app/build.gradle.kts` 中替换 `YOUR_AMAP_API_KEY`
4. 运行 `./gradlew assembleDebug` 验证构建

## 项目依赖库 (Gradle 自动下载)

- AndroidX Core, AppCompat, Material, ConstraintLayout
- Navigation Component 2.7.7
- Lifecycle ViewModel + LiveData 2.7.0
- Hilt DI 2.50
- Room 2.6.1
- Firebase BOM 32.7.2 (Auth, Firestore, Crashlytics)
- Kotlin Coroutines 1.7.3
- WorkManager 2.9.0
- Play Services Location 21.1.0
- 高德 3D 地图 10.0.600 + 搜索 9.7.1
- ViewPager2, SwipeRefreshLayout
- JUnit 4.13.2, MockK 1.13.9, Espresso 3.5.1
