# Pixel AOD for OPlus

[简体中文](README.zh-CN.md) | English

A modern **libxposed** (`API 101`) Xposed module that brings a Google Pixel–style **Always-On Display (AOD)** and **Lockscreen Clock** experience to **OPlus / ColorOS / OxygenOS** devices, while preserving native platform display, biometric, and power-management ownership.

## Features

### Pixel Clock & Variable Typography
- **Google Sans Flex Variable Font Morphing**: Smoothly animates between bold lockscreen typography and light AOD typography, with ColorOS 16 & ColorOS 17 (Android 17) compatibility.
- **Customizable Clock Geometry & Style**:
  - **AOD Weight** & **Lockscreen Weight**
  - **Clock Width** (`wdth`, `75–125`) & **Clock Roundness** (`ROND`, `0–100`)
  - **Lockscreen Clock Color**: Dynamic Wallpaper (Material You / Monet) or Pure White
- **Persistent OPlus `ClockPlugin` Integration**: Keeps lockscreen-to-AOD and unlocked-to-AOD transitions aligned without fighting native SystemUI lifecycle.

### At a Glance & Weather
- **Multi-Source Weather Engine**:
  - **Auto** / **System Weather** / **Breezy Weather**
  - **System Weather** supports both **CN ColorOS** (`com.coloros.weather.service` / `com.coloros.weather2`) and **Global OxygenOS / ColorOS Export** (`net.oneplus.weather`), with a stock lockscreen weather mirror fallback.
- **Weather Icons**: Bundled **Google Weather** icon pack (12 icons) by default, with support for external icon packs (e.g., Breezy Weather icon providers).
- **Tomorrow's Forecast**: Displays next-day weather summary during a user-configured time window.
- **Weather Alerts**: Displays active severe weather alerts from Breezy Weather.
- **Calendar Events**: Shows the next upcoming event within 24 hours, with customizable calendar app icon source.

### AOD Notifications, Media & Peek
- **Monochrome AOD Notification Icons**: Renders clean monochrome notification icons + overflow count (`+N`), media playback info (title, artist, app icon), and timeout handling.
- **OPlus System & Fluid Cloud (Live Alert) Adaptation**:
  - Supports ColorOS 17 **Flashlight** (via camera torch state when Fluid Cloud / Seedling skips posting a standard notification), **Clock / Timer**, **Screen Recorder**, **Sound Recorder**, **In-Call**, **VPN**, **Screen Cast**, **USB**, and **Hotspot** icons.
- **`com.fankes.coloros.notify` Compatibility**: Reuses monochrome status bar icons injected by `com.fankes.coloros.notify` on AOD without hook conflicts, protected by a pixel-level chroma guard so non-monochrome bitmaps are never wrongly tinted.
- **Pixel Ambient Notification Peek**: Optional Pixel-style incoming notification display on pure OLED black (tinted monochrome icon, app name, bold title, and body).
- **Lockscreen Notification Compatibility Fix**: Prevents OPlus SystemUI from hiding lockscreen notifications after unlocking and re-locking.

### Fingerprint (UDFPS) & AOD Behavior
- **UDFPS Visuals**: Optional Pixel-style under-display fingerprint icon, **HDR Press Glow**, **Monet Unlock Success Ripple**, and smooth **AOD Exit Fade-out**, while keeping native OPlus optical/HBM authority intact.
- **Power Saving & Pocket Mode**:
  - Respects native OPlus AOD display modes (Power Saving / Scheduled / All Day) with optional **Brief AOD on New Notification** and **Keep AOD While Charging** in Power Saving mode.
  - **Pocket Mode** via proximity sensor and selectable **Unlocked-to-AOD Transition** (`Animated` or `Direct`).

### Material 3 Expressive (M3E) Module UI
- Built with **Android 16 Dynamic Color** tonal surfaces, overview dashboard cards, segmented lists, M3E check-thumb switches, and a floating pill navigation dock.
- Supports **Follow System / Chinese / English** language switching and one-tap **Restart SystemUI** (root).

## Compatibility

- **OS**: Android 8.0+ (API 26+), primarily developed and tested on **ColorOS 16 / 17 (Android 16 / 17)** on OPlus / OnePlus devices.
- **Framework**: **LSPosed** or **Vector** with modern libxposed API `101`.
- **Scope**: `staticScope=true` targeting `com.android.systemui` only (no manual scope selection needed on supported managers).

> **Note**: SystemUI internals vary across ROM versions and devices. Treat this module as experimental outside the OPlus/ColorOS builds it targets.

## Build & Install

### Requirements
- JDK 17
- Android SDK Platform 36 & Build Tools 36.1.0

### Build Debug APK
```bash
./gradlew :app:assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Automated GitHub Release
GitHub Actions (`.github/workflows/release.yml`) automatically runs unit tests, Android Lint, builds & signs the release APK, and publishes a GitHub Release when `versionName` / `versionCode` in `app/build.gradle` and `app/src/main/resources/META-INF/xposed/module.prop` are bumped on `main` (or when a `v*` tag is pushed).

### Install
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Enable the module in LSPosed / Vector and restart SystemUI (or reboot).

## Diagnostics

Enable **Debug Logging** in the module settings, then inspect logs via ADB:
```bash
adb logcat -s PixelAodOPlus
adb shell su -c 'grep -E "PixelAodOPlus|dev.codex.pixelaod" /data/adb/lspd/log/modules_*.log'
```

## License

No license has been selected yet. Do not redistribute or reuse the source unless the repository owner adds an explicit license.
