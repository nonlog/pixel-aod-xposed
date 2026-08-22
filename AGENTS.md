# AGENTS.md

## 适用范围与优先级

- 本文件是 Pixel AOD for OPlus 仓库级统一工作规则，适用于所有活跃分支与 worktree 中的自动化助手/agent。
- 用户当前会话中的明确指令优先于本文件；其它工具自己的说明文件只能补充工具用法，不得覆盖本文件或用户明确要求。
- 当前执行模式为直接执行：负责本轮任务的助手自行完成定位、修改、测试、构建、安装、设备验证和审查。除非用户明确要求，不再额外委托独立实现型 agent。

## AGENTS.md 管理规则

- **活跃 worktree 的 `AGENTS.md` 应保持逐字一致，不按分支维护不同规则。** 分支差异、阶段进度和设备状态不属于本文件。
- 本文件只保存跨分支长期有效的工程、安全、验证和交付规则。禁止写入当前 branch/worktree 路径、SystemUI PID、ADB 当前 serial、当前 APK hash、当前阶段编号、下一步任务或其它易过期状态。
- 当前工作状态写入对应 worktree 的 gitignored `.local/CHATGPT_WEB_HANDOFF.md`；架构/里程碑事实写入 tracked `docs/`。
- 修改本文件时，应把同一内容同步到所有仍在使用的已注册 worktree。历史 commit 中旧版本无需改写历史；任何历史分支重新投入开发前，先同步最新仓库级 `AGENTS.md`。
- 当统一规则最终提交并合入集成分支后，以集成分支/`master` 中的仓库根 `AGENTS.md` 作为持久 canonical source。未提交阶段以用户最近明确批准的统一工作副本为准。

## Worktree 与 Git 安全

- 开始任务时先确认当前实际仓库、branch 与 worktree：`git rev-parse --show-toplevel`、`git status --short --branch`；涉及多个 worktree 时再看 `git worktree list`。
- 除非用户明确要求切换，所有读写、构建、安装和验证都留在本轮任务已经选定的 worktree；不要根据旧文档中的硬编码路径自行跳到另一个 worktree。
- 不得为了“同步状态”把一个 worktree 的脏代码复制、搬运或 checkout 到另一个 worktree。
- 不得 `reset`、`clean`、`stash`、revert 或 checkout 丢弃用户已有改动，除非用户明确授权该具体操作。
- **已验证的独立开发阶段应及时 commit。** 当一个阶段的定向测试、构建和必要设备/行为验证已经完成，并且没有已知阻塞时，当前助手可以直接创建本地 checkpoint commit，无需用户逐次批准；commit 只包含该阶段及必要文档/规则变更，不夹带无关脏改动。
- **开发分支应及时 push 到对应远程分支作为异地备份。** 普通 fast-forward push 到当前开发分支无需用户逐次批准；push 后报告 branch 和 commit hash。
- 半完成、已知 broken 或仍在调查中的状态默认保持未提交；若长任务确需保存，可创建明确标记的 WIP commit，但不得把 WIP 合入稳定分支。
- **以下高影响 Git 操作仍必须获得用户明确授权：** merge/rebase/cherry-pick 到 `master` 或其它稳定集成分支、直接 push 到 `master`、force-push、重写/删除历史、删除分支/tag、创建正式 release/tag、回滚已共享历史。
- 禁止为了制造“漂亮历史”而事后伪造多个阶段 commit。旧阶段已经混在同一脏 worktree 时，先完整审计并建立一个真实的当前状态 checkpoint；从之后的新阶段开始按阶段正常 commit。

## 执行流程

### 1. 先定位，再修改

- 修 bug 时优先读取模块日志、LSPosed 持久日志、`logcat`、相关通知记录、用户给出的时间点/截图，再决定改法。
- 调查 Pixel AOD 运行时问题时，不得只依赖 `adb logcat` 当前环形缓冲区；同时检查 `/data/adb/lspd/log/modules_*.log`，尤其是用户给出具体时间点时，按时间窗口提取 `PixelAodOPlus` / `dev.codex.pixelaod` 相关行。
- 用户要求查看设备某时刻截图时，除非另有说明，默认指手机 `Pictures/Screenshots/`；拉取到 `D:\Downloads\Xposed_test\screenshots` 后再查看。
- 当前 worktree 没有本地 `.codegraph/` 索引时，不使用外层 worktree 的 codegraph 结果；优先使用 `rg` / `Select-String` / `git diff` / 直接读取源码。若 codegraph 指向其它 worktree 或外层索引，立即停止使用。

### 2. 多步骤任务先给简洁计划，再直接执行

- 多步骤 bugfix、功能修改、重构或多文件任务，先在聊天中说明简洁计划，再开始实际操作。
- 用户已经明确说“修复”“实现”“继续”“就这样做”或给出明确实施方案时，视为已经批准，不再重复等待确认。
- 用户明确要求“只解释”“只 review”“不要改代码”时保持只读。
- 新证据推翻原判断时，应在当前助手内重新定位并修正方案，不把开放式根因调查转交给另一个实现型 agent。

### 3. 版本号与更新日志

- 产出可安装构建、交付用户测试、准备提交，或用户明确要求提升版本/写更新日志时，同步维护：
  - `app/build.gradle`：`versionCode`、`versionName`
  - `app/src/main/resources/META-INF/xposed/module.prop`：`versionCode`、`version`
  - `CHANGELOG.md`：文件顶部的新版本条目
- `app/build.gradle` 与 `module.prop` 的版本字段必须一致。
- **用户可见 `versionName` 与 Android 内部 `versionCode` 完全分离。** `versionCode` 只负责升级排序，不得从内部 9000 系列推导出 `0.1.9000`、`0.1.9001` 之类可见版本。
- 预 `0.2` 阶段使用正常、人类可读的语义版本序列，例如 `0.1.9`、`0.1.10`、`0.1.11`；内部 `versionCode` 只需保持单调递增。
- 设置页、release 标题、CHANGELOG 标题和面向用户的版本描述只显示 `versionName`。不得显示 `0.1.9 (9000)`、`0.1.9（9000）` 或其它把 `versionCode` 拼到版本名后的括号 build 号。技术诊断确有需要时，可将 `versionCode=...` 作为独立字段单列。
- 全部 Grill 已接受决策对应的实现阶段完成并通过最终回归后，正式进入 `0.2.0`；内部 `versionCode` 继续单调递增，不影响可见版本命名。
- CHANGELOG 每个版本条目至少写明：执行模型/助手、修改目标、已验证成功项、未完成/推迟项，避免后续重复踩坑。

### 4. 构建、安装与用户验证

- 使用项目 wrapper：`.\gradlew.bat :app:assembleDebug`。
- 当前已验证 JDK 17：`D:\Programs\Scoop\apps\temurin17-jdk\current`。最终/重要构建应显式为当前进程设置 `JAVA_HOME`，避免复用错误的 JDK 21 Gradle daemon。
- 当前 Android SDK：`D:\Android\Sdk`。如果 AgentDock 进程未继承 SDK 环境，为当前命令设置：
  ```powershell
  $env:JAVA_HOME='D:\Programs\Scoop\apps\temurin17-jdk\current'
  $env:ANDROID_HOME='D:\Android\Sdk'
  $env:ANDROID_SDK_ROOT='D:\Android\Sdk'
  .\gradlew.bat --no-daemon ...
  ```
- 不要为解决临时 SDK 发现问题而复制/新建另一份 `local.properties`；优先使用当前 worktree 已有配置或进程环境变量。
- 安装优先覆盖安装：`adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`；除非用户明确要求，不先 uninstall。
- 只有运行时代码/Xposed 元数据需要重新载入时才重启 `com.android.systemui`；纯文档改动不制造无意义的 SystemUI 重启。
- 自动化日志/测试可以证明技术条件，但缺少必要物理观察时不要擅自宣布用户视觉 bug 已完全修复。

## 设备连接与私密信息

- 安装、抓日志、录屏或其它可能改变设备状态的 adb 操作前，先运行 `adb devices -l`。
- 用户显式指定 serial 时，本任务所有 adb 命令都固定使用 `-s <serial>`。
- 用户未指定时：
  - 只有一个 `device` 状态 transport：选择它并在后续固定 serial。
  - 有多个 transport：用 `ro.serialno`、model/device/build fingerprint 判断是否同一物理设备；无法证明时不要随机选，报告列表并等待用户指定。
  - 已确认同一物理设备时，优先级：USB/有线 > 局域网无线 > FRP/loopback。
- `127.0.0.1:15556/15557` 只是本项目 FRP 兜底端点；已有更高优先级连接时不得主动切过去。兜底 connect 失败一次后不要盲目重试。
- 一旦本任务选定 transport，后续安装、日志、录屏、SystemUI 重载和验证都保持同一 serial，除非连接失效或用户明确要求切换。
- 设备 PIN 只允许在用户明确授权的测试中，从当前 worktree gitignored 的 `.local/device-pin.txt` 读取；不得回显、写入日志/文档/diff/Telegram/Claude-mem，也不得用它修改设备安全设置。
- Token/API key/password/private key 等秘密不得写入仓库、命令输出、handoff、Telegram 内容或 Claude-mem。

## Telegram 阶段通知

- 对实际执行的多步骤任务，当前助手负责 Telegram 阶段通知。
- 开始任务、完成重要阶段、遇到真实阻塞、任务结束时发送简短中文进度；不要按每条命令刷屏。
- 每条通知只说明：完成了什么、结果如何、下一步是什么；异常时再增加阻塞/风险。
- 优先使用现有共享 helper：`D:\Downloads\Xposed_test\pixel-aod-coui-port\.local\send-stage.ps1`。不要读取、打印或展开 helper 内部 secret。
- 如必须直接读取私密配置，优先当前 worktree `.local/secrets.env`，再使用已知共享私密配置位置；token 不得出现在命令输出、日志、diff、checkpoint 或记忆中。
- Telegram 通知失败最多合理重试一次；仍失败则继续主任务，并在聊天说明通知失败。

## 项目概况

- 项目名：Pixel AOD for OPlus
- 包名：`dev.codex.pixelaod`
- 类型：Android Xposed/LSPosed 模块
- 语言：Kotlin + Java
- 构建：Gradle（`compileSdk 36`, `minSdk 26`）
- UI：Jetpack Compose（BOM `2024.10.00`）

## 本机开发环境

| 工具 | 当前已验证路径/方式 |
|---|---|
| JDK 17 | `D:\Programs\Scoop\apps\temurin17-jdk\current` |
| Android SDK | `D:\Android\Sdk` |
| build-tools | `D:\Android\Sdk\build-tools\36.1.0`（另有 35.0.0） |
| platform | `D:\Android\Sdk\platforms\android-36` |
| ADB | 通过当前 PATH / Android platform-tools；实际使用前以 `adb version` / `adb devices -l` 为准 |
| Gradle | 项目自带 wrapper |

## LSPosed / Modern Xposed 约束

- 现代入口：`META-INF/xposed/module.prop` + `META-INF/xposed/java_init.list`。
- `module.prop` 保持 `minApiVersion=101`、`targetApiVersion=101`、`staticScope=true`。
- 当前最小静态作用域固定为 `com.android.systemui`；旧 `staticScope=false` 只属于历史升级来源。
- 不得将 `io.github.libxposed.*` 实现类打包进 APK，只使用 `compileOnly` stubs。
- 出现“模块未识别/未注入/安装后异常”时，先检查最终 APK 的 `META-INF/xposed/*` 和版本元数据，再判断框架问题。

## M9 / Android 17 parity 不变量

- Android 17 parity Grill 已完成 Q1-Q65。没有新证据表明存在新的用户可见架构选择时，不开 Q66。
- M9 parity/adapter 工作优先修改输入、authority、lifecycle 边界，不为“更像 AOSP”重写稳定动画核心。
- 最高优先级：不得破坏已经稳定的 Lockscreen <-> AOD transition、weight interpolation、Large <-> Small morph、glyph/colon continuity 和正常 1x 节奏。
- 用户报告真实回归时，先停止叠加新功能并修复回归。
- 保持已确认的 OOS 锁屏通知兼容修正：SystemUI 仍是一般可见性/隐私 authority，但不得再次破坏“解锁后再熄屏仍保留合格锁屏通知”的窄修复。

## 会话连续性与本地状态

- `.local/CHATGPT_WEB_HANDOFF.md` 是每个 worktree 自己的本地恢复摘要；用于记录当前 branch/worktree、不可破坏约束、已验证阶段、脏/新增文件、测试/build/device 状态、未解决问题和下一步。
- `.local/CHATGPT_WEB_HANDOFF.md` 必须保持 gitignored；不同 worktree 可以且应该有不同 handoff，不要把其瞬时状态复制进 `AGENTS.md`。
- 新会话恢复顺序：仓库根 `AGENTS.md` -> 当前 worktree `.local/CHATGPT_WEB_HANDOFF.md`（如存在）-> 相关 tracked `docs/` / Claude-mem durable context（如可用）-> 实时 `git status/diff` 与设备状态。实时磁盘状态优先于旧摘要/记忆。
- Claude-mem 只保存长期有效决策、约束和已经验证的阶段结论；不要把大段瞬时日志或秘密写进去。
- `.local/secrets.env`、`.local/device-pin.txt` 等本地私密文件必须保持 gitignored。
