# Wave 0 阶段报告：项目脚手架

> **状态**: ✅ 完成  
> **日期**: 2026-04-29  
> **目标**: 可编译运行的空项目（Kotlin + Hilt + Room + Firebase + 高德地图）

---

## 任务完成情况

| 任务 | 描述 | 状态 | 验证 |
|------|------|:--:|------|
| T0.1 | Kotlin 项目结构 + 全部依赖 + ProGuard + google-services.json | ✅ | build.gradle.kts 配置完整 |
| T0.2 | Hilt DI：DreamTravelApp + 4 个 Module | ✅ | 编译通过 |
| T0.3 | Room：AppDatabase + 3 个 DAO | ✅ | Room 编译通过 |
| T0.4 | App 图标 + 主题 + strings.xml | ✅ | 资源文件齐全 |

---

## 产出文件

### Gradle 配置
| 文件 | 描述 |
|------|------|
| `build.gradle.kts`（根） | 插件版本声明：AGP 8.2.2, Kotlin 1.9.22, Hilt 2.50, KSP, Firebase |
| `app/build.gradle.kts` | 应用配置：minSdk=26, targetSdk=34, compileSdk=34, 全部依赖 |
| `settings.gradle.kts` | 仓库配置：Google + Maven Central |
| `gradle.properties` | AndroidX, R8, JVM 参数 |
| `app/proguard-rules.pro` | Firebase / 高德 / Room / Hilt / Coroutines 保护规则 |

### 依赖清单
- **AndroidX**: core-ktx, appcompat, material, constraintlayout, navigation, lifecycle, viewpager2, swiperefresh
- **Hilt**: hilt-android 2.50 + ksp compiler
- **Room**: room-runtime 2.6.1 + room-ktx + ksp compiler
- **Firebase**: Auth, Firestore, Crashlytics（BOM 32.7.2）
- **Coroutines**: kotlinx-coroutines-android 1.7.3 + play-services 1.7.3
- **WorkManager**: work-runtime-ktx 2.9.0 + hilt-work
- **定位**: play-services-location 21.1.0
- **高德地图**: 3dmap 10.0.600 + search 9.7.1（⚠️ v2.1 已修复为统一聚合包）
- **测试**: JUnit 4.13.2, MockK 1.13.9, kotlinx-coroutines-test, AndroidX Test, Espresso

### DI 模块
| 文件 | 提供 |
|------|------|
| `di/DatabaseModule.kt` | Room AppDatabase + 3 个 DAO |
| `di/RepositoryModule.kt` | DreamRepository 接口绑定 |
| `di/LocationModule.kt` | FusedLocationProviderClient |
| `di/FirebaseModule.kt` | FirebaseFirestore, FirebaseAuth |

### 数据库
| 文件 | 描述 |
|------|------|
| `data/local/AppDatabase.kt` | Room Database，3 表，version=1 |
| `data/local/dao/PlaceDao.kt` | Place CRUD + Flow 查询 |
| `data/local/dao/TodoDao.kt` | Todo CRUD + 状态更新 + 计数 |
| `data/local/dao/DwellEventDao.kt` | DwellEvent CRUD + 活跃事件查询 |
| `data/local/entity/Entity.kt` | PlaceEntity, TodoEntity, DwellEventEntity |

### 资源文件
| 文件 | 描述 |
|------|------|
| `res/values/strings.xml` | 38 个字符串资源 |
| `res/values/themes.xml` | Material3 主题 |
| `res/values/colors.xml` | 配色方案 |
| `res/mipmap-*/` | App 图标（6 密度） |

---

## 构建阻塞记录

| 问题 | 状态 | 解决方案 |
|------|:--:|------|
| 高德 3dmap:10.0.600 + search:9.7.1 重复类冲突 | ✅ 已修复 | 替换为 `3dmap-location-search:10.0.700_loc6.4.5_sea9.7.2` 统一聚合包 |

---

## 验收
- [x] `./gradlew assembleDebug` 成功（依赖解析通过）
- [x] App 启动不崩溃（Hilt DI 链路正常）
- [x] DAO 编译通过（Room KSP 生成代码）
- [x] 安装后桌面显示图标

---

*报告版本：v1.0 | DreamTravel Wave 0*
