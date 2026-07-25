# Pixel AOD for OPlus

Pixel-style Always-On Display and lockscreen clock replacement for OPlus/OnePlus SystemUI.

This is an Android Xposed module for OPlus-oriented ROMs. It uses the modern
libxposed metadata path and replaces selected SystemUI presentation layers while
leaving the platform's display, biometric, and power-management ownership intact.

## Features

- Pixel-inspired AOD and lockscreen clock layouts with Google Sans Flex rendering.
- Persistent OPlus ClockPlugin host integration for smoother lockscreen-to-AOD handoff.
- AOD notification icons, live alert icons, notification overflow labels, media metadata,
  and media timeout handling.
- Schedule, continuous, and trigger-oriented AOD display policies with proximity and
  power-policy awareness.
- Optional Pixel fingerprint icon, while preserving native OOS touch, HBM, visibility,
  and authentication behavior.
- Structured diagnostic logging for AOD lifecycle, native suppression, notifications,
  media, and fingerprint layers.

## Compatibility

- Android API 26 or newer.
- OPlus/OnePlus SystemUI implementation targeted by this module.
- LSPosed or Vector with modern libxposed API version 101 support.
- Scope the module to `com.android.systemui` in the manager application.

This project is device- and SystemUI-version-sensitive. Treat it as experimental outside
the OPlus environment it was developed against.

## Build

Requirements:

- JDK 17
- Android SDK Platform 36 and Build Tools 36.1.0

```bash
./gradlew :app:assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install

Install as an upgrade to preserve the module configuration:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Enable the module and grant the SystemUI scope in LSPosed or Vector. Restart SystemUI
or reboot before validating behavior.

## Diagnostics

Enable `Debug Logging` in the module UI, then inspect both log sources:

```bash
adb logcat -s PixelAodOPlus
adb shell su -c 'grep -E "PixelAodOPlus|dev.codex.pixelaod" /data/adb/lspd/log/modules_*.log'
```

The persistent LSPosed module log is required for investigations that span a SystemUI
restart or exceed logcat's ring buffer.

## Repository Layout

- `app/`: Android module source, resources, and tests.
- `docs/`: lifecycle, power-policy, notification-pulse, and roadmap notes.
- `scripts/`: AOD diagnostic capture helpers.
- `tools/`: log extraction utilities.

## Development Branches

- `master`: stable integration branch.
- `agent/codex`: Codex development branch.
- `agent/claude`: Claude development branch.
- `agent/antigravity`: Antigravity development branch.

The branches are synchronized when a validated integration is ready. Local device logs,
screenshots, IDE files, and build outputs are intentionally ignored.

## License

No license has been selected yet. Do not redistribute or reuse the source unless the
repository owner adds an explicit license.
