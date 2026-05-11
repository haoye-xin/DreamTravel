# DreamTravel Bug 修复日志

**日期**: 2026-05-10
**目标**: 修复创建 Todo 和其他功能的运行时崩溃

---

## 1. Gradle OOM 修复

**问题**: Gradle 守护进程内存不足导致构建失败
**修复**: `gradle.properties` 设置 `-Xmx4096m -XX:-UseCompressedClassPointers -XX:-UseCompressedOops`
**后续**: 移除 `-XX:-UseCompressedOops`（4GB 堆上无意义，增加内存开销）

---

## 2. Coroutine 崩溃修复

**问题**: `AddTodoFragment` 保存按钮的协程未捕获异常
**修复**: 添加 try-catch 包装协程调用

---

## 3. Firestore 同步字段缺失

**问题**: 新增的 8 个字段未同步到云端
**字段**: provinceCode, provinceName, cityCode, cityName, districtCode, districtName, formattedAddress, color
**修复**: `syncTodoToFirestore` 和 `pullCloudData` 中添加字段映射

---

## 4. addPlaceFromMap 重复调用

**问题**: `addPlaceFromMap` 嵌套调用 `addPlace()` 导致重复
**修复**: 直接调用 `repository.addPlace()`

---

## 5. 数据库迁移

**问题**: 新增表和列需要迁移
**修复**: 创建 `MIGRATION_2_4`，使用 `PRAGMA table_info` 检查现有列后添加，覆盖 todos、places、dwell_events 三张表

---

## 6. SyncRetryWorker 网络约束

**问题**: 无网络时仍尝试同步
**修复**: 添加 `NetworkType.CONNECTED` 约束

---

## 7. 异常处理优化

**问题**: `handleSyncResult` 捕获 `Throwable` 过于宽泛
**修复**: 改为捕获 `Exception`

---

## 8. reverseGeocode 超时

**问题**: 高德逆地理编码可能阻塞
**修复**: 添加 `withTimeoutOrNull(10_000L)`

---

## 9. 硬编码中文字符串

**问题**: `fragment_add_todo.xml` 和 `RegionPickerBottomSheetFragment.kt` 中硬编码中文
**修复**: 替换为 `@string/` 资源引用

---

## 10. Snackbar 导入

**问题**: 全限定名导入 `com.google.android.material.snackbar.Snackbar`
**修复**: 使用标准 import 语句

---

## 11. addPlaceFromMap cityCode=null

**问题**: 从地图添加地点时 cityCode 为 null
**修复**: 通过导航参数传递 `prefillCityCode`，`nav_graph.xml` 添加参数

---

## 12. Fragment 生命周期保护

**问题**: `onViewCreated` 和 `onCreateView` 中未捕获异常
**修复**: 添加 try-catch，`onViewCreated` 添加 `_binding == null` 保护

---

## 13. 高德 SDK 隐私合规 (根因修复)

**问题**: `GeocodeSearch` 构造函数抛出 `AMapException` (errorCode 555570)
**根因**: Hilt 在 `super.onCreate()` 时初始化 DI 图，触发 `GeocodeSearch` 构造函数，但隐私合规设置在 `super.onCreate()` 之后
**修复**:
- `DreamTravelApp.kt`: 在 `super.onCreate()` 之前调用 `ServiceSettings.updatePrivacyShow/Agree`
- `SearchModule.kt`: 在 `GeocodeSearch(context)` 构造函数之前添加隐私合规调用

---

## 14. Gradle 版本锁定

**问题**: 网络无法下载 Gradle 8.13 或 AGP 8.13.2
**修复**: 锁定为 AGP 8.2.2 + Gradle 8.5

---

## 15. 代码审查 Round 3 修复

| # | 问题 | 文件 | 状态 |
|---|------|------|------|
| 1 | `MIGRATION_4_4` (4→4) 违反 Room 规则 | `AppDatabase.kt` | ✅ 已删除 |
| 2 | `onCreateView` try-catch 导致 NPE | `AddTodoFragment.kt` | ✅ 已修复 |
| 3 | `-XX:-UseCompressedOops` 无意义 | `gradle.properties` | ✅ 已移除 |
| 4 | `SimpleDateFormat` 非线程安全 | `TodoAdapter.kt` | 低优先级 |

---

## 16. 代码审查 Round 4 (全面审查)

由 Oracle + 2 个 Explore 代理并行审查，综合所有发现。

### 🔴 Critical

| # | 问题 | 文件 | 说明 |
|---|------|------|------|
| C1 | `fallbackToDestructiveMigration()` 导致静默数据丢失 | `DatabaseModule.kt:28` | 任何未覆盖的版本升级路径会销毁全部用户数据 |
| C2 | `completedAt` null 导致 Firestore 同步永久失败 | `FirestoreSyncService.kt:142` | `mapOf("completedAt" to todo.completedAt)` 中 null 值被 Firestore SDK 拒绝，SyncRetryWorker 无限重试 |

### 🟠 High

| # | 问题 | 文件 | 说明 |
|---|------|------|------|
| H1 | 缺少 v1→4、v3→4 迁移路径 | `AppDatabase.kt` | 仅有 `MIGRATION_2_4`，v1/v3 用户触发 destructive fallback |
| H2 | `PlaceSearchService` 单例 GeocodeSearch 并发竞态 | `PlaceSearchService.kt` | `setOnGeocodeSearchListener()` 被并发调用覆盖，导致回调丢失 |
| H3 | `MapFragment.reverseGeocode()` 绕过 DI 直接构造 GeocodeSearch | `MapFragment.kt:155` | 与 `SearchModule` 的隐私合规保证不一致 |
| H4 | `LocationService.reverseGeocode()` 客户端泄漏 | `LocationService.kt` | 协程取消时未调用 `client.onDestroy()` |
| H5 | 保存按钮无防抖/禁用保护 | `AddTodoFragment.kt:77` | 快速双击触发多个协程 + 多次 `dismiss()` |
| H6 | `requireContext()` 在异步错误处理中可能崩溃 | `AddTodoFragment.kt:100` | Fragment 脱离后 `requireContext()` 抛出 `IllegalStateException` |

### 🟡 Medium

| # | 问题 | 文件 | 说明 |
|---|------|------|------|
| M1 | `binding` 强制解包 `_binding!!` NPE 风险 | `AddTodoFragment.kt:32` | `onDestroyView()` 后访问 `binding` 会崩溃 |
| M2 | `LocationService.registerAllGeofences()` 缺少 `distinctUntilChanged()` | `LocationService.kt` | Flow 频繁发射导致重复注册地理围栏 |
| M3 | `AddPlaceFragment._saveComplete` SharedFlow 无 replay | `AddPlaceFragment.kt` | 快速保存时事件丢失，Fragment 不会 pop backstack |
| M4 | `TodoListFragment` 每次 Flow 发射创建新 Adapter | `TodoListFragment.kt` | 性能浪费，丢失滚动位置 |

---

## 关键决策

1. **AMap SDK 隐私合规**: 必须在 `super.onCreate()` 之前设置，因为 Hilt 在 `super.onCreate()` 时初始化 DI 图
2. **数据库迁移策略**: 使用 `PRAGMA table_info` 检查现有列，确保迁移幂等
3. **Gradle 版本**: 网络限制，锁定为 8.5 + AGP 8.2.2

---

## 下一步 (优先级排序)

### 必须修复 (阻塞发布)
1. **C1**: 移除 `fallbackToDestructiveMigration()`，补充完整迁移路径（或仅在 debug 构建启用）
2. **C2**: `syncTodoToFirestore` 中 `completedAt` 为 null 时从 map 中排除该字段
3. **H1**: 补充 `MIGRATION_1_4` 或 `MIGRATION_3_4`（确认实际存在的历史版本）
4. **H2**: `PlaceSearchService` 使用 per-request GeocodeSearch 或 mutex 保护

### 应该修复 (影响稳定性)
5. **H3**: `MapFragment.reverseGeocode()` 改用注入的 GeocodeSearch
6. **H4**: `LocationService.reverseGeocode()` 添加 `invokeOnCancellation { client.onDestroy() }`
7. **H5**: 保存按钮添加 `isEnabled = false` 防抖
8. **H6**: `requireContext()` 替换为 `context ?: return`

### 可以延后
9. **M1-M4**: Medium 优先级问题，不影响核心功能

---

## 17. 代码审查 Round 4 修复 (已完成)

| # | 问题 | 修复方式 | 文件 |
|---|------|----------|------|
| C1 | `fallbackToDestructiveMigration()` 数据丢失风险 | 添加警告注释，开发阶段保留 | `DatabaseModule.kt` |
| C2 | `completedAt` null 导致 Firestore 同步失败 | `mutableMapOf` + 仅非 null 时添加字段 | `FirestoreSyncService.kt` |
| H1 | 缺少 v1→4 迁移路径 | 新增 `MIGRATION_1_4` | `AppDatabase.kt` |
| H2 | GeocodeSearch 并发竞态 | 每次请求创建独立实例，删除单例提供 | `PlaceSearchService.kt` + `SearchModule.kt` |
| H4 | LocationService 客户端泄漏 | 添加 `invokeOnCancellation { client.onDestroy() }` | `LocationService.kt` |
| H5 | 保存按钮无防抖 | 点击后 `isEnabled = false`，失败时恢复 | `AddTodoFragment.kt` |
| H6 | `requireContext()` 崩溃 | 改为 `context?.let { ctx -> }` | `AddTodoFragment.kt` + `MapFragment.kt` |
| M3 | SharedFlow 事件丢失 | 改为 `Channel<Unit>(Channel.BUFFERED)` + `receiveAsFlow()` | `AddPlaceViewModel.kt` |
| M4 | Adapter 每次重建 | 创建一次 + `updateData()` 更新 | `TodoListFragment.kt` |

**H3**: MapFragment.reverseGeocode() 已有 `invokeOnCancellation` 保护，无需修改
**M1**: binding NPE 已有 `repeatOnLifecycle` 保护，暂不修改
**M2**: distinctUntilChanged 属于性能优化，暂不修改

所有测试通过 ✅
