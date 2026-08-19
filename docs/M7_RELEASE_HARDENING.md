# M7 Release Hardening

状态：**进行中**  
开始时间：2026-08-19  
冻结基线：`e7374956927f7ad8f89a870059b6218f66c1777e` / `0.1.380` / versionCode `390`

## 1. Entry gate

- [x] 用户完成 0.1.375 UDFPS 物理验收：无 idle/wake/sleep 面板高亮；COUI 指纹大小、背景和样式正常；已录入指纹识别与 success ripple 正常。
- [x] 设备确认安装 `0.1.375 / 385`，物理验收 APK SHA-256：`E00C35ABAE749DAB4F28F14826C7AF48689FFEE88C7C27A88CB6A03AAF322D26`。
- [x] 修复提交已推送：`339976e495b744352ea5408c4af40c02f4839cff`，`origin/agent/coui-port` 与本地 HEAD 一致。
- [x] 提交后完整 JVM 回归：`372/372`，0 failures / 0 errors / 0 skipped。
- [x] `:app:assembleDebug` 成功；提交后审计构建 APK 为 19,764,691 bytes，SHA-256 `6DEBC9797402113BCDF5AD36FABE0DB3522FE0EDD8F3420A1FE82354FE0848D0`。
- [x] APK packaging：Modern entry `dev.codex.pixelaod.PixelAodModernEntry`；API `101/101`；`staticScope=true`；唯一 scope `com.android.systemui`；无 legacy `assets/xposed_init`。
- [x] tracked worktree clean。

说明：0.1.375 完成 M7 初始入场后，M7 内发生了三次用户明确授权的收口变更：0.1.376 增加满电 `Charged`；0.1.377 将 UDFPS 发布策略收窄为 OPlus 系统图标 + Pixel AOD 独立 success ripple；0.1.380 将当前天气图标最终定为 18dp 可见图案。当前所有矩阵与 soak 以 0.1.380 为准。stable tag 只能在最终 release artifact 再次安装/哈希/物理确认后创建。

### M7 feature exception — 0.1.376 Charged state

- [x] 用户授权：充电完成后 AOD 电池文案从 `Charging` 切换为 `Charged`。
- [x] 实现仅使用 Android battery broadcast 状态：连接且 `CHARGING` → `Charging`；连接且 `FULL` → `Charged`；连接、100%、`NOT_CHARGING` → `Charged` fallback；未连接/unknown/discharging 不声明状态后缀。
- [x] 完整 JVM 回归 `377/377`，0 failures / 0 errors / 0 skipped；assemble 与 `git diff --check` PASS。
- [x] 0.1.376 测试 APK：19,764,695 bytes，SHA-256 `521BFC474EB1BE48E3786703C4889923FD71ED5691E2F2C185925B8F0F74B73C`。
- [x] USB 覆盖安装成功；设备 `0.1.376 / 386`，installed `base.apk` hash 与测试 APK 完全一致；仅一次 SystemUI reload `7149 → 18609`。
- [x] AOD 真实 48% / `CHARGING` 截图显示 `48% · Charging`；可逆 battery-service `100% / FULL` 仿真截图显示 `100% · Charged`；随后 `dumpsys battery reset` 已恢复真实 48% / CHARGING，SystemUI PID 保持 `18609`。
- [x] 功能提交已推送：`007eb752da53679dd2a31305711646bdca318236`。
- [x] **soak clock 从 0.1.376 重新从 0 小时开始。**

### M7 UDFPS ownership exception — 0.1.377

- [x] 用户决定停止自定义主指纹图标的 panel-highlight 调试：发布配置固定为 `pixel_fingerprint_icon=false`，OPlus 完全拥有主指纹图标、pressed carrier、alpha/scale/animation、HDR/local-HBM 与 AOD 指纹生命周期。
- [x] `udfps_success_ripple=true` 与图标替换解耦；Pixel AOD 只读取原生 fingerprint View 的几何作为独立 overlay 的圆心，不写 vendor glyph / pressed / HDR 状态。
- [x] 完整 JVM 回归 `382/382`；0.1.377 已物理确认原生 OPlus 指纹/按压行为正常且 Pixel AOD success ripple 可见。
- [x] 六次无手指 wake/sleep 中，模块 HDR-window preparation、HDR surface write、pressed carrier write、pressed icon configuration、native icon restore、touch handling 均为 0 次，SystemUI PID 稳定。
- [x] 0.1.377 已提交推送：`e3261b2f00624adbaeda7e8a1f4df786763c9a88`。

### M7 weather-size finalization — 0.1.380

- [x] 用户以系统 AOD 天气作为视觉参照，最终要求当前天气 provider artwork 为约 18dp；保持 22dp slot、4dp gap、provider-native colors 与所有 row geometry 不变。
- [x] 完整 JVM 回归 `382/382`，0 failures/errors/skips；assemble 与 `git diff --check` PASS。
- [x] 0.1.380 APK：19,764,803 bytes，SHA-256 `AEE59B94C319FAC89B90AB8CB559566E00F51DB786DBBF963A5C9651D21143D1`。
- [x] USB 覆盖安装成功；设备 version/hash 完全匹配；仅一次 SystemUI reload `27123 → 21323`；系统图标 UDFPS 设置保持 `replacement=false / ripple=true`。
- [x] 生产提交已推送：`e7374956927f7ad8f89a870059b6218f66c1777e`。
- [x] **soak clock 从 0.1.380 重新从 0 小时开始。**

## 2. 全场景功能矩阵

以下项目必须在冻结基线上重新走一遍；此前单项验收可作为参考证据，但不能替代 M7 矩阵。

### Clock / scene

- [x] LS Large：用户清除可显示通知后，0.1.380 实机锁屏进入 Large；大钟、日期、当前天气与锁屏布局正常，SystemUI PID `4688` 不变。证据 `.local/m7_soak_01380_20260819/large_empty/ls_large.png`。
- [x] LS Small：0.1.380 当前真实通知场景连续帧确认时钟、日期、天气、通知布局正常；证据 `.local/m7_matrix_01380_20260819/transitions_explicit/`。
- [ ] LS Immersed：scene 进入、退出与布局。
- [x] AOD Large：无可显示 notification/media content 时稳定进入 Large；大钟、日期/当前天气与 battery row 正常，无 partial-content row。证据 `.local/m7_soak_01380_20260819/large_empty/aod_large_probe.png` 与 `empty_after_media.png`。
- [x] AOD Small：真实 `NOTIFICATIONS` partial-content 场景稳定；运行时 mapping 为 `requestedScene=LARGE → visualScene=SMALL`，`iconCount=3/5`。
- [x] LS → AOD：显式 `SLEEP(223)` 连续帧在 Small/content 场景无 host 黑帧/错位；本轮 empty/Large 也确认 LS Large → AOD Large 两端稳定，SystemUI PID 不变。
- [x] AOD → LS：显式 `WAKEUP(224)` 连续帧在 Small/content 场景无 host 黑帧/错位；本轮 empty/Large 也确认 AOD Large → LS Large 两端稳定，锁屏视觉正常。
- [x] 分钟变化：16:06→16:11 被动连续 6 帧跨 5 次自然分钟变化，时钟每分钟正常更新、无冻结；COUI 整行光学重心变化保持预期，SystemUI PID `4688` 不变。证据 `.local/m7_soak_01380_20260819/minute_ticks/minute_contact.png`。
- [ ] burn-in 位移：本轮被动样本已确认固定 weather/info X `737→730`、notification row X `125→117`，方向/量级一致且无累计跳变；但同一 5 分钟窗口内没有同时活跃的 Forecast/media，因此完整共同锚点仍待后续自然场景补齐。

### Content

- [x] empty：用户清除可显示通知后，AOD 无 notification/media row 并选择 Large；后台常驻 notification keys 被正确过滤，不干扰 semantic content。
- [x] notification-only：semantic runtime `contentKind=NOTIFICATIONS`，AOD icon row 正常，STOP media 后无旧 media row 残留。
- [x] media-only：empty baseline 上启动 PixelPlay，MediaSession 为 `PLAYING` 时 AOD 显示媒体 title/artist 且无 notification icon row；`STOP` 后 MediaSession=`NONE`，立即恢复 Large/empty 且无旧媒体残留。证据 `.local/m7_soak_01380_20260819/large_empty/media_only.png`。
- [x] media + notifications：PixelPlay PLAYING 时 AOD 显示标题/artist media row，notification icons 同时保留；截图 `.local/m7_matrix_01380_20260819/media/playing.png`。
- [x] notification overflow / `+N`：自然通知在 16:06–16:09 触发 5 icons + `+1`，16:10 通知减少后 `+1` 自动消失并收缩回 5 icons，无需合成测试通知。
- [x] current weather：provider-native 彩色 icon、18dp 可见图案、22dp slot / 4dp gap 与文字/日期位置正确；0.1.380 稳定 AOD 已抓图。
- [x] Forecast：临时将窗口从 `21:00–23:30` 扩到 `00:00–23:59` 后，AOD 出现 `Tmr 31° / 24°`，同场景 LS 不显示 Forecast；随后已恢复原 `21:00–23:30`。证据 `.local/m7_matrix_01380_20260819/contextual/forecast_contact.png`。
- [ ] Weather Alert。
- [ ] Calendar contextual。
- [x] charging / battery 文案：已验证真实 `CHARGING` 与模拟 `FULL`，见 M7 feature exception 证据。
- [ ] USB、hotspot/tethering、system-status icon retained behavior：USB/system-status icon 已在 AOD row 实机确认；hotspot/tethering 尚未测试。

### UDFPS — release configuration: OPlus system icon + Pixel AOD success ripple

- [x] pixel_fingerprint_icon=false：模块不替换/修改主 fingerprint glyph，不接管 pressed carrier / HDR / local-HBM / AOD 生命周期。
- [x] 无手指 wake/sleep runtime proof：六次循环中模块 FOD/HBM mutation 日志全为 0，SystemUI PID 稳定。
- [x] 原生 OPlus 指纹图标与按压/识别行为正常（用户物理验收）。
- [x] udfps_success_ripple=true：真实识别成功后 Pixel AOD success ripple 正常（用户物理验收）。
- [x] 系统 dwell/press ripple 保持原生；仅在自定义 success overlay 有有效 geometry target 时抑制 native unlock ripple，避免双成功动画。
- [ ] 多次连续真实认证后确认无 SystemUI restart、overlay 残留或 success ripple 失效。

## 3. 压力与生命周期矩阵

- [x] 至少 20 次无手指 LS ↔ AOD / wake ↔ sleep 循环：已在当前 0.1.380 baseline 重新完成 20/20；SystemUI PID `4688` 全程不变、restart=0、最终 Dozing，FATAL=0 / ANR=0。
- [x] 连续跨至少 5 次分钟变化：16:06→16:11 六个 AOD 样本全程 Dozing/PID `4688`，时钟无冻结；date/weather 与 notification row 仅呈连续 burn-in 小位移，没有累计偏移。
- [x] media start → pause → resume → track change → stop：PixelPlay MediaSession 实际状态依次 `PLAYING → PAUSED → PLAYING → NEXT(active item 80→81) → NONE`；AOD media row 随 STOP 清除，PID 保持稳定。
- [x] 通知新增/移除/overflow 收缩：自然 `+1` overflow 在后续通知减少时自动消失，notification row 宽度从约 716px 收缩到约 584px，无旧 `+1`/旧图标残留。
- [ ] Forecast/contextual 在有效时间窗边界切换时不出现双行、锁屏泄露或横移。
- [ ] 充电插拔与电量文案刷新。
- [x] 设置持久化：`debug_logging=true` 写入后执行一次 intentional M7 SystemUI restart `21323 → 4688`，新进程读回仍为 true；验证 fresh injection 后已实时恢复 `debug_logging=false`。

## 4. Runtime / health gate

每轮物理矩阵结束后检查：

- [x] 没有新的 FATAL EXCEPTION（当前 M7 runtime batch：0）。
- [x] 没有 ANR（当前 M7 runtime batch：SystemUI ANR=0）。
- [x] 没有非预期 SystemUI PID 重启：当前 0.1.380 的 20-cycle batch 为 `4688 → 4688`，restart=0。
- [x] 没有重复 primary clock/UDFPS owner：fresh PID 4688 日志仅见同一 `ClockViewRoot@5da523c` / `CouiClockHostView@5c3d76e`，并明确记录 legacy primary path 被 startup owner 阻断；UDFPS release mode 不接管主图标。
- [x] 当前主动矩阵未出现新 FATAL/ANR 或 fresh PixelAod hook failure；SystemUI 经过 media/contextual/20-cycle 后保持稳定。
- [x] AOD/锁屏切换后 Window/Surface 收敛：当前仅 1 个 OPlus main FOD window + 1 个 pressed FOD window；success ripple overlay 仅 1 个 Window 记录且 idle 为 `NO_SURFACE`。

## 5. Soak gate

Soak 起点：**2026-08-19 16:00 +08:00**（0.1.380 / `e737495`；最后一次 intentional M7 SystemUI restart 与 20-cycle 主动压力测试已结束；从此不再重装/重载/改生产代码，除非发现 blocker。）

- 16:16 后用户清除了可显示通知并授权利用无通知窗口测试 Large；本轮只做正常 wake/sleep、screencap 与 PixelPlay media lifecycle，没有生产代码修改、APK 安装或 SystemUI restart，因此 **soak 仍从 16:00 连续计时**。Large/empty/media-only 结束后 PID 仍为 4688，FATAL=0 / ANR=0。

- [ ] 冻结生产代码后正常使用至少 24 小时。
- [ ] soak 期间不修改生产代码、不重新安装候选 APK。
- [ ] 无用户可见 clock/AOD/UDFPS/content 回归。
- [ ] 无非预期 SystemUI restart / FATAL / ANR。
- [ ] 无明显待机功耗异常或 AOD 无法进入低功耗状态。
- [ ] soak 结束后再次执行一次核心 smoke：LS→AOD→LS、media、notification、真实指纹 unlock/ripple。

如果 soak 期间发现需要修改 SystemUI runtime 的问题：修复后升版本，重新通过自动 gate + 受影响物理矩阵，并从 0 小时重新开始 soak。

## 6. Release exit gate

全部满足后才能退出 M7：

- [ ] 第 2–5 节全部 PASS，所有例外有明确记录并由用户接受。
- [ ] 最终完整 JVM tests / assemble / `git diff --check` PASS。
- [ ] 最终 APK metadata/static scope 再检查。
- [ ] 最终 release artifact 覆盖安装、设备 version/hash 完全匹配。
- [ ] 最终 artifact 做一次核心物理 smoke。
- [ ] CHANGELOG 与 handoff 收口，只保留当前有效 release 状态。
- [ ] tracked worktree clean，远端分支与本地一致。
- [ ] 最后才创建 stable tag；tag 指向通过上述全部门的最终 commit。

## 7. Change control

M7 期间禁止把“顺手优化”混入 hardening。发现非 blocker 的新功能/重构想法只记录到 M8/TODO，不在当前冻结基线上实施。
