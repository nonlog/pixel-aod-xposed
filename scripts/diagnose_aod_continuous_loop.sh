#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-D:/enviroment/ADB/adb.exe}"
SERIAL="${SERIAL:-}"
CYCLES="${CYCLES:-1}"
SETTLE_SEC="${SETTLE_SEC:-14}"
WAKE_BEFORE_SLEEP="${WAKE_BEFORE_SLEEP:-1}"
OUT_DIR="${OUT_DIR:-$ROOT_DIR/logs/aod-continuous-diagnostics/$(date +%Y%m%d-%H%M%S)}"

usage() {
  cat <<'USAGE'
Usage:
  CYCLES=1 SETTLE_SEC=14 ./scripts/diagnose_aod_continuous_loop.sh
  SERIAL=<adb-serial> CYCLES=1 SETTLE_SEC=14 ./scripts/diagnose_aod_continuous_loop.sh

This captures Continuous AOD lifecycle logs and reports proxy red signals:
  - OOS hide callbacks suppressed while Continuous AOD is active
  - native timeout callbacks allowed without Pixel AOD reassert
  - OOS Doze screen state forced OFF during an active Continuous trace
  - Pixel overlay hidden while displayState=OFF
  - reassert-after-hide loops
  - proximity-near hiding
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
  run_adb shell su -c \
    "'for f in \$(ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -n 4); do echo ==== \$f; tail -n 16000 \$f; done'" \
    > "$OUT_DIR/lspd_modules.txt" 2>&1 || true
}

collect_logcat() {
  run_adb logcat -d -t 10000 > "$OUT_DIR/logcat.txt" 2>&1 || true
}

filter_events() {
  local pattern
  pattern='PixelAodOPlus|AOD policy decision|AOD overlay decision|AOD overlay visibility decision|Pixel AOD overlay visibility|requested AOD frame refresh|suppressed OPlus AOD energy-saving hide|allowed OPlus AOD energy-saving hide|reasserted Pixel AOD after OPlus energy-saving hide|scheduling Pixel AOD native-timeout reassert|reasserted Pixel AOD after native timeout hide|skipped Pixel AOD native-timeout reassert|blocked DreamService doze OFF|allowed DreamService doze OFF|rewrote DreamService doze screen state|refreshing known AOD host visibility|displayState=OFF|displayState=DOZE|reason=proximity-near|notifyHideCallback|onEnergySavingNotifyHide|hideByTimeoutReceiver|native-timeout|DreamService\\[DozeService\\]|UpdateDoze mDozeScreenState|DisplayPowerController|dozeScreenState=OFF|dozeScreenState=DOZE|fingerprint|Fingerprint|FOD|fod|Udfps|udfps'
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
  local trace suppressed allowed reasserted off_hidden off_refresh prox hidden_visible_false visible_true unexpected_off_hidden
  local native_timeout_allowed native_timeout_scheduled native_timeout_reasserted native_timeout_skipped
  local doze_off_updates doze_suspend_updates doze_off_rewritten native_timeout_without_reassert
  trace="$(latest_screen_off_trace || true)"
  suppressed="$(count_trace_events "$trace" 'suppressed OPlus AOD energy-saving hide')"
  allowed="$(count_trace_events "$trace" 'allowed OPlus AOD energy-saving hide')"
  reasserted="$(count_trace_events "$trace" 'reasserted Pixel AOD after OPlus energy-saving hide|#suppressed-hide')"
  native_timeout_allowed="$(count_trace_events "$trace" 'allowed OPlus AOD energy-saving hide.*reason=native-timeout-callback')"
  native_timeout_scheduled="$(count_trace_events "$trace" 'scheduling Pixel AOD native-timeout reassert')"
  native_timeout_reasserted="$(count_trace_events "$trace" 'reasserted Pixel AOD after native timeout hide')"
  native_timeout_skipped="$(count_trace_events "$trace" 'skipped Pixel AOD native-timeout reassert')"
  doze_off_updates="$(
    { grep -E 'UpdateDoze mDozeScreenState=1|dozeScreenState=OFF' "$OUT_DIR/pixel_aod_events.txt" || true; } \
      | wc -l \
      | tr -d ' '
  )"
  doze_suspend_updates="$(
    { grep -E 'UpdateDoze mDozeScreenState=4|dozeScreenState=DOZE_SUSPEND' "$OUT_DIR/pixel_aod_events.txt" || true; } \
      | wc -l \
      | tr -d ' '
  )"
  doze_off_rewritten="$(count_trace_events "$trace" 'rewrote DreamService doze screen state')"
  if [[ "$native_timeout_allowed" -gt 0 && "$native_timeout_reasserted" -eq 0 ]]; then
    native_timeout_without_reassert=1
  else
    native_timeout_without_reassert=0
  fi
  off_hidden="$(
    if [[ -n "$trace" ]]; then
      { grep -F "trace=$trace" "$OUT_DIR/pixel_aod_events.txt" || true; } \
        | { grep -E 'AOD overlay decision.*visible=false.*displayState=OFF|AOD overlay visibility decision.*visible=false.*displayState=OFF|Pixel AOD overlay visibility=hidden.*displayState=OFF|requested AOD frame refresh.*visibility=8 shown=false.*displayState=OFF' || true; } \
        | wc -l \
        | tr -d ' '
    else
      echo 0
    fi
  )"
  off_refresh="$(count_trace_events "$trace" 'requested AOD frame refresh.*visibility=8 shown=false.*displayState=OFF')"
  prox="$(count_trace_events "$trace" 'reason=proximity-near')"
  hidden_visible_false="$(count_trace_events "$trace" 'AOD overlay visibility decision.*visible=false')"
  visible_true="$(count_trace_events "$trace" 'AOD overlay visibility decision.*visible=true')"
  if [[ -n "$trace" ]]; then
    unexpected_off_hidden="$(
      { grep -F "trace=$trace" "$OUT_DIR/pixel_aod_events.txt" || true; } \
        | { grep -E 'AOD overlay decision.*visible=false.*displayState=OFF|AOD overlay visibility decision.*visible=false.*displayState=OFF|Pixel AOD overlay visibility=hidden.*displayState=OFF' || true; } \
        | { grep -v 'reason=proximity-near' || true; } \
        | wc -l \
        | tr -d ' '
    )"
  else
    unexpected_off_hidden=0
  fi

  {
    echo "AOD continuous diagnostic summary"
    echo "time=$(now_utc)"
    echo "serial=$SERIAL"
    echo "cycles=$CYCLES settleSec=$SETTLE_SEC wakeBeforeSleep=$WAKE_BEFORE_SLEEP"
    echo "latestTrace=${trace:-none}"
    echo "package=$({ run_adb shell dumpsys package dev.codex.pixelaod 2>/dev/null | grep -E 'versionCode=|versionName=' || true; } | tr -d '\r' | paste -sd ';' -)"
    echo
    echo "counts.suppressedHide=$suppressed"
    echo "counts.allowedHide=$allowed"
    echo "counts.reassertAfterHide=$reasserted"
    echo "counts.nativeTimeoutAllowed=$native_timeout_allowed"
    echo "counts.nativeTimeoutReassertScheduled=$native_timeout_scheduled"
    echo "counts.nativeTimeoutReasserted=$native_timeout_reasserted"
    echo "counts.nativeTimeoutSkipped=$native_timeout_skipped"
    echo "counts.nativeTimeoutWithoutReassert=$native_timeout_without_reassert"
    echo "counts.dozeOffUpdates=$doze_off_updates"
    echo "counts.dozeSuspendUpdates=$doze_suspend_updates"
    echo "counts.dozeOffRewritten=$doze_off_rewritten"
    echo "counts.offHidden=$off_hidden"
    echo "counts.unexpectedOffHidden=$unexpected_off_hidden"
    echo "counts.offRefreshHidden=$off_refresh"
    echo "counts.proximityBlocked=$prox"
    echo "counts.overlayVisibleFalse=$hidden_visible_false"
    echo "counts.overlayVisibleTrue=$visible_true"
    echo
    echo "verdict:"
    if [[ -z "$trace" ]]; then
      echo "RED no screen-off AOD trace captured"
    fi
    if [[ "$unexpected_off_hidden" -gt 0 ]]; then
      echo "RED black-frame proxy captured: Pixel overlay hidden while displayState=OFF"
    fi
    if [[ "$suppressed" -gt 0 ]]; then
      echo "SUSPECT native hide callback suppressed; this can block OOS fingerprint timeout"
    fi
    if [[ "$reasserted" -gt 0 ]]; then
      echo "SUSPECT reassert-after-hide loop captured"
    fi
    if [[ "$native_timeout_without_reassert" -gt 0 ]]; then
      echo "RED native timeout hide was allowed without Pixel AOD reassert"
    fi
    if [[ "$native_timeout_allowed" -gt 0 && "$doze_off_updates" -gt 0 && "$native_timeout_reasserted" -eq 0 ]]; then
      echo "RED native timeout coincided with OOS Doze OFF and no Pixel reassert"
    fi
    if [[ "$native_timeout_allowed" -gt 0 && "$doze_off_updates" -gt 0 && "$native_timeout_reasserted" -gt 0 ]]; then
      echo "INFO native timeout coincided with OOS Doze OFF but Pixel reassert ran"
    fi
    if [[ "$doze_off_rewritten" -gt 0 ]]; then
      echo "INFO DreamService Doze OFF requests were rewritten while Continuous AOD was active"
    fi
    if [[ "$prox" -gt 0 ]]; then
      echo "INFO proximity-near policy hid overlay; this is expected when the sensor reports near"
    fi
    if [[ -n "$trace"
        && "$unexpected_off_hidden" -eq 0
        && "$suppressed" -eq 0
        && "$reasserted" -eq 0
        && "$native_timeout_without_reassert" -eq 0 ]]; then
      echo "NO_RED_SIGNAL no known Continuous AOD hide proxy captured"
    fi
  } > "$OUT_DIR/summary.txt"

  if [[ -n "$trace" ]]; then
    grep -F "trace=$trace" "$OUT_DIR/pixel_aod_events.txt" > "$OUT_DIR/latest_trace_events.txt" || true
  fi
}

{
  echo "AOD continuous diagnostic run"
  echo "start=$(now_utc)"
  echo "serial=$SERIAL"
} > "$OUT_DIR/run.txt"

run_adb logcat -c >/dev/null 2>&1 || true
run_adb shell log -t PixelAodContinuousDiag "start $(basename "$OUT_DIR") cycles=$CYCLES" >/dev/null 2>&1 || true

for i in $(seq 1 "$CYCLES"); do
  if [[ "$WAKE_BEFORE_SLEEP" == "1" ]]; then
    echo "cycle $i: WAKEUP" | tee -a "$OUT_DIR/run.txt"
    run_adb shell input keyevent WAKEUP >/dev/null 2>&1 || true
    sleep 1
  fi
  echo "cycle $i: SLEEP" | tee -a "$OUT_DIR/run.txt"
  run_adb shell input keyevent SLEEP >/dev/null 2>&1 || true
  sleep "$SETTLE_SEC"
done

run_adb shell log -t PixelAodContinuousDiag "end $(basename "$OUT_DIR")" >/dev/null 2>&1 || true
collect_logcat
collect_lspd_logs
filter_events
write_summary

cat "$OUT_DIR/summary.txt"
echo
echo "Artifacts: $OUT_DIR"
