# Wave 7 阶段报告：测试

> **状态**: ✅ 代码完成（需真机执行）  
> **日期**: 2026-04-30  
> **目标**: 单元测试 + 插桩测试 + 手动 QA 清单

---

## 任务完成情况

| 任务 | 描述 | 状态 | 验证 |
|------|------|:--:|------|
| T7.1 | 单元测试：UseCase + Repository | ✅ | 4 个测试文件，36+ 测试用例 |
| T7.2 | 插桩测试：Room + Notification + 基础 UI | ✅ | 2 个测试文件 |
| T7.3 | 手动 QA：6 个场景清单 | ✅ | QA 清单已创建 |

---

## 产出文件

### 单元测试（`app/src/test/`）

| 文件 | 测试目标 | 用例数 |
|------|------|:--:|
| `domain/CityDetectionUseCaseTest.kt` | normalizeCityName + isCityMatch 匹配逻辑 | 14 |
| `domain/DwellTimerUseCaseTest.kt` | 状态机：onEnterCity / tickDwell / onExitCity | 11 |
| `domain/ReminderSchedulerUseCaseTest.kt` | 通知触发 + 用户 Action 响应 | 9 |
| `data/repository/DreamRepositoryImplTest.kt` | CRUD 操作 + Firestore 同步验证 | 10 |

**总计：4 个文件，44 个测试用例**

### 插桩测试（`app/src/androidTest/`）

| 文件 | 测试目标 | 用例数 |
|------|------|:--:|
| `AppDatabaseTest.kt` | Room 数据库创建 + CRUD + 状态更新 + DwellEvent 生命周期 | 7 |
| `notification/NotificationHelperTest.kt` | 通知构建 + 取消 + 不崩溃 | 3 |

**总计：2 个文件，10 个测试用例**

### 手动 QA 清单

| 文件 | 内容 |
|------|------|
| `.sisyphus/qa-checklist.md` | 6 个场景 × 多个步骤，含预期结果和通过标记列 |

---

## 测试覆盖详情

### CityDetectionUseCase（14 用例）
- ✅ normalizeCityName：去除 市/州/地区/自治州/白族自治州 后缀
- ✅ normalizeCityName：空白/空字符串
- ✅ normalizeCityName：无变化纯城市名
- ✅ isCityMatch：精确匹配 / cityCode 匹配 / 子串匹配 / 不匹配

### DwellTimerUseCase（11 用例）
- ✅ onEnterCity：Started / AlreadyTriggered / Continuing / ShouldTrigger 四种状态
- ✅ tickDwell：NotDwelling / Ticking / ShouldTrigger 三种状态
- ✅ onExitCity：标记 EXITED / 无事件不崩溃
- ✅ clearEvents：调用 DAO

### ReminderSchedulerUseCase（9 用例）
- ✅ triggerReminder：PENDING 发送通知 / IN_PROGRESS 发送通知
- ✅ triggerReminder：跳过 COMPLETED / 跳过 SKIPPED
- ✅ triggerReminder：空列表 / 全部已完成 → 不发送
- ✅ handleUserAction：COMPLETED / IN_PROGRESS / PASSING 三种响应

### DreamRepositoryImpl（10 用例）
- ✅ getPlaces / getActivePlaces / getPlaceById
- ✅ addPlace / updatePlace / deletePlace（本地 + 远端验证）
- ✅ getTodos / addTodo（本地 + 远端验证）
- ✅ updateTodoStatus / updateAllTodosStatus / incrementRemindCount / countPendingTodos

---

## 测试执行

| 命令 | 说明 |
|------|------|
| `./gradlew :app:testDebugUnitTest` | 运行所有单元测试 |
| `./gradlew :app:connectedAndroidTest` | 运行所有插桩测试（需连接设备） |

---

## 验收
- [x] 44 个单元测试代码写入
- [x] 10 个插桩测试代码写入
- [x] 手动 QA 6 场景清单
- [ ] `./gradlew :app:testDebugUnitTest` 执行通过（需 Android SDK）
- [ ] `./gradlew :app:connectedAndroidTest` 执行通过（需真机/模拟器）
- [ ] 手动 QA 全部场景执行（需真机）

---

*报告版本：v1.0 | DreamTravel Wave 7*
