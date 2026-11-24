# Pixel AOD for OPlus

简体中文 | [English](README.md)

一个基于现代 **libxposed**（`API 101`）的 Xposed 模块，为 **OPPO / 一加 / ColorOS / OxygenOS** 设备提供原生 **Google Pixel 风格息屏显示（AOD）与锁屏时钟**，同时尽量保留系统原生的显示、指纹光学/HBM 与功耗管理机制。

## 功能特性

### Pixel 时钟与可变字体
- **Google Sans Flex 可变字体形变**：支持锁屏粗体与息屏细体之间的平滑过渡动画，已适配 **ColorOS 16** 与 **ColorOS 17（Android 17）**。
- **丰富的时钟外观自定义**：
  - 独立调节 **AOD 字重** 与 **锁屏字重**
  - **时钟字宽**（`wdth`，`75–125`）与 **时钟圆角**（`ROND`，`0–100`）
  - **锁屏时钟配色**：跟随壁纸动态取色（Material You / Monet）或纯白
- **深度集成系统 `ClockPlugin` 宿主**：接管锁屏与 AOD 时钟呈现，减少切换闪烁与错位。

### At a Glance 与天气
- **多数据源天气引擎**：
  - 支持 **自动 / 系统天气 / Breezy Weather** 切换
  - **系统天气**同时适配 **大陆版 ColorOS**（`com.coloros.weather.service` / `com.coloros.weather2`）与 **国际版 OxygenOS / ColorOS Export**（`net.oneplus.weather`），并带有原锁屏天气文本镜像兜底
- **天气图标**：内置全套 **Google Weather** 彩色天气图标包（12 种状态），也支持调用第三方图标包（如 Breezy Weather 图标提供器）。
- **明日预报**：支持在自定义时间段内显示次日天气预报。
- **天气预警**：支持显示 Breezy Weather 的有效气象预警。
- **日历日程**：显示未来 24 小时内的下一条日程，支持自定义日历图标来源应用。

### AOD 通知图标、媒体与通知卡片（Peek）
- **单色 AOD 通知图标与媒体信息**：支持显示单色通知图标及溢出计数（`+N`）、当前播放媒体标题/艺术家/应用图标及超时清理。
- **系统图标与流体云（Live Alert）适配**：
  - 适配 ColorOS 17 **手电筒**（在流体云接管而不发常规通知时通过相机 Torch 状态自动补全 AOD 图标）、**闹钟/计时器**、**录屏**、**录音**、**通话中**、**VPN**、**投屏**、**USB 连接** 与 **热点** 等状态图标。
- **兼容 `com.fankes.coloros.notify`**：与 `com.fankes.coloros.notify` 提供的单色状态栏图标共存无冲突，可在 AOD 直接复用其单色图标，并内置像素饱和度校验（卸载该模块后也不会将彩色原生图标误染成纯白剪影）。
- **Pixel Ambient 通知卡片（Peek）**：可选开启，在纯黑 OLED 背景上以原生 Pixel 风格展示新通知（单色小图标 + 应用名 + 标题 + 正文）。
- **锁屏通知兼容修复**：防止 OPlus 系统在解锁后再次息屏时误隐藏锁屏通知。

### 屏下指纹（UDFPS）与 AOD 行为
- **指纹视觉与动效**：可选开启 Pixel 风格屏下指纹图标、**按压 HDR 高亮**、**解锁成功 Monet 全屏涟漪** 与 **AOD 退出平滑淡出**，同时保持系统原生指纹识别与光学调度。
- **省电模式增强与口袋模式**：
  - 遵循系统原生 AOD 显示模式（省电 / 定时 / 全天），并支持在省电模式下开启 **新通知短暂亮起 AOD** 与 **充电期间保持 AOD**。
  - 支持基于距离传感器的 **口袋模式**，以及 **已解锁直接息屏进入 AOD** 的过渡动画选择（平滑过渡 / 直接显示）。

### Material 3 Expressive（M3E）模块界面
- 采用 **Android 16 动态取色（Dynamic Color）**、Tonal 层级卡片、概览仪表盘、分段列表、带对勾滑块的 M3E 开关以及悬浮胶囊底栏。
- 支持 **跟随系统 / 中文 / English** 语言切换与一键 **重启 SystemUI**（需 Root）。

## 兼容性

- **系统要求**：Android 8.0+（API 26+），主要针对 **OPlus / 一加 ColorOS 16 / 17（Android 16 / 17）** 开发与实机测试。
- **Xposed 框架**：支持现代 libxposed API `101` 的 **LSPosed** 或 **Vector**。
- **作用域**：已声明 `staticScope=true`，仅作用域 `com.android.systemui`（受支持的管理器无需手动勾选作用域）。

> **提示**：不同机型与系统版本的 SystemUI 实现存在差异，在非目标 OPlus/ColorOS 版本上请谨慎使用。

## 构建与安装

### 环境要求
- JDK 17
- Android SDK Platform 36 与 Build Tools 36.1.0

### 本地构建 Debug 包
```bash
./gradlew :app:assembleDebug
```
输出路径：`app/build/outputs/apk/debug/app-debug.apk`

### 自动发布 Release
推送新版本提交到 `main`（同步更新 `app/build.gradle` 与 `app/src/main/resources/META-INF/xposed/module.prop` 中的 `versionName` / `versionCode`）或推送 `v*` 标签后，GitHub Actions（`.github/workflows/release.yml`）会自动执行单元测试、Android Lint、签名打包并发布 GitHub Release。

### 安装
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
在 LSPosed / Vector 中启用模块后重启 SystemUI（或重启手机）即可生效。

## 诊断与日志

在模块设置中开启 **调试日志** 后，可通过 ADB 查看输出：
```bash
adb logcat -s PixelAodOPlus
adb shell su -c 'grep -E "PixelAodOPlus|dev.codex.pixelaod" /data/adb/lspd/log/modules_*.log'
```

## 开源协议

本项目暂未指定开源协议。在仓库所有者明确添加协议前，请勿擅自二次分发或复用源码。
