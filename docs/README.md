# DreamTravel - 梦想提醒器

> 当你到达梦想的城市时，不再忘记曾经想做的事。

## 功能

- 🌆 设置梦想城市 + 在那里想做的事
- 📍 到达城市后停留一段时间（默认30分钟）自动提醒
- ✅ 对提醒做出反应：已完成 / 进行中（周期重提醒） / 路过（下次再提醒）
- 🗺️ 高德地图展示所有梦想之地
- 🔒 数据本地存储（Room）+ Firebase 云端备份
- 👤 匿名登录 / 邮箱账号（可选升级，跨设备同步）

## 快速开始

### 环境要求

- JDK 17+
- Android SDK 34
- Android 8.0+ 设备

### 配置

1. 在 [Firebase 控制台](https://console.firebase.google.com/) 创建项目，下载 `google-services.json` 到 `app/` 目录
2. 在 [高德开放平台](https://console.amap.com/) 获取 API Key
3. 在 `app/build.gradle.kts` 替换 API Key：
   ```kotlin
   manifestPlaceholders["AMAP_API_KEY"] = "你的高德APIKey"
   ```

### 构建

```bash
./gradlew assembleDebug
```

### 安装

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 使用流程

1. **首次引导**：分阶段授权——先允许前台定位，再开启后台定位（建议都选「始终允许」）
2. **添加梦想**：搜索城市（真实高德地理编码）→ 设定驻留时间 → 保存
3. **添加待办**：在梦想详情页添加想做的事
4. **到达城市**：App 在后台检测位置，停留超时后发送通知（支持单条 Todo 粒度操作）
5. **响应提醒**：通知栏直接操作单条待办——已完成 / 进行中 / 路过
6. **账号升级**（可选）：设置页 → 邮箱注册，数据跨设备同步

## 技术架构

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin |
| 架构 | MVVM + Repository |
| DI | Hilt |
| 本地存储 | Room |
| 云端 | Firebase Firestore + Auth |
| 地图 | 高德地图 SDK（3D + 搜索） |
| 账号 | Firebase Auth（匿名 + 邮箱） |
| 状态监测 | StatusManager（网络/定位/权限/同步） |
| 后台 | ForegroundService + WorkManager |

## 项目结构

```
app/src/main/java/com/dreamtravel/
├── di/              # 依赖注入（Hilt）
├── data/
│   ├── local/       # Room 数据库（DAO + Entity）
│   ├── remote/      # Firestore 同步 + AMap 搜索 + 账号管理
│   ├── repository/  # 数据仓库
│   └── model/       # 领域模型
├── domain/          # 业务逻辑（驻留检测/城市匹配/提醒调度）
├── service/         # 后台服务（定位/Geofence/开机恢复）
├── notification/    # 通知系统（提醒/Action/周期重提醒）
├── ui/
│   ├── onboarding/  # 引导页（分阶段授权）
│   ├── places/      # 梦想城市列表 + 添加
│   ├── todos/       # 待办事项
│   ├── map/         # 交互式地图
│   └── settings/    # 设置（账号/数据/隐私）
└── util/            # 工具类（权限/定位/状态管理/常量）
```

## 许可证

MIT