#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-D:/enviroment/ADB/adb.exe}"
SERIAL="${SERIAL:-}"
AUTO_SETTLE_SEC="${AUTO_SETTLE_SEC:-10}"
PULSE_ENTER_AOD_SEC="${PULSE_ENTER_AOD_SEC:-5}"
PULSE_WAIT_SEC="${PULSE_WAIT_SEC:-12}"
RUN_CONTINUOUS="${RUN_CONTINUOUS:-0}"
OUT_DIR="${OUT_DIR:-$ROOT_DIR/logs/aod-smoke-suite/$(date +%Y%m%d-%H%M%S)}"

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/diagnose_aod_smoke_suite.sh
  SERIAL=<adb-serial> AUTO_SETTLE_SEC=10 PULSE_WAIT_SEC=12 ./scripts/diagnose_aod_smoke_suite.sh
  RUN_CONTINUOUS=1 ./scripts/diagnose_aod_smoke_suite.sh

Runs a compact AOD diagnostic suite:
  - trigger-auto: one WAKEUP -> SLEEP cycle
  - native-pulse: enter AOD and post the module test notification
  - continuous: optional Continuous AOD loop when RUN_CONTINUOUS=1

Outputs:
  logs/aod-smoke-suite/<timestamp>/summary.txt
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

mkdir -p "$OUT_DIR"
STEP_STATUS_FILE="$OUT_DIR/step_status.tsv"
: > "$STEP_STATUS_FILE"

now_utc() {
  date -u +%Y-%m-%dT%H:%M:%SZ
}

run_step() {
  local name="$1"
  shift
  local step_dir="$OUT_DIR/$name"
  mkdir -p "$step_dir"
  echo "suite: running $name"
  if "$@" > "$step_dir/stdout.txt" 2> "$step_dir/stderr.txt"; then
    echo -e "$name\t0" >> "$STEP_STATUS_FILE"
  else
    local status="$?"
    echo -e "$name\t$status" >> "$STEP_STATUS_FILE"
  fi
  cat "$step_dir/stdout.txt"
}

run_step trigger-auto \
  env SERIAL="$SERIAL" ADB="$ADB" OUT_DIR="$OUT_DIR/trigger-auto" \
    MODE=auto CYCLES=1 SETTLE_SEC="$AUTO_SETTLE_SEC" TAP_AFTER_OFF=0 \
    "$ROOT_DIR/scripts/diagnose_aod_trigger_loop.sh"

run_step native-pulse \
  env SERIAL="$SERIAL" ADB="$ADB" OUT_DIR="$OUT_DIR/native-pulse" \
    MODE=pulse PULSE_ENTER_AOD_SEC="$PULSE_ENTER_AOD_SEC" \
    PULSE_WAIT_SEC="$PULSE_WAIT_SEC" \
    "$ROOT_DIR/scripts/diagnose_aod_trigger_loop.sh"

if [[ "$RUN_CONTINUOUS" == "1" ]]; then
  run_step continuous \
    env SERIAL="$SERIAL" ADB="$ADB" OUT_DIR="$OUT_DIR/continuous" \
      CYCLES=1 SETTLE_SEC="$AUTO_SETTLE_SEC" \
      "$ROOT_DIR/scripts/diagnose_aod_continuous_loop.sh"
fi

{
  echo "AOD smoke suite summary"
  echo "time=$(now_utc)"
  echo "serial=$SERIAL"
  echo "autoSettleSec=$AUTO_SETTLE_SEC"
  echo "pulseEnterAodSec=$PULSE_ENTER_AOD_SEC"
  echo "pulseWaitSec=$PULSE_WAIT_SEC"
  echo "runContinuous=$RUN_CONTINUOUS"
  echo
  echo "steps:"
  while IFS=$'\t' read -r name status; do
    [[ -z "$name" ]] && continue
    echo "- $name exit=$status"
    if [[ -f "$OUT_DIR/$name/summary.txt" ]]; then
      grep -E '^(verdict:|RED |SUSPECT |NO_RED_SIGNAL|SUITE_|counts\.|package=|systemuiPid=)' \
        "$OUT_DIR/$name/summary.txt" || true
    else
      echo "SUSPECT missing summary for $name"
    fi
    echo
  done < "$STEP_STATUS_FILE"

  if grep -R -E '^(RED |SUSPECT )' "$OUT_DIR" >/dev/null 2>&1; then
    echo "SUITE_ATTENTION red or suspect signal captured"
  elif awk -F '\t' '$2 != "0" { found = 1 } END { exit found ? 0 : 1 }' "$STEP_STATUS_FILE"; then
    echo "SUITE_ATTENTION one or more diagnostic steps failed"
  else
    echo "SUITE_OK no red or suspect signal captured"
  fi
} > "$OUT_DIR/summary.txt"

cat "$OUT_DIR/summary.txt"
echo
echo "Artifacts: $OUT_DIR"
