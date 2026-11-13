#!/usr/bin/env bash
# Deploy the three independently built Nova apps to an explicitly selected emulator.
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="${1:?Usage: deploy-nova-banners.sh emulator-NNNN}"
[[ "$TARGET" =~ ^emulator-[0-9]+$ ]] || { echo 'An emulator serial is required.' >&2; exit 1; }
export JAVA_HOME="${JAVA_HOME:-/opt/jdk17}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
WIN_ADB=/mnt/d/Android/platform-tools/adb.exe
if [ -x "$WIN_ADB" ]; then
    ADB=("$WIN_ADB")
else
    ADB=(adb)
fi
[[ "$("${ADB[@]}" -s "$TARGET" get-state | tr -d '\r')" == device ]] || exit 1
SNAPSHOT="$ROOT_DIR/backups/${TARGET}_original/metadata.env"
[[ -s "$SNAPSHOT" ]] || { echo "Missing emulator snapshot: $SNAPSHOT" >&2; exit 1; }
for app in nova-family nova-kara nova-quiz; do
    echo "==> Building $app"
    (cd "$ROOT_DIR/$app" && ./gradlew assembleRelease --offline)
done
status=0
for app in nova-family nova-kara nova-quiz; do
    apk="$ROOT_DIR/$app/app/build/outputs/apk/release/app-release.apk"
    [[ -f "$apk" ]] || { echo "Missing APK: $apk" >&2; status=1; continue; }
    if [ "${ADB[0]}" = "$WIN_ADB" ]; then apk="$(wslpath -w "$apk")"; fi
    echo "==> Installing $app on $TARGET"
    "${ADB[@]}" -s "$TARGET" install -r "$apk" || status=1
done
exit "$status"
