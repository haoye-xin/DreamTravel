# Wave 2 阶段报告：定位引擎 ⭐核心

> **状态**: ✅ 完成  
> **日期**: 2026-04-29  
> **目标**: 城市检测 + 驻留计时器（高德地图 + Geofence + ForegroundService）

---

## 任务完成情况

| 任务 | 描述 | 状态 | 验证 |
|------|------|:--:|------|
| T2.1 | ACCESS_FINE_LOCATION + 后台权限 + Android 10+/9- 分支处理 | ✅ | 权限请求流程完整 |
| T2.2 | 高德搜索 API → 城市坐标 + cityCode | ✅ | 搜索"大理"→ 返回坐标 |
| T2.3 | GeofenceWakeReceiver：20km 唤醒围栏 + 初始检测 | ✅ | 编译通过 |
| T2.4 | LocationService：前台 Service + 协程驻留检查 | ✅ | 编译通过 |

---

## 产出文件

### 权限管理
| 文件 | 内容 |
|------|------|
| `util/PermissionUtils.kt` | 权限列表 + 请求逻辑 + Android 版本分支 |
| `util/LocationUtils.kt` | 位置精度检查 + 网络定位兜底 |

### 城市检测
| 文件 | 内容 |
|------|------|
| `domain/CityDetectionUseCase.kt` | 城市名标准化 + 匹配逻辑（cityCode + 双向包含） |
| `ui/places/AddPlaceViewModel.kt` | 高德搜索集成 |

### 驻留计时器
| 文件 | 内容 |
|------|------|
| `domain/DwellTimerUseCase.kt` | 状态机：ENTER → DWELLING → TRIGGERED → EXITED |
| `util/Constants.kt` | 围栏半径 20km / 检查间隔 2min / 精度 100m / 容差 5min |

### 后台服务
| 文件 | 内容 |
|------|------|
| `service/LocationService.kt` | ForegroundService + 协程循环（每 2 分钟逆地理编码检查） |
| `service/GeofenceWakeReceiver.kt` | 围栏进入广播 → 启动 LocationService |
| `service/BootReceiver.kt` | BOOT_COMPLETED → 恢复围栏注册 |

---

## 核心架构：城市检测混合策略

```
1. 高德获取城市中心坐标
2. 设置 20km 半径「唤醒围栏」
3. 进入围栏 → LocationService 启动驻留检查协程
4. 每 2 分钟逆地理编码确认城市名匹配
5. 匹配成功 → 累加驻留计时器
6. 驻留时间 ≥ 阈值 → 触发通知
7. 离开围栏 → 清理计时器
```

**关键修正（v2.0）**：原计划使用 WorkManager PeriodicWorkRequest（最小 15min），修正为 LocationService 内协程 `delay(2.minutes)` 循环。

---

## 参数配置

| 参数 | 值 | 说明 |
|------|------|------|
| 唤醒围栏半径 | 20km | 避免 GPS 漂移误判 |
| 驻留检查间隔 | 2 分钟 | 协程循环 |
| GPS 精度要求 | ≤ 100m | 精度门控 |
| 计时器容差 | 5 分钟 | 短暂离开不重置 |
| GPS 不可用超时 | 5 分钟 | 超时暂停 |

---

## 验收
- [x] 权限请求/拒绝/跳转设置流程正确
- [x] 高德搜索 API 集成（城市名 + cityCode）
- [x] Geofence 围栏注册 + 广播接收
- [x] ForegroundService 持久通知 + 协程循环
- [ ] 真机到达检测 + 驻留计时验证（需真机 - Wave 8）

---

*报告版本：v1.0 | DreamTravel Wave 2*
