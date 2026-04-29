# DreamTravel - 基于位置的梦想提醒器 · 实施计划

> **计划版本**: v1.1（已通过高精度审查）  
> **日期**: 2026-04-29  
> **审查**: Metis（预审）+ 深度自查（5 个关键缺口已修复）  
> **平台**: Android (Kotlin)  
> **MVP 范围**: 核心提醒链路

---

## 1. 概述与目标

### 1.1 产品目标
当一个「梦想提醒器」——用户预设想去的地方（城市）和在那里想做的事，当手机检测到用户到达该城市并停留超过设定时间后，系统发送通知提醒。

### 1.2 核心用户流程
```
用户创建梦想地点（城市）→ 添加待办事项 → 到达城市 → 停留N分钟 → 
收到通知 → 响应（已完成 / 进行中 / 路过）
```

### 1.3 MVP 范围
| IN | OUT |
|----|-----|
| 创建梦想城市 + 待办事项 | 旅行日记 / 照片记录 |
| 城市级到达检测（逆地理编码） | 社交分享 |
| 系统通知栏提醒 + 三种响应 | 多设备同步 |
| 地图界面（高德）选点 | 数据分析面板 |
| 离线可用（Room 本地存储） | 语音/视频功能 |
| Firebase 匿名登录 + 云端备份 | 正式账户系统 |
| 周期性重提醒（进行中状态） | 智能推荐 |

---

## 2. 架构决策

### 2.1 技术栈
| 层级 | 选型 | 理由 |
|------|------|------|
| 语言 | Kotlin | Android 官方首选，协程天然支持异步 |
| 架构 | MVVM + Repository | 成熟模式，清晰分层 |
| DI | Hilt | Android 官方推荐，编译期注入 |
| 本地存储 | Room | AndroidX ORM，编译期 SQL 校验 |
| 云端存储 | Firestore | 免费额度够用，实时同步 |
| 认证 | Firebase Anonymous Auth | 对用户透明，零摩擦 |
| 地图 | 高德地图 SDK | 国内定位精准，免费额度大 |
| 异步 | Kotlin Coroutines + Flow | 响应式数据流 |
| 后台任务 | WorkManager | 兼容 Doze 模式 |

### 2.2 城市检测机制（关键架构决策）
Android GeofencingClient 仅支持圆形围栏，无法直接匹配不规则城市边界。采用**混合策略**：

```
┌─────────────────────────────────────────────┐
│           城市检测混合策略                      │
│                                               │
│  1. 高德获取城市中心坐标                        │
│  2. 设置 20km 半径「唤醒围栏」                   │
│  3. 进入唤醒围栏 → 启动逆地理编码周期检查         │
│  4. 逆地理编码确认城市名匹配 → 启动驻留计时器      │
│  5. 每次位置更新确认仍在同城 → 计时器继续         │
│  6. 驻留时间 ≥ 设定阈值 → 触发通知              │
│  7. 离开唤醒围栏 → 清理计时器                    │
└─────────────────────────────────────────────┘
```

**参数设定：**
- 唤醒围栏半径：20km（平衡检测范围和电池）
- 逆地理编码周期：每 2 分钟检查一次
- GPS 精度要求：≤ 100m
- 驻留计时器容差：离开城市 ≤ 5 分钟后回来继续计时（防止 GPS 漂移重置）

### 2.3 数据同步策略
- **本地优先**：Room 为主数据源，UI 只读本地数据
- **Firestore 为备份**：网络可用时自动同步，采用 last-write-wins 策略
- **离线队列**：离线变更暂存 Room，上线后批量同步

### 2.4 Repository 抽象层（未来迁移预留）
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

### 3.1 Room Entity
```kotlin
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,                    // 城市名，如 "大理"
    val cityCode: String? = null,        // 高德城市编码
    val latitude: Double,                // 城市中心纬度
    val longitude: Double,               // 城市中心经度
    val dwellMinutes: Int = 30,          // 驻留时间（分钟）
    val isActive: Boolean = true,        // 是否启用
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "todos", foreignKeys = [...])
data class TodoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val placeId: String,                 // 关联地点
    val title: String,                   // 待办标题
    val notes: String = "",              // 备注
    val status: TodoStatus = TodoStatus.PENDING,
    val remindIntervalMinutes: Int = 1440, // 进行中状态的重提醒间隔（默认1天）
    val remindCount: Int = 0,            // 已提醒次数（用于通知展示"已提醒 N 次"）
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

enum class TodoStatus { PENDING, IN_PROGRESS, COMPLETED, SKIPPED }

// 驻留事件记录
@Entity(tableName = "dwell_events")
data class DwellEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val placeId: String,                 // 关联地点
    val enteredAt: Long,                 // 进入城市的时间戳
    val exitedAt: Long? = null,          // 离开城市的时间戳
    val triggeredAt: Long? = null,       // 通知触发的时间戳
    val status: DwellStatus = DwellStatus.ENTERED
)

enum class DwellStatus { ENTERED, DWELLING, TRIGGERED, EXITED }
```

### 3.2 Firestore 文档结构（镜像 Room，增加 userId）
```
/places/{placeId}  →  { userId, name, cityCode, lat, lng, dwellMinutes, isActive, createdAt }
/todos/{todoId}    →  { userId, placeId, title, notes, status, remindIntervalMinutes, createdAt }
/dwellEvents/{id}  →  { userId, placeId, enteredAt, exitedAt, triggeredAt, status }
```

---

## 4. 项目结构

```
app/src/main/java/com/dreamtravel/
├── DreamTravelApp.kt              # Application + Hilt 入口
├── MainActivity.kt                # 单 Activity
├── di/                            # Hilt 模块
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
│   │   ├── DreamRepository.kt          # 接口
│   │   └── DreamRepositoryImpl.kt      # Room + Firestore 实现
│   └── model/
│       ├── Place.kt
│       ├── Todo.kt
│       └── DwellEvent.kt
├── domain/
│   ├── CityDetectionUseCase.kt         # 城市检测核心逻辑
│   ├── DwellTimerUseCase.kt            # 驻留计时器
│   └── ReminderSchedulerUseCase.kt     # 提醒调度
├── service/
│   ├── LocationService.kt              # 前台 Service（持续定位）
│   ├── GeofenceWakeReceiver.kt         # 唤醒围栏广播接收器
│   ├── DwellCheckWorker.kt             # WorkManager 周期检查
│   └── ReminderAlarmWorker.kt          # 周期性重提醒 Worker
├── notification/
│   ├── NotificationHelper.kt           # 通知构建与发送
│   └── NotificationActionReceiver.kt   # 通知按钮点击处理
├── ui/
│   ├── places/
│   │   ├── PlaceListFragment.kt
│   │   ├── PlaceListViewModel.kt
│   │   ├── AddPlaceFragment.kt
│   │   └── AddPlaceViewModel.kt
│   ├── todos/
│   │   ├── TodoListFragment.kt
│   │   ├── TodoListViewModel.kt
│   │   ├── AddTodoFragment.kt
│   │   └── AddTodoViewModel.kt
│   ├── map/
│   │   ├── MapFragment.kt
│   │   └── MapViewModel.kt
│   └── onboarding/
│       └── OnboardingFragment.kt
└── util/
    ├── LocationUtils.kt
    ├── PermissionUtils.kt
    └── Constants.kt
```

---

## 5. 实施任务（分波次）

### Wave 0: 项目脚手架

> **目标**: 可编译运行的空白项目，依赖全部就绪

**T0.1 - 创建 Android 项目**
- [ ] 创建 Kotlin 项目，minSdk=26, targetSdk=34, compileSdk=34
- [ ] 配置 Gradle (Kotlin DSL)，添加所有依赖
- [ ] 依赖清单：Hilt, Room, Firestore, Firebase Auth, Firebase Crashlytics, Coroutines, WorkManager, 高德地图 SDK, Navigation Component, ViewBinding
- [ ] 配置 KSP 插件（Room + Hilt 注解处理器必需）
- [ ] 配置 ProGuard/R8 规则（保留高德 SDK、Firebase 等关键类）
- [ ] 配置 google-services.json (Firebase 项目)
- [ ] 配置高德地图 API Key (AndroidManifest.xml)
- [ ] 配置 Firebase Crashlytics（基础崩溃上报）
- [ ] 验证：`./gradlew assembleDebug` 成功

**T0.2 - Hilt DI 基础设施**
- [ ] 创建 `DreamTravelApp.kt`（@HiltAndroidApp）
- [ ] 创建 `di/DatabaseModule.kt`（Room 数据库实例）
- [ ] 创建 `di/RepositoryModule.kt`（Repository 绑定）
- [ ] 创建 `di/LocationModule.kt`（FusedLocationProviderClient 提供）
- [ ] 验证：App 启动不崩溃，Hilt 注入正常

**T0.3 - Room 数据库初始化**
- [ ] 创建 `data/local/AppDatabase.kt`（@Database，entities=[PlaceEntity, TodoEntity, DwellEventEntity]）
- [ ] 创建 `data/local/dao/PlaceDao.kt`（CRUD + 按激活状态查询）
- [ ] 创建 `data/local/dao/TodoDao.kt`（CRUD + 按地点/状态查询）
- [ ] 创建 `data/local/dao/DwellEventDao.kt`（插入事件 + 查询最近事件）
- [ ] 创建 Room Migration 策略（fallbackToDestructiveMigration 用于 MVP）
- [ ] 验证：Database 实例创建成功，DAO 编译通过

---

### Wave 1: 数据层

> **目标**: 完整的数据读写链路，本地 Room + 远程 Firestore 双写

**T1.1 - 数据模型定义**
- [ ] 在 `data/model/Place.kt` 定义 Place domain model（与 Room Entity 解耦）
- [ ] 在 `data/model/Todo.kt` 定义 Todo domain model
- [ ] 在 `data/model/DwellEvent.kt` 定义 DwellEvent domain model
- [ ] 创建 Entity ↔ Domain Model 映射扩展函数
- [ ] 验证：模型编译通过

**T1.2 - Repository 接口与实现**
- [ ] 创建 `data/repository/DreamRepository.kt` 接口（参见 §2.4）
- [ ] 创建 `data/repository/DreamRepositoryImpl.kt`：
  - [ ] `getPlaces()` → Room Flow 监听
  - [ ] `addPlace()` → 先写 Room，异步写 Firestore
  - [ ] `updatePlace()` → 同上
  - [ ] `getTodos(placeId)` → Room Flow 监听
  - [ ] `addTodo()` → 先写 Room，异步写 Firestore
  - [ ] `updateTodoStatus()` → 更新状态+时间戳，同步 Firestore
- [ ] 实现离线队列：写入失败时暂存，网络恢复后重试
- [ ] 验证：Repository 单元测试通过（mock Room + mock Firestore）

**T1.3 - Firestore 同步服务**
- [ ] 创建 `data/remote/FirestoreSyncService.kt`
- [ ] 实现 `syncPlaces()`：拉取远程 Places → 合并到 Room（last-write-wins）
- [ ] 实现 `syncTodos()`：拉取远程 Todos → 合并到 Room
- [ ] 实现 `pushPendingChanges()`：Room 中未同步的变更推送到 Firestore
- [ ] 监听网络状态（ConnectivityManager），自动触发同步
- [ ] 验证：两台设备添加同一 dream → 同步成功

**T1.4 - Firebase 匿名认证**
- [ ] 在 `DreamTravelApp.kt` 初始化 Firebase
- [ ] 应用启动时自动执行 `signInAnonymously()`
- [ ] Token 刷新处理
- [ ] 认证失败时的降级策略（允许只使用本地功能）
- [ ] 验证：首次启动自动获得匿名 UID，无需用户交互

---

### Wave 2: 定位引擎

> **目标**: 核心的城市检测 + 驻留计时器逻辑

**T2.1 - 位置权限管理**
- [ ] 创建 `util/PermissionUtils.kt`：检查/请求 `ACCESS_FINE_LOCATION` + `ACCESS_BACKGROUND_LOCATION`
- [ ] **Android 版本适配**：
  - Android 10+（API 29+）：后台定位需单独请求 `ACCESS_BACKGROUND_LOCATION`
  - Android 9-（API 28-）：后台定位随前台权限一起授予，无需额外请求
  - 代码中使用 `@RequiresApi` 和 `Build.VERSION.SDK_INT` 分支处理
- [ ] 实现权限被拒绝后的引导（跳转系统设置页）
- [ ] 实现前台 Service 权限声明（FOREGROUND_SERVICE_LOCATION，Android 10+）
- [ ] 实现「仅在使用时允许」降级方案（后台无法检测，前台弹提示）
- [ ] 验证：权限请求流程正确，拒绝后有引导

**T2.2 - 城市中心坐标获取**
- [ ] 集成高德地图 SDK 搜索 API
- [ ] 实现 `CitySearchUseCase`：用户输入城市名 → 返回城市中心经纬度 + cityCode
- [ ] 搜索结果缓存（Room 或内存）
- [ ] 处理城市名歧义（如「大理」返回云南省大理市，而非其他同名地）
- [ ] 验证：搜索"大理"→ 返回 (25.6°N, 100.2°E) 附近坐标

**T2.3 - 唤醒围栏服务**
- [ ] 创建 `service/GeofenceWakeReceiver.kt`（BroadcastReceiver）
- [ ] 对每个活跃的 dream Place，以城市中心为圆心、20km 为半径注册 Geofence
- [ ] 实现 Geofence 生命周期管理：Place 激活时注册，停用时注销
- [ ] 处理最多 100 个 geofence 的 API 限制（超出时分批或提示用户）
- [ ] Geofence ENTER 事件 → 启动 DwellCheckWorker
- [ ] Geofence EXIT 事件 → 取消 DwellCheckWorker
- [ ] **关键边界情况**：Place 激活时用户可能已在围栏内（如在家添加"北京"的梦想）：
  - 注册 Geofence 后立即用 `GeofencingClient` 的 `getCurrentLocation` 做一次初始检测
  - 若当前位置在城市唤醒圈内 → 直接启动 DwellCheckWorker
  - 避免「人在城里但收不到提醒」的漏洞
- [ ] 验证：mock GPS 进入城市唤醒圈 → DwellCheckWorker 启动；在家添加本地城市梦想 → 同样启动

**T2.4 - 驻留检查 Worker**
- [ ] 创建 `service/DwellCheckWorker.kt`（PeriodicWorkRequest，2分钟周期）
- [ ] 每次执行：
  1. 获取当前位置（FusedLocationProviderClient）
  2. 逆地理编码（高德 ReGeocode API）→ 获取城市名
  3. 比对当前城市名与目标城市名（忽略「市/州」后缀差异）
  4. 如果在目标城市 → 累加驻留计数器
  5. 如果不在目标城市 → 重置驻留计数器（容差 5 分钟短时离开）
  6. 驻留时间 ≥ dwellMinutes → 触发通知
- [ ] **Worker 停止策略**（关键：防止通知后继续计时而误触发）：
  - 通知触发后 → 停止**本周期** Worker
  - 用户选择「已完成」或「路过」→ 永久停止该城市的 DwellCheckWorker
  - 用户选择「进行中」→ 重启 Worker（用于监测用户是否仍在城内以便重提醒）
  - 用户离开唤醒圈（EXIT 事件）→ 取消 DwellCheckWorker + 取消重提醒
- [ ] GPS 不可用时 fallback 到网络定位（NETWORK_PROVIDER）
- [ ] GPS 精度 > 100m 时跳过本次检查
- [ ] 离开 ≤ 5 分钟又回到同城 → 继续计时（不重置）
- [ ] 验证：模拟在城内 30 分钟 → 通知触发；中途离开 → 计时重置

**T2.5 - 前台定位 Service**
- [ ] 创建 `service/LocationService.kt`（Foreground Service）
- [ ] 显示持久通知：「DreamTravel 正在检测你的位置」
- [ ] 协调 Geofence + DwellCheckWorker 的生命周期
- [ ] 电池优化：当所有 Places 停用或用户远离所有唤醒圈时，停止 Service
- [ ] 设备重启后自动恢复（BOOT_COMPLETED 广播）
- [ ] 验证：Service 在后台持续运行，不被系统杀死

---

### Wave 3: 通知系统

> **目标**: 通知的构建、发送与用户交互

**T3.1 - 通知渠道与构建**
- [ ] 创建 `notification/NotificationHelper.kt`
- [ ] 创建通知渠道 `dream_reminder`（优先级 HIGH，允许气泡）
- [ ] 构建驻留触发通知：
  - 标题：「你在 {城市名}！」
  - 内容：列出该城市所有未完成的 Todo（最多 3 行，超出折叠为「还有 N 项...」）
  - 大视图（BigTextStyle）：展开显示所有 Todo
- [ ] 通知 ID 规则：`placeId.hashCode()` 保证同一城市只保留一个通知
- [ ] 验证：通知展示正确，点击跳转 Todo 列表页

**T3.2 - 通知操作按钮**
- [ ] 在通知上添加 3 个 Action Button：
  - 「✅ 已完成」→ 标记所有 Todo 为 COMPLETED
  - 「🔄 进行中」→ 标记所有 Todo 为 IN_PROGRESS → 启动周期性重提醒
  - 「📍 路过」→ 标记为 SKIPPED → 清理通知 → 下次进城重新提醒
- [ ] 创建 `notification/NotificationActionReceiver.kt`（BroadcastReceiver）
- [ ] 处理用户点击 Action 后的状态更新 + 通知更新
- [ ] 验证：点击「已完成」→ 所有 Todo 状态变更 + 通知消失

**T3.3 - 周期性重提醒**
- [ ] 创建 `service/ReminderAlarmWorker.kt`（OneTimeWorkRequest，可被后续链式调度）
- [ ] 当用户选择「进行中」后：
  - 启动 `ReminderAlarmWorker`（OneTimeWorkRequest，延迟 remindIntervalMinutes）
  - Worker 触发时重新发送通知（内容同首次，使用 `remindCount` 字段显示「已提醒 N 次」）
  - 递增 Todo 的 `remindCount`
  - 重提醒持续直到用户标记「已完成」或「路过」
- [ ] **退出城市中断重提醒**：当用户离开唤醒圈（EXIT 事件）时，取消未执行的重提醒 Worker
- [ ] 设置重提醒频率上限（最少 30 分钟间隔，防止骚扰）
- [ ] 验证：标记进行中 → 每 24 小时收到重提醒（默认设置）；离开城市 → 重提醒停止

---

### Wave 4: UI 层 - 核心页面

> **目标**: 完整的 MVVM UI 流程

**T4.1 - 导航架构**
- [ ] 配置 Navigation Component（单 Activity + NavHostFragment）
- [ ] 定义导航图（nav_graph.xml）：
  - `placeList` → `addPlace` → `placeDetail(todoList)` → `addTodo`
  - `placeList` → `map`
- [ ] 统一 Toolbar 样式
- [ ] 验证：页面间导航流畅，返回栈正确

**T4.2 - 地点列表页（首页）**
- [ ] 创建 `ui/places/PlaceListFragment.kt` + `PlaceListViewModel.kt`
- [ ] ViewModel 通过 `DreamRepository.getPlaces()` 获取 Flow<List<Place>>
- [ ] RecyclerView 展示所有梦想城市，每项显示：
  - 城市名、驻留时间、待办完成进度（如 "2/5 已完成"）
  - 激活/停用开关
  - 滑动删除（确认对话框）
- [ ] FAB 按钮 → 导航到添加地点页
- [ ] 空状态提示：「还没有梦想之地，点击 + 添加第一个吧 ✨」
- [ ] 验证：列表正确展示、开关切换生效、滑动删除成功

**T4.3 - 添加/编辑地点页**
- [ ] 创建 `ui/places/AddPlaceFragment.kt` + `AddPlaceViewModel.kt`
- [ ] 城市搜索框（高德搜索 API，带下拉建议列表）
- [ ] 选择城市后自动填充名称 + 坐标 + cityCode
- [ ] 驻留时间选择器（NumberPicker：15/30/45/60/90/120 分钟 + 自定义）
- [ ] 保存按钮 → 写入 Repository → 自动注册 Geofence → 返回列表
- [ ] 编辑模式：复用同一页面，预填已有数据
- [ ] 验证：搜索"大理"→ 选择 → 设 30 分钟 → 保存 → 列表出现新条目

**T4.4 - 地点详情页（Todo 列表）**
- [ ] 创建 `ui/todos/TodoListFragment.kt` + `TodoListViewModel.kt`
- [ ] 展示该地点下所有 Todo，按状态分组（未完成 / 已完成 / 已路过）
- [ ] 每项 Todo 显示标题、状态标签、添加时间
- [ ] 支持左滑标记完成/路过
- [ ] FAB → 添加新 Todo
- [ ] 验证：Todo 状态正确分组显示

**T4.5 - 添加 Todo 对话框**
- [ ] 创建 `ui/todos/AddTodoFragment.kt`（BottomSheetDialogFragment）
- [ ] 输入框：Todo 标题（必填）
- [ ] 输入框：备注（可选）
- [ ] 重提醒间隔选择（默认跟随 Place 设定）
- [ ] 保存后自动关联当前 Place
- [ ] 验证：添加 Todo 后刷新列表

**T4.6 - 地图页面**
- [ ] 创建 `ui/map/MapFragment.kt` + `MapViewModel.kt`
- [ ] 集成高德地图 MapView
- [ ] 在地图上标记所有梦想城市（Marker），点击显示城市名
- [ ] 定位按钮：移动到当前 GPS 位置
- [ ] 长按地图添加地点（弹出城市搜索确认）
- [ ] 验证：地图正常加载，Marker 显示，定位可用

---

### Wave 5: 首次引导与隐私

> **目标**: 用户首次打开 App 的引导流程

**T5.1 - 引导页**
- [ ] 创建 `ui/onboarding/OnboardingFragment.kt`
- [ ] 3 屏引导（ViewPager2）：
  - 屏1：欢迎 → 「记录梦想之地，不再错过想做的事」
  - 屏2：定位说明 → 「DreamTravel 需要在后台获取位置，才能在你到达时提醒」
  - 屏3：权限请求 → 「请允许位置权限」→ 请求按钮
- [ ] 仅在首次启动显示（SharedPreferences 标记）
- [ ] 验证：首次安装显示引导，再次打开不再显示

**T5.2 - 权限请求流程**
- [ ] 引导页请求 `ACCESS_FINE_LOCATION`
- [ ] 获得前台权限后，解释并请求 `ACCESS_BACKGROUND_LOCATION`（「始终允许」）
- [ ] 用户拒绝后台权限 → 显示降级提示：「提醒功能需要后台定位，你可以在设置中随时开启」
- [ ] 权限被永久拒绝 → 显示跳转设置页按钮
- [ ] 验证：各种权限组合下的行为符合预期

**T5.3 - 隐私声明**
- [ ] 在引导页提供隐私政策入口（WebView 加载隐私页面或本地文本）
- [ ] 明确说明：位置数据仅用于本地城市匹配，不上传精确轨迹到云端
- [ ] 验证：隐私声明可访问

---

### Wave 6: 边界情况与健壮性

> **目标**: 处理异常场景，保证 App 稳定运行

**T6.1 - 离线容错**
- [ ] Room 数据完整时 → UI 正常展示（不依赖网络）
- [ ] Firestore 同步失败 → 本地数据不受影响，静默重试（指数退避）
- [ ] 离线时添加的 Place/Todo → 上线后自动同步
- [ ] 验证：飞行模式下正常使用 → 联网后数据同步

**T6.2 - 城市名匹配容错**
- [ ] 实现城市名标准化（去除「市」「州」「地区」等后缀 + 简体中文统一）
- [ ] 高德逆地理编码返回的「城区/县」也匹配（如「大理市」→「大理」）
- [ ] 城市名歧义兜底：使用 cityCode 精确匹配
- [ ] 验证：各种城市名变体正确匹配

**T6.3 - 多城市重叠处理**
- [ ] 当用户在同一唤醒圈内设了多个 dream cities
- [ ] 驻留检查时逐一比对，哪个匹配就启动哪个的计时器
- [ ] 多个同时匹配 → 分别计时，触发时发送各自的通知
- [ ] 验证：设「大理」和「昆明」，到大理只触发大理的提醒

**T6.4 - GPS 信号异常处理**
- [ ] GPS 不可用超过 5 分钟 → 暂停计时器，切换网络定位尝试
- [ ] 精度持续不足（> 200m）→ 暂停计时，待精度恢复后继续
- [ ] 用户手动关闭定位 → 清理所有 Geofence + Worker
- [ ] 验证：GPS 信号丢失 → 计时暂停 → 恢复后正确继续

**T6.5 - 电池优化豁免引导**
- [ ] 检测是否被加入电池优化白名单
- [ ] 未加入 → 引导用户添加（跳转 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`）
- [ ] 引导文案说明：「免除电池优化可以让提醒更及时」
- [ ] 验证：电池优化豁免流程可用

**T6.6 - App 进程被杀后恢复**
- [ ] 设备重启 → BOOT_COMPLETED 广播重新注册 Geofence + 启动 Service
- [ ] App 被强制停止 → 下次打开时检查并恢复活跃的 Geofence
- [ ] 验证：重启/强杀后功能正常恢复

---

### Wave 7: 测试

> **目标**: 核心逻辑有测试覆盖

**T7.1 - 单元测试**
- [ ] `DwellTimerUseCase` 测试：
  - 正常驻留 30 分钟触发
  - 中途离开重置
  - 5 分钟内返回继续
  - 精度不足跳过
- [ ] `CityDetectionUseCase` 测试：城市名匹配/不匹配/近似匹配
- [ ] `DreamRepositoryImpl` 测试（mock Room + Firestore）：
  - 写入 Room 成功 → Firestore 异步写入
  - 离线写入 Room → 恢复后同步 Firestore
- [ ] 验证：`./gradlew testDebugUnitTest` 通过

**T7.2 - 插桩测试（Android Instrumentation）**
- [ ] 通知触发测试：mock GPS + mock 高德响应 → 验证通知内容
- [ ] 通知 Action 测试：模拟点击「已完成」→ 验证 Todo 状态变更
- [ ] Room 迁移测试：旧版本数据 → 新版本正确升级
- [ ] 权限流程测试：不同权限组合下的 UI 状态
- [ ] **全链路集成测试**：注册 Geofence → mock GPS 进入 → DwellCheckWorker 启动 → 计时到达 → 通知触发 → 模拟点击响应 → 验证状态变更
- [ ] 验证：`./gradlew connectedAndroidTest` 通过

**T7.3 - 手动 QA 场景**
- [ ] 场景1：首次安装 → 引导 → 添加梦想 → 模拟到达 → 通知触发 → 标记完成
- [ ] 场景2：飞行模式 → 添加 Todo → 联网 → 数据同步
- [ ] 场景3：到达城市 → 标记「进行中」→ 等待重提醒 → 标记完成
- [ ] 场景4：标记「路过」→ 离开 → 再回来 → 重新收到提醒
- [ ] 场景5：电池优化未豁免 → 引导豁免 → 后台检测正常工作
- [ ] **场景6（关键边界）**：在家添加本地城市的梦想 → 保存后立即触发检测（无需物理移动）→ 停留 30 分钟 → 收到通知

---

## 6. 验收标准（Agent-Executable）

| ID | 场景 | Given | When | Then |
|----|------|-------|------|------|
| AC-1 | 城市到达检测 | 设备 GPS 定位于梦想城市内 | 停留 ≥ dwellMinutes 分钟 | 通知触发，标题含城市名，内容列出待办 |
| AC-2 | 驻留计时重置 | 用户在城市内停留 15 分钟 | 离开城市超过 5 分钟 | 计时器重置为 0 |
| AC-3 | 短时离开继续 | 用户在城市内停留 15 分钟 | 离开 3 分钟后返回 | 计时器从 15 分钟继续（非重置） |
| AC-4 | 已完成响应 | 通知展示 | 点击「已完成」按钮 | 所有 Todo 标记 COMPLETED，通知消失 |
| AC-5 | 进行中重提醒 | 通知展示，点击「进行中」 | 等待 remindInterval 分钟 | 重提醒通知再次出现 |
| AC-6 | 路过重新提醒 | 通知展示，点击「路过」 | 离开城市 → 再次进入并停留 | 新通知出现 |
| AC-7 | 离线可用 | 飞行模式 | 添加 Place + Todo | 保存成功，联网后同步到 Firestore |
| AC-8 | 城市名匹配 | 逆地理编码返回「大理白族自治州」 | 梦想城市为「大理」 | 匹配成功 |
| AC-9 | 多城市独立 | 设了「大理」和「丽江」 | 到达大理停留 30 分钟 | 仅大理通知触发，丽江不触发 |
| AC-10 | 权限降级 | 用户拒绝后台定位 | 关闭定位并提示引导 | 不崩溃，显示降级说明 |

---

## 7. 风险清单

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 高德 API 不稳定/限流 | 中 | 城市检测失败 | 本地缓存 cityCode + 上次位置，API 不可用时对比缓存 |
| Android 厂商杀后台 | 高 | 后台检测中断 | 引导用户加白名单，前台 Service 保活 |
| 电池消耗投诉 | 中 | 用户卸载 | 2 分钟间隔 + GPS 精度门控，不是所有梦想都激活时停止 Service |
| GPS 漂移导致误判 | 中 | 不该发通知时发了 | 5 分钟容差 + 精度门控 + 逆地理编码二次确认 |
| Firebase 成本超预期 | 低 | 费用增加 | Spark 免费额度监控 + 升级前告警 |
| 城市名歧义匹配失败 | 中 | 到了不提醒 | cityCode 精确匹配 + 用户反馈机制 |

---

## 8. 依赖与前置条件

- [ ] Firebase 项目创建（获取 google-services.json）
- [ ] 高德开放平台注册（获取 API Key）
- [ ] Android 签名证书（debug 即可用于 MVP）
- [ ] 测试设备（至少一台 Android 10+ 真机，模拟器 GPS mock 不可靠）

---

## Final Verification Wave

> ⚠️ 此阶段必须由用户（你）逐项确认，不得自动跳过。

实施完成后，请在真机上验证以下场景：

- [ ] **全流程**：安装 → 引导 → 创建梦想 → 到达城市 → 等待 → 通知 → 响应
- [ ] **离线**：飞行模式下完整使用 → 联网 → 数据不丢失
- [ ] **重提醒**：标记进行中 → 等待周期 → 再次收到提醒
- [ ] **电池**：使用一天，检查电池消耗是否异常
- [ ] **杀进程恢复**：强制停止 App → 重新打开 → 梦想数据完好
- [ ] **多城市**：设 2 个梦想城市 → 到达其中 1 个 → 只触发对应提醒

**验证完成后，在此处标记：** ☐ 全部通过 → 项目可上线

---

*计划版本 v1.0 | 生成于 2026-04-29 | 待用户审阅后执行*
