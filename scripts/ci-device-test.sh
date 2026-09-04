#!/usr/bin/env bash
set -euo pipefail

export ADB_VENDOR_KEYS="${ADB_VENDOR_KEYS:-$HOME/.android/adbkey}"

: "${ADB_TARGET:?ADB_TARGET must be set by the runner workflow}"
APK="candidate/app-debug.apk"
PACKAGE="dev.codex.pixelaod"
DIAG="device-diagnostics"
mkdir -p "$DIAG"

if [[ ! -f "$APK" ]]; then
  echo "Candidate APK not found: $APK" >&2
  exit 1
fi

adb kill-server >/dev/null 2>&1 || true
adb start-server >/dev/null
adb disconnect "$ADB_TARGET" >/dev/null 2>&1 || true
sleep 1
adb connect "$ADB_TARGET" >/dev/null 2>&1 || true
for _ in $(seq 1 10); do
  if [[ "$(adb -s "$ADB_TARGET" get-state 2>/dev/null || true)" == "device" ]]; then
    break
  fi
  adb disconnect "$ADB_TARGET" >/dev/null 2>&1 || true
  sleep 1
  adb connect "$ADB_TARGET" >/dev/null 2>&1 || true
done
if [[ "$(adb -s "$ADB_TARGET" get-state 2>/dev/null || true)" != "device" ]]; then
  echo "ADB target unavailable: $ADB_TARGET" >&2
  adb devices -l || true
  exit 1
fi

MODEL="$(adb -s "$ADB_TARGET" shell getprop ro.product.model | tr -d '\r')"
DEVICE="$(adb -s "$ADB_TARGET" shell getprop ro.product.device | tr -d '\r')"
PHYSICAL_SERIAL="$(adb -s "$ADB_TARGET" shell getprop ro.serialno | tr -d '\r')"
if [[ "$MODEL" != "CPH2573" || "$DEVICE" != "OP595DL1" ]]; then
  echo "Unexpected test device: model=$MODEL device=$DEVICE" >&2
  exit 1
fi

LOCAL_HASH="$(sha256sum "$APK" | awk '{print $1}')"
START_SEC="$(date +%s)"
PRE_PID="$(adb -s "$ADB_TARGET" shell pidof com.android.systemui | tr -d '\r' || true)"

STAGE_DIR="$(mktemp -d)"
trap 'rm -rf "$STAGE_DIR"' EXIT
split -b 4m "$APK" "$STAGE_DIR/apk.part."
REMOTE_STAGE="/data/local/tmp/pixelaod-ci-${GITHUB_RUN_ID:-manual}.apk"
adb -s "$ADB_TARGET" shell "rm -f '$REMOTE_STAGE' /data/local/tmp/pixelaod-ci-part-*"
PART_INDEX=0
for PART in "$STAGE_DIR"/apk.part.*; do
  REMOTE_PART="/data/local/tmp/pixelaod-ci-part-$(printf '%02d' "$PART_INDEX")"
  adb -s "$ADB_TARGET" push "$PART" "$REMOTE_PART"
  PART_INDEX=$((PART_INDEX + 1))
done
adb -s "$ADB_TARGET" shell "cat /data/local/tmp/pixelaod-ci-part-* > '$REMOTE_STAGE' && rm -f /data/local/tmp/pixelaod-ci-part-*"
STAGED_HASH="$(adb -s "$ADB_TARGET" shell sha256sum "$REMOTE_STAGE" | awk '{print $1}' | tr -d '\r')"
if [[ "$LOCAL_HASH" != "$STAGED_HASH" ]]; then
  echo "Staged APK digest mismatch: local=$LOCAL_HASH staged=$STAGED_HASH" >&2
  exit 1
fi
if ! INSTALL_OUTPUT="$(adb -s "$ADB_TARGET" shell pm install -r -d "$REMOTE_STAGE" 2>&1 | tr -d '\r')"; then
  echo "$INSTALL_OUTPUT" >&2
  exit 1
fi
echo "$INSTALL_OUTPUT"
adb -s "$ADB_TARGET" shell rm -f "$REMOTE_STAGE"
REMOTE_APK="$(adb -s "$ADB_TARGET" shell pm path "$PACKAGE" | sed -n 's/^package://p' | head -n1 | tr -d '\r')"
if [[ -z "$REMOTE_APK" ]]; then
  echo "Installed package path not found" >&2
  exit 1
fi
REMOTE_HASH="$(adb -s "$ADB_TARGET" shell su -c "sha256sum '$REMOTE_APK'" | awk '{print $1}' | tr -d '\r')"
if [[ "$LOCAL_HASH" != "$REMOTE_HASH" ]]; then
  echo "APK digest mismatch: local=$LOCAL_HASH remote=$REMOTE_HASH" >&2
  exit 1
fi

adb -s "$ADB_TARGET" shell su -c 'killall com.android.systemui'
POST_PID=""
for _ in $(seq 1 30); do
  sleep 1
  POST_PID="$(adb -s "$ADB_TARGET" shell pidof com.android.systemui | tr -d '\r' || true)"
  if [[ -n "$POST_PID" && "$POST_PID" != "$PRE_PID" ]]; then
    break
  fi
done
if [[ -z "$POST_PID" || "$POST_PID" == "$PRE_PID" ]]; then
  echo "SystemUI did not restart cleanly: before=$PRE_PID after=$POST_PID" >&2
  exit 1
fi

sleep 6
adb -s "$ADB_TARGET" shell dumpsys power > "$DIAG/power.txt" || true
adb -s "$ADB_TARGET" shell dumpsys window > "$DIAG/window.txt" || true
adb -s "$ADB_TARGET" logcat -d -v epoch | awk -v start="$START_SEC" '$1 + 0 >= start' > "$DIAG/logcat-since-install.txt" || true
LSPD_LOG="$(adb -s "$ADB_TARGET" shell su -c 'ls -1t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -n 1' | tr -d '\r' || true)"
if [[ -n "$LSPD_LOG" ]]; then
  adb -s "$ADB_TARGET" exec-out su -c "cat '$LSPD_LOG'" > "$DIAG/lspd-modules.log" || true
fi

WAKEFULNESS="$(grep -oE 'mWakefulness=[A-Za-z]+' "$DIAG/power.txt" | head -n1 || true)"
{
  echo "ADB_TARGET=$ADB_TARGET"
  echo "PHYSICAL_SERIAL=$PHYSICAL_SERIAL"
  echo "MODEL=$MODEL"
  echo "DEVICE=$DEVICE"
  echo "APK_SHA256=$LOCAL_HASH"
  echo "SYSTEMUI_PRE_PID=$PRE_PID"
  echo "SYSTEMUI_POST_PID=$POST_PID"
  echo "${WAKEFULNESS:-mWakefulness=unknown}"
} | tee "$DIAG/summary.txt"

if grep -Eqi 'FATAL EXCEPTION|ANR in com\.android\.systemui|OutOfMemoryError|Fatal signal|DeadSystemException' "$DIAG/logcat-since-install.txt"; then
  echo "SystemUI fatal health pattern detected after install" >&2
  grep -Ei 'FATAL EXCEPTION|ANR in com\.android\.systemui|OutOfMemoryError|Fatal signal|DeadSystemException' "$DIAG/logcat-since-install.txt" >&2 || true
  exit 1
fi

if [[ "$WAKEFULNESS" != "mWakefulness=Dozing" ]]; then
  echo "::warning::Device is not currently Dozing; workflow will not wake/sleep the phone automatically."
fi

echo "### Device validation" >> "$GITHUB_STEP_SUMMARY"
echo "- Target: $ADB_TARGET" >> "$GITHUB_STEP_SUMMARY"
echo "- Model: $MODEL / $DEVICE" >> "$GITHUB_STEP_SUMMARY"
echo "- APK SHA-256: \`$LOCAL_HASH\`" >> "$GITHUB_STEP_SUMMARY"
echo "- SystemUI PID: $PRE_PID -> $POST_PID" >> "$GITHUB_STEP_SUMMARY"
echo "- ${WAKEFULNESS:-mWakefulness=unknown}" >> "$GITHUB_STEP_SUMMARY"
