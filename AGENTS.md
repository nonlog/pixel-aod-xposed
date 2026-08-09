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

- 安装或抓日志前先看 `adb devices`。
- 如果设备状态是 `offline`、`unauthorized`、没有在线设备，或存在多个在线设备但用户未指定序列号，不要盲目重试，也不要随机选择设备；直接把实际状态告诉用户。
- 如果用户已经指定设备序列号，则所有 `adb` 命令都必须显式使用 `-s <serial>`。

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
- `staticScope=false`
- 不得将 `io.github.libxposed.*` 实现类打包进 APK，只能使用 `compileOnly` stubs。
- 如果出现“模块未被识别”“未注入”“安装后行为异常”等问题，优先检查最终 APK 中的 `META-INF/xposed/*` 和版本元数据是否正确，再判断是否是框架问题。
