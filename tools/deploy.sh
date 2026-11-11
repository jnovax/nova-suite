#!/usr/bin/env bash
# tools/deploy.sh — Selective APK builder and deployer
# Usage: ./tools/deploy.sh <launcher|parental-control|flipclock|artmuseum|landscape|screensavers|all> [TARGET_DEV]
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

TARGET="${1:-all}"
TARGET_DEV="${2:-}"

# Validate target argument before device discovery
case "$TARGET" in
    launcher|parental-control|parental|flipclock|artmuseum|landscape|screensavers|all)
        ;;
    *)
        echo "Error: Unknown target '$TARGET'. Use: launcher | parental-control | flipclock | artmuseum | landscape | screensavers | all" >&2
        exit 1
        ;;
esac

ADB_CMD="adb"
IS_WIN_ADB=false
ADB_EXTRA=()

if [ -n "$TARGET_DEV" ]; then
    if [[ "$TARGET_DEV" == emulator-* || "$TARGET_DEV" == 127.0.0.1:* ]] && [ -x "/mnt/d/Android/platform-tools/adb.exe" ]; then
        ADB_CMD="/mnt/d/Android/platform-tools/adb.exe"
        IS_WIN_ADB=true
        if /mnt/d/Android/platform-tools/adb.exe -P 5038 devices </dev/null 2>/dev/null | grep -q "$TARGET_DEV"; then
            ADB_EXTRA=(-P 5038)
        fi
    fi
else
    TARGET_DEV=$(adb devices 2>/dev/null | grep -E '\b(device)\b' | awk '{print $1}' | head -n 1 || true)
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
fi

if [ -z "$TARGET_DEV" ]; then
    echo "Error: No ADB device connected." >&2
    exit 1
fi

echo "=========================================================="
echo "  🚀 Deploying Target: $TARGET to $TARGET_DEV (via $ADB_CMD ${ADB_EXTRA[*]})"
echo "=========================================================="

adb_exec() {
    if [ "$IS_WIN_ADB" = true ]; then
        "$ADB_CMD" ${ADB_EXTRA[@]+"${ADB_EXTRA[@]}"} "$@" </dev/null
    else
        adb "$@"
    fi
}

adb_install() {
    local apk="$1"
    if [ "$IS_WIN_ADB" = true ]; then
        mkdir -p /mnt/d/Android/temp
        local fname="$(basename "$apk")"
        cp "$apk" "/mnt/d/Android/temp/$fname"
        "$ADB_CMD" ${ADB_EXTRA[@]+"${ADB_EXTRA[@]}"} -s "$TARGET_DEV" install -r "D:\\Android\\temp\\$fname" </dev/null
    else
        adb -s "$TARGET_DEV" install -r "$apk"
    fi
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

deploy_artmuseum() {
    echo "==> Building Screensaver Art Museum APK..."
    (cd "$ROOT_DIR" && ./gradlew :apps:screensaver-artmuseum:assembleRelease)
    local apk=""
    if [ -d "$ROOT_DIR/apps/screensaver-artmuseum/build/outputs/apk/release" ]; then
        apk=$(find "$ROOT_DIR/apps/screensaver-artmuseum/build/outputs/apk/release" -name "*.apk" 2>/dev/null | head -n 1 || true)
    fi
    if [ -z "$apk" ] || [ ! -f "$apk" ]; then
        echo "Error: Art Museum APK not found" >&2
        exit 1
    fi
    echo "==> Installing Art Museum Screensaver to $TARGET_DEV..."
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
    artmuseum)
        deploy_artmuseum
        ;;
    landscape)
        deploy_landscape
        ;;
    screensavers)
        deploy_flipclock
        deploy_artmuseum
        deploy_landscape
        ;;
    all)
        deploy_parental
        deploy_flipclock
        deploy_artmuseum
        deploy_landscape
        deploy_launcher
        ;;
esac

echo "✅ Deploy completed successfully!"
