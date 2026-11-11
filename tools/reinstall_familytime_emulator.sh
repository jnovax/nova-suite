#!/usr/bin/env bash
# Explicit, data-erasing signing migration for an Android emulator only.
# Usage: ./tools/reinstall_familytime_emulator.sh DEVICE --erase-data
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DEVICE="${1:?Device required}"
[ "${2:-}" = "--erase-data" ] || { echo 'Pass --erase-data to confirm FamilyTime data will be erased.' >&2; exit 1; }
APK="$ROOT_DIR/apps/parental-control/build/outputs/apk/release/parental-control-release.apk"
PACKAGE="vn.familytv.parentalcontrol"
SERVICE="$PACKAGE/$PACKAGE.platform.ForegroundMonitorService"
[ -s "$APK" ] || { echo 'Build the FamilyTime release APK first.' >&2; exit 1; }
[ "$(adb -s "$DEVICE" shell getprop ro.kernel.qemu | tr -d '\r')" = 1 ] || { echo 'Refusing: target is not an Android emulator.' >&2; exit 1; }
OLD_PATH=$(adb -s "$DEVICE" shell pm path "$PACKAGE" | tr -d '\r' | sed -n 's/^package://p' | head -n 1)
[ -n "$OLD_PATH" ] || { echo 'FamilyTime is not installed; use normal deploy.' >&2; exit 1; }
OLD_SERVICES=$(adb -s "$DEVICE" shell settings get secure enabled_accessibility_services | tr -d '\r')
TAG="$(date +%Y%m%d_%H%M%S)_familytime_signing"
"$ROOT_DIR/tools/backup.sh" "$DEVICE" "$TAG"
CLEAN_DEV=$(printf '%s' "$DEVICE" | tr -c 'A-Za-z0-9._-' '_')
SNAPSHOT="$ROOT_DIR/backups/${CLEAN_DEV}_${TAG}"
test -s "$SNAPSHOT/metadata.env"
adb -s "$DEVICE" pull "$OLD_PATH" "$SNAPSHOT/previous_familytime.apk"
test -s "$SNAPSHOT/previous_familytime.apk"
restore_service() {
    case ":$OLD_SERVICES:" in
        *":$SERVICE:"*)
            current=$(adb -s "$DEVICE" shell settings get secure enabled_accessibility_services | tr -d '\r')
            case ":$current:" in
                *":$SERVICE:"*) ;;
                *)
                    if [ -z "$current" ] || [ "$current" = null ]; then current="$SERVICE"; else current="$current:$SERVICE"; fi
                    adb -s "$DEVICE" shell settings put secure enabled_accessibility_services "$current"
                    adb -s "$DEVICE" shell settings put secure accessibility_enabled 1
                    ;;
            esac
            ;;
    esac
}
echo 'Removing old FamilyTime and its app data from the emulator...'
adb -s "$DEVICE" uninstall "$PACKAGE"
if ! adb -s "$DEVICE" install -r "$APK"; then
    echo 'New APK install failed; restoring the previous APK (app data cannot be restored).' >&2
    adb -s "$DEVICE" install -r "$SNAPSHOT/previous_familytime.apk" || true
    restore_service || true
    exit 1
fi
restore_service
adb -s "$DEVICE" shell pm path "$PACKAGE"
echo "Previous APK and snapshot: $SNAPSHOT"
