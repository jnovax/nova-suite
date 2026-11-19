#!/usr/bin/env bash
# Deploy the three independently built Nova apps to an explicitly selected device.
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="${1:?Usage: deploy-nova-banners.sh emulator-NNNN|TV_IP:PORT}"
COMPONENT="${2:-all}"
case "$COMPONENT" in
    all) APPS=(nova-family nova-kara nova-quiz) ;;
    family) APPS=(nova-family) ;;
    *) echo 'Component must be all or family.' >&2; exit 1 ;;
esac
[[ "$TARGET" =~ ^emulator-[0-9]+$ || "$TARGET" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:[0-9]+$ ]] || { echo 'An explicit emulator serial or TV IP:port is required.' >&2; exit 1; }
export JAVA_HOME="${JAVA_HOME:-/opt/jdk17}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
WIN_ADB=/mnt/d/Android/platform-tools/adb.exe
if [[ "$TARGET" == emulator-* ]] && [ -x "$WIN_ADB" ]; then
    ADB=("$WIN_ADB")
else
    ADB=(adb)
fi
[[ "$("${ADB[@]}" -s "$TARGET" get-state </dev/null | tr -d '\r')" == device ]] || exit 1
CLEAN_TARGET="$(printf '%s' "$TARGET" | tr -c 'A-Za-z0-9._-' '_')"
SNAPSHOT="$ROOT_DIR/backups/${CLEAN_TARGET}_original/metadata.env"
[[ -s "$SNAPSHOT" ]] || { echo "Missing device snapshot: $SNAPSHOT" >&2; exit 1; }
for app in "${APPS[@]}"; do
    echo "==> Building $app"
    (cd "$ROOT_DIR/$app" && ./gradlew assembleRelease --offline)
done
status=0
for app in "${APPS[@]}"; do
    apk="$ROOT_DIR/$app/app/build/outputs/apk/release/app-release.apk"
    [[ -f "$apk" ]] || { echo "Missing APK: $apk" >&2; status=1; continue; }
    if [ "${ADB[0]}" = "$WIN_ADB" ]; then apk="$(wslpath -w "$apk")"; fi
    echo "==> Installing $app on $TARGET"
    "${ADB[@]}" -s "$TARGET" install -r "$apk" </dev/null || status=1

    if [ "$app" = "nova-family" ]; then
        echo "==> Granting permissions & configuring Accessibility for FamilyTime on $TARGET..."
        "${ADB[@]}" -s "$TARGET" shell appops set vn.familytv.parentalcontrol SYSTEM_ALERT_WINDOW allow </dev/null >/dev/null 2>&1 || true
        "${ADB[@]}" -s "$TARGET" shell appops set vn.familytv.parentalcontrol GET_USAGE_STATS allow </dev/null >/dev/null 2>&1 || true
        "${ADB[@]}" -s "$TARGET" shell appops set vn.familytv.parentalcontrol ACCESS_RESTRICTED_SETTINGS allow </dev/null >/dev/null 2>&1 || true
        "${ADB[@]}" -s "$TARGET" shell pm grant vn.familytv.parentalcontrol android.permission.POST_NOTIFICATIONS </dev/null >/dev/null 2>&1 || true

        cur_a11y=$("${ADB[@]}" -s "$TARGET" shell settings get secure enabled_accessibility_services </dev/null 2>/dev/null | tr -d '\r' || true)
        [ "$cur_a11y" = "null" ] && cur_a11y=""
        stripped=$(echo "$cur_a11y" | tr ':' '\n' | grep -v "^vn\.familytv\.parentalcontrol" | tr '\n' ':' | sed -E 's/:$//' || true)
        "${ADB[@]}" -s "$TARGET" shell settings put secure enabled_accessibility_services "${stripped:-:}" </dev/null >/dev/null 2>&1 || true
        sleep 0.5

        service_name="vn.familytv.parentalcontrol/.platform.ForegroundMonitorService"
        if [ -n "$stripped" ]; then
            new_a11y="${stripped}:${service_name}"
        else
            new_a11y="${service_name}"
        fi
        "${ADB[@]}" -s "$TARGET" shell settings put secure enabled_accessibility_services "$new_a11y" </dev/null >/dev/null 2>&1 || true
        "${ADB[@]}" -s "$TARGET" shell settings put secure accessibility_enabled 1 </dev/null >/dev/null 2>&1 || true

        "${ADB[@]}" -s "$TARGET" shell am start -n vn.familytv.parentalcontrol/.MainActivity </dev/null >/dev/null 2>&1 || true
        sleep 0.5
        "${ADB[@]}" -s "$TARGET" shell am start -a android.intent.action.MAIN -c android.intent.category.HOME </dev/null >/dev/null 2>&1 || true
    fi
done
exit "$status"
