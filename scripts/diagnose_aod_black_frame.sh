#!/usr/bin/env bash
set -euo pipefail

export MSYS_NO_PATHCONV=1

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-D:/enviroment/ADB/adb.exe}"
SERIAL="${SERIAL:-}"
MODE="${MODE:-observe}"
CYCLES="${CYCLES:-1}"
SETTLE_SEC="${SETTLE_SEC:-20}"
OUT_DIR="${OUT_DIR:-$ROOT_DIR/logs/aod-black-frame-diagnostics/$(date +%Y%m%d-%H%M%S)}"

usage() {
  cat <<'USAGE'
Usage:
  SETTLE_SEC=30 ./scripts/diagnose_aod_black_frame.sh
  MODE=cycle CYCLES=3 SETTLE_SEC=12 ./scripts/diagnose_aod_black_frame.sh
  SERIAL=<adb-serial> MODE=observe SETTLE_SEC=60 ./scripts/diagnose_aod_black_frame.sh

Modes:
  observe  Clear logcat, wait while you reproduce manually, then collect logs.
  cycle    Drive WAKEUP -> SLEEP cycles, wait after each sleep, then collect logs.

This captures logcat plus LSPosed module logs and correlates:
  - native hide callbacks: notifyHideCallback / onEnergySavingNotifyHide
  - FOD/UDFPS callbacks: FOD AOD diagnostic
  - OOS AOD state: AodData setAodIsInShow
  - display handoff: screenTurnedOff / SurfaceFlinger power mode / blank_mode
  - Pixel overlay decisions and native-timeout reasserts
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

run_adb() {
  "${ADB_SERIAL[@]}" "$@"
}

now_utc() {
  date -u +%Y-%m-%dT%H:%M:%SZ
}

collect_lspd_logs() {
  run_adb shell su -c 'for f in $(ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -n 4); do echo ==== $f; tail -n 24000 $f; done' \
    > "$OUT_DIR/lspd_modules.txt" 2>&1 || true
}

collect_logcat() {
  run_adb logcat -d -t 16000 > "$OUT_DIR/logcat.txt" 2>&1 || true
}

filter_events() {
  local pattern
  pattern='PixelAodOPlus|FOD AOD diagnostic|AOD policy decision|AOD overlay decision|AOD overlay visibility decision|Pixel AOD overlay visibility|requested AOD frame refresh|notifyHideCallback|onEnergySavingNotifyHide|hideByTimeoutReceiver|native-timeout|AodData-->setAodIsInShow|DreamService\[DozeService\]|setDozeScreenState|UpdateDoze|screenTurnedOff|screenTurningOff|SurfaceFlinger|OplusSurfaceFlinger|Setting power mode|blank_mode|DisplayPowerController|dozeScreenState|fingerprint|Fingerprint|Udfps|udfps|FOD|fod'
  {
    echo "==== LSPosed module events ===="
    grep -E "$pattern" "$OUT_DIR/lspd_modules.txt" || true
    echo
    echo "==== logcat events ===="
    grep -E "$pattern" "$OUT_DIR/logcat.txt" || true
  } > "$OUT_DIR/pixel_aod_events.txt"
}

latest_screen_off_trace() {
  grep -E 'trace=aod-[^ ]+.*traceSource=screen-off|noted Pixel AOD screen-off trace=aod-[^ ]+' \
    "$OUT_DIR/pixel_aod_events.txt" \
    | sed -nE 's/.*trace=(aod-[^ ]+).*/\1/p' \
    | tail -n 1
}

count_events() {
  local pattern="$1"
  { grep -E "$pattern" "$OUT_DIR/pixel_aod_events.txt" || true; } \
    | wc -l \
    | tr -d ' '
}

count_trace_events() {
  local trace="$1"
  local pattern="$2"
  if [[ -z "$trace" ]]; then
    echo 0
    return
  fi
  { grep -F "trace=$trace" "$OUT_DIR/pixel_aod_events.txt" || true; } \
    | { grep -E "$pattern" || true; } \
    | wc -l \
    | tr -d ' '
}

write_summary() {
  local trace fod_events fod_hide_events native_allowed native_scheduled native_reasserted
  local aod_false surface_power black_proxy overlay_false overlay_true doze_off_rewritten
  trace="$(latest_screen_off_trace || true)"
  fod_events="$(count_events 'FOD AOD diagnostic')"
  fod_hide_events="$(count_events 'FOD AOD diagnostic.*(hide|Hide|setFpIconVisibilityInAOD|setFingerprintIconShow)')"
  native_allowed="$(count_events 'allowed OPlus AOD energy-saving hide.*(notifyHideCallback|onEnergySavingNotifyHide|native-timeout-callback)')"
  native_scheduled="$(count_events 'scheduling Pixel AOD native-timeout reassert')"
  native_reasserted="$(count_events 'reasserted Pixel AOD after native timeout hide')"
  aod_false="$(count_events 'AodData-->setAodIsInShow: false|AodData-->setAodIsInShow false')"
  surface_power="$(count_events 'SurfaceFlinger.*Setting power mode|Setting power mode|blank_mode')"
  doze_off_rewritten="$(count_events 'rewrote DreamService doze screen state')"
  overlay_false="$(count_trace_events "$trace" 'AOD overlay visibility decision.*visible=false|Pixel AOD overlay visibility=hidden')"
  overlay_true="$(count_trace_events "$trace" 'AOD overlay visibility decision.*visible=true|Pixel AOD overlay visibility=visible')"
  black_proxy="$(count_trace_events "$trace" 'AOD overlay visibility decision.*visible=false.*displayState=OFF|Pixel AOD overlay visibility=hidden.*displayState=OFF')"

  {
    echo "AOD black-frame diagnostic summary"
    echo "time=$(now_utc)"
    echo "serial=$SERIAL"
    echo "mode=$MODE cycles=$CYCLES settleSec=$SETTLE_SEC"
    echo "latestTrace=${trace:-none}"
    echo "systemuiPid=$(run_adb shell pidof com.android.systemui 2>/dev/null | tr -d '\r' || true)"
    echo "package=$({ run_adb shell dumpsys package dev.codex.pixelaod 2>/dev/null | grep -E 'versionCode=|versionName=' || true; } | tr -d '\r' | paste -sd ';' -)"
    echo
    echo "counts.fodDiagnostic=$fod_events"
    echo "counts.fodHideLikeDiagnostic=$fod_hide_events"
    echo "counts.nativeTimeoutAllowed=$native_allowed"
    echo "counts.nativeTimeoutReassertScheduled=$native_scheduled"
    echo "counts.nativeTimeoutReasserted=$native_reasserted"
    echo "counts.aodDataFalse=$aod_false"
    echo "counts.surfacePowerMode=$surface_power"
    echo "counts.dozeOffRewritten=$doze_off_rewritten"
    echo "counts.traceOverlayVisibleFalse=$overlay_false"
    echo "counts.traceOverlayVisibleTrue=$overlay_true"
    echo "counts.traceBlackFrameProxy=$black_proxy"
    echo
    echo "verdict:"
    if [[ -z "$trace" ]]; then
      echo "RED no screen-off trace captured"
    fi
    if [[ "$black_proxy" -gt 0 ]]; then
      echo "RED Pixel overlay hid while displayState=OFF"
    fi
    if [[ "$native_allowed" -gt 0 && "$aod_false" -gt 0 && "$native_reasserted" -gt 0 ]]; then
      echo "SUSPECT full native AOD hide window: native timeout allowed, AodData=false, then Pixel reassert"
    fi
    if [[ "$fod_hide_events" -gt 0 && "$aod_false" -eq 0 ]]; then
      echo "INFO FOD hide-like callbacks fired without whole-AOD false signal"
    fi
    if [[ "$surface_power" -gt 0 && "$black_proxy" -eq 0 ]]; then
      echo "INFO display/panel power transitions occurred without Pixel overlay hide logs"
    fi
    if [[ "$fod_events" -eq 0 ]]; then
      echo "INFO no FOD diagnostic callbacks captured"
    fi
    if [[ -n "$trace" && "$black_proxy" -eq 0 && "$native_allowed" -eq 0 && "$aod_false" -eq 0 ]]; then
      echo "NO_RED_SIGNAL no known black-frame proxy captured"
    fi
  } > "$OUT_DIR/summary.txt"

  if [[ -n "$trace" ]]; then
    grep -F "trace=$trace" "$OUT_DIR/pixel_aod_events.txt" > "$OUT_DIR/latest_trace_events.txt" || true
  fi
}

{
  echo "AOD black-frame diagnostic run"
  echo "start=$(now_utc)"
  echo "serial=$SERIAL"
  echo "mode=$MODE cycles=$CYCLES settleSec=$SETTLE_SEC"
} > "$OUT_DIR/run.txt"

run_adb logcat -c >/dev/null 2>&1 || true
run_adb shell log -t PixelAodBlackFrameDiag "start $(basename "$OUT_DIR") mode=$MODE" >/dev/null 2>&1 || true

case "$MODE" in
  observe)
    echo "observe mode: reproduce manually within ${SETTLE_SEC}s"
    sleep "$SETTLE_SEC"
    ;;
  cycle)
    for i in $(seq 1 "$CYCLES"); do
      echo "cycle $i: WAKEUP" | tee -a "$OUT_DIR/run.txt"
      run_adb shell input keyevent WAKEUP >/dev/null 2>&1 || true
      sleep 1
      echo "cycle $i: SLEEP" | tee -a "$OUT_DIR/run.txt"
      run_adb shell input keyevent SLEEP >/dev/null 2>&1 || true
      sleep "$SETTLE_SEC"
    done
    ;;
  *)
    echo "Unsupported MODE=$MODE; use observe or cycle." >&2
    exit 2
    ;;
esac

run_adb shell log -t PixelAodBlackFrameDiag "end $(basename "$OUT_DIR")" >/dev/null 2>&1 || true
collect_logcat
collect_lspd_logs
filter_events
write_summary

cat "$OUT_DIR/summary.txt"
echo
echo "Artifacts: $OUT_DIR"
