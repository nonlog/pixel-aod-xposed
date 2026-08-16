# COUI Expressive 迁移路线图

本文件是 Pixel AOD for OPlus 的 COUI 2.5 迁移权威路线图。它定义行为来源、所有权边界、分阶段交付和回滚门槛；它不是一次性生产实现清单，也不授权在本 worktree 中直接修改生产代码。

## 基线与行为来源

- 当前可用回滚基线是 commit `a1f7e8dcee77db73b08f785319567b50f634ecd2`，版本 `0.1.331`。
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

## 非目标

- 不做 broad settings redesign。
- 不移植 YAAP layout。
- 不做 speculative Doze power rewrite。
- 不直接导入 raw decompiler helper。
- 不在本路线图阶段扩大到与 COUI scene/content、clock、UDFPS、OPlus bridge 或明确 retained inventory 无关的功能。
