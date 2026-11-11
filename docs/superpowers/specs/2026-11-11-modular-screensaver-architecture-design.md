# 📺 Architectural Design Specification: Modular Android TV ScreenSaver System

- **Date:** 2026-11-11
- **Target Version:** `v1.3.0`
- **Components:**
  - `apps/launcher` (`com.appletv.launcher`, ~298 KB target maintained)
  - `apps/screensaver-flipclock` (`com.appletv.screensaver.flipclock`, ~150 KB standalone)
  - `apps/screensaver-artmuseum` (`com.appletv.screensaver.artmuseum`, ~2.5 MB standalone offline)
- **Git Branch:** `feature/screensaver`
- **Status:** Approved (Ready for Implementation Planning)

---

## 1. Executive Summary & Goals

### 1.1 Problem Statement
In previous iterations, embedding visual screensavers (like Fliqlo mechanical clocks, art galleries, and video aerial flyovers) directly into the main launcher threatened the core design tenet of Nova TV Launcher: **an ultra-lightweight (~298 KB), bloat-free tvOS 18 Home experience**. Including media assets and dedicated rendering loops directly inside `apps/launcher` causes unnecessary APK bloat, complicates maintenance, and forces every user to keep visual themes they may not want.

### 1.2 Target Solution & Architecture
Adopt a **Modular Android DreamService + Open Intent Screensaver Architecture**:
1. **Preserve Ultra-Lightweight Launcher (`apps/launcher`)**:
   - The launcher APK remains ultra-compact (~298 KB).
   - Retains only the native **4K Wallpaper Slideshow** (reusing existing wallpapers in `/sdcard/Android/data/com.appletv.launcher/files/wallpapers/`) and the **In-Launcher Idle Watchdog**.
2. **Dynamic Screensaver Discovery**:
   - The launcher automatically detects all installed screensavers via `PackageManager.queryIntentServices(new Intent("android.service.dreams.DreamService"), PackageManager.GET_META_DATA)`.
   - Any screensaver installed on the TV (whether from our monorepo or third-party apps like Aerial Dream or Fliqlo TV) dynamically appears in `LauncherSettingsActivity` with its native icon and app title.
3. **Dual Activation Mechanism**:
   - **In-Launcher Zero-Latency Launch (<16ms)**: Direct intent launch to the screensaver's exported activity for in-launcher idle watchdog and Quick Settings 1-Click.
   - **System Dream Synchronization**: Synchronizes user selection to Android TV OS secure settings (`screensaver_components`) via `WRITE_SECURE_SETTINGS` so the screensaver also triggers during global system sleep.
4. **Independent Standalone Screensaver Modules in Monorepo**:
   - `apps/screensaver-flipclock`: Pure Java 17 retro mechanical split-flap flip clock (Fliqlo style), OLED burn-in safe, ~150 KB APK.
   - `apps/screensaver-artmuseum`: Pure Java 17 curated art gallery with 15-20 classical masterpieces (Van Gogh, Monet, Da Vinci, Vermeer...), tvOS glass museum card overlay, Ken Burns slow pan/zoom, ~2.5 MB APK.

---

## 2. Monorepo Architecture & Directory Structure

```
nova-tv-launcher/
├── apps/
│   ├── launcher/                                # ID: com.appletv.launcher (~298 KB)
│   │   ├── src/main/java/com/appletv/launcher/
│   │   │   ├── ScreensaverDiscovery.java        # Scans system DreamServices via PackageManager
│   │   │   ├── ScreensaverPreferences.java      # Manages selection (OFF, WALLPAPERS, EXTERNAL)
│   │   │   ├── ScreenSaverActivity.java         # Built-in 4K Wallpaper slideshow engine
│   │   │   ├── WallpaperSlideshowView.java      # Ken Burns & crossfade for local wallpapers
│   │   │   ├── LauncherSettingsActivity.java    # Display & ScreenSaver configuration tab
│   │   │   └── MainActivity.java                # Idle Watchdog timer & remote input tracking
│   │
│   ├── parental-control/                        # ID: vn.familytv.parentalcontrol
│   │
│   ├── screensaver-flipclock/                   # ID: com.appletv.screensaver.flipclock (~150 KB)
│   │   ├── build.gradle                         # Standalone Android application module
│   │   └── src/main/
│   │       ├── AndroidManifest.xml              # Declares DreamService & exported Activity
│   │       ├── res/xml/dream_info.xml           # Dream metadata
│   │       └── java/com/appletv/screensaver/flipclock/
│   │           ├── FliqloClockView.java         # Pure Canvas 3D split-flap flip clock
│   │           ├── FlipClockDreamService.java   # Android DreamService entry point
│   │           └── FlipClockActivity.java       # Instant launch full-screen Activity
│   │
│   └── screensaver-artmuseum/                   # ID: com.appletv.screensaver.artmuseum (~2.5 MB)
│       ├── build.gradle                         # Standalone Android application module
│       └── src/main/
│           ├── AndroidManifest.xml              # Declares DreamService & exported Activity
│           ├── res/xml/dream_info.xml           # Dream metadata
│           ├── assets/paintings/                # 15-20 curated classical artworks (WebP/JPEG)
│           └── java/com/appletv/screensaver/artmuseum/
│               ├── ArtCatalog.java              # Painting metadata (Title, Artist, Year, Museum)
│               ├── ArtMuseumView.java           # Ken Burns renderer with dual-image crossfade
│               ├── MuseumCardView.java          # Frosted glass pill overlay with artwork info
│               ├── ArtMuseumDreamService.java   # Android DreamService entry point
│               └── ArtMuseumActivity.java       # Instant launch full-screen Activity
│
├── core/contract/                               # Shared contracts (if needed)
├── tools/
│   ├── deploy.sh                                # Deploy: launcher | flipclock | artmuseum | all
│   └── apply.sh                                 # Grants WRITE_SECURE_SETTINGS during setup
└── settings.gradle                              # Includes launcher, parental-control, screensaver-*
```

---

## 3. Detailed Component Specifications

### 3.1 `apps/launcher` (Orchestrator & Built-in Wallpaper Slideshow)

#### A. `ScreensaverDiscovery.java`
- Discovers installed screensavers dynamically:
  ```java
  public static List<ScreensaverOption> getAvailableScreensavers(Context context) {
      PackageManager pm = context.getPackageManager();
      Intent dreamIntent = new Intent("android.service.dreams.DreamService");
      List<ResolveInfo> services = pm.queryIntentServices(dreamIntent, PackageManager.GET_META_DATA);
      // Maps ResolveInfo into ScreensaverOption (packageName, serviceName, activityName, label, icon)
  }
  ```
- Graceful Fallback:
  - If a previously selected external screensaver is uninstalled, `getActiveScreensaver()` detects the missing package and safely falls back to `ScreensaverPreferences.MODE_WALLPAPERS`.

#### B. `ScreensaverPreferences.java`
- Manages preferences in SharedPreferences (`screensaver_prefs`):
  - `mode`: `MODE_OFF (0)`, `MODE_WALLPAPERS (1)`, `MODE_EXTERNAL (2)`.
  - `selected_component`: Package and Service component string (e.g. `com.appletv.screensaver.flipclock/.FlipClockDreamService`).
  - `selected_activity`: Activity component string (e.g. `com.appletv.screensaver.flipclock/.FlipClockActivity`).
  - `timeout_minutes`: `0` (Off), `2`, `5`, `10`, `15` minutes.
- Dual Synchronization:
  - When setting an external screensaver, if `WRITE_SECURE_SETTINGS` is held:
    ```java
    Settings.Secure.putString(cr, "screensaver_components", componentName);
    Settings.Secure.putInt(cr, "screensaver_enabled", 1);
    ```

#### C. `MainActivity.java` (In-Launcher Idle Watchdog)
- Listens for remote interactions (`dispatchKeyEvent`, `onUserInteraction`).
- Resets inactivity timer on any user key press.
- When inactivity timer expires:
  - If mode == `MODE_WALLPAPERS`: Starts internal `ScreenSaverActivity`.
  - If mode == `MODE_EXTERNAL`: Launches the external screensaver's exported activity with `Intent.FLAG_ACTIVITY_NEW_TASK` and `overridePendingTransition(0, 0)`.

#### D. Built-in `ScreenSaverActivity.java` (4K Wallpaper Slideshow)
- Renders local 4K wallpapers from `/sdcard/Android/data/com.appletv.launcher/files/wallpapers/`.
- Ken Burns pan & zoom with 1.5s crossfade between wallpapers every 20 seconds.
- Instant dismissal (<16ms) upon remote key press.

---

### 3.2 `apps/screensaver-flipclock` (Fliqlo Mechanical Split-Flap Clock)

#### A. Architecture & Visual Engine
- **Target Size**: ~150 KB APK.
- **Pure Java Canvas Drawing (`FliqloClockView.java`)**:
  - Implements split-flap digit card animation using `android.graphics.Camera` / `Matrix` 3D projection.
  - Draws upper static half, lower static half, and the flipping flap with dynamic shadow gradient.
  - Automatically adapts to system 12h/24h format (`DateFormat.is24HourFormat(context)`).
  - Displays subtle `AM` / `PM` text when 12h format is active.
- **OLED Burn-in Protection**:
  - 100% pure black OLED background (`#000000`).
  - Subtle micro-drift: Center coordinates shift by $\pm 15\text{px}$ every 5 minutes to prevent OLED phosphor retention.
- **Entry Points**:
  - `FlipClockDreamService`: Declared with `BIND_DREAM_SERVICE` and `@xml/dream_info`.
  - `FlipClockActivity`: Exported activity, full-screen, keeps screen on, dismisses immediately on any key press.

---

### 3.3 `apps/screensaver-artmuseum` (Classical Masterpieces Gallery)

#### A. Architecture & Visual Engine
- **Target Size**: ~2.5 MB APK.
- **Curated Offline Art Gallery (`assets/paintings/` & `ArtCatalog.java`)**:
  - Contains 15-20 high-quality classical masterpieces, optimized in WebP:
    - Vincent van Gogh — *The Starry Night* (1889, MoMA)
    - Claude Monet — *Water Lilies* (1916, Musée de l'Orangerie)
    - Leonardo da Vinci — *Mona Lisa* (1503, Musée du Louvre)
    - Johannes Vermeer — *Girl with a Pearl Earring* (1665, Mauritshuis)
    - Katsushika Hokusai — *The Great Wave off Kanagawa* (1831, Tokyo National Museum)
    - Gustav Klimt — *The Kiss* (1908, Belvedere)
    - Rembrandt van Rijn — *The Night Watch* (1642, Rijksmuseum)
    - Georges Seurat — *A Sunday on La Grande Jatte* (1884, Art Institute of Chicago)
    - Caspar David Friedrich — *Wanderer above the Sea of Fog* (1818, Hamburger Kunsthalle)
    - Edgar Degas — *The Dance Class* (1874, Musée d'Orsay)
- **Cinematic Presentation (`ArtMuseumView.java`)**:
  - Subtle Ken Burns pan and scale (1.0x to 1.08x over 25 seconds).
  - 1.5s seamless crossfade transition between artworks.
- **tvOS Glass Museum Card Overlay (`MuseumCardView.java`)**:
  - Frosted capsule in bottom-left corner displaying Artwork Title, Artist, Year, and Museum.
  - Fades in smoothly on slide change, lingers for 6 seconds, then gently fades out to leave an unobstructed view of the art.
- **Entry Points**:
  - `ArtMuseumDreamService` & `ArtMuseumActivity`.

---

## 4. UI Design: Launcher Settings Integration

In `LauncherSettingsActivity.java` (Tab 1: **Display & ScreenSaver**):

```
┌────────────────────────────────────────────────────────────────────────┐
│ MÀN HÌNH BẢO VỆ (SCREENSAVER)                                         │
│ Chọn kiểu màn hình chờ khi TV không sử dụng                           │
├────────────────────────────────────────────────────────────────────────┤
│  ( ) Tắt (Off)                                                         │
│  (•) 4K Wallpapers (Mặc định trong Launcher)                           │
│  ( ) 🕒 FlipClock (Đồng hồ lật cổ điển Fliqlo)                         │
│  ( ) 🎨 Art Museum (Triển lãm kiệt tác hội họa kinh điển)              │
│  ( ) [Tên các Screensaver khác cài trên hệ thống...]                   │
├────────────────────────────────────────────────────────────────────────┤
│ THỜI GIAN CHỜ (TIMEOUT)                                               │
│  [ Tắt ]   [ 2 phút ]   [• 5 phút ]   [ 10 phút ]   [ 15 phút ]        │
├────────────────────────────────────────────────────────────────────────┤
│  [ ▶ Xem thử / Preview ]      [ ⚙ Cài đặt Screensaver hệ thống ]      │
└────────────────────────────────────────────────────────────────────────┘
```

- In Kid Mode (`ModeManager.isKidMode(ctx)`), `LauncherSettingsActivity` remains strictly inaccessible (finishes on launch as per Guardrail 3 in `AGENTS.md`). The configured screensaver continues to run automatically when the TV is idle in Kid Mode.

---

## 5. Deployment & Gradle Build System

### 5.1 Root `settings.gradle`
```groovy
include ':apps:launcher'
include ':apps:parental-control'
include ':apps:screensaver-flipclock'
include ':apps:screensaver-artmuseum'
include ':core:contract'
```

### 5.2 `./tools/deploy.sh` Expansion
The deployment tool is updated to allow selective or unified builds:
- `./tools/deploy.sh launcher [TV_IP]`: Builds and installs only the launcher.
- `./tools/deploy.sh flipclock [TV_IP]`: Builds and installs only FlipClock.
- `./tools/deploy.sh artmuseum [TV_IP]`: Builds and installs only Art Museum.
- `./tools/deploy.sh all [TV_IP]`: Builds and installs all applications.

---

## 6. Resilience, Error Handling & Safety Guardrails

1. **Uninstalled App Resilience**:
   - If an external screensaver is uninstalled by the user, the launcher watchdog detects this via `PackageManager` resolution check and automatically falls back to `MODE_WALLPAPERS`.
2. **Crash & Security Exception Prevention**:
   - `startActivity` calls to external screensavers are wrapped in `try-catch (ActivityNotFoundException | SecurityException e)` blocks with diagnostic logging.
3. **Hardware-Speed Remote Keys Protected**:
   - Conforms strictly to Guardrail 2 (`AGENTS.md`): Screensavers do not intercept volume, power, or protected system keys.
   - Any remote button press dismisses the screensaver immediately (< 16ms).
4. **Zero Background Resource Leakage**:
   - Screensaver activities cleanly destroy their rendering views, bitmaps, and handlers on `onDestroy()`, leaving zero memory footprint when inactive.

---

## 7. Verification & Automated Testing Plan

### 7.1 Automated Unit Tests
- **`ScreensaverDiscoveryTest.java`** (in `apps/launcher`):
  - Validates discovery of mock DreamServices.
  - Tests fallback to `MODE_WALLPAPERS` when component is missing.
- **`ScreensaverPreferencesTest.java`** (in `apps/launcher`):
  - Tests persistence and retrieval of `MODE_OFF`, `MODE_WALLPAPERS`, and `MODE_EXTERNAL`.
  - Verifies correct timeout mapping.
- **`FliqloTimeTest.java`** (in `apps/screensaver-flipclock`):
  - Verifies 12h/24h conversion and digit formatting.
  - Verifies AM/PM calculation.
- **`ArtCatalogTest.java`** (in `apps/screensaver-artmuseum`):
  - Validates that all catalog entries correspond to valid asset files.
  - Validates playlist cycling without index out-of-bounds.

### 7.2 Manual & Physical Device Verification
- **APK Size Verification**:
  - `apps/launcher`: Must remain ~298 KB (no size inflation).
  - `apps/screensaver-flipclock`: ~150 KB.
  - `apps/screensaver-artmuseum`: ~2.5 MB.
- **TV Execution Test (TCL Android 14 & Emulator)**:
  - 1-Click Screensaver from Quick Settings launches chosen screensaver instantly.
  - Inactivity watchdog triggers after timeout.
  - Remote key press instantly returns to launcher home without flicker.
  - System sleep outside launcher invokes selected DreamService.
