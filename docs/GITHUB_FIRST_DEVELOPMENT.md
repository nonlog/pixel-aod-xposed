# GitHub-first development

Pixel AOD for OPlus uses GitHub as the canonical development and build environment.

## Canonical repository and branch

- Public source repository: `nonlog/pixel-aod-xposed`.
- Canonical integration branch: `main`.
- New implementation work uses short-lived `feature/*`, `fix/*`, or agent branches and is pushed to GitHub early.
- The old `master` branch is retained as historical state and must not be treated as the current integration branch.
- Log/Windows local worktrees are emergency mirrors only. They are not a source of truth and must not contain the only copy of active code.

## Build path

`.github/workflows/ci.yml` is the authoritative build gate. It uses a GitHub-hosted Ubuntu runner, JDK 17, Android SDK 36/build-tools 36.1.0, the project Gradle wrapper, unit tests, and `assembleDebug`.

Every CI build uploads the debug APK and its SHA-256 as a workflow artifact. A developer should not need Log to compile or package the module.

## Device path

`.github/workflows/device-test.yml` is a manually triggered safe device-validation workflow.

- `pixel-aod-jp`: JP self-hosted runner, local ADB endpoint `127.0.0.1:15556`.
- `pixel-aod-tyo`: TYO self-hosted runner, local ADB endpoint `127.0.0.1:15557`.
- Both endpoints are FRP STCP visitors bound to loopback only; phone ADB is not exposed publicly.
- The workflow installs with `adb install -r`, verifies the installed APK SHA-256, reloads SystemUI, captures diagnostics, and scans for fatal runtime patterns.
- The safe workflow never wakes, sleeps, unlocks, screenshots, records, or injects notifications. If the device is not Dozing it reports that fact and stops short of changing display state.

Interactive AOD/lockscreen operations remain explicit user-approved tests. Visual acceptance is still a human gate.

## Git identity

Repository commits produced by automated development must use the repository-local identity `Codex <codex@openai.com>`. Do not change a machine-wide Git identity.

## Log fallback

Log keeps the existing `15556` and `15557` FRP visitor configuration only as an emergency/manual path. It is not required for normal development, CI, builds, or device installation. Its visitors may be started on demand instead of as a mandatory boot dependency.
