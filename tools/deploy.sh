#!/usr/bin/env bash
# tools/deploy.sh — Selective APK builder and deployer
# Usage: ./tools/deploy.sh <launcher|parental-control|flipclock|landscape|screensavers|all> [TARGET_DEV]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Ensure Java and Android SDK environment if present
if [ -z "${JAVA_HOME:-}" ] && [ -d "/opt/jdk17" ]; then
    export JAVA_HOME="/opt/jdk17"
fi
if [ -z "${ANDROID_HOME:-}" ] && [ -d "/opt/android-sdk" ]; then
    export ANDROID_HOME="/opt/android-sdk"
fi
if [ -n "${JAVA_HOME:-}" ] || [ -n "${ANDROID_HOME:-}" ]; then
    export PATH="${JAVA_HOME:-/usr}/bin:${ANDROID_HOME:-/opt/android-sdk}/platform-tools:${ANDROID_HOME:-/opt/android-sdk}/cmdline-tools/latest/bin:$PATH"
fi

is_target() {
    case "$1" in
        launcher|parental-control|parental|flipclock|landscape|screensavers|all)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

ARG1="${1:-}"
ARG2="${2:-}"

TARGET="all"
TARGET_DEV=""

if [ -n "$ARG1" ]; then
    if is_target "$ARG1"; then
        TARGET="$ARG1"
        TARGET_DEV="$ARG2"
    else
        # ARG1 is likely device ID or IP
        TARGET_DEV="$ARG1"
        if [ -n "$ARG2" ]; then
            if is_target "$ARG2"; then
                TARGET="$ARG2"
            else
                echo "Error: Unknown target '$ARG2'. Use: launcher | parental-control | flipclock | landscape | screensavers | all" >&2
                exit 1
            fi
        fi
    fi
fi

# Fallback to environment variables if TARGET_DEV not specified
TARGET_DEV="${TARGET_DEV:-${TV_HOST:-${TV_IP:-}}}"

ADB_CMD="adb"
IS_WIN_ADB=false
ADB_EXTRA=()

is_port_reachable() {
    local host="$1"
    local port="$2"
    if command -v nc >/dev/null 2>&1; then
        nc -z -w 1 "$host" "$port" >/dev/null 2>&1
        return $?
    fi
    if command -v ping >/dev/null 2>&1; then
        ping -c 1 -W 1 "$host" >/dev/null 2>&1
        return $?
    fi
    return 0
}

safe_adb_connect() {
    local target="$1"
    local host="${target%:*}"
    local port="${target#*:}"
    [ "$port" = "$host" ] && port="5555"

    if ! is_port_reachable "$host" "$port"; then
        return 1
    fi

    if command -v timeout >/dev/null 2>&1; then
        timeout 5 adb connect "$target" >/dev/null 2>&1 || true
    else
        adb connect "$target" >/dev/null 2>&1 || true
    fi
}

# If TARGET_DEV was explicitly specified, normalize and connect if network device
if [ -n "$TARGET_DEV" ]; then
    if [[ "$TARGET_DEV" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        TARGET_DEV="${TARGET_DEV}:5555"
    fi
    if [[ "$TARGET_DEV" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:[0-9]+$ ]]; then
        echo "==> Connecting to $TARGET_DEV..."
        safe_adb_connect "$TARGET_DEV" || true
    fi

    if [[ "$TARGET_DEV" == emulator-* || "$TARGET_DEV" == 127.0.0.1:* ]] && [ -x "/mnt/d/Android/platform-tools/adb.exe" ]; then
        ADB_CMD="/mnt/d/Android/platform-tools/adb.exe"
        IS_WIN_ADB=true
        if /mnt/d/Android/platform-tools/adb.exe -P 5038 devices </dev/null 2>/dev/null | grep -q "$TARGET_DEV"; then
            ADB_EXTRA=(-P 5038)
        fi
    fi
else
    # Auto-discovery:
    # 1. Check already connected devices in adb
    TARGET_DEV=$(adb devices 2>/dev/null | grep -E '\b(device)\b' | awk '{print $1}' | head -n 1 || true)

    # 2. Check Windows adb for WSL emulators
    if [ -z "$TARGET_DEV" ] && [ -x "/mnt/d/Android/platform-tools/adb.exe" ]; then
        TARGET_DEV=$(/mnt/d/Android/platform-tools/adb.exe devices </dev/null 2>/dev/null | grep -E '\b(device)\b' | awk '{print $1}' | head -n 1 || true)
        if [ -z "$TARGET_DEV" ]; then
            TARGET_DEV=$(/mnt/d/Android/platform-tools/adb.exe -P 5038 devices </dev/null 2>/dev/null | grep -E '\b(device)\b' | awk '{print $1}' | head -n 1 || true)
            if [ -n "$TARGET_DEV" ]; then
                ADB_EXTRA=(-P 5038)
            fi
        fi
        if [ -n "$TARGET_DEV" ]; then
            ADB_CMD="/mnt/d/Android/platform-tools/adb.exe"
            IS_WIN_ADB=true
        fi
    fi

    # 3. Check mDNS discovery
    if [ -z "$TARGET_DEV" ]; then
        MDNS_DEV=$(adb mdns services 2>/dev/null | grep -E '_adb\._tcp|_adb-tls-connect\._tcp' | head -n 1 | awk '{print $NF}' || true)
        if [ -n "$MDNS_DEV" ]; then
            echo "==> Auto-discovered Android TV via mDNS: $MDNS_DEV. Connecting..."
            safe_adb_connect "$MDNS_DEV" || true
            if [ "$(adb -s "$MDNS_DEV" get-state 2>/dev/null | tr -d '\r' || true)" = "device" ]; then
                TARGET_DEV="$MDNS_DEV"
            fi
        fi
    fi

    # 4. Probe known default TV targets
    if [ -z "$TARGET_DEV" ]; then
        DEFAULT_TARGETS=("192.168.1.7:5555" "192.168.1.10:5555")
        for cand in "${DEFAULT_TARGETS[@]}"; do
            if safe_adb_connect "$cand"; then
                echo "==> Found reachable TV target at $cand. Connected."
                if [ "$(adb -s "$cand" get-state 2>/dev/null | tr -d '\r' || true)" = "device" ]; then
                    TARGET_DEV="$cand"
                    break
                fi
            fi
        done
    fi
fi

if [ -z "$TARGET_DEV" ]; then
    echo "❌ Error: Could not detect or connect to any Android TV device." >&2
    echo "" >&2
    echo "Probed addresses:" >&2
    echo "  - 192.168.1.7:5555 (default primary TV)" >&2
    echo "  - 192.168.1.10:5555 (secondary TV)" >&2
    echo "  - mDNS discovery" >&2
    echo "" >&2
    echo "Troubleshooting:" >&2
    echo "  1. Make sure the TV is turned ON and connected to Wi-Fi / LAN." >&2
    echo "  2. Check TV IP address: Settings ⚙️ -> Network & Internet -> Connected Wi-Fi -> IP address." >&2
    echo "  3. Ensure USB Debugging is ON in TV Developer Options." >&2
    echo "  4. Pass the TV IP directly: ./tools/deploy.sh $TARGET <TV_IP>" >&2
    exit 1
fi

adb_exec() {
    if [ "$IS_WIN_ADB" = true ]; then
        "$ADB_CMD" ${ADB_EXTRA[@]+"${ADB_EXTRA[@]}"} "$@" </dev/null
    else
        adb "$@"
    fi
}

DEV_STATE=$(adb_exec -s "$TARGET_DEV" get-state 2>/dev/null | tr -d '\r' || true)
if [ "$DEV_STATE" != "device" ]; then
    echo "❌ Error: Device $TARGET_DEV is not in 'device' state (currently: '${DEV_STATE:-disconnected}')." >&2
    if [ "$DEV_STATE" = "unauthorized" ]; then
        echo "Tip: Please look at your TV screen and accept the 'Allow USB debugging' prompt." >&2
    else
        echo "Tip: Ensure the TV is on and connected to the same network, then retry." >&2
    fi
    exit 1
fi

echo "=========================================================="
echo "  🚀 Deploying Target: $TARGET to $TARGET_DEV (via $ADB_CMD ${ADB_EXTRA[*]})"
echo "=========================================================="

adb_install() {
    local apk="$1"
    local install_cmd
    if [ "$IS_WIN_ADB" = true ]; then
        mkdir -p /mnt/d/Android/temp
        local fname="$(basename "$apk")"
        cp "$apk" "/mnt/d/Android/temp/$fname"
        install_cmd=("$ADB_CMD" ${ADB_EXTRA[@]+"${ADB_EXTRA[@]}"} -s "$TARGET_DEV" install -r "D:\\Android\\temp\\$fname")
    else
        install_cmd=(adb -s "$TARGET_DEV" install -r "$apk")
    fi

    local out
    if ! out=$("${install_cmd[@]}" </dev/null 2>&1); then
        echo "$out" >&2
        if echo "$out" | grep -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE"; then
            echo "⚠️ Signature mismatch detected for $(basename "$apk"). Existing app was signed with a different key." >&2
            local pkg=""
            local aapt_bin="${ANDROID_HOME:-/opt/android-sdk}/build-tools/34.0.0/aapt"
            if [ -x "$aapt_bin" ]; then
                pkg=$("$aapt_bin" dump badging "$apk" 2>/dev/null | grep -E "^package: name=" | sed -E "s/.*name='([^']+)'.*/\1/" || true)
            elif command -v aapt >/dev/null 2>&1; then
                pkg=$(aapt dump badging "$apk" 2>/dev/null | grep -E "^package: name=" | sed -E "s/.*name='([^']+)'.*/\1/" || true)
            fi
            if [ -n "$pkg" ]; then
                echo "==> Performing clean reinstall of $pkg (preserving accessibility settings)..."
                local old_acc
                old_acc=$(adb_exec -s "$TARGET_DEV" shell settings get secure enabled_accessibility_services 2>/dev/null | tr -d '\r' || true)
                adb_exec -s "$TARGET_DEV" uninstall "$pkg" || true
                "${install_cmd[@]}" </dev/null
                if [ -n "$old_acc" ] && [ "$old_acc" != "null" ]; then
                    adb_exec -s "$TARGET_DEV" shell settings put secure enabled_accessibility_services "$old_acc" 2>/dev/null || true
                    adb_exec -s "$TARGET_DEV" shell settings put secure accessibility_enabled 1 2>/dev/null || true
                fi
                echo "✅ Successfully reinstalled $pkg"
                return 0
            fi
        fi
        return 1
    fi
    echo "$out"
}

sync_time() {
    local host_s dev_s
    host_s=$(date +%s)
    dev_s=$(adb_exec -s "$TARGET_DEV" shell date +%s 2>/dev/null || echo 0)
    dev_s=$(echo "$dev_s" | tr -d '\r\n')
    if [[ "$dev_s" =~ ^[0-9]+$ ]] && [ "$dev_s" -gt 0 ]; then
        local diff=$(( host_s - dev_s ))
        diff=${diff#-}
        if [ "$diff" -gt 60 ]; then
            echo "==> Syncing device clock with host ($diff seconds drift)..."
            local now_ms
            now_ms=$(python3 -c 'import time; print(int(time.time() * 1000))' 2>/dev/null || echo "${host_s}000")
            adb_exec -s "$TARGET_DEV" shell cmd alarm set-time "$now_ms" 2>/dev/null || true
        fi
    fi
}

deploy_launcher() {
    echo "==> Building Launcher APK..."
    (cd "$ROOT_DIR" && ./gradlew :apps:launcher:assembleRelease)
    local apk="$ROOT_DIR/apps/launcher/build/outputs/apk/release/apps-launcher-release.apk"
    if [ ! -f "$apk" ]; then
        apk="$ROOT_DIR/apps/launcher/build/outputs/apk/release/launcher-release.apk"
    fi
    if [ ! -f "$apk" ]; then
        apk=$(find "$ROOT_DIR/apps/launcher/build/outputs/apk/release" -name "*.apk" 2>/dev/null | head -n 1 || true)
    fi
    if [ -z "$apk" ] || [ ! -f "$apk" ]; then
        echo "Error: Launcher APK not found in $ROOT_DIR/apps/launcher/build/outputs/apk/release" >&2
        exit 1
    fi
    echo "==> Installing Launcher to $TARGET_DEV..."
    adb_install "$apk"
    echo "==> Restarting Launcher..."
    adb_exec -s "$TARGET_DEV" shell am start -n com.appletv.launcher/.MainActivity
}

deploy_parental() {
    echo "==> Building Parental Control APK..."
    (cd "$ROOT_DIR" && ./gradlew :apps:parental-control:assembleRelease)
    local apk=""
    if [ -d "$ROOT_DIR/apps/parental-control/build/outputs/apk/release" ]; then
        apk=$(find "$ROOT_DIR/apps/parental-control/build/outputs/apk/release" -name "*.apk" 2>/dev/null | head -n 1 || true)
    fi
    if [ -z "$apk" ] || [ ! -f "$apk" ]; then
        echo "Error: Parental Control APK not found in $ROOT_DIR/apps/parental-control/build/outputs/apk/release" >&2
        exit 1
    fi
    echo "==> Installing Parental Control to $TARGET_DEV..."
    adb_install "$apk"
}

deploy_flipclock() {
    echo "==> Building Screensaver FlipClock APK..."
    (cd "$ROOT_DIR" && ./gradlew :apps:screensaver-flipclock:assembleRelease)
    local apk=""
    if [ -d "$ROOT_DIR/apps/screensaver-flipclock/build/outputs/apk/release" ]; then
        apk=$(find "$ROOT_DIR/apps/screensaver-flipclock/build/outputs/apk/release" -name "*.apk" 2>/dev/null | head -n 1 || true)
    fi
    if [ -z "$apk" ] || [ ! -f "$apk" ]; then
        echo "Error: FlipClock APK not found" >&2
        exit 1
    fi
    echo "==> Installing FlipClock Screensaver to $TARGET_DEV..."
    adb_install "$apk"
}

deploy_landscape() {
    echo "==> Building Screensaver Landscape APK..."
    (cd "$ROOT_DIR" && ./gradlew :apps:screensaver-landscape:assembleRelease)
    local apk=""
    if [ -d "$ROOT_DIR/apps/screensaver-landscape/build/outputs/apk/release" ]; then
        apk=$(find "$ROOT_DIR/apps/screensaver-landscape/build/outputs/apk/release" -name "*.apk" 2>/dev/null | head -n 1 || true)
    fi
    if [ -z "$apk" ] || [ ! -f "$apk" ]; then
        echo "Error: Landscape Screensaver APK not found" >&2
        exit 1
    fi
    echo "==> Installing Landscape Screensaver to $TARGET_DEV..."
    adb_install "$apk"
}


sync_time

# Clean up obsolete Art Museum screensaver if present on target device
if adb_exec -s "$TARGET_DEV" shell pm list packages com.appletv.screensaver.artmuseum 2>/dev/null | grep -q "com.appletv.screensaver.artmuseum"; then
    echo "==> Removing obsolete Art Museum screensaver ($TARGET_DEV)..."
    adb_exec -s "$TARGET_DEV" uninstall com.appletv.screensaver.artmuseum </dev/null >/dev/null 2>&1 || true
fi

case "$TARGET" in
    launcher)
        deploy_launcher
        ;;
    parental-control|parental)
        deploy_parental
        ;;
    flipclock)
        deploy_flipclock
        ;;
    landscape)
        deploy_landscape
        ;;
    screensavers)
        deploy_flipclock
        deploy_landscape
        ;;
    all)
        deploy_parental
        deploy_flipclock
        deploy_landscape
        deploy_launcher
        ;;
esac

echo "✅ Deploy completed successfully!"
