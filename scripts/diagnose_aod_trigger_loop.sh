#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-D:/enviroment/ADB/adb.exe}"
SERIAL="${SERIAL:-}"
MODE="${MODE:-auto}"
CYCLES="${CYCLES:-1}"
SETTLE_SEC="${SETTLE_SEC:-14}"
TAP_AFTER_OFF="${TAP_AFTER_OFF:-0}"
WAKE_BEFORE_SLEEP="${WAKE_BEFORE_SLEEP:-1}"
OUT_DIR="${OUT_DIR:-$ROOT_DIR/logs/aod-trigger-diagnostics/$(date +%Y%m%d-%H%M%S)}"

usage() {
  cat <<'USAGE'
Usage:
  MODE=auto CYCLES=1 SETTLE_SEC=14 TAP_AFTER_OFF=0 ./scripts/diagnose_aod_trigger_loop.sh
  SERIAL=<adb-serial> MODE=observe ./scripts/diagnose_aod_trigger_loop.sh

Modes:
  auto     Send WAKEUP then SLEEP per cycle, optionally tap after screen-off, then collect logs.
  observe  Do not send input events; collect a log window while the user reproduces manually.

Outputs:
  logs/aod-trigger-diagnostics/<timestamp>/
    summary.txt
    logcat.txt
    lspd_modules.txt
    pixel_aod_events.txt
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ ! -x "$ADB" ]]; then
  echo "ADB not executable: $ADB" >&2
  exit 2
fi

mapfile -t ONLINE_DEVICES < <("$ADB" devices | awk 'NR > 1 && $2 == "device" { print $1 }')
if [[ -z "$SERIAL" ]]; then
  if [[ "${#ONLINE_DEVICES[@]}" -ne 1 ]]; then
    echo "Expected exactly one online adb device; found ${#ONLINE_DEVICES[@]}." >&2
    "$ADB" devices >&2
    echo "Set SERIAL=<device> and retry." >&2
    exit 2
  fi
  SERIAL="${ONLINE_DEVICES[0]}"
fi

ADB_SERIAL=("$ADB" -s "$SERIAL")
mkdir -p "$OUT_DIR"

now_utc() {
  date -u +%Y-%m-%dT%H:%M:%SZ
}

run_adb() {
  "${ADB_SERIAL[@]}" "$@"
}

dump_power_state() {
  run_adb shell dumpsys power 2>/dev/null \
    | grep -E 'mWakefulness=|mInteractive=|Display Power|mScreenState=|mDozeScreenState=' \
    | head -n 20 || true
}

collect_lspd_logs() {
  run_adb shell su -c \
    "'for f in \$(ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -n 4); do echo ==== \$f; tail -n 12000 \$f; done'" \
    > "$OUT_DIR/lspd_modules.txt" 2>&1 || true
}

collect_logcat() {
  run_adb logcat -d -t 8000 > "$OUT_DIR/logcat.txt" 2>&1 || true
}

filter_events() {
  local pattern
  pattern='PixelAodOPlus|PixelAodModern|AOD native trigger|OOS AOD trigger mapping|started trigger-only Pixel AOD brief display|expired trigger-only Pixel AOD brief display|blocked trigger-only Pixel AOD brief display|skipped native short-wake Pixel AOD trigger|native short-wake trigger candidate|triggerBrief|AOD policy decision|AOD overlay decision|AOD overlay visibility decision|screen-state|onDreamingStarted|onDreamingStopped|onEnergySavingNotifyHide|notifyHideCallback|reason=non-display-trigger|fingerprint|Fingerprint|FOD|fod'
  {
    echo "==== LSPosed module events ===="
    grep -E "$pattern" "$OUT_DIR/lspd_modules.txt" || true
    echo
    echo "==== logcat events ===="
    grep -E "$pattern" "$OUT_DIR/logcat.txt" || true
  } > "$OUT_DIR/pixel_aod_events.txt"
}

count_events() {
  local pattern="$1"
  { grep -E "$pattern" "$OUT_DIR/pixel_aod_events.txt" || true; } | wc -l | tr -d ' '
}

write_summary() {
  local started expired non_display screen_off_brief prox_started active_tail systemui_pid pkg_version
  started="$(count_events 'started trigger-only Pixel AOD brief display')"
  expired="$(count_events 'expired trigger-only Pixel AOD brief display')"
  non_display="$(count_events 'reason=non-display-trigger')"
  screen_off_brief="$(count_events 'started trigger-only Pixel AOD brief display.*type=screen-off|type=screen-off.*started trigger-only Pixel AOD brief display')"
  prox_started="$(count_events 'started trigger-only Pixel AOD brief display.*nativeTrigger=proximity|nativeTrigger=proximity.*started trigger-only Pixel AOD brief display')"
  active_tail="$({ tail -n 80 "$OUT_DIR/pixel_aod_events.txt" | grep -E 'triggerBriefActive=true' || true; } | wc -l | tr -d ' ')"
  systemui_pid="$(run_adb shell pidof com.android.systemui 2>/dev/null | tr -d '\r' || true)"
  pkg_version="$({ run_adb shell dumpsys package dev.codex.pixelaod 2>/dev/null \
    | grep -E 'versionCode=|versionName=' || true; } | tr -d '\r' | paste -sd ';' -)"

  {
    echo "AOD trigger diagnostic summary"
    echo "time=$(now_utc)"
    echo "serial=$SERIAL"
    echo "mode=$MODE cycles=$CYCLES settleSec=$SETTLE_SEC tapAfterOff=$TAP_AFTER_OFF wakeBeforeSleep=$WAKE_BEFORE_SLEEP"
    echo "systemuiPid=$systemui_pid"
    echo "package=$pkg_version"
    echo
    echo "counts.startedBrief=$started"
    echo "counts.expiredBrief=$expired"
    echo "counts.nonDisplayTriggerSkipped=$non_display"
    echo "counts.screenOffBriefStarted=$screen_off_brief"
    echo "counts.proximityBriefStarted=$prox_started"
    echo "counts.triggerBriefActiveInTail=$active_tail"
    echo
    echo "verdict:"
    if [[ "$screen_off_brief" -gt 0 ]]; then
      echo "RED screen-off incorrectly started brief AOD"
    fi
    if [[ "$prox_started" -gt 0 ]]; then
      echo "RED proximity incorrectly started brief AOD"
    fi
    if [[ "$started" -gt 0 && "$expired" -eq 0 ]]; then
      echo "RED brief AOD started but no expiry event was captured"
    fi
    if [[ "$active_tail" -gt 0 ]]; then
      echo "SUSPECT triggerBriefActive still appears near the end of the log window"
    fi
    if [[ "$screen_off_brief" -eq 0 && "$prox_started" -eq 0 && ! ( "$started" -gt 0 && "$expired" -eq 0 ) ]]; then
      echo "NO_RED_SIGNAL no known log-level trigger bug captured"
    fi
    echo
    echo "powerStateAfter:"
    dump_power_state
  } > "$OUT_DIR/summary.txt"
}

{
  echo "AOD trigger diagnostic run"
  echo "start=$(now_utc)"
  echo "serial=$SERIAL"
  echo "mode=$MODE"
  echo
  echo "powerStateBefore:"
  dump_power_state
} > "$OUT_DIR/run.txt"

run_adb shell log -t PixelAodDiag "start $(basename "$OUT_DIR") mode=$MODE cycles=$CYCLES" >/dev/null 2>&1 || true

case "$MODE" in
  auto)
    for i in $(seq 1 "$CYCLES"); do
      if [[ "$WAKE_BEFORE_SLEEP" == "1" ]]; then
        echo "cycle $i: WAKEUP" | tee -a "$OUT_DIR/run.txt"
        run_adb shell input keyevent WAKEUP >/dev/null 2>&1 || true
        sleep 1
      fi
      echo "cycle $i: SLEEP" | tee -a "$OUT_DIR/run.txt"
      run_adb shell input keyevent SLEEP >/dev/null 2>&1 || true
      sleep 1
      if [[ "$TAP_AFTER_OFF" == "1" ]]; then
        echo "cycle $i: TAP" | tee -a "$OUT_DIR/run.txt"
        run_adb shell input tap 540 1600 >/dev/null 2>&1 || true
      fi
      sleep "$SETTLE_SEC"
    done
    ;;
  observe)
    echo "observe mode: reproduce manually within ${SETTLE_SEC}s" | tee -a "$OUT_DIR/run.txt"
    sleep "$SETTLE_SEC"
    ;;
  *)
    echo "Unknown MODE=$MODE" >&2
    usage >&2
    exit 2
    ;;
esac

run_adb shell log -t PixelAodDiag "end $(basename "$OUT_DIR")" >/dev/null 2>&1 || true
collect_logcat
collect_lspd_logs
filter_events
write_summary

cat "$OUT_DIR/summary.txt"
echo
echo "Artifacts: $OUT_DIR"
