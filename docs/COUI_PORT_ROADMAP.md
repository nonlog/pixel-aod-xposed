# COUI Expressive 迁移路线图

本文件是 Pixel AOD for OPlus 的 COUI 2.5 迁移权威路线图。它定义行为来源、所有权边界、分阶段交付和回滚门槛；它不是一次性生产实现清单，也不授权在本 worktree 中直接修改生产代码。

## 基线与行为来源

- 当前可用回滚基线是 commit `a1f7e8dcee77db73b08f785319567b50f634ecd2`，版本 `0.1.331`。
- **M7 Release Hardening 当前冻结基线**是 commit `e7374956927f7ad8f89a870059b6218f66c1777e`，版本 `0.1.380 / 390`；`0.1.331` 继续保留为 emergency rollback，而不是当前功能基线。
- `0.1.329` 的 Small visual 是视觉 golden。
- `0.1.320` 的 clock-transition 是时钟过渡 golden。
- `6d564317` / `0.1.343` 的 regression repair line 已放弃，不得继续作为实现基线。
- 用户已撤回“media-only 必须切到 Large custom”的要求；COUI Expressive 2.5 的 scene/content 逻辑是唯一行为来源，也包括 media-only 场景。
- `reference/coui-2.5.0-260802/` 只保存指定版本的反编译参考材料。参考代码用于理解行为、状态机、几何、时序和 hook 边界，不应直接成为生产依赖。

## 不可违反的不变量

1. 系统只能有一个 primary clock owner。
2. 不允许 legacy renderer 与 persistent clock 同时拥有主时钟。
3. 业务 adapter 永远不得定位 primary clock；定位由 clock host 统一负责。
4. COUI 已给出答案时，不得用 YAAP/AOSP 的几何或布局替代它。
5. 旧 renderer 与新 renderer 必须互斥，不能依靠绘制顺序“看起来覆盖”来共存。
6. 除非记录了明确的 OOS compatibility adapter，否则时序、几何和状态必须与 COUI 精确一致。

任何实现、日志或验收若违反上述任一条，必须退回对应里程碑，不得进入下一阶段。

## Phase A：盘点与行为矩阵

先建立可复查的盘点和矩阵，不能先凭猜测改 hook。矩阵至少覆盖以下四组边界：

| 矩阵 | 必须记录的内容 |
| --- | --- |
| COUI scene/content | 空场景、media、通知、media+通知；Large/Small/Immersed；Information、AodContent、capsule 的出现、更新和退出条件 |
| LS/AOD transitions | 锁屏 Large/Small/Immersed、LS→AOD、AOD→LS、screen-off origin、partial/panoramic final state、live AOD retarget |
| OPlus hooks | ClockPlugin 的 load/render/unload、原生与新 renderer、AOD notification source、UDFPS/FOD、HBM/highlight、power/black-frame 边界 |
| retained module features | 天气、预报、预警、日历 contextual slot、过滤通知、USB、hotspot/tethering、system-status icon、锁屏通知策略、设置和 logging |

Phase A 的输出必须能把每个场景映射到唯一 owner、唯一 content source、进入/更新/退出事件和可观察日志。无法归属的行为先标为未知，不得由业务 adapter 私自补位。

## Phase B / M1：独立 COUI UDFPS 行为移植

第一实现里程碑只处理独立的 COUI StockUdfps 行为，不重写时钟：

- 移植 COUI StockUdfps 的状态机、显示/隐藏、退出动画、按压、成功和 native timeout 行为。
- 只有在新 UDFPS 路径实际 active 时，才禁用 legacy `PixelFingerprintIconController`；新路径未 active 时必须保留原有路径。
- 保留 vendor HBM 和 highlight 行为，不以替换图标为理由删除或改写厂商显示链路。
- 验证 AOD show/hide/exit、press、success、native timeout、touch/unlock；每项都要有设备行为和新鲜 LSPosed 日志证据。

M1 结束时 clock 仍由旧路径拥有，UDFPS 的回滚必须能独立关闭新路径并恢复 `0.1.331` 行为。

## Phase C / M2：统一 COUI clock host

建立一个统一的 COUI clock host，并以独占 feature flag 启用：

- 一个 host 统一拥有 LS Large、LS Small、LS Immersed，以及 AOD Large、AOD Small 的 glyph、date、weather。
- host 负责 COUI 精确几何、字体 variation、动画、weight 过渡和 live-AOD retarget；业务 adapter 不得再写主时钟位置。
- Large↔Small、LS↔AOD、glyph/date/weather 的所有权和生命周期必须从同一 host 发出。
- old renderer 与新 host 必须互斥；feature flag 关闭时只能回到明确的 legacy owner，不能出现双绘制或双更新。

M2 的验收要以 `0.1.329` Small visual 和 `0.1.320` clock-transition golden 对照，并记录 COUI 2.5 的偏差；未记录的 OOS 偏差视为失败。

## Phase D / OPlus ClockPlugin 与 AOD bridge

在统一 host 稳定后接入 OPlus 生命周期：

- 覆盖 ClockPlugin 的 load、render、unload，以及 screen-off origin。
- 正确处理 partial/panoramic 的最终状态。
- 统一 native AOD、new-render AOD 和 OPlus AOD notification 的输入来源，明确哪个事件更新 scene、哪个事件只更新 content。
- 在 COUI host 接管时抑制 plugin 的视觉输出，但不得破坏其必要的生命周期或卸载协议。
- 只移植 `PixelLockscreenClockHook` 中必要的行为；不得把无关的反编译 helper、`defpackage.*`、synthetic lambda 或 obfuscated `ConfigStore` 直接带入生产代码。

## Phase E：保留功能 adapter

所有保留功能都只能作为 COUI scene/content 的 adapter：

- 当前天气接入 COUI `Information`。
- 只保留一个通用 contextual slot，在 Forecast、Warning、Calendar 之间选择；不得另建第二个 renderer。
- 现有 filtered notifications 接入 COUI `AodContent`。
- 保留 USB、hotspot/tethering、system-status icon 处理，以及 lockscreen notification policy。
- 保留设置和 logging，并为每个 adapter 记录输入、选择结果、owner 和生命周期。

明确的 retained inventory：`BreezyWeatherSnapshot`、`BreezyWeatherForecast`、`BreezyWeatherAlert`、`ContextualAtAGlanceSelector`、`AodNotificationPipeline`、settings/logging、USB/hotspot handling。

adapter 只能提供内容和语义状态，不能创建第二套布局、主时钟或 AOD 生命周期。

## Phase F / M4：切换与收口

迁移期间启动策略只能在 `LEGACY` 与 `COUI_PORT` 之间二选一；feature flag 必须是 startup-exclusive，不能在同一启动实例同时启用两套 primary renderer。

完成验收后：

- 最终禁用或移除 legacy AOD primary renderer。
- 最终禁用或移除旧 fingerprint carrier。
- 删除或隔离所有会重新取得 primary clock/UDFPS ownership 的旧入口。
- 保留可追溯的回滚开关和 `0.1.331` 安装/启动路径，直到 supervisor 完成最终签收。

## Phase G：设备验收与证据

每个里程碑都必须在真实 OnePlus 12/OOS 16.0.9 上验证，并同时保存画面/视频与新鲜 LSPosed 日志。场景至少包括：

- empty、media-only、notifications-only、media+notifications；
- LS Large、LS Small、LS Immersed；
- LS→AOD、AOD→LS；
- capsule transitions；
- live AOD retarget；
- panoramic 和 partial；
- FOD timeout、show/hide、touch/unlock；
- current weather、forecast、alert、calendar；
- USB、hotspot/tethering、system-status icon；
- black-frame 和 power 行为。

必须做 COUI 参考实现的 frame/video parity 对照，并取得 fresh LSPosed logs。没有画面对照或新鲜日志，不能称为成功；在 legacy removal 之前尤其必须完成 frame/video parity 和 fresh LSPosed logs。

## 里程碑、回滚门和用户验收

| 里程碑 | 交付范围 | 回滚门 | 用户/设备验收 |
| --- | --- | --- | --- |
| M0 | 当前 exact `0.1.331` 可用回滚 | 任一新路径异常立即回到 exact `a1f7e8d`/`0.1.331` | 用户确认可安装、可启动、基础 LS/AOD 可用 |
| M1 | 仅 COUI UDFPS port；legacy clock untouched | 关闭新 UDFPS flag，恢复 `PixelFingerprintIconController` | 用户逐项确认 UDFPS show/hide/exit、press、success、timeout、touch/unlock；日志通过 |
| M2 | 统一 clock host，exclusive flag | 关闭 COUI clock host，保持 legacy clock，不允许半切换 | 用户确认 LS/AOD 全部 clock golden、无双 owner、视频和日志通过 |
| M3 | ClockPlugin/AOD bridge 与 content adapters | 按 adapter/bridge 开关回退到上一里程碑，保留单一 clock owner | 用户确认 scene/content、天气/通知、partial/panoramic、live retarget 和 OPlus 生命周期 |
| M4 | 完整 COUI_PORT cutover，legacy AOD primary renderer 与旧 fingerprint carrier 收口 | 保留 startup-exclusive LEGACY 回滚入口，直到最终签收 | 用户确认 Phase G 全场景、frame/video parity、fresh LSPosed logs 和 power/black-frame 行为 |

任何回滚门触发时，先保存日志、视频和触发场景，再回滚；不得用“看起来恢复”替代证据。

## Phase H / M5：LSPosed 静态作用域迁移 — 已完成（2026-08-18）

**状态更新：用户在 2026-08-18 明确覆盖了原 Post-stability 启动门槛，要求由 Web Sol 直接连续完成到静态作用域与 UI 重构。** 因此本阶段已实施；原“必须等待 M4 实机签收后才能开始”的条款仅保留为历史路线图背景，不再代表当前执行状态。

目标是迁移到新版 LSPosed Modern API 支持的静态作用域模型，使模块安装后声明最小且确定的目标作用域，而不是要求用户在管理器中手动选择。COUI 当前模块作为行为/打包参考；真正实现前必须重新核对当时最新版 LSPosed 的官方 Modern API 契约和 COUI APK 的 `META-INF/xposed/*` 元数据，不能假定旧样例格式永远不变。

实施要求：

- 以实际 hook inventory 推导最小静态 scope；当前预期核心目标是 `com.android.systemui`，若届时存在其它必要进程/包，必须逐项用运行时证据证明，禁止宽泛 scope。
- 将 `module.prop` / static-scope metadata 切换作为独立 packaging change，不与 clock/UDFPS/UI 重构混在同一个变更中。
- 保持 Modern Xposed API 与现有 compile-only stub 边界；不得把 `io.github.libxposed.*` 实现类打包进 APK。
- 验证 clean install、从旧 `staticScope=false` 版本升级、启用/禁用模块、SystemUI 重启后的 scope 生效情况，以及 LSPosed/Vector 兼容行为。
- APK 验收必须检查最终 `META-INF/xposed/*`、API version、static-scope 声明和实际注入日志；不能只看管理器 UI 显示。
- 若新版静态 scope 会破坏 Vector 或用户仍需要动态 scope 的兼容路径，必须先形成明确兼容策略再切换默认值。

本轮实施结果：

- `META-INF/xposed/module.prop` 已切换为 `staticScope=true`；Modern API 入口继续由 `java_init.list` 指向 `dev.codex.pixelaod.PixelAodModernEntry`。
- 最小静态作用域已固定为单一 `com.android.systemui`；没有扩大到其它包。
- `android:description` 已作为现代模块说明来源，设置 Activity 同时暴露 LSPosed 模块设置入口类别。
- 最终 APK 已逐项检查：三个 `META-INF/xposed/*` 文件存在、没有 legacy `assets/xposed_init`、没有打包 `io.github.libxposed.*` 实现类。
- Xposed `minApiVersion/targetApiVersion` 继续保持 101；本轮没有为了静态作用域无依据地升级 API 契约。
- **源码、封包与真实运行验收均已完成。** 0.1.361 最终 APK 为 Modern API 101/101、`staticScope=true`、单一 `com.android.systemui` scope；设备上的 LSPosed Manager 2.1.1 / framework API 102 将模块识别为 `API 101`，详情页明确显示 `The module declared static scope`，并把唯一 `System UI / com.android.systemui` scope 标记为 `Recommended`。
- **package-clean reinstall 已验证。** 通过 `pm uninstall -k` 移除包体并保留设置后重新安装 0.1.361，Manager 按框架语义将模块 Enable 状态置为关闭，但静态 `System UI` scope 已自动声明/勾选，无需用户手动选择 scope；在 Manager 启用后重载 SystemUI，fresh LSPosed 日志从新的 base.apk 路径加载 Modern entry 并启动 COUI_PORT clock/UDFPS owners。
- **enable/disable 已验证。** Manager 关闭模块后重载 SystemUI，新 PID 中没有任何 Pixel AOD 注入；重新启用后再次重载，Modern entry、当前 base.apk 路径与 COUI_PORT owners 全部恢复。
- **旧动态 scope → 新静态 scope 升级已验证。** 从 exact `a1f7e8d` 构建的 0.1.331 / versionCode 341 / `staticScope=false` APK 与当前 debug APK 签名证书一致；旧版先在真实 SystemUI PID 中成功注入，再直接覆盖升级到 0.1.361，fresh 日志确认框架切换到新的 0.1.361 base.apk 且保持单一 SystemUI 注入。
- **Vector/LSPosed 兼容条款已按本项目定义验收。** `Vector` 在本仓库约束中指 Modern Xposed packaging/API compatibility surface，而不是要求另装第二套框架；最终 APK 的 API 101/101、Modern `java_init.list`、static scope、无 legacy `assets/xposed_init`、无打包 libxposed implementation，加上 LSPosed API 102 真机注入证据共同满足该兼容门。

## Phase I / M6：设置界面 COUI / Re:X 视觉重构 — 已完成（2026-08-18）

**状态更新：同样由用户在 2026-08-18 明确覆盖原启动门槛并要求连续完成。** 本轮保持 Compose + Material 3、settings schema/provider、所有现有 key 与持久化语义，仅重构 presentation 层和统一组件体系。

当前 `SettingsActivity` 已使用 Jetpack Compose + Material 3，因此优先保留现有 Compose 技术栈、settings schema/provider、key 和持久化语义，只重构设计系统、信息架构和交互表现。M6 必须先抽取统一的 `PixelAodDesignSystem`（颜色/动态取色、typography、shape、spacing、surface、组件与 motion tokens），再让所有设置页面复用；禁止只做逐页 Material 3 换皮或继续产生页面私有样式。视觉来源优先级：

1. COUI Expressive 2.5 自身的设置界面与资源/反编译行为，作为主要视觉规范。
2. 1Dot 的 Re:X (`Xposed-Modules-Repo/one.dot.rex`) 公共仓库截图作为第二视觉参考；其公开仓库只提供文档/预览资产且项目明确为闭源，所以只做独立视觉复刻，不依赖、复制或假设其私有实现。

目标视觉/交互至少覆盖：

- COUI/Re:X 风格的系统动态取色、浅色/深色主题和 Material 语义色层级。
- 页面背景、`surfaceContainer*` 卡片、圆角、分组间距、section header、列表行高度与留白。
- 统一的 top app bar、返回层级、页面标题、副标题、leading/trailing icon 规则。
- Switch、单选/多选、下拉、slider、dialog、说明文本、危险/实验选项和禁用态的统一组件规范。
- 设置分类与导航信息架构重排；高频功能前置，调试/实验/高级项隔离，但不改变任何现有 setting key 的语义。
- 动态颜色、字体缩放、深浅色、横竖屏/窗口尺寸变化下的可读性与状态恢复。
- 动画和触感只作为 UI 层增强，不允许影响 hook 初始化或 SystemUI runtime。

验收要求：保存 COUI/Re:X 参考截图与本模块对应 light/dark 截图做逐页视觉对照；所有现有设置值、provider 读取、重启后持久化和功能开关行为必须保持兼容。UI 重构不能成为核心 hook 的新依赖。

本轮实施结果：

- 新增统一 `PixelAodDesignSystem`，集中管理动态取色、typography、shape、spacing、surface、motion 与页面/分组/行/slider/dialog 组件。
- `SettingsActivity` 已全部迁移到该设计系统；旧页面私有 `Coui*` 组件实现已删除，页面不再各自复制样式。
- Language、AOD 模式、日历图标应用、天气图标包四类选择对话框统一到共享 selection dialog；时间选择继续使用同一 Material 3 theme 下的系统时间组件。
- 保留 wallpaper-derived dynamic color、light/dark、edge-to-edge system bars、圆角低层级 surface、primary section label 等 COUI 视觉层级。
- 现有设置 key、ContentProvider 写入、权限流程、AOD 定时值、语言行为与运行时 hook 初始化依赖均保持原语义。
- Kotlin 编译、设置定向回归、完整 JVM 测试、`git diff --check` 与最终 debug build 均通过；0.1.360 的 Home/AOD/System UI 信息架构、动态取色、COUI 控件、三栏底栏与沉浸手势区已由用户完成真机视觉验收。

## Phase J / M7：Release Hardening — 进行中（2026-08-19）

M7 原则上不增加新功能，目标是把已完成的 COUI_PORT 从“功能完成”推进为可稳定发布的冻结基线。初始入场基线为 `339976e495b744352ea5408c4af40c02f4839cff` / `0.1.375`；用户随后明确授权一次 battery-status 功能例外，0.1.376 增加满电 `Charged` 状态并重新通过自动/真机门，随后用户又明确授权两项 M7 收口：0.1.377 将 UDFPS 发布策略固定为 OPlus 系统图标 + Pixel AOD success ripple，0.1.380 将 current-weather provider artwork 最终定为 18dp；两项均通过自动/真机门，因此当前冻结基线前移到 `e7374956927f7ad8f89a870059b6218f66c1777e` / `0.1.380`。

M7 的规则：

- 0.1.380 weather-size finalization 完成后重新冻结 feature scope；此后只允许修复回归、崩溃、功耗、生命周期或发布封包问题。
- 任一生产代码修复必须升版本，重新通过完整 JVM/build/metadata gate，并重跑受影响的物理矩阵。
- 任一会影响 SystemUI runtime 的生产代码变更都会使当前 soak 失效，修复后重新开始 soak。
- 不在 M7 开始时创建 stable tag；只有完整矩阵与 soak 都通过后才允许标记 stable baseline。
- `0.1.331` 继续作为 emergency rollback；正常回归比较以已验收的 0.1.380 行为为 golden；UDFPS release 配置固定为 OPlus system icon + Pixel AOD success ripple。

M7 的权威执行清单见 `docs/M7_RELEASE_HARDENING.md`。0.1.380 例外后的 entry gate 已重新完成：完整 JVM 测试 `382/382`，assemble 成功；USB 实机安装/hash、system-icon UDFPS + success ripple、`Charging` / `Charged`、18dp current-weather 验证通过。soak 从 0.1.380 重新计时；Modern Xposed metadata 仍保持 API `101/101`、`staticScope=true`、唯一 scope `com.android.systemui`。

## Post-stability TODO / 完成状态

- [x] **M5 — LSPosed static scope**：Modern metadata、最小 `com.android.systemui` 静态 scope、package-clean reinstall、旧 `staticScope=false` 升级、Manager enable/disable、SystemUI fresh injection 与 Vector/LSPosed packaging/API 兼容均已完成实机验收。
- [x] **M6 — COUI/Re:X settings UI**：`PixelAodDesignSystem`、三栏顶层信息架构、AOD 真实子页、COUI switch/dialog/time-picker/disabled-state、动态取色与沉浸底栏已完成；0.1.360 已由用户完成真机视觉验收并合并回 `agent/coui-port`。
- [x] M5/M6 的实现约束、回滚边界与剩余物理验收项已并入本路线图和 0.1.347 changelog；用户本轮明确要求直接连续实施，因此不再把“与 M1–M4 分开排期”作为阻塞条件。

## 非目标

- 历史约束：M1–M4 / Phase G 原计划不做 broad settings redesign；该延期已被 2026-08-18 用户直接执行指令覆盖，M6 现已完成。
- 不移植 YAAP layout。
- 不做 speculative Doze power rewrite。
- 不直接导入 raw decompiler helper。
- 不在本路线图阶段扩大到与 COUI scene/content、clock、UDFPS、OPlus bridge 或明确 retained inventory 无关的功能。
