# Wave 4 阶段报告：UI 层

> **状态**: ✅ 完成  
> **日期**: 2026-04-29  
> **目标**: 完整 MVVM UI（Navigation Component + 6 个 Fragment）

---

## 任务完成情况

| 任务 | 描述 | 状态 | 验证 |
|------|------|:--:|------|
| T4.1 | Navigation Component + nav_graph.xml | ✅ | 6 个目的地 + 4 个 Action |
| T4.2 | PlaceListFragment + ViewModel + Adapter | ✅ | RecyclerView + 空状态 + SwipeRefresh |
| T4.3 | AddPlaceFragment + ViewModel（高德搜索 + NumberPicker） | ✅ | 城市搜索 + 驻留时间选择 |
| T4.4 | TodoListFragment + ViewModel + 状态分组 | ✅ | 分组展示 + 状态标识 |
| T4.5 | AddTodoFragment（BottomSheet） | ✅ | BottomSheetDialog |
| T4.6 | MapFragment + ViewModel（高德 MapView + Marker） | ✅ | 地图展示 + 标记点 |

---

## 产出文件

### 导航
| 文件 | 内容 |
|------|------|
| `res/navigation/nav_graph.xml` | 导航图：6 个目的地（onboarding → placeList → addPlace / todoList / map → addTodo） |
| `MainActivity.kt` | Navigation Host Activity |

### 梦想城市管理
| 文件 | 描述 |
|------|------|
| `ui/places/PlaceListFragment.kt` | 首页：RecyclerView + 激活开关 + 空状态 + SwipeRefresh |
| `ui/places/PlaceListViewModel.kt` | 数据加载 + 状态管理 |
| `ui/places/PlaceAdapter.kt` | RecyclerView Adapter + DiffUtil |
| `ui/places/AddPlaceFragment.kt` | 城市搜索 + NumberPicker 驻留时间 |
| `ui/places/AddPlaceViewModel.kt` | 高德搜索集成 + 保存逻辑 |
| `res/layout/fragment_place_list.xml` | PlaceList 布局 |
| `res/layout/fragment_add_place.xml` | AddPlace 布局 |

### 待办管理
| 文件 | 描述 |
|------|------|
| `ui/todos/TodoListFragment.kt` | 状态分组：待完成 / 进行中 / 已完成 / 已路过 |
| `ui/todos/TodoListViewModel.kt` | 按 placeId 加载 + 状态过滤 |
| `ui/todos/TodoAdapter.kt` | RecyclerView Adapter + 状态着色 |
| `ui/todos/AddTodoFragment.kt` | BottomSheetDialog：标题 + 备注 |
| `ui/todos/AddTodoViewModel.kt` | 保存逻辑 |
| `res/layout/fragment_todo_list.xml` | TodoList 布局 |
| `res/layout/fragment_add_todo.xml` | AddTodo BottomSheet 布局 |

### 地图
| 文件 | 描述 |
|------|------|
| `ui/map/MapFragment.kt` | 高德 MapView + Marker 展示 + 长按添加 |
| `ui/map/MapViewModel.kt` | 数据加载 |
| `res/layout/fragment_map.xml` | 地图布局（MapView + fab） |

### 主 Activity
| 文件 | 描述 |
|------|------|
| `res/layout/activity_main.xml` | ConstraintLayout + FragmentContainerView + BottomNavigationView |

---

## UI 流程

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│ Onboarding   │────▶│  PlaceList   │────▶│  AddPlace   │
│ (引导)       │     │  (梦想列表)   │     │  (添加梦想)   │
└─────────────┘     └──────┬───────┘     └─────────────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
     ┌────────▼───┐  ┌─────▼──────┐  ┌──▼──────┐
     │  TodoList   │  │    Map     │  │  AddTodo │
     │  (待办列表)  │  │   (地图)   │  │  (添加待办)│
     └────────────┘  └────────────┘  └─────────┘
```

---

## 验收
- [x] Navigation Component 6 个目的地配置
- [x] PlaceList RecyclerView + 空状态 + SwipeRefresh
- [x] AddPlace 高德搜索 + NumberPicker
- [x] TodoList 状态分组展示
- [x] AddTodo BottomSheet
- [x] MapFragment MapView + Marker

---

*报告版本：v1.0 | DreamTravel Wave 4*
