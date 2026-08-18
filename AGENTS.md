# AGENTS.md

## 适用对象

- 本文件约束 Codex。
- Claude Code 使用同目录下的 `CLAUDE.md`。

## 分支与工作区

- Codex 的默认开发分支是 `agent/codex`。
- 除非用户明确要求操作其他分支或其他 worktree，否则所有代码改动、构建、安装、提交都只在 `D:\Downloads\Xposed_test\pixel-aod-codex` 中进行。
- 未经用户明确许可，不得向 `master`、`agent/antigravity`、`agent/claude` 或任何其他分支提交。
- 如果用户要求查看其他分支，默认只读；只有用户明确要求同步、合并、cherry-pick、修复或提交时，才允许在对应分支动手。

## 执行流程

### 1. 先定位，再修改

- 修 bug 时，优先读取模块日志、LSPosed 日志、`logcat`、相关通知记录或用户指定时刻的截图，再决定改法。
- 调查 Pixel AOD 运行时问题时，不得只依赖 `adb logcat` 当前环形缓冲区；必须同时检查 LSPosed 持久化模块日志 `/data/adb/lspd/log/modules_*.log`，尤其是用户给出具体时间点时，优先从该目录按时间窗口提取 `PixelAodOPlus` / `dev.codex.pixelaod` 相关行。
- 当用户要求查看设备上某时刻的截图时，除非用户另有说明，默认指已连接手机的 `Pictures/Screenshots/`；将目标截图拉取到 `D:\Downloads\Xposed_test\screenshots` 后再查看。

- 在 `D:\\Downloads\\Xposed_test\\pixel-aod-codex` 没有本地 `.codegraph/` 索引前，不要使用 codegraph 查询该项目；优先使用 `rg` / `sed` / `git diff` 读取当前真实文件。如果 codegraph 提示结果来自 `D:\\Downloads\\Xposed_test` 外层 worktree，立即停止使用 codegraph，避免 300 秒超时。

### 2. 先给计划，再开始实现

- 对多步骤 bugfix、功能修改、重构或任何会改动多个文件的任务，先给出简洁计划，再开始写代码。
- 如果用户已经明确说“修复”“实现”“就这样做”“继续”或直接给出明确实施方案，例如 `PLEASE IMPLEMENT THIS PLAN`，视为已经批准执行，不要再次等待确认。
- 如果用户明确说“只解释”“只 review”“不要改代码”，则保持只读。

### 3. 版本号与更新日志必须写清楚

- 当任务会产出可安装构建、交付用户测试、准备提交，或用户明确要求“提升版本号 / 写更新日志”时，必须同步更新以下文件：
- `app/build.gradle`：`versionCode`、`versionName`
- `app/src/main/resources/META-INF/xposed/module.prop`：`versionCode`、`version`
- `CHANGELOG.md`：在文件顶部添加新版本条目
- `app/build.gradle` 与 `module.prop` 中的版本号必须保持一致，不允许只改其中一处。
- **CHANGELOG 条目必须写明（每次写更新日志都要遵守）：**
  1. **修改模型**：哪个 AI/代理完成了本次改动（例如 Grok / Claude Code / Codex）。
  2. **修改了哪些问题**：简要列出目标 bug/功能。
  3. **哪些成功了**：用户确认或日志验证通过的项，标注 Success。
  4. **哪些失败/暂存了**：未修好、部分修好或明确推迟的项，标注 Deferred/Failed，避免下次重复踩坑。

### 4. 构建、安装、交给用户验证

- 构建使用项目自带 wrapper：`./gradlew :app:assembleDebug`
- 在隔离 Luna worktree 中，每次执行 Gradle 前都必须仅为当前进程设置 SDK 环境变量：
  ```powershell
  $env:ANDROID_HOME='D:\Android\sdk'
  $env:ANDROID_SDK_ROOT='D:\Android\sdk'
  .\gradlew.bat ...
  ```
  禁止先运行未设置这些变量的裸 Gradle 命令，也不要为了隔离 worktree 的 SDK 发现而创建或复制 `local.properties`。
- 安装一律优先使用覆盖安装：`adb install -r app/build/outputs/apk/debug/app-debug.apk`
- 除非用户明确要求卸载，否则禁止先 `uninstall` 再安装。
- 安装后重启 `com.android.systemui`，然后告知用户“已可测试”。
- 不要擅自宣布 bug 已修复；最终行为是否正确，由用户在设备上验证。

### 5. 提交规则

- 未经用户明确要求，不得提交代码。
- 用户要求提交时，只提交本次任务直接相关的改动，并在回复中说明 commit hash。

## 设备连接

- 安装、抓日志、录屏或执行任何可能改变设备状态的 `adb` 操作前，先运行 `adb devices -l`，确认当前在线 transport。
- **用户显式指定的序列号优先级最高。** 一旦用户指定 `<serial>`，本次任务所有 `adb` 命令都必须显式使用 `-s <serial>`，不得因为下面的自动优先级切换到其他 transport。
- 用户未指定序列号时：
  - 只有一个 `device` 状态的在线 transport：直接使用它，并在后续命令固定 `-s <serial>`。
  - 有多个在线 transport：先分别用 `adb -s <serial> shell getprop ro.serialno`，并结合 `ro.product.model` / `ro.product.device` / build fingerprint 判断它们是否确实是**同一台物理设备**。无法证明是同一台设备时，不得随机选择，直接向用户报告实际列表并等待指定。
  - 若确认多个 transport 属于同一台物理设备，自动选择优先级固定为：**USB/有线 > 局域网无线（非 loopback 的 `host:port`）> FRP/loopback 无线（`127.0.0.1:<port>`）**。
- `127.0.0.1:15556/15557` 是本项目的**FRP 兜底重连端点，不是全局首选序列号**。只有在目标设备没有任何可用的更高优先级在线 transport、且 `127.0.0.1:15556/15557` 本身未在线或为 `offline` 时，才执行**一次** `adb connect 127.0.0.1:15556/15557`，然后重新运行 `adb devices -l`。如果同一物理设备已经通过 USB 或局域网无线在线，不得为了命中该端点而主动切换到 FRP。
- 这一次显式 `adb connect 127.0.0.1:15556/15557` 后仍为 `offline`、`unauthorized`、未出现在线设备，或出现多个不同物理设备且用户未指定序列号时，不要盲目重试；把实际状态告诉用户。
- 一旦本次任务选定 transport，后续安装、日志、录屏、SystemUI 重载和验证都固定使用同一个 `-s <serial>`，除非该 transport 失效或用户明确要求切换；失效时重新按上述规则选择。

## Luna Worker 阶段 Telegram 通知

- 本节约束实际执行任务的 **Codex CLI `gpt-5.6-luna` worker**；ChatGPT Web supervisor 不负责代发阶段通知。
- worker 在开始一个任务时先把工作划分为少量、可验证的阶段（例如：基线检查 → 实现 → 定向回归测试与构建 → 设备安装与运行验证 → 证据整理与阶段审查）。不要把每条 shell 命令、每个测试类拆成独立阶段。
- **阶段通知必须使用单一自然语言，禁止中英夹杂。默认使用中文；只有用户明确要求英文时才整条使用英文。** 命令、类名、方法名、分支名、hash、错误码等无法自然翻译的技术标识可以原样保留，但说明这些标识的句子必须保持同一种语言。
- **阶段通知必须写“人话”，面向用户说明进度，而不是复制内部计划或工程术语堆砌。** 每条只回答三件事：刚完成了什么、结果是否正常、接下来要做什么。能用“定向回归测试”“构建安装包”“设备连接”“等待审查”说明时，不要写 `focused regressions`、`build gate`、`transport selection`、`review gate` 这类中英混杂内部表达；不要把测试 filter 数量、内部状态机名或 gate id 塞进普通阶段通知，除非它们对理解阻塞原因确有必要。
- **每完成一个阶段、进入下一阶段之前，必须向 Telegram 发送一条消息。** 中文推荐格式：`Pixel AOD｜<任务>：已完成<阶段>。结果：<一句通俗结论>。下一步：<下一阶段>。`
- 如果某阶段被真实错误、权限、设备状态或需要用户决策的问题阻塞，在停止/等待前发送中文人话：`Pixel AOD｜<任务>：<阶段>被阻塞。原因：<通俗原因>。下一步：<动作或等待项>。` 同一阶段内部的普通可恢复重试不要刷屏。
- Telegram token 只能从私密 env 文件读取，变量名为 `TELEGRAM_BOT_TOKEN`；chat id 优先读取 `TELEGRAM_CHAT_ID`。查找顺序：当前 worktree 的 `.local/secrets.env` → `D:\Downloads\Xposed_test\pixel-aod-coui-port\.local\secrets.env`。不得把 token 写入 `AGENTS.md`、源码、命令输出、日志、diff、checkpoint、Claude-mem 或 git。
- 当前项目的目标 chat id 为 `1690630220`；若私密 env 中没有 `TELEGRAM_CHAT_ID`，可使用该值，但仍不得回显 bot token。
- 发送请求时在运行时从环境变量构造 Telegram Bot API 请求，不得把展开后的 token 打印到终端。通知失败最多做一次短重试；仍失败时记录“Telegram stage notification failed”并继续任务，不能因此让实现/验证卡死。



## 用户直接执行覆盖 — 2026-08-18

- 用户已明确终止本项目的 Sol→Luna 实现委托。自本条起，ChatGPT Web Sol 直接负责定位、修改代码、测试、构建、设备验证、M5 静态作用域迁移与 M6 设置 UI 重构；不得再启动、resume、prompt、send-keys 或以其他方式调用 Luna/Codex executor，除非用户之后再次明确授权。
- 本覆盖优先于下文所有“唯一 Luna executor / Web supervisor 只审查”的旧工作流条款；旧条款仅作为历史记录保留，不再约束当前执行方式。
- 用户已明确授权当前任务连续推进至：COUI 行为对齐完成 → M5 LSPosed static scope 迁移 → M6 `PixelAodDesignSystem` + 全设置页面 UI 重构。此前 roadmap 中“稳定后再开始 M5/M6”的阻塞条件被本次用户指令覆盖，但每阶段仍必须完成定向测试、构建和可行的运行验证后再进入下一阶段。
- Sol 每完成一个有意义阶段后，直接使用现有 Telegram 私密配置发送一条纯中文、人话阶段通知；不得回显任何私密配置内容。通知只说明：完成了什么、结果如何、下一步是什么。
- 仍保持：不 reset/clean/revert 用户脏改动；不 commit/push，除非用户另行明确要求；不随意重启手机或 ADB server。

## 项目概况

- 项目名：Pixel AOD for OPlus
- 包名：`dev.codex.pixelaod`
- 类型：Android Xposed 模块
- 语言：Kotlin + Java
- 构建：Gradle（`compileSdk 36`, `minSdk 26`）
- UI：Jetpack Compose（BOM `2024.10.00`）

## 本机开发环境

以下路径是本机已知可用环境；构建或安装失败时优先核对这里：

| 工具 | 路径 | 备注 |
|---|---|---|
| JDK 17 | `D:\\enviroment\\jdk-17.0.1` | `JAVA_HOME` 已设置 |
| Android SDK | `D:\\Android\\sdk` | `local.properties` 已配置 `sdk.dir` |
| build-tools | `D:\\Android\\sdk\\build-tools\\36.1.0` | 另有 `35.0.0` |
| platforms | `D:\\Android\\sdk\\platforms\\android-36` | |
| ADB | `D:\\enviroment\\ADB\\adb` | v1.0.41 |
| Gradle | 使用项目自带 `./gradlew` | 不依赖全局 Gradle |

## Vector / LSPosed 相关约束

- 现代入口使用：
- `META-INF/xposed/module.prop`
- `META-INF/xposed/java_init.list`
- `module.prop` 需保持：
- `minApiVersion=101`
- `targetApiVersion=101`
- `staticScope=true`；当前 `pixel-aod-coui-port` 的最小静态作用域固定为 `com.android.systemui`，与 M5 已实施状态一致。旧版 `staticScope=false` 只作为升级来源，不再是当前构建约束。
- 不得将 `io.github.libxposed.*` 实现类打包进 APK，只能使用 `compileOnly` stubs。
- 如果出现“模块未被识别”“未注入”“安装后行为异常”等问题，优先检查最终 APK 中的 `META-INF/xposed/*` 和版本元数据是否正确，再判断是否是框架问题。

## `pixel-aod-coui-port` Worktree 覆盖（高优先级）

- 当当前仓库为 `D:\Downloads\Xposed_test\pixel-aod-coui-port` 或当前分支为 `agent/coui-port` 时，本节覆盖上文 `agent/codex` / `pixel-aod-codex` 的默认工作区规则：所有本任务读写、构建、测试和设备验证都留在当前 `pixel-aod-coui-port` worktree，除非用户明确要求切换。
- 不得因为旧的默认 worktree 文案把 M1/M2 脏改动搬回 `pixel-aod-codex`，也不得 reset/clean 用户现有改动。

## ChatGPT Web 会话连续性与本地私密配置

本节用于防止 ChatGPT Web 会话达到硬上限后丢失项目进度；它与 Codex CLI 自己的上下文窗口无关。

- **不要管理 Codex CLI 的上下文大小。** Codex 可以自行 compact；不得因为 Codex token/context 使用量暂停、杀掉、重开或切换正在工作的 executor。
- **真正需要防护的是 ChatGPT Web 会话。** 如果宿主暴露上下文使用率/剩余量/接近上限提示，则持续监控：约 70% 起提高 checkpoint 频率，约 80%–85% 或出现明确上限预警时，在最近一个可验证安全边界完成 handoff 并切换新 Web 会话；不要等到硬上限。精确百分比不可见时，不得假装能感知，改用下面的阶段性 checkpoint 机制。
- ChatGPT supervisor 的单一权威恢复文件是 `.local/CHATGPT_WEB_HANDOFF.md`。至少在以下时机原子刷新：每个已验证阶段/vertical slice 结束、重大用户决策后、启动/更换唯一 executor 后、长时间设备验证前后、以及准备进入会产生大量日志/diff 的步骤前。
- handoff 必须记录：当前 branch/worktree、目标与不可破坏约束、已经**验证**的里程碑及证据、当前唯一 Codex thread/session 名称与 ID、脏/新增文件、测试/build/device 状态、未解决问题、下一步精确动作，以及一段可直接交给新 Web 会话的 continuation prompt。
- 新 ChatGPT Web 会话恢复顺序：读本 `AGENTS.md` → 读 `.local/CHATGPT_WEB_HANDOFF.md` → 读取 `pixel-aod-coui-port` 的 Claude-mem durable context → 核对实时 `git status/diff` 与唯一 Codex executor 是否仍在运行 → 以实时状态修正 handoff 中可能过时的信息后继续。不要要求用户重述已 checkpoint 的进度。
- Claude-mem 是第二保险，只保存长期有效的决策、约束和**已验证**阶段结论；不要把大段瞬时日志当持久记忆。**任何 token/API key/password/private key 都禁止写入 Claude-mem。**
- Web supervisor 继续负责决策/审查；实现由**唯一一个**显式 `gpt-5.6-luna` + `model_reasoning_effort=max` 的 Codex CLI executor 执行。传输层自动重连时先让同一 executor 自恢复，不得并行再开第二个实现会话。
- 项目本地私密配置统一放 `.local/secrets.env`，该目录必须被 `.gitignore` 排除。Telegram bot token 使用 `TELEGRAM_BOT_TOKEN=<secret>`；需要时从该文件读取，但不得在日志、回复、diff、checkpoint、Claude-mem 或 git 中回显 secret 值。

## Herdr 可见 Codex 执行模式

- Pixel AOD 的默认实现 executor 运行在 Herdr 持久 session `pixel-aod` 中；用户可随时执行 `herdr session attach pixel-aod` 查看同一个终端。
- Herdr pane/agent 当前约定：pane `w1:p1`，agent 名 `pixel-aod-luna`。Codex thread/session id 以 `.local/CHATGPT_WEB_HANDOFF.md` 的最新 checkpoint 和 `herdr --session pixel-aod agent list` 为准，不要猜测。
- 实现模型固定为 `gpt-5.6-luna` + `model_reasoning_effort=max`。Sol/Web supervisor 负责决策、审查和阶段验收；同一时刻只允许一个实现 executor。
- 不再使用不可见后台 `codex exec` 作为日常实现承载，也不要为启动 Codex 修改 Windows Terminal settings、PowerShell profile 或临时 `wt -d` 链。只有 Herdr 本身不可用且用户明确同意降级时，才可临时使用其他承载方式，并必须保证没有并发 executor。
- Herdr 内优先通过 `herdr agent read/wait/prompt` 监控/驱动；不要依据 UI focus 猜 pane。使用显式 session 名、agent 名或 pane id。
- 若 `herdr agent start --kind codex` 在 PowerShell pane 因 Node `codex.cmd` shim 报 `%1 is not a valid Win32 application`，不要改 WT/PowerShell。可在同一 Herdr pane 前台直接运行绝对路径 `codex.exe`，待 Herdr hook 自动识别后用 `herdr agent rename <pane-id> pixel-aod-luna` 命名；仍必须保持同一个可 attach pane。
## Sol/Luna 事件驱动 Supervisor Gate（高优先级）

- 默认工作流是 **Web Sol supervisor → 唯一 Herdr Luna executor → supervisor gate → Web Sol review**。Sol 是技术决策者和 reviewer；Luna 是执行者。不得把 Sol 退化成只转述用户现象、泛泛列 TODO、或让 Luna 自己从头找根因的任务分发器，也不得让 Luna 自行跨过需要 supervisor 审核的阶段边界。
- **Sol 在每次下发新的实现阶段或 BUG correction 前必须先参与定位与决策。** 至少先只读检查与当前问题直接相关的源码/当前 diff、最新日志或物理证据，以及适用的 AOSP/OPlus/COUI/reference 实现；据此给出当前最可能根因、需要保持的 invariant、修改范围和验收条件。能定位到文件/类/方法/状态机/调用时序时，prompt 必须明确写出这些位置和预期改法，而不是只写“调查/修复这个 bug”。
- **Sol 的执行指令必须尽量是 code-level plan。** 对每个问题说明：为什么当前行为会错、应改哪条控制流/状态转换/owner/visibility/alpha/thread/数据语义、哪些路径禁止回退或重引入、应增加什么 targeted test/instrumentation、需要什么 build/device/physical evidence。若证据还不足以唯一确定根因，Sol 也必须先把假设收敛为少量可证伪分支并指定精确探针；不得把开放式 root-cause search 全部甩给 Luna。
- **Luna 只在 Sol 已批准的设计边界内自主执行机械实现、测试、构建和设备取证。** Luna 可以报告新发现并提出替代方案；如果新证据推翻了 Sol 的根因判断，或修复需要改变 primary owner、架构、状态机语义、关键 thread/生命周期策略，Luna 应在安全边界停下并交回 Sol 决策，而不是自行扩大方案。
- **Sol 在每个 supervisor gate 必须做真正的 code/evidence review。** 不能只复述 Luna summary；必须查看本 gate 相关 diff/源码/测试/日志/物理证据，判断实现是否符合之前的 code-level plan、是否引入回归、用户视觉反馈是否推翻日志结论。FAIL/BLOCKED 时应给出具体到代码路径或验证探针的 correction；除非确实缺少必要证据，不得只回复“继续调查”。
- 本 worktree 的本地 bridge 位于 `.local/sol-luna/`（gitignored），权威配置为 `.local/sol-luna/bridge.json`。当前唯一 executor 必须保持 Herdr `pixel-aod` / agent `pixel-aod-luna` / thread `01a00d74-60c3-75a3-be92-5ee930ee926a`，除非用户或 Sol 在明确阶段边界更新 bridge 配置。
- **阶段内不需要 Web 轮询，但这不等于把技术判断委托给 Luna。** Luna 独立执行已经由 Sol 定义清楚的 TDD、实现、build、安装/设备验证和证据收集；Sol 不持续轮询命令级进度。只有用户主动反馈、真实 blocker、executor 异常停止，或到达下面的 gate 时才再次介入并做下一轮判断。
- 到达 material gate（例如 implementation+tests 完成、physical validation 完成、或无法继续的真实 blocker）时，Luna 必须先运行 `.local/sol-luna/emit-gate.ps1` 写结构化事件，至少包含唯一 `event_id`、`stage`、`status`、简洁 summary、changed files、evidence、blockers；事件中禁止出现 token、cookie、password、private key 或其它 secret。
- Luna 随后在该 turn 的**最终回复中单独一行**输出 `SUPERVISOR_GATE:<event_id>`，然后停止推进下一阶段，等待 Sol review 或用户明确 override。不得每个命令/普通 turn 都发 gate。
- 用户级 `~/.codex/config.toml` 的 `notify` 由 `C:\Users\www\.codex\hooks\sol-luna-notify.js` 接管。wrapper 必须先保留既有 OpenAI `codex-computer-use.exe turn-ended` 行为，再只对匹配本 project/thread 且含 `SUPERVISOR_GATE` 的 `agent-turn-complete` 做 bridge 处理；同一 `event_id` 必须幂等去重。
- Sol 收到新 gate 后才读取 `.local/sol-luna/supervisor_event.json`、相关 diff/源码/证据并做 review。PASS：用同一 Herdr Luna thread 下发下一阶段并运行 `.local/sol-luna/ack-gate.ps1 ... -Outcome PASS`；FAIL/BLOCKED：给出精确 correction/阻断处理并 ack 对应结果。不得为 review 启动第二个 executor。
- **浏览器唤醒是 primary transport。** `.local/sol-luna/emit-gate.ps1` 在写完 event/wake 后会异步启动 `.local/sol-luna/browser-wake.ps1`；用户级 Codex `notify` 也会对同一 gate 触发它作为冗余入口。browser bridge 只向 Pixel AOD Web Sol Project chat 注入短 `LUNA_SUPERVISOR_GATE:<event_id>` / bootstrap，不传输完整日志或 diff；本地 event/handoff、Claude-mem、git/Herdr/runtime evidence 才是状态真相。
- **Web 会话自动轮换：** 精确 Web context 百分比不可见时不得猜。`.local/sol-luna/web_session.json` 保存当前 conversation 指针和 generation；默认每 4 个 material gate 或 12 小时主动轮换，若出现高置信 conversation-length failure 则立即轮换。轮换必须直接打开 `Pixel AOD for OPlus` Project landing，并只在其 `New chat in Pixel AOD for OPlus` composer 内创建新 chat；禁止先在 `https://chatgpt.com/` 建 global seed chat 再 Move to project。发送后必须确认最终 URL 是本 project-qualified conversation，才可更新 `web_session.json`。
- 新 Web Sol 的 bootstrap 恢复顺序固定为：`AGENTS.md` → `.local/CHATGPT_WEB_HANDOFF.md` → Claude-mem `pixel-aod-coui-port` → `.local/sol-luna/{bridge.json,supervisor_event.json,sol_ack.json,web_session.json}` → 实时 git status/diff 与同一 Herdr Luna thread → 当前 gate review。不要复制整段旧网页聊天，也不要要求用户重述。
- Browser automation 只能操作已登录 Chrome 的 ChatGPT transport UI；不得导出/复制 cookie。composer 通过 UI Automation 定位；文本输入必须使用 Win32 `SendInput(KEYEVENTF_UNICODE)` 绕过当前键盘布局/中文 IME，并按小块发送、逐块读取 ValuePattern 验证完整前缀，最终全文完全一致后才允许点击 Send。若发现任何预先存在的非空用户 draft，或输入内容与预期存在任何差异，必须 fail-closed，绝不发送/覆盖。browser 失败不能 ack/删除 event。
- `Pixel AOD Sol Gate` ChatGPT condition-watch 是 **fallback only**：先只读小型 bridge 状态。browser delivery=`sent` 后按 `bridge.browser_wake.fallback_grace_minutes`（当前 20 分钟）等待 browser-injected Web Sol ack；只有 delivery=`failed`、超过 grace 仍无 delivery、或 `sent` 超过 grace 仍未 ack 时，automation 才允许读取 AGENTS/handoff、代码、diff、Herdr、日志或设备并自行完成 fallback review。
- 若当前 Codex 进程是在用户级 `notify` 配置落地前启动的，也不影响实时唤醒：Luna 本身运行 `emit-gate.ps1` 就会触发 browser bridge；`notify` 只提供第二触发和未来 resume 后的原生 turn-complete 冗余。
