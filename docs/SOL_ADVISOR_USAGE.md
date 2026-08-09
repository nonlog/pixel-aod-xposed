# Sol Advisor 使用说明

本文记录 Pixel AOD for OPlus 项目中的 Sol Advisor 配置和使用约定。

## 作用范围

- Client：Codex
- Scope：project
- Workspace：`D:\Downloads\Xposed_test\pixel-aod-codex`
- 配置只为这个工作区生成项目级原生角色文件，不会修改其他项目的角色配置。
- 配置跨会话保留，但不会自动启用 Sol Advisor，也不会改变主会话在模型选择器中选择的模型。

## 当前角色配置

| 用途 | 模型 | Reasoning | 说明 |
|---|---|---|---|
| Orchestrator | inherit | inherit | 继承当前主会话；建议使用 Sol / High |
| Routine implementation | `gpt-5.6-terra` | `high` | 有界、机械性的实现任务 |
| High-complexity implementation | `gpt-5.6-terra` | `high` | 疑难调试、并发或影响范围较大的实现任务 |
| Advisor/reviewer | `gpt-5.6-sol` | `high` | 行为上只读的架构咨询和最终审查 |
| Optional app-task lane | `gpt-5.6-luna` | `max` | 仅在明确指定 Luna task lane 时使用 |

配置采用 `fail-closed`：指定角色、模型、推理等级或运行时证据不可用时停止，不自动替换成其他模型。

Advisor 的 `readonly: true` 是角色行为约束。只有 Codex 运行时明确报告 `read-only` sandbox，才能称为操作系统强制只读。

## 使用 Terra 原生实现通道

在请求中明确写：

```text
Use $sol-advisor:orchestration.
Use the configured native Terra implementation lane.

<要实现或修复的内容>
```

Sol Advisor 会先读取项目配置，由主会话负责需求、架构、验收和最终判断；Terra / High 负责实际实现。实现完成后，主会话必须检查真实 diff、重新执行验证，并交给新的 Sol / High reviewer 审查。

原生通道不会因为 Terra 不可用而切换到 Luna 或其他模型。

## 使用 Luna / Max app-task 通道

Luna 通道与 Terra 原生角色完全独立，只在当前请求明确授权时启用：

```text
Use $sol-advisor:orchestration.
Use the Luna task lane for this task.

<要实现或修复的内容>
```

它会创建单独、用户可见的 Codex app task。不要把“启用了 Luna 配置”理解为默认使用 Luna；没有上述明确指令时不会进入该通道。

## 在某次请求中禁用 Sol Advisor

直接写：

```text
不要使用 sol-advisor，由当前主会话和我在模型选择器中指定的模型直接处理。
```

项目级角色文件存在并不代表每次任务都会调用它们。未显式调用 `$sol-advisor:orchestration` 时，可以由当前主模型直接工作。

## 常用提示词

疑难 bug，使用 Terra 实现并由 Sol 审查：

```text
Use $sol-advisor:orchestration.
Use the configured native Terra implementation lane.
先读取设备日志和 LSPosed 持久日志，定位根因后修复。不要提交，构建并安装后等待我测试。
```

只做分析，不修改代码：

```text
Use $sol-advisor:orchestration.
只使用 advisor 做只读分析，不修改代码、不构建、不安装。
```

不使用插件：

```text
不要使用 sol-advisor。由当前主模型直接分析和实现。
```

## Pixel AOD 项目的额外约束

- 遵守项目根目录 `AGENTS.md`。
- 先检查模块日志、LSPosed 持久日志、`logcat`、通知记录或用户指定录屏，再修改代码。
- 所有设备命令使用用户指定的设备序列号；存在多个设备时不得随机选择。
- 可安装构建必须同步更新 `app/build.gradle`、`module.prop` 和 `CHANGELOG.md`。
- 未经明确要求不得提交或推送。
- 构建成功不等于设备运行时修复成功，最终视觉和行为结果由用户验证。

## Windows 注意事项

Sol Advisor 0.5.0 的 MCP 服务最初按 POSIX 权限位和目录 `fsync` 校验数据目录。Windows/Bun 不提供相同语义，本机插件缓存已做最小兼容处理：Windows 使用主机管理的 NTFS ACL，并跳过不受支持的文件/目录 `fsync`；POSIX 平台仍保留原安全检查和同步行为。

插件更新可能覆盖缓存中的兼容处理。如果再次出现以下错误，需要先检查插件版本和 Windows 兼容状态：

```text
PLUGIN_DATA must be private (no group/world permission bits)
EPERM: operation not permitted, fsync
```
