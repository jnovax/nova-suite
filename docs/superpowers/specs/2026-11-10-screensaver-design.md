# 📺 Architectural Design Specification: Android TV ScreenSaver System for Nova TV Launcher

- **Date:** 2026-11-10
- **Target Version:** `v1.3.0`
- **Component:** `apps/launcher` (`com.appletv.launcher`)
- **Git Branch:** `feature/screensaver`
- **Status:** Approved (Ready for Implementation Planning)

---

## 1. Executive Summary & Goals

### 1.1 Problem Statement
The current launcher has a minimal screensaver implementation ([ClockDream.java](file:///home/lamnh/projects/nova-tv-launcher/apps/launcher/src/main/java/com/appletv/launcher/ClockDream.java)) that drifts a text clock over an ambient gradient. While lightweight, it lacks:
1. Integration into **Launcher Settings** ([LauncherSettingsActivity.java](file:///home/lamnh/projects/nova-tv-launcher/apps/launcher/src/main/java/com/appletv/launcher/LauncherSettingsActivity.java)) for user customization.
2. Multiple visual experiences matching the Apple TV ecosystem (4K Wallpaper slideshow with Ken Burns pan/zoom, and tvOS Aerial flyover videos).
3. Reliable **In-Launcher Idle Watchdog** activation (triggering screensaver after $N$ minutes of user inactivity without relying on vendor-restricted system DreamService policies).
4. Direct in-place preview and streamlined Android TV System Dream integration.

### 1.2 Target Experience
Transform Android TV into an authentic Apple TV ambient experience when idle:
- **3 Visual Modes:**
  1. `Ambient Clock`: Deep black OLED burn-in safe clock with shifting radial glow.
  2. `4K Wallpaper Slideshow`: Cycles through local 4K wallpapers (`/sdcard/Android/data/com.appletv.launcher/files/wallpapers/`) with smooth 20s Ken Burns pan & zoom and 1.5s crossfade.
  3. `tvOS Aerial Video`: Official Apple TV aerial flyover videos (Dubai, Hawaii, Greenland, London, Tokyo) streamed from Apple CDN with smart 2-video local LRU caching and offline fallback.
- **Hybrid Activation (Launcher Idle + System Dream):**
  - Configurable inactivity timeout in launcher ($2, 5, 10, 15$ mins, or Off).
  - Standard Android `DreamService` for system-wide idle outside the launcher.
  - 1-Click trigger from Quick Settings Control Center and Remote Key Mapper.
  - Instant wakeup (< 16ms) upon any remote key press.
- **Liquid Glass Settings:**
  - Expanded **Display & ScreenSaver** tab in `LauncherSettingsActivity` with interactive preview and direct shortcut to system screensaver settings.

---

## 2. System Architecture

```
                               ┌────────────────────────────────────────────────┐
                               │           TRIGGER SOURCES                      │
                               └───────────────────────┬────────────────────────┘
                                                       │
               ┌───────────────────────────────────────┼────────────────────────────────────────┐
               │                                       │                                        │
┌──────────────┴──────────────┐         ┌──────────────┴──────────────┐          ┌──────────────┴──────────────┐
│  In-Launcher Idle Watchdog  │         │   Quick Settings 1-Click    │          │    Android DreamService     │
│    (MainActivity.java)      │         │   & Settings Live Preview   │          │     (AppleTvDream.java)     │
└──────────────┬──────────────┘         └──────────────┬──────────────┘          └──────────────┬──────────────┘
               │                                       │                                        │
               └───────────────────────► ┌─────────────┴───────────────┐ ◄──────────────────────┘
                                         │    ScreenSaverActivity      │
                                         │     (Unified Engine)        │
                                         └─────────────┬───────────────┘
                                                       │
               ┌───────────────────────────────────────┼────────────────────────────────────────┐
               │                                       │                                        │
┌──────────────┴──────────────┐         ┌──────────────┴──────────────┐          ┌──────────────┴──────────────┐
│      AmbientClockView       │         │    WallpaperSlideshowView   │          │       AerialVideoView       │
│    (OLED Burn-in Safe)      │         │   (Ken Burns 4K Slideshow)  │          │   (Apple CDN + Smart Cache) │
└─────────────────────────────┘         └─────────────────────────────┘          └─────────────────────────────┘
                                                       │
                                        ┌──────────────┴──────────────┐
                                        │      tvOS Glass Overlay     │
                                        │    (Clock + Location Tag)   │
                                        └─────────────────────────────┘
```

### 2.1 Unified Engine Principle
Rather than maintaining separate rendering code in `DreamService` and an in-app screensaver Activity, all visual rendering and playback is encapsulated within **`ScreenSaverActivity`** (or reusable engine views).
- `ScreenSaverActivity` is used for In-Launcher idle triggers, Quick Settings 1-Click, and Live Preview.
- `AppleTvDream` (extending `DreamService`) acts as the Android OS entry point, delegating to the same engine views when the TV OS invokes a Dream.
- Result: **Zero code duplication**, consistent visual behavior across all TV brands (TCL, Sony, Xiaomi, Hisense, Chromecast).

---

## 3. Detailed Component Specifications

### 3.1 `ScreenSaverActivity` (Display Engine & Controller)
- **Manifest Declaration:**
  ```xml
  <activity
      android:name=".ScreenSaverActivity"
      android:exported="false"
      android:theme="@android:style/Theme.NoTitleBar.Fullscreen"
      android:launchMode="singleTask"
      android:screenOrientation="landscape"
      android:configChanges="orientation|keyboardHidden|screenSize" />
  ```
- **Window Flags:**
  - `FLAG_FULLSCREEN`, `FLAG_KEEP_SCREEN_ON`.
  - Disable window enter/exit animations via `overridePendingTransition(0, 0)` for instantaneous appearance and dismissal.
- **Remote Key Handling & Instant Wakeup:**
  - Overrides `dispatchKeyEvent(KeyEvent event)`:
    - Any `ACTION_DOWN` on any key (D-Pad, Back, Enter, Home, Play, Pause...) immediately calls `finish()` and `overridePendingTransition(0, 0)`.
  - Overrides `onUserInteraction()`: calls `finish()`.
  - Wakeup latency is strictly within 1 frame (< 16ms).

### 3.2 Visual Mode Engines

#### Mode 1: `AmbientClockView` (Minimal Clock & Ambient Glow)
- Replaces/modernizes `ClockDream`:
  - `TextClock` with tvOS typography (thin numerals, custom letter spacing).
  - Every 60s, smoothly fades out (900ms), moves to a new pseudo-random location within safe margins (10% to 90% screen bounds), and fades back in (1200ms) to prevent OLED burn-in.
  - Background radial gradient glow slowly transitions color across a curated deep palette over 8 seconds using `ArgbEvaluator`.
  - Resource usage: ~0% CPU, 0 network, < 2 MB RAM.

#### Mode 2: `WallpaperSlideshowView` (4K Ken Burns Slideshow)
- **Image Source:**
  - Scans `/sdcard/Android/data/com.appletv.launcher/files/wallpapers/` for `*.jpg` files.
  - Fallback sequence if empty:
    1. Active launcher wallpaper `/sdcard/Android/data/com.appletv.launcher/files/wallpaper.jpg`.
    2. Built-in drawable `home_backdrop.xml`.
- **Double-Buffering & Crossfade:**
  - 2 layered `ImageView` elements (`currentImageView` and `nextImageView`).
  - Slide duration: **20 seconds per image**.
  - **Ken Burns Animation:** Smooth scale transform ($1.0 \to 1.05$ or $1.05 \to 1.0$) combined with slight translation ($±20\text{dp}$) over 20,000ms using `LinearInterpolator`.
  - **Crossfade Transition:** 1,500ms alpha crossfade when swapping to the next image.
- **Memory Safety:**
  - Reuses [WallpaperSize.java](file:///home/lamnh/projects/nova-tv-launcher/apps/launcher/src/main/java/com/appletv/launcher/WallpaperSize.java) downsampling logic to decode at 1080p target resolution (`RGB_565`).
  - Bitmaps are explicitly recycled on image transition and Activity destruction to guarantee no OOM.

#### Mode 3: `AerialVideoView` (tvOS Aerial flyover)
- **Manifest Asset (`res/raw/aerial_manifest.json`):**
  - Bundled JSON containing official Apple TV 1080p H.264 video streams:
    ```json
    [
      {
        "id": "b1-1",
        "name": "Dubai Marina at Night",
        "url": "http://flat128.org/aerial/b1-1.mov"
      },
      {
        "id": "b2-1",
        "name": "Greenland Glaciers",
        "url": "http://flat128.org/aerial/b2-1.mov"
      },
      {
        "id": "b3-1",
        "name": "Hawaii Coastline",
        "url": "http://flat128.org/aerial/b3-1.mov"
      }
    ]
    ```
- **Playback Architecture:**
  - Native Android `MediaPlayer` with `TextureView` (hardware accelerated, zero third-party dependencies).
  - Video loops continuously; next video starts after completion.
- **Smart Cache:**
  - Cache directory: `/sdcard/Android/data/com.appletv.launcher/cache/screensaver/`.
  - Max cache size: 2 videos (~200MB - 400MB total) managed via LRU.
  - Streams video over HTTP/HTTPS when online, caching chunks in the background. If already cached, plays directly from disk.
- **Offline & Error Resilience:**
  - If network is unavailable and no cached video exists, gracefully falls back to `WallpaperSlideshowView`.
  - On any `MediaPlayer.OnErrorListener`, automatically falls back to wallpaper slideshow without showing errors or crashing.

### 3.3 `OverlayInfoView` (Floating tvOS Glass Overlay)
- Positioned at the bottom-left or top-right:
  - Thin tvOS clock + date.
  - Location badge (e.g., *"Dubai Marina at Night"*) displayed only during Aerial Video mode.
  - Position shifts slightly every 60s along with the clock to prevent burn-in.
  - Can be toggled on/off in Launcher Settings.

---

## 4. Settings & Configuration Architecture

### 4.1 Configuration Keys ([Config.java](file:///home/lamnh/projects/nova-tv-launcher/apps/launcher/src/main/java/com/appletv/launcher/Config.java))

| Preference Key | Type | Default Value | Description |
|---|---|---|---|
| `screensaver_mode` | `int` | `2` (`MODE_WALLPAPERS`) | `0 = Off`, `1 = Clock`, `2 = Wallpapers`, `3 = Aerial` |
| `screensaver_timeout_mins` | `int` | `5` | `0 = Off`, `2`, `5`, `10`, `15` minutes |
| `screensaver_show_overlay` | `boolean` | `true` | Show floating clock and location tag |

Helper methods:
- `Config.getScreensaverMode(Context)` / `Config.setScreensaverMode(Context, int)`
- `Config.getScreensaverTimeoutMinutes(Context)` / `Config.setScreensaverTimeoutMinutes(Context, int)`
- `Config.isScreensaverOverlayEnabled(Context)` / `Config.setScreensaverOverlayEnabled(Context, boolean)`

### 4.2 UI Integration in `LauncherSettingsActivity`
- **Tab Update:** Rename Tab 1 from `tabDisplayNight` to **Display & ScreenSaver** (`tab_display_night` string -> *"Display & ScreenSaver"* / *"Màn hình & ScreenSaver"*).
- **Pane Layout:**
  - Section 1: **ScreenSaver Mode** (Radio Pills: Off / Clock / 4K Wallpapers / Aerial Video).
  - Section 2: **Launcher Inactivity Timeout** (Radio Pills: 2m / 5m / 10m / 15m / Off).
  - Section 3: **Info Overlay** (Toggle Pill: On / Off).
  - Section 4: **Quick Actions**:
    - `[ ▶ Preview ScreenSaver ]`: Launches `ScreenSaverActivity` immediately.
    - `[ ⚙ System Dream Settings ]`: Opens `new Intent(Settings.ACTION_DREAM_SETTINGS)` with fallback to `Settings.ACTION_DISPLAY_SETTINGS`.
  - Section 5: **Night Dimmer** (Preserves existing dimmer mode and intensity controls).
- **Kid Mode Guardrail:**
  - In accordance with Guardrail 3 (`AGENTS.md`), `LauncherSettingsActivity` is strictly inaccessible in Kid Mode (`finish()` on `onCreate`). ScreenSaver will still trigger automatically in Kid Mode according to saved preferences.

### 4.3 Control Center Sync ([QuickSettingsActivity.java](file:///home/lamnh/projects/nova-tv-launcher/apps/launcher/src/main/java/com/appletv/launcher/QuickSettingsActivity.java))
- The "Start Screensaver" row is updated to directly launch `ScreenSaverActivity` with `FLAG_ACTIVITY_NEW_TASK`, ensuring instant activation without vendor system restrictions.

---

## 5. In-Launcher Idle Watchdog Architecture

### 5.1 Inactivity Detection ([MainActivity.java](file:///home/lamnh/projects/nova-tv-launcher/apps/launcher/src/main/java/com/appletv/launcher/MainActivity.java))
- Main loop handler:
  ```java
  private final Handler idleHandler = new Handler(Looper.getMainLooper());
  private final Runnable idleScreensaverRunnable = this::launchScreensaver;
  ```
- Reset trigger:
  - Invoked inside `onUserInteraction()` and `dispatchKeyEvent()`:
    ```java
    idleHandler.removeCallbacks(idleScreensaverRunnable);
    int timeoutMins = Config.getScreensaverTimeoutMinutes(this);
    int mode = Config.getScreensaverMode(this);
    if (timeoutMins > 0 && mode > 0) {
        idleHandler.postDelayed(idleScreensaverRunnable, timeoutMins * 60_000L);
    }
    ```
- Foreground lifecycle awareness:
  - `onPause()` / `onStop()`: `idleHandler.removeCallbacks(idleScreensaverRunnable)` (prevents firing when watching media or using another app).
  - `onResume()`: Re-reads timeout config and restarts the watchdog timer.

---

## 6. Testing & Quality Assurance Plan

### 6.1 Automated Unit Tests
- **`ConfigScreensaverTest.java`**:
  - Verifies default values (`mode = 2`, `timeout = 5`, `overlay = true`).
  - Verifies boundary conditions (handling negative values or out-of-range mode IDs).
- **`AerialManifestTest.java`**:
  - Validates `res/raw/aerial_manifest.json` parse integrity.
  - Confirms all URLs are well-formed and names are non-empty.
- **`IdleWatchdogTest.java`**:
  - Tests timeout calculations and callback scheduling logic.

### 6.2 Manual Verification on Connected Android TV (TCL G08)
1. **Idle Trigger Test:** Set timeout to 2 minutes on Home screen; leave remote untouched; verify screensaver launches at 2:00.
2. **Instant Wakeup Test:** Press D-Pad Center; verify return to launcher within < 16ms with zero animation stutter.
3. **Mode Switching Test:** Cycle through Clock, Wallpapers, and Aerial modes in Settings; verify each mode renders correctly.
4. **Offline Resilience Test:** Disconnect TV Wi-Fi while in Aerial mode; verify graceful fallback to 4K Wallpaper slideshow without crash.
5. **Memory Profile Test:** Execute `adb shell dumpsys meminfo com.appletv.launcher`; confirm RAM stays under 35 MB extra during 4K slideshow and video playback.
6. **Kid Mode Test:** Switch to Kid Mode; verify ScreenSaver continues to activate on idle, but Settings remain locked.

---

## 7. Migration & Rollback Strategy
- Non-breaking changes: `ClockDream` service name and manifest intent filter are preserved so existing system settings bindings continue to function.
- All new preferences default to safe values that require zero user setup.
- If rollback is needed: switch git branch back to `main` and deploy.
