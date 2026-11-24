# Pixel AOD for OPlus

[简体中文](README.zh-CN.md) | English

An Xposed module that brings a Google Pixel–style Always-On Display (AOD) and lockscreen clock to ColorOS / OxygenOS devices.

## Features

- **Pixel Clock & Typography**: Google Sans Flex variable clock with smooth lockscreen-to-AOD transitions, customizable weight, width, roundness, and dynamic Monet or pure white clock colors.
- **At a Glance & Weather**: Supports both System Weather (CN ColorOS & Global OxygenOS) and Breezy Weather, with a built-in Google Weather icon pack, tomorrow's forecast, weather alerts, and upcoming calendar events.
- **AOD Notifications & Media**: Monochrome AOD notification icons, media playback info, OPlus Fluid Cloud / system status icon adaptation (including ColorOS 17 flashlight), and compatibility with `com.fankes.coloros.notify`.
- **Pixel Notification Peek**: Optional Pixel-style incoming notification display on AOD.
- **Fingerprint & AOD Behavior**: Optional Pixel fingerprint icon, HDR press glow, Monet unlock ripple, pocket mode, and Power Saving AOD enhancements (wake on notification / keep on while charging).
- **Material 3 Expressive UI**: Clean settings app with Dynamic Color, Chinese/English support, and one-tap SystemUI restart.

## Requirements

- ColorOS / OxygenOS device (tested on ColorOS 16 / 17, Android 16 / 17)
- LSPosed or Vector (libxposed API 101)

## Installation

1. Download the latest APK from [Releases](https://github.com/nonlog/pixel-aod-xposed/releases) and install it.
2. Enable the module in LSPosed / Vector (scope `com.android.systemui` is declared automatically).
3. Restart SystemUI or reboot your device.

## Build

```bash
./gradlew :app:assembleDebug
```

## License

[MIT](LICENSE)
