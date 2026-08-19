# M7 Release Hardening

状态：**进行中**  
开始时间：2026-08-19  
冻结基线：`007eb752da53679dd2a31305711646bdca318236` / `0.1.376` / versionCode `386`

## 1. Entry gate

- [x] 用户完成 0.1.375 UDFPS 物理验收：无 idle/wake/sleep 面板高亮；COUI 指纹大小、背景和样式正常；已录入指纹识别与 success ripple 正常。
- [x] 设备确认安装 `0.1.375 / 385`，物理验收 APK SHA-256：`E00C35ABAE749DAB4F28F14826C7AF48689FFEE88C7C27A88CB6A03AAF322D26`。
- [x] 修复提交已推送：`339976e495b744352ea5408c4af40c02f4839cff`，`origin/agent/coui-port` 与本地 HEAD 一致。
- [x] 提交后完整 JVM 回归：`372/372`，0 failures / 0 errors / 0 skipped。
- [x] `:app:assembleDebug` 成功；提交后审计构建 APK 为 19,764,691 bytes，SHA-256 `6DEBC9797402113BCDF5AD36FABE0DB3522FE0EDD8F3420A1FE82354FE0848D0`。
- [x] APK packaging：Modern entry `dev.codex.pixelaod.PixelAodModernEntry`；API `101/101`；`staticScope=true`；唯一 scope `com.android.systemui`；无 legacy `assets/xposed_init`。
- [x] tracked worktree clean。

说明：0.1.375 完成 M7 初始入场后，用户在 M7 内明确授权一次 battery-status 功能例外。该例外已作为 0.1.376 独立提交并重新冻结 feature scope；从此所有矩阵与 soak 以 0.1.376 为准。stable tag 只能在最终 release artifact 再次安装/哈希/物理确认后创建。

### M7 feature exception — 0.1.376 Charged state

- [x] 用户授权：充电完成后 AOD 电池文案从 `Charging` 切换为 `Charged`。
- [x] 实现仅使用 Android battery broadcast 状态：连接且 `CHARGING` → `Charging`；连接且 `FULL` → `Charged`；连接、100%、`NOT_CHARGING` → `Charged` fallback；未连接/unknown/discharging 不声明状态后缀。
- [x] 完整 JVM 回归 `377/377`，0 failures / 0 errors / 0 skipped；assemble 与 `git diff --check` PASS。
- [x] 0.1.376 测试 APK：19,764,695 bytes，SHA-256 `521BFC474EB1BE48E3786703C4889923FD71ED5691E2F2C185925B8F0F74B73C`。
- [x] USB 覆盖安装成功；设备 `0.1.376 / 386`，installed `base.apk` hash 与测试 APK 完全一致；仅一次 SystemUI reload `7149 → 18609`。
- [x] AOD 真实 48% / `CHARGING` 截图显示 `48% · Charging`；可逆 battery-service `100% / FULL` 仿真截图显示 `100% · Charged`；随后 `dumpsys battery reset` 已恢复真实 48% / CHARGING，SystemUI PID 保持 `18609`。
- [x] 功能提交已推送：`007eb752da53679dd2a31305711646bdca318236`。
- [x] **soak clock 从 0.1.376 重新从 0 小时开始。**

## 2. 全场景功能矩阵

以下项目必须在冻结基线上重新走一遍；此前单项验收可作为参考证据，但不能替代 M7 矩阵。

### Clock / scene

- [ ] LS Large：时钟、日期、天气、通知布局和视觉。
- [ ] LS Small：时钟、日期、天气、通知布局和视觉。
- [ ] LS Immersed：scene 进入、退出与布局。
- [ ] AOD Large。
- [ ] AOD Small。
- [ ] LS → AOD：位置、weight、glyph、date/weather 连续动画，无黑帧/跳帧。
- [ ] AOD → LS：位置、weight、glyph、date/weather 连续动画，无瞬间错位。
- [ ] 分钟变化：COUI 的整行光学重心调整保持预期，无额外漂移。
- [ ] burn-in 位移：时钟、forecast/media/notification content 的共同锚点保持正确。

### Content

- [ ] empty。
- [ ] notification-only。
- [ ] media-only。
- [ ] media + notifications。
- [ ] notification overflow / `+N`。
- [ ] current weather：彩色 icon、14dp 可见图案、文字/日期位置正确。
- [ ] Forecast：只在 AOD 显示；与 media、notification icon 行使用相同内容锚点；时间窗进入/退出正确。
- [ ] Weather Alert。
- [ ] Calendar contextual。
- [x] charging / battery 文案：已验证真实 `CHARGING` 与模拟 `FULL`，见 M7 feature exception 证据。
- [ ] USB、hotspot/tethering、system-status icon retained behavior。

### UDFPS

- [ ] 无手指连续亮屏/熄屏：无瞬时或持续 panel highlight。
- [ ] 锁屏 idle 30 秒：无 local-HBM 卡亮。
- [ ] AOD idle：dashed/outline 样式与退出动画正常。
- [ ] 真实按下：HDR press illumination 只在 live touch 时出现。
- [ ] 识别成功：success ripple 正常。
- [ ] 识别失败/松手：pressed carrier 与 HDR 正确复位。
- [ ] 多次连续认证后无异常 HBM、无 fingerprint carrier 残留。

## 3. 压力与生命周期矩阵

- [ ] 至少 20 次无手指 LS ↔ AOD / wake ↔ sleep 循环，无 SystemUI restart、无 FOD 高亮残留。
- [ ] 连续跨至少 5 次分钟变化，时钟和 contextual content 无冻结/累计偏移。
- [ ] media start → pause → resume → track change → stop 多轮切换。
- [ ] 通知连续新增/移除/overflow 收缩，AOD content 不残留旧状态。
- [ ] Forecast/contextual 在有效时间窗边界切换时不出现双行、锁屏泄露或横移。
- [ ] 充电插拔与电量文案刷新。
- [ ] 设置页关键开关修改并重启 SystemUI 后持久化。

## 4. Runtime / health gate

每轮物理矩阵结束后检查：

- [ ] 没有新的 FATAL EXCEPTION。
- [ ] 没有 ANR。
- [ ] 没有非预期 SystemUI PID 重启。
- [ ] 没有重复 primary clock/UDFPS owner。
- [ ] 无持续异常 warning/error 表明 hook 循环或 surface 泄漏。
- [ ] AOD/锁屏切换后 Window/Surface 生命周期收敛。

## 5. Soak gate

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
