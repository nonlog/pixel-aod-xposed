# `viettran-edgeAI/codex_workflow` 对本项目的适配评估

调研日期：2026-08-03
外部仓库固定证据版本：[`2df3925136302a2e2dfb06c32543774ce49e5864`](https://github.com/viettran-edgeAI/codex_workflow/tree/2df3925136302a2e2dfb06c32543774ce49e5864)（`main` 当次读取结果）。

## 结论

**有条件地有帮助，但不应原样安装。**

它最值得借用的是重型任务的工作方法：把一次改动限定为小任务包、明确谁实现/谁验证、把失败证据返回原执行者，并以会话交接文档保存长期进度。这个项目的 AOD、锁屏、通知和指纹改动会跨 SystemUI 生命周期，适合这样的边界管理。

但本项目是依赖 OPlus SystemUI、LSPosed/Vector 和真机状态的 Android Xposed 模块；编译或单元测试不能证明屏幕关闭、AOD 进入/退出和动画没有回归。外部工作流的默认配置偏后端和确定性测试，必须把“真机安装、重启 SystemUI、持久 LSPosed 日志、用户视觉确认”加入验收条件，才能使用。

外部仓库当前树只包含工作流说明、路由 Markdown 和角色 TOML，没有 Android/Gradle/ADB/LSPosed/设备验证脚本；它不会直接改善本项目的构建、安装或运行时诊断。[固定版本文件树](https://github.com/viettran-edgeAI/codex_workflow/tree/2df3925136302a2e2dfb06c32543774ce49e5864)

## 外部仓库的具体机制

1. **三条显式路线。**轻量路线默认自行处理；中型路线仍由主代理直接完成；重型路线由主代理协调工作者。路线不会自动切换，须由用户在提示中指定。[README 第 61-77 行](https://github.com/viettran-edgeAI/codex_workflow/blob/2df3925136302a2e2dfb06c32543774ce49e5864/README.md#L61-L77)

2. **重型路线的角色分工与小任务包。**主代理负责计划、边界、整合和关键审查；`explorer` 只读调研，`executor_luna` 实现，`tester` 独立验证，`doc-writer` 维护持久文档。每个任务包须含目标、所有权、验收、相关路径、验证和受保护区域。[heavy route 第 1-4、26-51 行](https://github.com/viettran-edgeAI/codex_workflow/blob/2df3925136302a2e2dfb06c32543774ce49e5864/codex_workflow/heavy_route.md#L1-L4) [第 26-51 行](https://github.com/viettran-edgeAI/codex_workflow/blob/2df3925136302a2e2dfb06c32543774ce49e5864/codex_workflow/heavy_route.md#L26-L51)

3. **执行—验证修复回路。**执行者先做最小相关检查；测试者再做独立覆盖和更宽回归。若是生产缺陷，回到同一执行者修复，之后再给测试者验证；不允许为了通过而弱化验证。[heavy route 第 70-90 行](https://github.com/viettran-edgeAI/codex_workflow/blob/2df3925136302a2e2dfb06c32543774ce49e5864/codex_workflow/heavy_route.md#L70-L90)

4. **跨会话状态。**它建议以 `agent_docs/` 的项目概览、结构、进度、决策记录和最近会话交接保存长期任务状态，并限制何时写进度/交接文件。[AGENTS.md 第 57-74、99-111 行](https://github.com/viettran-edgeAI/codex_workflow/blob/2df3925136302a2e2dfb06c32543774ce49e5864/codex_workflow/AGENTS.md#L57-L74)

5. **并行与共享状态保护。**它要求独立的只读检查可以批量执行，而 Git 写入、相互依赖的编辑、共享构建目录或共享设备的构建/测试必须串行执行。[AGENTS.md 第 23-49 行](https://github.com/viettran-edgeAI/codex_workflow/blob/2df3925136302a2e2dfb06c32543774ce49e5864/codex_workflow/AGENTS.md#L23-L49)

## 与 Pixel AOD 的匹配点

| 外部机制 | 为什么适合本项目 | 应如何落地 |
|---|---|---|
| 有范围的 `explorer` 调研 | 本模块直接挂钩 OPlus 私有 SystemUI；一次变更前常要找类、回调、既有日志与 ROM 差异。项目自身也把 OOS 日志和被 hook 的类视为最终运行时事实。 | 对不熟悉的 OOS 类、LSPosed 日志窗口或外部 API，先派只读调研；只报告类/符号/日志证据，不直接修改行为。 |
| 执行者与验证者分离 | 生命周期竞态很容易出现“代码和单测通过、真机动画回归”。当前 README 明确要求安装后重启 SystemUI，并同时查 `logcat` 和持久 LSPosed 日志。 | 实现者负责代码、`:app:testDebugUnitTest`/构建；验证者只执行串行真机验收：安装、模块 scope、SystemUI 重启、指定屏幕状态转换、日志与截图。 |
| 小任务包与受保护区域 | 当前近几次修改已经将通知交接偏移、图标顺序、设置 UI 分成独立提交；而 AOD 黑帧、Doze、字重动画属于高风险区域。 | 每个包只允许一个表面，例如“通知行 x 偏移”；明确写出“不改 Doze、黑帧、锁屏/AOD 字重动画”，并列出必须观察的状态转换。 |
| 持久交接记录 | 本项目已有 `CHANGELOG.md` 和 `docs/` 的生命周期映射，且同一个现象需要跨多次设备测试。 | 不必立即引入完整 `agent_docs/`；只在持续数天的复杂问题中新增简短的任务状态记录，保留“复现条件、APK 版本、设备/SystemUI、日志时间窗、已验证/未验证”。 |

本项目的事实基础：它针对 OPlus/OnePlus SystemUI、要求在 LSPosed/Vector 中 scope 到 `com.android.systemui`，并明确提示设备和 SystemUI 版本敏感。[本项目 README 第 22-30 行](../README.md#L22-L30) 其调试要求同时保留 `PixelAodOPlus` 的 `logcat` 与 LSPosed 模块持久日志。[本项目 README 第 60-70 行](../README.md#L60-L70) 生命周期文档也说明短暂的 screen-off 入口容易从 ring buffer 消失，必须针对用户报告的时间窗查看持久日志。[OOS_AOD_LIFECYCLE_MAPPING.md 第 25、40 行](OOS_AOD_LIFECYCLE_MAPPING.md#L25)

## 建议采用的最小部分

不要安装外部仓库；从下一次**跨文件或跨设备状态**的改动起，人工采用下列四项即可：

1. 在任务开头写一个不超过一屏的任务包：目标、唯一代码表面、受保护区域、构建检查、真机验收场景。
2. 将只读发现与代码实现分开：先收集 OOS 类/日志/调用链，再动代码；共享设备、Gradle 输出和 Git 操作仍串行。
3. 把验收固定为两层：
   - 构建层：`./gradlew :app:testDebugUnitTest`（有相关测试时）和 `:app:assembleDebug`；
   - 真机层：`adb install -r`、确认 LSPosed/Vector scope、重启 SystemUI、执行指定 AOD/锁屏转换、检查两类日志及视觉结果。
4. 对超过一次会话的故障，记录可复现证据和未验证项；不要把“单测通过”写成“设备行为已确认”。

这保留外部工作流最有价值的边界、证据和交接机制，却不引入新的全局代理定义或强制工作流层。

## 明确不建议采用的部分

1. **不要按其安装指令在本项目自动执行。**安装指南会复制根 `AGENTS.md`、创建一套 `agent_docs/`、覆盖工作流路由和用户目录下已有的五个代理定义，并修改 `~/.codex/config.toml` 的 multi-agent 配置。[setup guide 第 31-146 行](https://github.com/viettran-edgeAI/codex_workflow/blob/2df3925136302a2e2dfb06c32543774ce49e5864/workflow_setup_guide.md#L31-L146) 这会与当前项目/桌面环境的已有指令和代理配置发生覆盖风险。

2. **不要接受其“自动清理”步骤。**该指南最后明确要求删除项目根目录中的 `README.md` 等文件。[setup guide 第 216-229 行](https://github.com/viettran-edgeAI/codex_workflow/blob/2df3925136302a2e2dfb06c32543774ce49e5864/workflow_setup_guide.md#L216-L229) 在本仓库中这将触及真实项目 README，不能自动执行。

3. **不要保留其自动 `git add .` 和提交规则。**重型路线在会话结束时要求对“有意义的项目改动”执行 `git add .` 和自动提交。[heavy route 第 100-115 行](https://github.com/viettran-edgeAI/codex_workflow/blob/2df3925136302a2e2dfb06c32543774ce49e5864/codex_workflow/heavy_route.md#L100-L115) 对有未关联本地改动、APK/截图/日志或多个实验分支的设备项目不安全；应始终按路径或 hunk 人工审查后再提交。

4. **不要原样使用其默认“测试优先”定义。**外部 README 自称主要为后端、且非常偏重测试的默认流程。[README 第 44-55 行](https://github.com/viettran-edgeAI/codex_workflow/blob/2df3925136302a2e2dfb06c32543774ce49e5864/README.md#L44-L55) 本项目的运行时真相是 OOS 生命周期、SystemUI hook 和真机视觉；自动化测试是必要但不充分的门槛。

5. **不要假设其中的模型/多代理配置在当前环境可用。**其角色定义固定写入 `gpt-5.6-luna`，而安装指南要求旧式 `[features.multi_agent_v2]` 的 `tool_namespace = "agents"`。[explorer.toml 第 1-12 行](https://github.com/viettran-edgeAI/codex_workflow/blob/2df3925136302a2e2dfb06c32543774ce49e5864/codex_workflow/explorer.toml#L1-L12) [setup guide 第 114-136 行](https://github.com/viettran-edgeAI/codex_workflow/blob/2df3925136302a2e2dfb06c32543774ce49e5864/workflow_setup_guide.md#L114-L136) 未逐项核对当前 Codex 版本、模型目录和代理 API 前，兼容性没有证据。

## 采纳前提与后续建议

若以后确实要试用完整流程，先在**独立工作树和独立 Codex 配置备份**中做只读比对；不要覆盖 `AGENTS.md`、用户级代理 TOML 或 `config.toml`。同时把 tester 的验收模板改为本项目的双层门槛，并把黑帧/Doze/锁屏到 AOD 的字重动画列为默认受保护区域。

此外，GitHub API 当前对该仓库返回 `license: null`；在未获得作者许可或确认许可证前，不应复制其文本或配置作为可再分发项目资产。[仓库元数据 API](https://api.github.com/repos/viettran-edgeAI/codex_workflow)

## 证据范围

- 外部工作流的机制、安装副作用和模型配置均来自该 GitHub 仓库的 README、安装指南、`AGENTS.md`、路由与代理 TOML，链接已固定到当次读取的 commit。
- 本项目适配判断来自当前仓库 README、生命周期文档和当前开发约束；没有执行外部工作流安装、没有修改 Codex 配置、没有运行设备操作。
