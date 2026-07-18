# Panel Handoff Blackout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the unavoidable OOS screen-off panel blank into one controlled presentation blackout without changing the terminal Doze power state.

**Architecture:** A pure `PanelHandoffGate` owns timing and generation guards. Existing hook and view classes translate confirmed OOS events into gate operations and presentation visibility. Diagnostics correlate gate events with compositor and fingerprint events.

**Tech Stack:** Java, Android SystemUI hooks, Gradle wrapper, ADB, LSPosed persistent logs, Perfetto.

## Global Constraints

- Work only on `agent/codex` in `D:/Downloads/Xposed_test/pixel-aod-codex`.
- Preserve the uncommitted `0.1.183` DND, stock-media suppression, and first-frame fixes.
- Do not rewrite display power state or brightness.
- Do not uninstall the module and do not commit without explicit user instruction.

---

### Task 1: Runtime Differential

**Files:**
- Modify: `.debug-journal.md`
- Modify: `scripts/diagnose_aod_black_frame.sh`

- [ ] Capture Pixel-only and COUI-only screen-off timelines.
- [ ] Correlate the first AOD frame, FOD surface lifecycle, panel handoff, and final suspend.
- [ ] Record the confirmed event that opens the presentation gate and the measured extension interval.

### Task 2: Gate State Controller

**Files:**
- Create: `app/src/main/java/dev/codex/pixelaod/PanelHandoffGate.java`
- Create: `app/src/test/java/dev/codex/pixelaod/PanelHandoffGateTest.java`
- Modify: `app/build.gradle`

- [ ] Add failing tests for generation replacement, duplicate coalescing, cancellation, and completion.
- [ ] Run the targeted JVM test and verify the expected failures.
- [ ] Implement the minimal pure state controller.
- [ ] Run the targeted JVM test and verify it passes.

### Task 3: OOS Integration

**Files:**
- Modify: `app/src/main/java/dev/codex/pixelaod/PixelAodClockView.java`
- Modify: `app/src/main/java/dev/codex/pixelaod/PixelAodHook.java`
- Modify: `app/src/main/java/dev/codex/pixelaod/OosAodLifecycleAdapter.java`

- [ ] Open the gate only from the runtime-confirmed panel-handoff event.
- [ ] Apply the gate only to Pixel presentation visibility while keeping stock suppression active.
- [ ] Cancel on screen-on, AOD exit, proximity-near, policy denial, module disable, and trace replacement.
- [ ] Reveal once via `postOnAnimation` after the matching generation completes.

### Task 4: Device Delivery

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `app/build.gradle`
- Modify: `app/src/main/resources/META-INF/xposed/module.prop`

- [ ] Bump to a version newer than the discarded `0.1.184 (191)` experiment.
- [ ] Run tests, debug build, `git diff --check`, and final APK Xposed metadata checks.
- [ ] Overwrite-install with `adb install -r` and restart SystemUI.
- [ ] Capture a post-install diagnostic sample and hand visual verification to the user.
