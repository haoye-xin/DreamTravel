# Wave 6 阶段报告：边界情况

> **状态**: ✅ 完成  
> **日期**: 2026-04-29  
> **目标**: 离线容错 + 城市名匹配 + 多城市独立 + GPS 异常 + 电池优化 + 杀进程恢复

---

## 任务完成情况

| 任务 | 描述 | 状态 | 验证 |
|------|------|:--:|------|
| T6.1 | Room 本地优先 + 离线容错 + 指数退避重试 | ✅ | 本地优先架构已实现 |
| T6.2 | 城市名匹配容错（CityDetectionUseCase） | ✅ | 单元测试覆盖 |
| T6.3 | 多城市独立计时 + 独立通知 | ✅ | 按 placeId 隔离 |
| T6.4 | GPS 异常处理（超时暂停 + 精度门控） | ✅ | 参数已配置 |
| T6.5 | 电池优化豁免引导 | ✅ | Manifest 已配 |
| T6.6 | BOOT_COMPLETED + 杀进程恢复 | ✅ | BootReceiver 已注册 |

---

## 各任务详情

### T6.1 离线容错
- **本地优先**：Room 做主数据源，所有写操作先写本地再异步同步 Firestore
- **离线队列**：变更暂存 Room，ConnectivityManager 监听网络恢复后批量同步
- **指数退避**：FirestoreSyncService 内置重试逻辑

### T6.2 城市名匹配容错
- **标准化**：`CityDetectionUseCase.normalizeCityName()` 去除「市」「州」「地区」「自治州」「白族自治州」后缀
- **cityCode 精确匹配**：优先使用 cityCode 精确匹配（最可靠）
- **双向包含**：标准化后支持「大理白族自治州」↔「大理」互相匹配
- **单元测试**：12 个测试用例覆盖边界

### T6.3 多城市独立
- **独立计时**：每个 Place 有独立的 DwellEventEntity 和计时器
- **独立通知**：通知 ID = `placeId.hashCode()`，确保不同城市通知不冲突
- **⚠️ 待验证**：需真机验证多城市重叠情况

### T6.4 GPS 异常处理
| 机制 | 参数 | 说明 |
|------|------|------|
| 精度门控 | ≤ 100m | GPS 精度不足时跳过本次检查 |
| 超时暂停 | 5 分钟 | GPS 连续不可用超时后暂停检测 |
| 网络定位兜底 | COARSE_LOCATION | GPS 不可用时使用网络定位 |

### T6.5 电池优化豁免
- Manifest 已声明 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- 引导流程中可引导用户授权

### T6.6 杀进程恢复
- `BootReceiver` 注册 `BOOT_COMPLETED` 广播
- 启动时 `DreamTravelApp.onCreate()` 恢复围栏注册
- `LocationService` 为 ForegroundService，被杀后系统会尝试重启

---

## 验收
- [x] Room 本地优先 + 离线容错架构
- [x] CityDetectionUseCase 容错逻辑 + 单元测试
- [x] 多城市按 placeId 隔离（代码层面）
- [x] GPS 异常参数配置
- [x] 电池优化豁免 Manifest 声明
- [x] BOOT_COMPLETED 广播注册
- [ ] 真机离线/杀进程恢复测试（需真机 - Wave 8）
- [ ] 真机多城市重叠测试（需真机 - Wave 8）

---

*报告版本：v1.0 | DreamTravel Wave 6*
