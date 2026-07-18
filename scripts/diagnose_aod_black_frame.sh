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

SYSTEMUI_PID_BEFORE="$(run_adb shell pidof com.android.systemui 2>/dev/null | tr -d '\r' || true)"

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
  pattern='PixelAodOPlus|FOD AOD diagnostic|FOD-only native-timeout hide|AOD policy decision|AOD overlay decision|AOD overlay visibility decision|Pixel AOD overlay visibility|requested AOD frame refresh|notifyHideCallback|onEnergySavingNotifyHide|hideByTimeoutReceiver|native-timeout|AodData-->setAodIsInShow|DreamService\[DozeService\]|setDozeScreenState|UpdateDoze|screenTurnedOff|screenTurningOff|SurfaceFlinger|OplusSurfaceFlinger|Setting power mode|blank_mode|DisplayPowerController|dozeScreenState|fingerprint|Fingerprint|Udfps|udfps|FOD|fod'
  {
    echo "==== LSPosed module events ===="
    grep -E "$pattern|PanelHandoffGate|setScreenState changed|startFadeInAnimation|startFadeOutAnimation|FadeIn Animation|FadeOut Animation" "$OUT_DIR/lspd_modules.txt" || true
    echo
    echo "==== logcat events ===="
    grep -E "$pattern|PanelHandoffGate|setScreenState changed|startFadeInAnimation|startFadeOutAnimation|FadeIn Animation|FadeOut Animation" "$OUT_DIR/logcat.txt" || true
  } > "$OUT_DIR/pixel_aod_events.txt"
}

latest_screen_off_trace() {
  local gate_trace
  gate_trace="$({ grep -E '[[:space:]]I[[:space:]]+PixelAodOPlus:' "$OUT_DIR/logcat.txt" || true; } \
    | { grep -E 'PanelHandoffGate (opened|replaced|completed|cancelled)|panelHandoffBlocked=true' || true; } \
    | sed -nE 's/.* trace=(aod-[^ ]+).*/\1/p' \
    | tail -n 1)"
  if [[ -n "$gate_trace" ]]; then
    echo "$gate_trace"
    return
  fi
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

count_logcat_trace_events() {
  local trace="$1"
  local pattern="$2"
  if [[ -z "$trace" ]]; then
    echo 0
    return
  fi
  { grep -E '[[:space:]]I[[:space:]]+PixelAodOPlus:' "$OUT_DIR/logcat.txt" || true; } \
    | { grep -F "trace=$trace" || true; } \
    | { grep -E "$pattern" || true; } \
    | wc -l \
    | tr -d ' '
}

count_logcat_events() {
  local pattern="$1"
  { grep -E '[[:space:]]I[[:space:]]+PixelAodOPlus:' "$OUT_DIR/logcat.txt" || true; } \
    | { grep -E "$pattern" || true; } \
    | wc -l \
    | tr -d ' '
}

display_off_duration_ms() {
  local on_off off_doze
  on_off="$({ grep -E 'setScreenState changed:ON->OFF' "$OUT_DIR/logcat.txt" || true; } | tail -n 1 | awk '{print $2}')"
  off_doze="$({ grep -E 'setScreenState changed:OFF->DOZE' "$OUT_DIR/logcat.txt" || true; } | tail -n 1 | awk '{print $2}')"
  if [[ -z "$on_off" || -z "$off_doze" ]]; then
    echo -1
    return
  fi
  awk -v start="$on_off" -v end="$off_doze" '
    function to_ms(value, parts) {
      split(value, parts, /[:.]/)
      return (((parts[1] * 60) + parts[2]) * 60 + parts[3]) * 1000 + parts[4]
    }
    BEGIN {
      duration = to_ms(end) - to_ms(start)
      if (duration < 0) duration += 86400000
      print duration
    }'
}

write_summary() {
  local trace fod_events fod_hide_events fod_only_invoked fod_only_suppressed native_allowed native_scheduled native_reasserted
  local aod_false surface_power black_proxy overlay_false overlay_true doze_off_rewritten
  local gate_opened gate_extended gate_cancelled gate_completed gate_stale gate_skipped
  local trace_gate_opened trace_gate_blocked trace_gate_cancelled trace_gate_completed trace_gate_terminal_count
  local display_on_off display_off_doze terminal_suspend fod_fade_in fod_fade_out off_duration_ms systemui_pid_after
  trace="$(latest_screen_off_trace || true)"
  fod_events="$(count_events 'FOD AOD diagnostic')"
  fod_hide_events="$(count_events 'FOD AOD diagnostic.*(hide|Hide|setFpIconVisibilityInAOD|setFingerprintIconShow)')"
  fod_only_invoked="$(count_events 'FOD-only native-timeout hide invoked')"
  fod_only_suppressed="$(count_events 'suppressed OPlus AOD native-timeout hide via FOD-only path')"
  native_allowed="$(count_events 'allowed OPlus AOD energy-saving hide.*(notifyHideCallback|onEnergySavingNotifyHide|native-timeout-callback)')"
  native_scheduled="$(count_events 'scheduling Pixel AOD native-timeout reassert')"
  native_reasserted="$(count_events 'reasserted Pixel AOD after native timeout hide')"
  aod_false="$(count_events 'AodData-->setAodIsInShow: false|AodData-->setAodIsInShow false')"
  surface_power="$(count_events 'SurfaceFlinger.*Setting power mode|Setting power mode|blank_mode')"
  doze_off_rewritten="$(count_events 'rewrote DreamService doze screen state')"
  overlay_false="$(count_trace_events "$trace" 'AOD overlay visibility decision.*visible=false|Pixel AOD overlay visibility=hidden')"
  overlay_true="$(count_trace_events "$trace" 'AOD overlay visibility decision.*visible=true|Pixel AOD overlay visibility=visible')"
  black_proxy="$(count_trace_events "$trace" 'AOD overlay visibility decision.*visible=false.*displayState=OFF|Pixel AOD overlay visibility=hidden.*displayState=OFF')"
  gate_opened="$(count_logcat_events 'PanelHandoffGate (opened|replaced)')"
  gate_extended="$(count_logcat_events 'PanelHandoffGate extended')"
  gate_cancelled="$(count_logcat_events 'PanelHandoffGate cancelled')"
  gate_completed="$(count_logcat_events 'PanelHandoffGate completed')"
  gate_stale="$(count_logcat_events 'PanelHandoffGate stale-generation skip')"
  gate_skipped="$(count_logcat_events 'PanelHandoffGate skipped')"
  trace_gate_opened="$(count_logcat_trace_events "$trace" 'PanelHandoffGate (opened|replaced)')"
  trace_gate_blocked="$(count_logcat_trace_events "$trace" 'panelHandoffBlocked=true')"
  trace_gate_cancelled="$(count_logcat_trace_events "$trace" 'PanelHandoffGate cancelled')"
  trace_gate_completed="$(count_logcat_trace_events "$trace" 'PanelHandoffGate completed')"
  trace_gate_terminal_count=$((trace_gate_completed + trace_gate_cancelled))
  display_on_off="$(count_events 'setScreenState changed:ON->OFF')"
  display_off_doze="$(count_events 'setScreenState changed:OFF->DOZE')"
  terminal_suspend="$(count_events 'setScreenState changed:DOZE->DOZE_SUSPEND|UpdateDoze mDozeScreenState=4')"
  fod_fade_in="$(count_events 'startFadeInAnimation|FadeIn Animation, start')"
  fod_fade_out="$(count_events 'startFadeOutAnimation|FadeOut Animation, start')"
  off_duration_ms="$(display_off_duration_ms)"
  systemui_pid_after="$(run_adb shell pidof com.android.systemui 2>/dev/null | tr -d '\r' || true)"

  {
    echo "AOD black-frame diagnostic summary"
    echo "time=$(now_utc)"
    echo "serial=$SERIAL"
    echo "mode=$MODE cycles=$CYCLES settleSec=$SETTLE_SEC"
    echo "latestTrace=${trace:-none}"
    echo "systemuiPidBefore=${SYSTEMUI_PID_BEFORE:-unknown}"
    echo "systemuiPidAfter=${systemui_pid_after:-unknown}"
    echo "package=$({ run_adb shell dumpsys package dev.codex.pixelaod 2>/dev/null | grep -E 'versionCode=|versionName=' || true; } | tr -d '\r' | paste -sd ';' -)"
    echo
    echo "counts.fodDiagnostic=$fod_events"
    echo "counts.fodHideLikeDiagnostic=$fod_hide_events"
    echo "counts.fodOnlyHideInvoked=$fod_only_invoked"
    echo "counts.fodOnlyNativeTimeoutSuppressed=$fod_only_suppressed"
    echo "counts.nativeTimeoutAllowed=$native_allowed"
    echo "counts.nativeTimeoutReassertScheduled=$native_scheduled"
    echo "counts.nativeTimeoutReasserted=$native_reasserted"
    echo "counts.aodDataFalse=$aod_false"
    echo "counts.surfacePowerMode=$surface_power"
    echo "counts.dozeOffRewritten=$doze_off_rewritten"
    echo "counts.traceOverlayVisibleFalse=$overlay_false"
    echo "counts.traceOverlayVisibleTrue=$overlay_true"
    echo "counts.traceBlackFrameProxy=$black_proxy"
    echo "counts.panelGateOpened=$gate_opened"
    echo "counts.panelGateExtended=$gate_extended"
    echo "counts.panelGateCancelled=$gate_cancelled"
    echo "counts.panelGateCompleted=$gate_completed"
    echo "counts.panelGateStaleSkip=$gate_stale"
    echo "counts.panelGateSkipped=$gate_skipped"
    echo "counts.latestTracePanelGateOpened=$trace_gate_opened"
    echo "counts.latestTracePanelHandoffBlocked=$trace_gate_blocked"
    echo "counts.latestTracePanelGateCancelled=$trace_gate_cancelled"
    echo "counts.latestTracePanelGateCompleted=$trace_gate_completed"
    echo "counts.displayOnToOff=$display_on_off"
    echo "counts.displayOffToDoze=$display_off_doze"
    echo "counts.terminalDozeSuspend=$terminal_suspend"
    echo "counts.fodFadeIn=$fod_fade_in"
    echo "counts.fodFadeOut=$fod_fade_out"
    echo "display.offDurationMs=$off_duration_ms"
    echo
    echo "verdict:"
    if [[ -z "$trace" ]]; then
      echo "RED no screen-off trace captured"
    fi
    if [[ "$black_proxy" -gt 0 ]]; then
      echo "RED Pixel overlay hid while displayState=OFF"
    fi
    if [[ "$trace_gate_completed" -gt 1 ]]; then
      echo "RED latest AOD trace revealed more than once"
    fi
    if [[ $((trace_gate_opened + trace_gate_blocked)) -gt 0 && "$trace_gate_terminal_count" -ne 1 ]]; then
      echo "RED latest AOD trace did not reach exactly one completion or cancellation"
    fi
    if [[ $((trace_gate_opened + trace_gate_blocked)) -gt 0 && "$terminal_suspend" -eq 0 ]]; then
      echo "RED panel gate ran without a terminal DOZE_SUSPEND event"
    fi
    if [[ -n "${SYSTEMUI_PID_BEFORE:-}" && -n "$systemui_pid_after" && "$SYSTEMUI_PID_BEFORE" != "$systemui_pid_after" ]]; then
      echo "RED SystemUI PID changed during capture"
    fi
    if [[ "$native_allowed" -gt 0 && "$aod_false" -gt 0 && "$native_reasserted" -gt 0 ]]; then
      echo "SUSPECT full native AOD hide window: native timeout allowed, AodData=false, then Pixel reassert"
    fi
    if [[ "$fod_only_suppressed" -gt 0 && "$aod_false" -eq 0 ]]; then
      echo "GOOD FOD-only native timeout path suppressed whole-AOD false signal"
    fi
    if [[ "$fod_only_suppressed" -gt 0 && "$aod_false" -gt 0 ]]; then
      echo "SUSPECT FOD-only path ran but whole-AOD false signal still occurred"
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
    if [[ -n "$trace" && "$black_proxy" -eq 0 && "$native_allowed" -eq 0 \
          && "$aod_false" -eq 0 && "$terminal_suspend" -gt 0 ]] \
          && { [[ $((trace_gate_opened + trace_gate_blocked)) -eq 0 ]] \
            || [[ "$trace_gate_terminal_count" -eq 1 ]]; }; then
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
