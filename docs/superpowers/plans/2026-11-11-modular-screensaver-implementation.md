# Modular Android TV ScreenSaver Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the screensaver system into an extensible, modular architecture where `apps/launcher` stays ultra-lightweight (~298 KB) with only native 4K Wallpapers and dynamic discovery, while FlipClock and ArtMuseum run as separate standalone APKs.

**Architecture:** Launcher uses `PackageManager` to dynamically discover any installed Android `DreamService` (including our standalone screensaver APKs and 3rd-party screensavers), displaying them in `LauncherSettingsActivity`. Dual activation triggers the selected screensaver's exported activity instantly (<16ms) on idle/1-click while syncing with Android TV system settings (`screensaver_components`).

**Tech Stack:** Pure Java 17, Android SDK 34 (minSdk 28), Android DreamService framework, Android Canvas 3D Matrix/Camera animation, Gradle multi-module build.

**Spec:** [docs/superpowers/specs/2026-11-11-modular-screensaver-architecture-design.md](file:///home/lamnh/projects/nova-tv-launcher/docs/superpowers/specs/2026-11-11-modular-screensaver-architecture-design.md)

## Global Constraints
- Target launcher APK size: strictly preserved at ~298 KB (no heavy graphics or asset bloat in `apps/launcher`).
- Standalone screensaver APKs: `apps/screensaver-flipclock` target ~150 KB, `apps/screensaver-artmuseum` target ~2.5 MB.
- Pure Java 17 across all modules, zero unnecessary 3rd-party dependencies.
- Hardware-speed remote keys protected: Screensavers must never consume volume, power, or protected navigation keys, and must wake up instantly (<16ms) on any remote press.
- Guardrail 3: In Kid Mode, `LauncherSettingsActivity` remains strictly locked.

---

### Task 1: Monorepo Multi-Module Configuration

**Files:**
- Modify: `settings.gradle`
- Create: `apps/screensaver-flipclock/build.gradle`
- Create: `apps/screensaver-artmuseum/build.gradle`

**Interfaces:**
- Produces: Gradle sub-projects `:apps:screensaver-flipclock` and `:apps:screensaver-artmuseum`.

- [ ] **Step 1: Update settings.gradle with new modules**
Add `:apps:screensaver-flipclock` and `:apps:screensaver-artmuseum` to `settings.gradle`.

- [ ] **Step 2: Create apps/screensaver-flipclock/build.gradle**
Configure Android application with `applicationId "com.appletv.screensaver.flipclock"`, Java 17 compatibility, unified signing, and minSdk 28.

- [ ] **Step 3: Create apps/screensaver-artmuseum/build.gradle**
Configure Android application with `applicationId "com.appletv.screensaver.artmuseum"`, Java 17 compatibility, unified signing, and minSdk 28.

- [ ] **Step 4: Verify Gradle configuration sync**
Run `./gradlew projects` and verify all 4 app/core modules are recognized.

- [ ] **Step 5: Commit**
```bash
git add settings.gradle apps/screensaver-flipclock/build.gradle apps/screensaver-artmuseum/build.gradle
git commit -m "build: configure screensaver-flipclock and screensaver-artmuseum sub-projects"
```

---

### Task 2: Standalone Module `apps/screensaver-flipclock`

**Files:**
- Create: `apps/screensaver-flipclock/src/main/AndroidManifest.xml`
- Create: `apps/screensaver-flipclock/src/main/res/xml/dream_info.xml`
- Create: `apps/screensaver-flipclock/src/main/res/values/strings.xml`
- Create: `apps/screensaver-flipclock/src/main/java/com/appletv/screensaver/flipclock/FliqloTimeHelper.java`
- Create: `apps/screensaver-flipclock/src/main/java/com/appletv/screensaver/flipclock/FliqloClockView.java`
- Create: `apps/screensaver-flipclock/src/main/java/com/appletv/screensaver/flipclock/FlipClockDreamService.java`
- Create: `apps/screensaver-flipclock/src/main/java/com/appletv/screensaver/flipclock/FlipClockActivity.java`
- Create: `apps/screensaver-flipclock/src/test/java/com/appletv/screensaver/flipclock/FliqloTimeHelperTest.java`

**Interfaces:**
- Produces:
  - DreamService: `com.appletv.screensaver.flipclock.FlipClockDreamService`
  - Exported Activity: `com.appletv.screensaver.flipclock.FlipClockActivity` (Action: `android.intent.action.MAIN`)
  - Helper: `FliqloTimeHelper.formatDigits(int hours, int minutes, boolean is24h)` -> `String[] {hourStr, minStr, amPm}`

- [ ] **Step 1: Write failing unit test for FliqloTimeHelper**
Test 12h vs 24h formatting, AM/PM calculation, digit padding.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew :apps:screensaver-flipclock:testDebugUnitTest`
Expected: Compilation failure or missing class.

- [ ] **Step 3: Implement FliqloTimeHelper and FliqloClockView**
Pure Java 17 canvas split-flap drawing, 3D Camera rotation, OLED anti-burn-in micro-drift.

- [ ] **Step 4: Implement FlipClockDreamService and FlipClockActivity**
Activity with `FLAG_KEEP_SCREEN_ON`, fullscreen, and instant dismissal on `dispatchKeyEvent`. DreamService delegating to `FliqloClockView`.

- [ ] **Step 5: Run tests and assembleRelease**
Run: `./gradlew :apps:screensaver-flipclock:testDebugUnitTest :apps:screensaver-flipclock:assembleRelease`
Expected: PASS and APK built (~150 KB).

- [ ] **Step 6: Commit**
```bash
git add apps/screensaver-flipclock/
git commit -m "feat(screensaver-flipclock): implement standalone Fliqlo split-flap screensaver"
```

---

### Task 3: Standalone Module `apps/screensaver-artmuseum`

**Files:**
- Create: `apps/screensaver-artmuseum/src/main/AndroidManifest.xml`
- Create: `apps/screensaver-artmuseum/src/main/res/xml/dream_info.xml`
- Create: `apps/screensaver-artmuseum/src/main/res/values/strings.xml`
- Create: `apps/screensaver-artmuseum/src/main/assets/paintings/` (15-20 optimized WebP classical masterpieces)
- Create: `apps/screensaver-artmuseum/src/main/java/com/appletv/screensaver/artmuseum/ArtPiece.java`
- Create: `apps/screensaver-artmuseum/src/main/java/com/appletv/screensaver/artmuseum/ArtCatalog.java`
- Create: `apps/screensaver-artmuseum/src/main/java/com/appletv/screensaver/artmuseum/ArtMuseumView.java`
- Create: `apps/screensaver-artmuseum/src/main/java/com/appletv/screensaver/artmuseum/MuseumCardView.java`
- Create: `apps/screensaver-artmuseum/src/main/java/com/appletv/screensaver/artmuseum/ArtMuseumDreamService.java`
- Create: `apps/screensaver-artmuseum/src/main/java/com/appletv/screensaver/artmuseum/ArtMuseumActivity.java`
- Create: `apps/screensaver-artmuseum/src/test/java/com/appletv/screensaver/artmuseum/ArtCatalogTest.java`

**Interfaces:**
- Produces:
  - DreamService: `com.appletv.screensaver.artmuseum.ArtMuseumDreamService`
  - Exported Activity: `com.appletv.screensaver.artmuseum.ArtMuseumActivity`
  - Catalog: `ArtCatalog.getPieces()` -> `List<ArtPiece>`

- [ ] **Step 1: Write failing unit test for ArtCatalog**
Verify catalog piece list size >= 10, valid asset paths, non-empty metadata fields (title, artist, year, museum).

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew :apps:screensaver-artmuseum:testDebugUnitTest`
Expected: Failure due to missing classes.

- [ ] **Step 3: Implement ArtPiece, ArtCatalog, and add artwork assets**
Curated classical masterpieces in WebP format with full metadata.

- [ ] **Step 4: Implement ArtMuseumView, MuseumCardView, DreamService, and Activity**
Ken Burns pan & zoom, dual-view crossfade, tvOS museum card overlay with 6s fade-out, instant remote key dismiss.

- [ ] **Step 5: Run tests and assembleRelease**
Run: `./gradlew :apps:screensaver-artmuseum:testDebugUnitTest :apps:screensaver-artmuseum:assembleRelease`
Expected: PASS and APK built (~2-3 MB).

- [ ] **Step 6: Commit**
```bash
git add apps/screensaver-artmuseum/
git commit -m "feat(screensaver-artmuseum): implement standalone Art Museum screensaver"
```

---

### Task 4: Launcher Dynamic Discovery & Preferences Engine

**Files:**
- Create: `apps/launcher/src/main/java/com/appletv/launcher/ScreensaverDiscovery.java`
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/ScreensaverPreferences.java`
- Create: `apps/launcher/src/test/java/com/appletv/launcher/ScreensaverDiscoveryTest.java`
- Modify: `apps/launcher/src/test/java/com/appletv/launcher/ScreensaverPreferencesTest.java`

**Interfaces:**
- Produces:
  - `ScreensaverDiscovery.ScreensaverItem` (id, title, packageName, serviceName, activityName, icon, isInternal)
  - `ScreensaverDiscovery.getAvailableScreensavers(Context)` -> `List<ScreensaverItem>`
  - `ScreensaverPreferences.getSelectedComponent(Context)` -> `String`
  - `ScreensaverPreferences.setSelectedComponent(Context, String componentName)`
  - `ScreensaverPreferences.syncSecureSettings(Context, String componentName)`

- [ ] **Step 1: Write failing unit tests for ScreensaverPreferences & Discovery fallback**
Verify mode storage, component storage, fallback to `MODE_WALLPAPERS` if component uninstalled.

- [ ] **Step 2: Run tests to verify they fail**
Run: `./gradlew :apps:launcher:testDebugUnitTest --tests "*Screensaver*"`
Expected: FAIL.

- [ ] **Step 3: Implement ScreensaverDiscovery and update ScreensaverPreferences**
Query `PackageManager` for `android.service.dreams.DreamService`, resolve companion activities, sync with `Settings.Secure.screensaver_components`.

- [ ] **Step 4: Run unit tests to verify they pass**
Run: `./gradlew :apps:launcher:testDebugUnitTest --tests "*Screensaver*"`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add apps/launcher/src/main/java/com/appletv/launcher/ScreensaverDiscovery.java \
        apps/launcher/src/main/java/com/appletv/launcher/ScreensaverPreferences.java \
        apps/launcher/src/test/java/com/appletv/launcher/ScreensaverDiscoveryTest.java \
        apps/launcher/src/test/java/com/appletv/launcher/ScreensaverPreferencesTest.java
git commit -m "feat(launcher): implement dynamic screensaver discovery and preferences sync"
```

---

### Task 5: Launcher Lightweight Wallpaper Engine Refactor

**Files:**
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/ScreenSaverActivity.java`
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/ScreensaverView.java`
- Remove: `apps/launcher/src/main/java/com/appletv/launcher/FliqloClockView.java` (moved to `:apps:screensaver-flipclock`)
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/ClockDream.java`

**Interfaces:**
- Consumes: `ScreensaverPreferences`
- Produces: Pure lightweight `ScreenSaverActivity` focusing solely on local 4K wallpaper Ken Burns slideshow.

- [ ] **Step 1: Refactor ScreenSaverActivity and ScreensaverView**
Remove clock and flipclock code from launcher's `ScreensaverView.java`, keeping only the lightweight 4K wallpaper slideshow.

- [ ] **Step 2: Delete FliqloClockView.java from apps/launcher**
Ensure zero code bloat remains in `apps/launcher`.

- [ ] **Step 3: Run launcher unit tests & verify compile**
Run: `./gradlew :apps:launcher:testDebugUnitTest :apps:launcher:assembleRelease`
Expected: PASS.

- [ ] **Step 4: Commit**
```bash
git add apps/launcher/src/main/java/com/appletv/launcher/
git commit -m "refactor(launcher): streamline launcher screensaver engine to 4K wallpapers only"
```

---

### Task 6: Launcher Settings UI & Watchdog Integration

**Files:**
- Modify: `apps/launcher/src/main/res/layout/activity_launcher_settings.xml`
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/LauncherSettingsActivity.java`
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/MainActivity.java`
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/QuickSettingsActivity.java`
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/HomeKeyService.java`

**Interfaces:**
- UI: Dynamic list of screensaver options in "Display & ScreenSaver" tab.
- Triggers: In-Launcher Idle Watchdog and Quick Settings 1-Click trigger `ScreensaverDiscovery.launchScreensaver(Context)`.

- [ ] **Step 1: Update LauncherSettingsActivity layout & logic**
Populate discovered screensavers dynamically (Off, 4K Wallpapers, FlipClock, Art Museum, etc.), wire preview button and system settings button.

- [ ] **Step 2: Update MainActivity idle watchdog & QuickSettingsActivity**
When idle watchdog fires or 1-Click is pressed, invoke `ScreensaverDiscovery.launchScreensaver(context)`.

- [ ] **Step 3: Verify Kid Mode protection**
Ensure `LauncherSettingsActivity` remains inaccessible in Kid Mode (`finish()` on `onCreate`).

- [ ] **Step 4: Run launcher test suite and assembleRelease**
Run: `./gradlew :apps:launcher:testDebugUnitTest :apps:launcher:assembleRelease`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add apps/launcher/
git commit -m "feat(launcher): integrate dynamic screensaver picker in settings and watchdog"
```

---

### Task 7: Deployment Tooling & Monorepo Release Verification

**Files:**
- Modify: `tools/deploy.sh`
- Modify: `AGENTS.md`
- Modify: `CHANGELOG.md`

**Interfaces:**
- Tool: `./tools/deploy.sh <launcher|parental-control|flipclock|artmuseum|all> [TV_IP]`

- [ ] **Step 1: Update tools/deploy.sh**
Add targets `flipclock` and `artmuseum`, update `all` to build and deploy the complete modular ecosystem.

- [ ] **Step 2: Run full monorepo build**
Run: `./gradlew assembleRelease`
Verify APK generation:
  - `apps/launcher/build/outputs/apk/release/launcher-release.apk` (~298 KB)
  - `apps/screensaver-flipclock/build/outputs/apk/release/screensaver-flipclock-release.apk` (~150 KB)
  - `apps/screensaver-artmuseum/build/outputs/apk/release/screensaver-artmuseum-release.apk` (~2.5 MB)

- [ ] **Step 3: Run all monorepo unit tests**
Run: `./gradlew testDebugUnitTest`
Expected: 100% tests PASS across all modules.

- [ ] **Step 4: Update AGENTS.md and CHANGELOG.md**
Document the modular screensaver architecture and deployment commands.

- [ ] **Step 5: Commit**
```bash
git add tools/deploy.sh AGENTS.md CHANGELOG.md
git commit -m "chore(tools): add modular screensaver deployment targets and update docs"
```
