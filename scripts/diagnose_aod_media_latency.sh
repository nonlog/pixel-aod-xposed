#!/usr/bin/env bash
set -euo pipefail

ADB="${ADB:-D:/enviroment/ADB/adb.exe}"
SERIAL="${SERIAL:-}"
PACKAGE="${PACKAGE:-com.theveloper.pixelplay}"
SETTLE_SEC="${SETTLE_SEC:-45}"
SAMPLE_INTERVAL_SEC="${SAMPLE_INTERVAL_SEC:-1}"
SLEEP_BEFORE_NEXT="${SLEEP_BEFORE_NEXT:-1}"
DISPATCH_KEY="${DISPATCH_KEY:-next}"

if [[ -z "$SERIAL" ]]; then
  mapfile -t devices < <("$ADB" devices | awk 'NR > 1 && $2 == "device" {print $1}')
  if [[ "${#devices[@]}" -ne 1 ]]; then
    printf 'Expected exactly one online device, found %s\n' "${#devices[@]}" >&2
    "$ADB" devices >&2
    exit 2
  fi
  SERIAL="${devices[0]}"
fi

timestamp="$(date +%Y%m%d-%H%M%S)"
out_dir="logs/aod-media-diagnostics/${timestamp}"
mkdir -p "$out_dir"

run_adb() {
  "$ADB" -s "$SERIAL" "$@"
}

sample_state() {
  local index="$1"
  local prefix="$out_dir/sample-${index}"
  {
    printf 'sample=%s wall=%s\n' "$index" "$(date -Is)"
    run_adb shell date
    run_adb shell dumpsys power | grep -E 'mWakefulness=|Display Power|mHoldingDisplaySuspendBlocker|mWakeLockSummary=' || true
  } > "${prefix}-state.txt" 2>&1
  run_adb shell dumpsys media_session > "${prefix}-media_session.txt" 2>&1 || true
  run_adb shell dumpsys notification --noredact > "${prefix}-notification.txt" 2>&1 || true
}

extract_sample_summary() {
  local index="$1"
  local media_file="$out_dir/sample-${index}-media_session.txt"
  local notification_file="$out_dir/sample-${index}-notification.txt"
  local state_file="$out_dir/sample-${index}-state.txt"
  {
    printf '%s\n' "--- sample ${index} ---"
    grep -E 'sample=|mWakefulness=|Display Power' "$state_file" || true
    grep -A35 -B3 "$PACKAGE" "$media_file" \
      | grep -E 'package=|PlaybackState|metadata:|description=|state=' || true
    grep -A70 -B3 "pkg=${PACKAGE}" "$notification_file" \
      | grep -E 'NotificationRecord|postTime=|android.title=|android.text=|android.subText=|category=|importance=|flags=' || true
  } >> "$out_dir/summary.txt"
}

printf 'AOD media latency diagnostic\n' > "$out_dir/summary.txt"
printf 'time=%s\nserial=%s\npackage=%s\nsettleSec=%s\nkey=%s\n\n' \
  "$(date -Is)" "$SERIAL" "$PACKAGE" "$SETTLE_SEC" "$DISPATCH_KEY" >> "$out_dir/summary.txt"

run_adb logcat -c || true
run_adb logcat -v threadtime > "$out_dir/logcat.txt" 2>&1 &
logcat_pid=$!
trap 'kill "$logcat_pid" 2>/dev/null || true' EXIT

sample_state "before"
extract_sample_summary "before"

if [[ "$SLEEP_BEFORE_NEXT" == "1" ]]; then
  run_adb shell input keyevent SLEEP || true
  sleep 3
  sample_state "aod-before-next"
  extract_sample_summary "aod-before-next"
fi

printf '\n%s\n' "--- dispatch ${DISPATCH_KEY} wall=$(date -Is) ---" >> "$out_dir/summary.txt"
run_adb shell cmd media_session dispatch "$DISPATCH_KEY" >> "$out_dir/summary.txt" 2>&1 || true

samples=$((SETTLE_SEC / SAMPLE_INTERVAL_SEC))
for ((i = 1; i <= samples; i++)); do
  sleep "$SAMPLE_INTERVAL_SEC"
  sample_state "$i"
  extract_sample_summary "$i"
done

kill "$logcat_pid" 2>/dev/null || true
trap - EXIT

{
  printf '\n%s\n' '--- LSPosed module log tail ---'
  while IFS= read -r remote_file; do
    remote_file="${remote_file//$'\r'/}"
    [[ -z "$remote_file" ]] && continue
    printf '==== %s\n' "$remote_file"
    run_adb shell su -c "tail -n 16000 '$remote_file'" 2>/dev/null || true
  done < <(run_adb shell su -c 'ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -n 5' 2>/dev/null || true)
} > "$out_dir/lspd_modules_tail.txt"

{
  printf '\n%s\n' '--- module media signals ---'
  grep -E 'AOD media|media notification cache|media-metadata|media-playback|media-controllers|refreshMediaLines|expiredMedia=|inactive media|package-timeout|cached notification from .*com\.theveloper\.pixelplay|com\.theveloper\.pixelplay' \
    "$out_dir/logcat.txt" "$out_dir/lspd_modules_tail.txt" 2>/dev/null || true
} >> "$out_dir/summary.txt"

printf '\nArtifacts: %s\n' "$out_dir" >> "$out_dir/summary.txt"
cat "$out_dir/summary.txt"
