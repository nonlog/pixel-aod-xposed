# Pixel AOD for OPlus

简体中文 | [English](README.md)

为 ColorOS / OxygenOS 设备带来 Google Pixel 风格息屏显示（AOD）与锁屏时钟的 Xposed 模块。

## 主要功能

- **Pixel 时钟与字体**：Google Sans Flex 可变字体时钟，支持锁屏到息屏平滑过渡，可自定义字重、字宽、圆角及动态取色/纯白配色。
- **At a Glance 与天气**：同时支持系统天气（兼容大陆版 ColorOS 与国际版 OxygenOS）和 Breezy Weather（使用 Breezy 需在 **设置 → 外部模块** 中开启 **数据共享 / Data sharing**），内置 Google 天气图标包，支持明日预报、天气预警与日历日程。
- **AOD 通知与媒体**：单色息屏通知图标、媒体播放信息、流体云与系统图标适配（含 ColorOS 17 手电筒），并兼容 `com.fankes.coloros.notify` 提供的单色图标。
- **Pixel 通知卡片（Peek）**：可选开启原生 Pixel 风格息屏通知预览。
- **指纹与息屏行为**：可选 Pixel 风格屏下指纹图标、按压 HDR 高亮、Monet 解锁涟漪、口袋模式，以及省电模式下的新通知亮起与充电常亮。
- **Material 3 Expressive 界面**：支持动态取色、中英文切换与一键重启 SystemUI。

## 环境要求

- ColorOS / OxygenOS 设备（主要在 ColorOS 16 / 17、Android 16 / 17 测试）
- LSPosed 或 Vector（支持 libxposed API 101）

## 安装使用

1. 前往 [Releases](https://github.com/nonlog/pixel-aod-xposed/releases) 下载并安装最新版 APK。
2. 在 LSPosed / Vector 中启用模块（已自动声明作用域 `com.android.systemui`）。
3. 重启 SystemUI 或重启设备生效。

## 本地构建

```bash
./gradlew :app:assembleDebug
```

## 开源协议

[MIT](LICENSE)
