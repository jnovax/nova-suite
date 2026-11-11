#!/usr/bin/env bash
# Compare certificates of the two installed APKs with the locally built launcher.
set -euo pipefail
DEVICE="${1:?Device required}"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APKSIGNER="${ANDROID_HOME:-/opt/android-sdk}/build-tools/34.0.0/apksigner"
LOCAL_APK="$ROOT_DIR/apps/launcher/build/outputs/apk/release/launcher-release.apk"
[ -x "$APKSIGNER" ] && [ -s "$LOCAL_APK" ] || { echo 'Build tools and launcher release APK are required.' >&2; exit 1; }
TEMP_DIR=$(mktemp -d)
cleanup() { unlink "$TEMP_DIR/launcher.apk" 2>/dev/null || true; unlink "$TEMP_DIR/familytime.apk" 2>/dev/null || true; rmdir "$TEMP_DIR" 2>/dev/null || true; }
trap cleanup EXIT
for pair in 'com.appletv.launcher:launcher' 'vn.familytv.parentalcontrol:familytime'; do
    package=${pair%:*}
    name=${pair#*:}
    remote=$(adb -s "$DEVICE" shell pm path "$package" | tr -d '\r' | sed -n 's/^package://p' | head -n 1)
    [ -n "$remote" ] || { echo "Not installed: $package" >&2; exit 1; }
    adb -s "$DEVICE" pull "$remote" "$TEMP_DIR/$name.apk" >/dev/null
    test -s "$TEMP_DIR/$name.apk"
done
fingerprint() {
    "$APKSIGNER" verify --print-certs "$1" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p'
}
expected=$(fingerprint "$LOCAL_APK")
launcher=$(fingerprint "$TEMP_DIR/launcher.apk")
familytime=$(fingerprint "$TEMP_DIR/familytime.apk")
[ -n "$expected" ] && [ "$launcher" = "$expected" ] && [ "$familytime" = "$expected" ] || { echo 'Installed APK certificates do not match the built launcher.' >&2; exit 1; }
echo "Launcher and FamilyTime installed certificates match: $expected"
