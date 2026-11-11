# Agent Runbook: Autonomous Android TV Transformation

This document is the single source of truth for AI agents (Antigravity, Claude Code, Cursor, Devin, Roo Code, Aider, Windsurf) executing tasks in this repository.

---

## 🎯 Primary Goal & Current Version (`v1.2.0`)
Transform any connected Android TV or Google TV (Sony, TCL, Xiaomi, OnePlus, Hisense, Shield, Chromecast, Fire TV) into an ultra-clean, ad-free Apple TV family ecosystem with:
1. **tvOS 18 Home Launcher (`apps/launcher`, ~298 KB):**
   - Lowered dock platter (`20dp` bottom margin) with tight `14dp` transition gap to the app grid below and persistent app labels under grid tiles (`UNHOVERED_NAME_ALPHA = 0.78f`, `1.0f` on focus).
   - **tvOS Long-Press `OK` Context Menu:** Pin/Unpin to Dock, Move Left/Right on Dock, Hide App, and System App Info directly via remote (strictly locked in `Kid Mode`).
   - **Control Center (`QuickSettingsActivity` + `HomeKeyService`):** In-place **Sleep Timer** (`Off / 15m / 30m / 60m / 90m` -> stops media and triggers TV standby via `GLOBAL_ACTION_LOCK_SCREEN`), **Night Dimmer** (`Off / 25% / 50% / 70%` via `TYPE_ACCESSIBILITY_OVERLAY`), **Bluetooth & Audio** shortcut, and **1-Click Screensaver**.
   - **Auto `[ 💾 USB ]` Top-Bar Pill:** Automatically appears when a removable USB/external drive is mounted; opens a 2-row glass menu for **Nova Video Player** (`org.courville.nova`) and **Storage & Safely Eject**.
   - **On-TV Remote Key Mapper & Hidden Apps Manager (`LauncherSettingsActivity`):** Interactive key capture dialog to bind any non-protected remote button to launcher actions or installed apps without ADB, plus 1-click app unhide.
2. **FamilyTime Parental Control (`apps/parental-control`) & Shared IPC (`core/contract`):**
   - Daily screen-time tracking (`ScreenTimeTracker`), 24-hour visual usage timeline (`UsageTimelineAnalyzer`, `TimelineBarView`), full-screen Time-Lock overlay (`TimeLockOverlayManager`) with quick time extensions (`+15m / +30m / +1h / All day`), and Parent PIN / Google Authenticator TOTP unlock (`TotpEngine`).
   - Live **Dynamic Time Pill (`[ 🧒 12m ]`)** on the Launcher top bar with traffic-light color alerts (White > 5m, Gold 2–5m, Red ≤ 2m) and automatic Kid Mode lock on screen-off.
3. **Native Apple TV Recent Apps Switcher (`RecentAppsActivity`):** Double-press Home gesture with live usage stats and background process termination.
4. **100% Hardware-Speed Volume Hold Acceleration:** Navigation, volume, and power keys are strictly protected (`Config.isProtectedRemoteKey`) and never intercepted in accessibility services.
5. **Safe, Reversible Multi-Brand Debloating & Official SmartTube.**

👉 *See [`CHANGELOG.md`](CHANGELOG.md) for full release notes and version history.*

---

## 🏗️ Monorepo Architecture

```
nova-tv-launcher/
├── apps/
│   ├── launcher/              # ID: com.appletv.launcher (tvOS 18 Home Launcher, ~298 KB)
│   ├── parental-control/      # ID: vn.familytv.parentalcontrol (FamilyTime Parental Control, ~144 KB)
│   ├── screensaver-flipclock/ # ID: com.appletv.screensaver.flipclock (Fliqlo Split-Flap Clock, ~19 KB)
│   └── screensaver-landscape/ # ID: com.appletv.screensaver.landscape (Landscape Art)
├── core/
│   └── contract/              # Shared IPC Contract (ModeContract, StatusProviderContract, TimeFormatter)
├── tools/                     # Unified ADB audit, deploy, backup, restore, and 4K wallpaper tools
└── profiles/                  # Verified multi-brand debloat profiles
```

---

## ⚡ Standard Execution Workflows

### A. Selective Build & Deployment (For Development / Updates)
```bash
# Deploy individual components, screensavers, or entire ecosystem to TV
./tools/deploy.sh <launcher|parental-control|flipclock|screensavers|all> [TV_IP]
```

### B. Full TV Setup & Debloat Workflow (Audit First)
When a user asks you to set up and debloat their TV at an IP address:

```bash
# Step 1: Run read-only audit (ZERO changes to TV)
./tools/audit.sh <TV_IP>

# Step 2: Present summary from reports/report_<ts>.md to user and get approval.

# Step 3: Run approved apply script
bash reports/apply_<ts>.sh
```

*(Optional 1-Click Interactive Mode: `./tools/setup.sh <TV_IP>`)*

### C. Simulation & Emulator Workflow
```bash
# Android TV Emulator / Local AVD:
./tools/deploy.sh all [EMULATOR_DEVICE_ID]
```
👉 *See [`docs/SIMULATION.md`](docs/SIMULATION.md) for full emulator, ADB testing, and simulation documentation.*

---

## 🛠️ Tool Suite Reference

The repository provides a single, unified tool suite. Always use these tools rather than manual raw ADB commands:

1. **`./tools/deploy.sh <launcher|parental-control|all> [TV_IP]`**:
   - Builds and installs `apps/launcher`, `apps/parental-control`, or both onto the target TV.
2. **`./tools/deploy_apk.sh [TV_IP]`**:
   - Captures a pre-install state snapshot (including pulling the previous installed APK to `previous.apk`) before installing a new Launcher APK.
3. **`./tools/audit.sh [TV_IP]`**:
   - 100% Read-Only inspection of TV specs, running packages, Home owner, and HDMI inputs.
   - Saves Day-1 baseline snapshot in `backups/<device>_original/`.
   - Generates `reports/report_<ts>.md` and editable `reports/plan_<ts>.txt`.
4. **`./tools/apply.sh <plan_file> [TV_IP]`** (or `bash reports/apply_<ts>.sh`):
   - Executes approved plan with atomic safety:
     - Pre-flight APK download & verification
     - Safety state snapshot in `backups/<device>_<ts>/`
     - Apple TV Launcher install & launch test (`am start -W`)
     - Home handover to `com.appletv.launcher` with auto-rollback
     - Debloat approved packages (strictly skipping `tools/protected.txt`)
     - SmartTube installation & stock YouTube disable
5. **`./tools/restore.sh <snapshot_dir> [TV_IP]`**:
   - Restores original stock launcher as default Home.
   - Re-enables all original system packages.
   - Resets animation scales back to original values.
   - Re-enables stock YouTube.
   - Offers clean uninstallation of Apple TV Launcher and SmartTube.
6. **`./tools/sync_wallpaper.sh <image_path> [TV_IP]`**:
   - Normalizes any input image to native 4K UHD (`3840x2160`, center-crop, MozJPEG quality 92).
   - Syncs safely via `/data/local/tmp` staging to bypass Android TV FUSE `fchown` restriction.
   - Destination on TV: `/sdcard/Android/data/com.appletv.launcher/files/wallpapers/`.
   - Verified via MD5 checksum matching. See [`docs/WALLPAPERS.md`](docs/WALLPAPERS.md) for full TV specs and constraints.

---

## 🖼️ TV Profile & Wallpaper Specifications

- **Default TV Targets**: `192.168.1.7:5555` / `192.168.1.10:5555` (TCL Smart TV Pro `G08`, Android 14, Board: `merak`).
- **Display**: Physical 4K UHD (`3840x2160`), UI Override `1920x1080` (320 dpi).
- **Code Constraints**:
  - `WallpaperSize.MAX_PIXELS = 8_400_000L`: Any image > 8.4M pixels (e.g. 4072x2280) is forced to `inSampleSize = 2` (1080p downscaled decode).
  - Target format is strictly `.jpg` (required by `rotate_daily.sh` daemon).
  - Always stage through `/data/local/tmp` before copying to `/sdcard/...` to avoid ADB `fchown: Operation not permitted`.
  - Detailed reference: [`docs/WALLPAPERS.md`](docs/WALLPAPERS.md).
  - Remote mapping reference: [`docs/REMOTE_MAPPING.md`](docs/REMOTE_MAPPING.md).

---

## 💻 Build & Development Environment

- **Java Development Kit:** `JAVA_HOME=/opt/jdk17` (OpenJDK 17.0.21)
- **Android SDK:** `ANDROID_HOME=/opt/android-sdk`
- **Local SDK Configuration:** `local.properties` contains `sdk.dir=/opt/android-sdk`
- **System PATH:** `$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH`
- **Gradle Release Build (All Modules):** `./gradlew assembleRelease`
- **Gradle Unit Tests (All Modules):** `./gradlew testDebugUnitTest :core:contract:test`
- **Detailed Reference:** [`docs/ENVIRONMENT.md`](docs/ENVIRONMENT.md)

---

## 🛡️ Critical Guardrails for Agents

1. **NEVER Disable Protected System Services:**
   - Always honor `tools/protected.txt` (Chromecast, Assistant, Gboard, Wi-Fi, System UI, GMS, setup wizards).
   - Never disable stock launchers via generic debloat; launcher handover is exclusively handled by `apply.sh`.
2. **Preserve Linux `InputDispatcher` Repeat Rate (Volume & D-Pad Keys):**
   - Accessibility services (`HomeKeyService`, `InputKeyService`) exist exclusively for Home double-press, TV input switching, custom shortcut keys, Night Dimmer overlay, and Sleep Timer standby.
   - Navigation, volume, and power keys (`Config.isProtectedRemoteKey`) must NEVER be intercepted or consumed in accessibility services to preserve Android's native repeat queue and hold acceleration.
3. **Kid Mode Security Boundary:**
   - Layout customization features (Long-press `OK` tile menu, Dock pin/unpin/reorder, App hiding/unhiding, Remote Key Mapper, and `LauncherSettingsActivity`) must remain strictly blocked whenever `ModeManager.isKidMode(ctx)` is `true`.
4. **Always Ensure Reversibility:**
   - Always verify that a snapshot exists before touching the TV.
   - To restore: `./tools/restore.sh backups/<device_snapshot_directory> [TV_IP]`.
5. **AI Agent Autonomous Approval Ban:**
   - AI agents must NEVER execute `./tools/setup.sh -y` or `./tools/apply.sh` autonomously without explicit user approval ("Approved" / "Proceed") in chat.
