# Changelog

## [1.1.0] - 2026-05-04

### Added

#### Phase A: MVP 核心链路做实
- **真实城市搜索**: 接入高德 GeocodeSearch API，输入城市名获取真实坐标，移除硬编码大理坐标
- **引导闭环**: 回访用户跳过引导页直接进入主页，基于 SharedPreferences 持久化状态
- **分阶段权限授权**: 引导页从 3 页扩展为 4 页，前台/后台位置权限分步请求；拒绝后显示解释对话框 + 设置引导
- **通知权限运行时请求**: API 33+ 在首次添加 Todo 时请求 POST_NOTIFICATIONS
- **隐私合规资料**: 完整隐私政策、用户协议、第三方 SDK 清单、数据删除说明

#### Phase B: 可靠性增强
- **双向同步系统**: Room 本地优先 + Firestore 云端回拉 + 冲突解决（updatedAt 时间戳），启动时自动拉取
- **账号升级路径**: 匿名登录 → 邮箱注册/登录升级，保留原有 Firestore 数据
- **用户态状态提示**: StatusManager + Snackbar，覆盖无网/GPS 关闭/权限缺失/同步失败
- **地图页交互增强**: 真实当前位置（蓝点）、Marker 点击跳转 Todo 列表、地图类型切换（标准/卫星）
- **Todo 粒度通知**: 通知按单条 Todo 操作（已完成/进行中/路过），不再按地点整体处理

#### Phase C: Todo 体系增强 + 分析基础
- **Todo 编辑/删除**: 长按 Todo 弹出上下文菜单，支持编辑（点击已完成/已路过条目）和删除操作；Room deleteTodo DAO + Firestore 同步删除
- **Todo 状态筛选**: TodoFilter 枚举（ALL/ACTIVE/HISTORY），ViewModel 通过 flatMapLatest 流式切换；Room 查询按 PENDING/IN_PROGRESS 和 COMPLETED/SKIPPED 分组
- **分析埋点框架**: AnalyticsManager 接口 + LogAnalyticsManager（Logcat 输出，Tag: DreamAnalytics）+ AnalyticsEvent 结构化事件枚举 + Hilt DI 模块
- **关键行为埋点**: 覆盖引导（完成/跳过）、权限（前台/后台/通知授信）、地点（添加/删除/开关）、Todo（添加/完成/进行中/跳过）、通知（展示/已完成/进行中/路过）、账号（注册/登录/退出/升级）
- **版本化发布文档**: CHANGELOG、QA 回归清单（覆盖 Phase A+B+C）、发布验收清单

### Changed
- 版本号从 1.0.0 升级至 1.1.0
- 引导页面从 3 页扩展为 4 页（API 29+）
- 地图 FAB 从"跳转大理"改为定位到真实 GPS 位置或所有 Markers 视野

### Fixed
- `AddPlaceViewModel._isSearching` 在搜索异常时不再永久卡住（try-catch-finally）
- `MapFragment` 原生 MapView 资源泄漏（恢复 onDestroy 调用）
- `OnboardingFragment` `@Suppress("DEPRECATION")` 范围从 class 级缩至 method 级
- `gradle.properties` 移除开发代理配置

### Security
- Firebase Crashlytics 集成
- 第三方 SDK 隐私政策完整披露

---

## [1.0.0] - 2026-04-29

### Added
- **项目脚手架**: Kotlin + Hilt + Room + Firestore + Navigation + WorkManager + 高德 SDK
- **数据层**: Room 实体（Place/Todo/DwellEvent）、DAO、Firestore 推送同步
- **定位引擎**: Geofence 唤醒围栏（20km）+ ForegroundService 协程驻留检查（2 分钟间隔）
- **通知系统**: BigTextStyle 通知、3 种 Action（已完成/进行中/路过）、WorkManager 重提醒
- **UI 层**: PlaceList（RecyclerView + 空状态）、AddPlace（搜索 + 驻留时间）、TodoList（状态分组 + 左滑）、Map（高德地图 + Marker）
- **引导页**: 3 屏 ViewPager2 引导 + SharedPreferences 仅首次显示
- **权限流程**: 前台 + 后台位置权限请求、降级提示、跳转设置
- **隐私入口**: 隐私政策、用户协议、数据使用说明
- **离线容错**: Room 主数据源 + 指数退避重试
- **城市名容错**: 「市/州」后缀标准化 + cityCode 精确匹配
- **多城市支持**: 独立计时 + 独立通知
- **开机自启**: BOOT_COMPLETED 广播恢复 LocationService
- **单元/插桩测试**: 51 个测试，覆盖 UseCase + Room + 通知

[1.1.0]: https://github.com/your-org/dreamtravel/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/your-org/dreamtravel/releases/tag/v1.0.0
