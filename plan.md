# DreamTravel 实施计划书

> **版本**: v2.0（ULW 就绪）  
> **平台**: Android 原生 Kotlin  
> **MVP 范围**: 核心提醒链路（创建梦想 → 城市检测 → 通知 → 响应）

---

## 1. 产品概述

**DreamTravel** 是一个基于城市位置的「梦想提醒器」。

**核心流程**：
```
用户创建梦想城市 → 添加待办事项 → 到达该城市 → 停留 N 分钟 →
收到通知 → 响应：✅已完成 / 🔄进行中 / 📍路过
```

**MVP 范围：**

| 包含 | 不包含 |
|------|--------|
| 创建梦想城市 + 多个待办 | 旅行日记 / 照片 |
| 城市级到达检测（高德逆地理编码） | 社交分享 |
| 系统通知栏提醒 + 三种响应 | 多设备同步 |
| 高德地图选点 | 数据分析面板 |
| 离线可用（Room 本地存储） | 语音/视频 |
| Firebase 匿名登录 + 云端备份 | 正式账户系统 |
| 周期性重提醒（进行中状态） | 智能推荐 |

---

## 2. 技术架构

### 2.1 技术栈

| 层级 | 选型 | 理由 |
|------|------|------|
| 语言 | Kotlin | Android 官方，协程异步 |
| 架构 | MVVM + Repository | 标准分层 |
| DI | Hilt | 编译期注入 |
| 本地存储 | Room | 编译期 SQL 校验 |
| 云端存储 | Firestore | 免费额度足 |
| 认证 | Firebase Anonymous Auth | 零摩擦 |
| 地图 | 高德地图 SDK | 国内精准 |
| 异步 | Kotlin Coroutines + Flow | 响应式 |
| 后台 | ForegroundService + WorkManager | 兼容 Doze |

### 2.2 城市检测机制（核心架构决策）

Android GeofencingClient 仅支持圆形围栏。采用**混合策略**：

```
1. 高德获取城市中心坐标
2. 设置 20km 半径「唤醒围栏」
3. 进入围栏 → LocationService 启动驻留检查协程
4. 每 2 分钟逆地理编码确认城市名匹配
5. 匹配成功 → 累加驻留计时器
6. 驻留时间 ≥ 阈值 → 触发通知
7. 离开围栏 → 清理计时器
```

> **关键修正（v2.0）**：原计划使用 WorkManager PeriodicWorkRequest 做 2 分钟周期检查。但 WorkManager 最小间隔是 15 分钟。修正为在 `LocationService`（ForegroundService）内部用协程 `delay(2.minutes)` 循环实现。ReminderAlarmWorker（24h 重提醒）不受影响。

**参数：**
- 唤醒围栏半径：20km
- 驻留检查间隔：每 2 分钟（协程循环）
- GPS 精度要求：≤ 100m
- 计时器容差：离开 ≤ 5 分钟返回 → 继续计时

### 2.3 数据同步

- **本地优先**：Room 做主数据源
- **Firestore 备份**：last-write-wins 策略
- **离线队列**：变更暂存 Room，上线后批量同步

### 2.4 Repository 抽象（未来迁移自租服务器）

```kotlin
interface DreamRepository {
    suspend fun getPlaces(): Flow<List<Place>>
    suspend fun addPlace(place: Place)
    suspend fun updatePlace(place: Place)
    suspend fun getTodos(placeId: String): Flow<List<Todo>>
    suspend fun addTodo(todo: Todo)
    suspend fun updateTodoStatus(todoId: String, status: TodoStatus)
}
// 实现: FirebaseDreamRepository (当前) → SelfHostedDreamRepository (未来)
```

---

## 3. 数据模型

```kotlin
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,           // "大理"
    val cityCode: String? = null,
    val latitude: Double,
    val longitude: Double,
    val dwellMinutes: Int = 30,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val placeId: String,
    val title: String,
    val notes: String = "",
    val status: TodoStatus = TodoStatus.PENDING,
    val remindIntervalMinutes: Int = 1440,
    val remindCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

enum class TodoStatus { PENDING, IN_PROGRESS, COMPLETED, SKIPPED }

@Entity(tableName = "dwell_events")
data class DwellEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val placeId: String,
    val enteredAt: Long,
    val exitedAt: Long? = null,
    val triggeredAt: Long? = null,
    val status: DwellStatus = DwellStatus.ENTERED
)

enum class DwellStatus { ENTERED, DWELLING, TRIGGERED, EXITED }
```

**Firestore 镜像**（增加 userId 字段）：
```
/places/{placeId} → { userId, name, cityCode, lat, lng, dwellMinutes, isActive, createdAt }
/todos/{todoId}   → { userId, placeId, title, notes, status, remindIntervalMinutes, createdAt }
/dwellEvents/{id} → { userId, placeId, enteredAt, exitedAt, triggeredAt, status }
```

---

## 4. 项目结构

```
app/src/main/java/com/dreamtravel/
├── DreamTravelApp.kt
├── MainActivity.kt
├── di/
│   ├── DatabaseModule.kt
│   ├── RepositoryModule.kt
│   └── LocationModule.kt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/PlaceDao.kt
│   │   ├── dao/TodoDao.kt
│   │   └── dao/DwellEventDao.kt
│   ├── remote/
│   │   └── FirestoreSyncService.kt
│   ├── repository/
│   │   ├── DreamRepository.kt
│   │   └── DreamRepositoryImpl.kt
│   └── model/
│       ├── Place.kt
│       ├── Todo.kt
│       └── DwellEvent.kt
├── domain/
│   ├── CityDetectionUseCase.kt
│   ├── DwellTimerUseCase.kt
│   └── ReminderSchedulerUseCase.kt
├── service/
│   ├── LocationService.kt          # 前台 Service + 驻留检查协程
│   ├── GeofenceWakeReceiver.kt     # 唤醒围栏广播
│   └── ReminderAlarmWorker.kt      # 重提醒 Worker
├── notification/
│   ├── NotificationHelper.kt
│   └── NotificationActionReceiver.kt
├── ui/
│   ├── places/   (PlaceList, AddPlace Fragments + ViewModels)
│   ├── todos/    (TodoList, AddTodo Fragments + ViewModels)
│   ├── map/      (MapFragment + MapViewModel)
│   └── onboarding/ (OnboardingFragment)
└── util/
    ├── LocationUtils.kt
    ├── PermissionUtils.kt
    └── Constants.kt
```

---

## 5. 实施任务（8 个 Wave）

### Wave 0: 项目脚手架
> 目标：可编译运行的空项目

- **T0.1** 创建 Android 项目：Kotlin, minSdk=26, targetSdk=34
  - 依赖：Hilt, Room, Firestore, Firebase Auth, Crashlytics, Coroutines, WorkManager, 高德地图 SDK, Navigation, ViewBinding
  - 配置 KSP、ProGuard/R8、google-services.json、高德 API Key
  - QA: `./gradlew assembleDebug` 成功

- **T0.2** Hilt DI：DreamTravelApp + DatabaseModule + RepositoryModule + LocationModule
  - QA: App 启动不崩溃

- **T0.3** Room 数据库：AppDatabase + DAOs（Place, Todo, DwellEvent）
  - QA: DAO 编译通过

- **T0.4** App 图标与基础样式：launcher icon + 主题色 + strings.xml
  - QA: 安装后桌面显示图标

### Wave 1: 数据层
> 目标：Room + Firestore 双写链路

- **T1.1** 数据模型定义：Place, Todo, DwellEvent domain model + Entity 映射
  - QA: 编译通过

- **T1.2** Repository 接口 + 实现（Room 优先写 + Firestore 异步同步）
  - QA: 单元测试 mock Room/Firestore 通过

- **T1.3** FirestoreSyncService：拉取合并 + 推送离线变更 + ConnectivityManager 监听
  - QA: 两台设备添加同一条 → 同步成功

- **T1.4** Firebase 匿名认证：启动时 signInAnonymously + 降级策略
  - QA: 首次启动自动获得 UID

### Wave 2: 定位引擎 ⭐核心
> 目标：城市检测 + 驻留计时器

- **T2.1** 权限管理：ACCESS_FINE_LOCATION + Android 10+/9- 分支处理 + 降级方案
  - QA: 权限请求/拒绝/跳转设置 流程正确

- **T2.2** 城市搜索集成：高德搜索 API → 返回城市中心坐标 + cityCode
  - QA: 搜索"大理"→ 返回 (25.6°N, 100.2°E)

- **T2.3** 唤醒围栏：GeofenceWakeReceiver + 20km 半径注册/注销 + Place 激活时初始检测
  - QA: mock GPS 进入 → 启动检测；在家添加本地城市 → 立即检测

- **T2.4** LocationService（前台 Service）：管理 Geofence 生命周期 + **协程循环**驻留检查
  - 每次循环：获取位置 → 逆地理编码 → 匹配城市名 → 累加计时 → 超阈值触发通知
  - 5 分钟容差 + 精度门控 + 不同响应状态的 Worker 控制
  - QA: 城内 30 分钟 → 通知触发；离开 → 计时重置

### Wave 3: 通知系统
> 目标：通知构建 + 用户交互

- **T3.1** 通知渠道（dream_reminder，HIGH 优先级）+ BigTextStyle 展示
  - QA: 通知正确展示，点击跳转 Todo 列表

- **T3.2** 3 个 Action 按钮：✅已完成 / 🔄进行中 / 📍路过 + BroadcastReceiver
  - QA: 点击已完成 → 状态变更 + 通知消失

- **T3.3** ReminderAlarmWorker：周期性重提醒（WorkManager, ≥30min 间隔）+ 退出城市时取消
  - QA: 标记进行中 → 24h 后重提醒；离开 → 停止

### Wave 4: UI 层
> 目标：完整 MVVM UI

- **T4.1** 导航架构：Navigation Component + nav_graph.xml
- **T4.2** 首页（PlaceList）：RecyclerView + 进度 + 激活开关 + 空状态
- **T4.3** 添加地点（AddPlace）：高德搜索 + NumberPicker 驻留时间
- **T4.4** 地点详情（TodoList）：状态分组 + 左滑操作
- **T4.5** 添加 Todo（BottomSheetDialog）
- **T4.6** 地图页面（MapFragment）：高德 MapView + Marker + 长按添加

### Wave 5: 引导与隐私
- **T5.1** 引导页（3 屏 ViewPager2）+ SharedPreferences 仅首次显示
- **T5.2** 权限请求流程：前台 → 后台 → 降级提示 → 跳转设置
- **T5.3** 隐私声明入口（WebView）+ 数据使用说明

### Wave 6: 边界情况
- **T6.1** 离线容错：Room 做主数据源 + 指数退避重试
- **T6.2** 城市名匹配容错：「市/州」后缀标准化 + cityCode 精确匹配
- **T6.3** 多城市重叠处理：独立计时 + 独立通知
- **T6.4** GPS 异常：超 5 分钟不可用暂停 + 网络定位兜底 + 精度门控
- **T6.5** 电池优化豁免引导
- **T6.6** App 杀进程恢复：BOOT_COMPLETED + 启动时检查恢复

### Wave 7: 测试
- **T7.1** 单元测试：DwellTimerUseCase + CityDetectionUseCase + Repository
- **T7.2** 插桩测试：通知触发 + Action 响应 + Room 迁移 + 权限流程 + 全链路集成
- **T7.3** 手动 QA：6 个场景（含「在家添加本地城市」边界）

### Wave 8: 收尾
- **T8.1** README.md 使用说明
- **T8.2** 最终验证：真机全流程测试

---

## 6. 验收标准

| ID | Given | When | Then |
|----|-------|------|------|
| AC-1 | GPS 在梦想城市内 | 停留 ≥ dwellMinutes | 通知触发，标题含城市名 |
| AC-2 | 停留 15 分钟后离开 | 离开 > 5 分钟 | 计时重置为 0 |
| AC-3 | 停留 15 分钟，离开 3 分钟 | 返回 | 计时从 15 分钟继续 |
| AC-4 | 通知展示 | 点击「已完成」 | Todo → COMPLETED，通知消失 |
| AC-5 | 通知展示 | 点击「进行中」 | remindInterval 后重提醒 |
| AC-6 | 点击「路过」 | 离开后再次进入 | 新通知出现 |
| AC-7 | 飞行模式 | 添加 Place + Todo | 保存成功，联网后同步 |
| AC-8 | 逆地理返回"大理白族自治州" | 梦想为"大理" | 匹配成功 |
| AC-9 | 设"大理"和"丽江" | 到大理 | 仅大理通知触发 |
| AC-10 | 拒绝后台定位 | 关闭定位 | 不崩溃，引导说明 |

---

## 7. 风险清单

| 风险 | 概率 | 缓解 |
|------|------|------|
| 高德 API 限流 | 中 | 本地缓存 cityCode + 上次位置 |
| 厂商杀后台 | 高 | 白名单引导 + ForegroundService |
| 电池消耗 | 中 | 精度门控 + 无活跃梦想时停 Service |
| GPS 漂移误判 | 中 | 5 分钟容差 + 精度门控 + 逆地理编码二次确认 |
| Firebase 成本超预期 | 低 | Spark 免费额度监控 |
| 城市名歧义匹配失败 | 中 | cityCode 精确匹配 |

---

## 8. 前置依赖

- [ ] Firebase 项目创建（获取 google-services.json）
- [ ] 高德开放平台注册（获取 API Key）
- [ ] Android 签名证书（debug 即可用于 MVP）
- [ ] Android 10+ 真机（模拟器 GPS mock 不可靠）

---

## 9. 最终验证清单

- [ ] 全流程：安装 → 引导 → 创建梦想 → 到达 → 通知 → 响应
- [ ] 离线：飞行模式下完整使用 → 联网 → 数据不丢失
- [ ] 重提醒：标记进行中 → 等待 → 再次收到
- [ ] 电池：使用一天，消耗正常
- [ ] 杀进程恢复：强停 → 重启 → 数据完好
- [ ] 多城市：设 2 个 → 只触发当前城市
- [ ] **关键边界**：在家添加本地城市 → 保存后立即检测 → 30 分钟后通知

---

*计划版本 v2.0 | ULW 就绪 | 2026-04-29*
