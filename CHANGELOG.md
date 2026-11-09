# Changelog & Release Notes

All notable changes to the `nova-tv-launcher` ecosystem (`apps/launcher`, `apps/parental-control`, `core/contract`, and `tools/`) are documented in this file.

---

## [v1.2.0] — 2026-11-09
### ✨ Zero-Bloat Practical TV Suite & Refined tvOS Dock Geometry (`versionCode 6`)

#### Added
- **tvOS Long-Press `OK` Tile Context Menu (`MainActivity`, `AppAdapter`, `Config`):**
  - Holding `OK` on any app tile in `Parent Mode` opens a frosted glass context menu directly under the tile:
    - **Pin to Dock / Unpin from Dock**: Customize dock favorites directly from the TV remote without editing `config.json`.
    - **Move Left / Move Right**: Reorder pinned apps on the Dock row.
    - **Hide App**: Instantly hide clutter apps from the grid (protects the Launcher and `vn.familytv.parentalcontrol` from accidental hiding).
    - **App Info**: Quick access to Android system application details (Force Stop / Clear Cache).
  - Strictly blocked in `Kid Mode` so children cannot alter the layout or Force Stop services.
- **Upgraded Control Center (`QuickSettingsActivity`, `HomeKeyService`):**
  - **Sleep Timer (`Off ➔ 15m ➔ 30m ➔ 60m ➔ 90m`)**: Cycles in-place with live remaining minutes indicator. When the timer reaches `0`, `HomeKeyService` dispatches `KEYCODE_MEDIA_STOP`, returns to Home, and puts the TV into standby via `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` (with Screensaver fallback).
  - **Night Dimmer (`Off ➔ 25% ➔ 50% ➔ 70%`)**: Hardware-accelerated full-screen dark scrim (`TYPE_ACCESSIBILITY_OVERLAY`) to reduce eye strain in dark rooms without altering TV backlight calibration. Automatically removes the view when `Off` (`0 byte` RAM overhead).
  - **Bluetooth & Audio Shortcut**: 1-click access to pair Bluetooth headphones or soundbars (`ACTION_BLUETOOTH_SETTINGS` with audio settings fallback).
  - **Start Screensaver**: Immediately launches the system screensaver (`Somnambulator`).
- **Auto-Appearing `[ 💾 USB ]` Top-Bar Pill (`MainActivity`, `activity_main.xml`):**
  - Automatically appears on the top-right Menu Bar when a removable USB flash drive or external hard drive is mounted (`StorageManager` + `ACTION_MEDIA_MOUNTED` / `UNMOUNTED` / `EJECT` receiver) and hides when unplugged.
  - Clicking `[ 💾 USB ]` opens a 2-row frosted glass menu:
    1. **Open Video Player (Nova)**: Launches `Nova Video Player` (`org.courville.nova`) or fallback media/file apps (`VLC`, `Kodi`, `TotalCommander`).
    2. **Storage & Safely Eject**: Opens `ACTION_INTERNAL_STORAGE_SETTINGS` to inspect capacity or safely unmount the drive.
- **On-TV Remote Key Mapper & Hidden Apps Manager (`LauncherSettingsActivity`):**
  - **Hidden Apps**: Lists all hidden apps and allows 1-click unhiding directly on TV.
  - **Interactive Remote Button Mapper**: Captures any non-protected remote button press and binds it to `wallpaper_cycle`, `wallpaper_picker`, `quick_settings`, `recent_apps`, `inputs`, or any installed TV app. Navigation, Volume, and Power keys are strictly guarded (`Config.isProtectedRemoteKey`).
  - **Follow Theme Reset**: Clears all on-TV Dock, Hidden Apps, and Remote Key overrides in one click.

#### Changed
- **Refined Dock Vertical Position & Grid Spacing (`DockDecoration`, `MainActivity`):**
  - Lowered the Dock glass platter closer to the bottom screen edge (`20dp` / `40px` margin instead of `45dp`), maximizing visible 4K wallpaper artwork.
  - Reduced the top offset of the first app row below the Dock from `64dp` (`128px`) to `14dp` (`28px`), eliminating the excessive empty gap when scrolling down while keeping the row 100% off-screen when resting on the Dock.
- **Always-Visible App Labels Below the Dock (`AppAdapter`, `item_app.xml`):**
  - Dock icons remain clean without bottom text labels (using the large Top Shelf `heroName` title above the Dock).
  - App grid rows below the Dock (`position >= COLUMNS`) now always display their app name below the card (`78%` white alpha when unfocused, `100%` white alpha with smooth `8dp` downward translation when focused so the `1.2333x` scaled card never overlaps its label).

---

## [v1.1.0] — 2026-11-08
### 👨‍👩‍👧 Monorepo Ecosystem, FamilyTime Parental Control & Custom Brand Art

#### Added
- **Multi-Project Gradle Monorepo:**
  - Consolidated `apps/launcher` (`com.appletv.launcher`), `apps/parental-control` (`vn.familytv.parentalcontrol`), and shared IPC library `core/contract` (`com.appletv.contract`) into a single repository.
  - Added `./tools/deploy.sh <launcher|parental-control|all> [TV_IP]` and `./tools/deploy_apk.sh` with automatic state and APK backup.
- **Dynamic Time Pill in Kid Mode (`MainActivity`, `ModeManager`, `StatusProvider`):**
  - Top-bar Mode button displays live remaining screen time (`[ 🧒 12m ]`, `[ 🧒 45s ]`) queried via `StatusProviderContract` and updated via `ACTION_MODE_CHANGED` broadcasts.
  - Traffic-light color states: White (`> 5m`), Warning Gold (`2m – 5m`), Alert Red (`≤ 2m`).
  - Automatic lock back to `Kid Mode` when the TV screen turns off (`ACTION_SCREEN_OFF`).
- **FamilyTime Parental Control (`apps/parental-control`):**
  - Tracks active usage across target entertainment apps (YouTube/TizenTube, MyTV, Nova Video Player, Spotify) and pauses automatically during screensaver.
  - Triggers interactive math/logic **Quiz Break** overlay after each 15-minute session in Kid Mode.
  - Offline **Google Authenticator (TOTP)** + recovery code authentication to switch to `Parent Mode`.
- **Custom High-Resolution Brand Banners (`AppAdapter`):**
  - Added bespoke tvOS banners for YouTube (`TizenTube Cobalt`), `MyTV`, `Nova Video Player`, and `Spotify`.

---

## [v1.0.0] — 2026-11-02
### 🍎 Initial Release: Apple TV tvOS 18 Launcher & Universal Debloat Suite

#### Added
- **tvOS 18 Leanback Home Screen (`MainActivity`, `AppAdapter`, `DockDecoration`):**
  - 5-column large icon layout (`300x180px`, `1.2333x` symmetrical center-zoom focus) with frosted glass Dock platter and Top Shelf hero title.
- **4K UHD Wallpaper Engine & Sync Pipeline (`WallpaperSize`, `WallpaperPickerAdapter`, `tools/sync_wallpaper.sh`):**
  - Visual horizontal 4K wallpaper picker with `RGB_565` `LruCache` memory protection, 1-button remote wallpaper cycling with 400ms crossfade, top-left frosted glass indicator banner, and FUSE-safe 4K MozJPEG sync script.
- **Native Apple TV Recent Apps Switcher (`RecentAppsActivity`):**
  - Double-press Home gesture opening a horizontal card carousel of recent apps with live usage stats (`PACKAGE_USAGE_STATS`), background process kill, and authentic empty state.
- **Zero-Lag Hardware Key Handling (`HomeKeyService`, `InputKeyService`):**
  - Preserves 100% hardware-speed volume hold acceleration by passing navigation and volume keys directly to Android's Linux `InputDispatcher`.
- **Audit-First Multi-Brand Debloat & Full-State Restore Suite (`tools/audit.sh`, `tools/apply.sh`, `tools/restore.sh`, `profiles/`):**
  - Read-only TV inspection, Day-1 baseline snapshots, safe `pm disable-user` debloating across 9 brand profiles (TCL, Sony, Xiaomi, OnePlus, Hisense, Shield, Chromecast, Fire TV, Universal), official ad-free SmartTube installation, and 1-click factory state rollback.
