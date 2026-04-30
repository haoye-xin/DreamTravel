# Wave 5 阶段报告：引导与隐私

> **状态**: ✅ 完成（含 v2.1 隐私补充）  
> **日期**: 2026-04-29（初版）/ 2026-04-30（隐私补充）  
> **目标**: 首次引导流程 + 权限请求 + 隐私声明

---

## 任务完成情况

| 任务 | 描述 | 状态 | 验证 |
|------|------|:--:|------|
| T5.1 | OnboardingFragment + ViewPager2（3 屏） | ✅ | 首次安装显示引导，后续跳过 |
| T5.2 | 权限请求流程：前台→后台→降级→跳转设置 | ✅ | 编译通过 |
| T5.3 | 隐私声明入口 + 数据使用说明 | ✅ | v2.1 已补充 |

---

## 产出文件

### 引导页
| 文件 | 内容 |
|------|------|
| `ui/onboarding/OnboardingFragment.kt` | 3 页引导 + 跳过/下一步 + SharedPreferences 仅首次 |
| `ui/onboarding/OnboardingPagerAdapter.kt` | ViewPager2 Adapter |
| `res/layout/fragment_onboarding.xml` | 引导布局：ViewPager2 + 跳过按钮 + 下一步按钮 + 隐私链接 |

### 权限管理
| 文件 | 内容 |
|------|------|
| `util/PermissionUtils.kt` | 权限列表：FINE_LOCATION + COARSE_LOCATION + BACKGROUND_LOCATION + POST_NOTIFICATIONS |
| `AndroidManifest.xml` | 权限声明（含 FOREGROUND_SERVICE_LOCATION + RECEIVE_BOOT_COMPLETED + REQUEST_IGNORE_BATTERY_OPTIMIZATIONS） |

### 隐私声明（T5.3 v2.1 新增）
| 文件 | 修改内容 |
|------|------|
| `res/values/strings.xml` | 新增 `privacy_policy_title` + `privacy_policy_content`（6 节中文隐私声明） |
| `res/layout/fragment_onboarding.xml` | 新增 `tvPrivacy` TextView 链接 |
| `ui/onboarding/OnboardingFragment.kt` | 新增 `showPrivacyDialog()` 方法 |

---

## 引导页设计

| 页码 | 标题 | 按钮文本 |
|:--:|------|------|
| 1 | 记录梦想之地，不再错过想做的事 | 下一步 |
| 2 | DreamTravel 需要在后台获取位置，才能在你到达时提醒 | 下一步 |
| 3 | 请允许位置权限，让梦想提醒准时送达 | 允许 |

隐私政策链接在所有页面底部可见。

---

## 隐私声明内容（6 节）

1. **数据收集**：位置信息 + 设备标识符
2. **数据存储**：本地优先 + Firebase 加密传输
3. **数据使用**：仅用于城市检测，不用于广告
4. **数据共享**：不向第三方出售或共享
5. **你的权利**：随时撤回权限 / 停止使用
6. **免责声明**：高德 SDK 遵循自身隐私政策

---

## 验收
- [x] 首次安装显示 3 页引导
- [x] 完成后不再显示（SharedPreferences）
- [x] 权限请求：前台 + 后台 + 降级引导
- [x] 隐私政策入口可点击 → 弹出对话框
- [x] 隐私声明内容完整、格式清晰
- [ ] 真机权限弹窗流程测试（需真机）

---

## 变更记录

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-04-29 | T5.1 + T5.2 完成 |
| v2.1 | 2026-04-30 | T5.3 补充：隐私声明对话框 |

---

*报告版本：v1.0 | DreamTravel Wave 5*
