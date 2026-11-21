#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

VERSION_NAME="$(sed -nE 's/^[[:space:]]*versionName[[:space:]]+"([^"]+)".*/\1/p' app/build.gradle | head -n 1)"
VERSION_CODE="$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]+([0-9]+).*/\1/p' app/build.gradle | head -n 1)"

if [[ -z "$VERSION_NAME" || -z "$VERSION_CODE" ]]; then
  echo "Failed to read versionName/versionCode from app/build.gradle" >&2
  exit 1
fi

TMP_KEYSTORE_DIR=""
cleanup() {
  if [[ -n "$TMP_KEYSTORE_DIR" && -d "$TMP_KEYSTORE_DIR" ]]; then
    rm -rf "$TMP_KEYSTORE_DIR"
  fi
}
trap cleanup EXIT

SIGNING_MODE="configured-env"
if [[ -z "${PIXEL_AOD_KEYSTORE_FILE:-}" || -z "${PIXEL_AOD_KEYSTORE_PASSWORD:-}" || -z "${PIXEL_AOD_KEY_ALIAS:-}" || -z "${PIXEL_AOD_KEY_PASSWORD:-}" ]]; then
  SIGNING_MODE="ephemeral-generated"
  TMP_KEYSTORE_DIR="$(mktemp -d)"
  KEYSTORE_PATH="$TMP_KEYSTORE_DIR/pixel-aod-release.p12"
  GEN_PASS="$(openssl rand -hex 24)"
  keytool -genkeypair \
    -storetype PKCS12 \
    -keystore "$KEYSTORE_PATH" \
    -storepass "$GEN_PASS" \
    -keypass "$GEN_PASS" \
    -alias pixel-aod-release \
    -keyalg RSA \
    -keysize 4096 \
    -validity 10000 \
    -dname "CN=Pixel AOD for OPlus, OU=Release, O=PixelAOD, C=US" >/dev/null 2>&1
  chmod 600 "$KEYSTORE_PATH"
  export PIXEL_AOD_KEYSTORE_FILE="$KEYSTORE_PATH"
  export PIXEL_AOD_KEYSTORE_PASSWORD="$GEN_PASS"
  export PIXEL_AOD_KEY_ALIAS="pixel-aod-release"
  export PIXEL_AOD_KEY_PASSWORD="$GEN_PASS"
fi

if command -v gradle >/dev/null 2>&1; then
  GRADLE_CMD=(gradle)
elif [[ -x "$PROJECT_DIR/gradlew" ]]; then
  GRADLE_CMD=("$PROJECT_DIR/gradlew")
else
  echo "Neither gradle nor ./gradlew found." >&2
  exit 127
fi

"${GRADLE_CMD[@]}" :app:assembleRelease --stacktrace

SIGNED_APK="app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$SIGNED_APK" ]]; then
  echo "Expected signed APK not found at $SIGNED_APK" >&2
  exit 1
fi

APKSIGNER="$(find "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/android/sdk}}/build-tools" -maxdepth 2 -name apksigner 2>/dev/null | sort -V | tail -n 1 || true)"
CERT_SHA256="unavailable"
if [[ -n "$APKSIGNER" && -x "$APKSIGNER" ]]; then
  CERT_OUT="$("$APKSIGNER" verify --verbose --print-certs "$SIGNED_APK")"
  CERT_SHA256="$(printf '%s\n' "$CERT_OUT" | sed -nE 's/.*certificate SHA-256 digest: ([0-9a-fA-F]+).*/\1/p' | head -n 1)"
fi

OUT_DIR="app/build/outputs/apk/release"
OUT_APK="${OUT_DIR}/PixelAOD-v${VERSION_NAME}-release.apk"
cp "$SIGNED_APK" "$OUT_APK"
(cd "$OUT_DIR" && sha256sum "$(basename "$OUT_APK")" > "$(basename "$OUT_APK").sha256")

echo "=== Release Build Complete ==="
echo "Version:      ${VERSION_NAME} (${VERSION_CODE})"
echo "Signing mode: ${SIGNING_MODE}"
echo "Signed APK:   ${OUT_APK}"
echo "APK SHA-256:  $(awk '{print $1}' "${OUT_APK}.sha256")"
echo "Cert SHA-256: ${CERT_SHA256}"
