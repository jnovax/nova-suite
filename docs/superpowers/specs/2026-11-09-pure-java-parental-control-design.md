# Design Specification: Pure Java Parental Control & TV D-Pad Wrap-Around Navigation

- **Date:** 2026-11-09
- **Status:** Approved
- **Authors:** AI Assistant & Pair Programmer
- **Target Application:** `apps/parental-control` (`vn.familytv.parentalcontrol`)
- **Target Version:** `v1.3.0`

---

## 1. Context & Motivation

In version `v1.2.0`, the monorepo structure consisted of:
- `core/contract`: 100% Pure Java 17 library (~15 KB).
- `apps/launcher`: 100% Pure Java 17 Leanback Android Launcher (~298 KB APK, build time ~3s).
- `apps/parental-control`: Mixed Kotlin/Android application (~530 KB after R8, previously 3.4 MB without R8, build time ~25-40s due to Kotlin compiler daemon).

### Primary Drivers for Full Java Migration:
1. **Monorepo Uniformity & Zero-Bloat Toolchain:** Eliminate `org.jetbrains.kotlin.android` plugin, `kotlin-stdlib`, and Kotlin compiler daemon overhead. The entire project builds purely with standard Java 17 and Android SDK build tools.
2. **Extreme TV APK Footprint (< 200 KB):** By removing all residual Kotlin metadata, synthetic lambdas, reflection tables, and optimizing the oversized Leanback banner (`family_time_app_art.png`), the release APK shrinks to **~150–180 KB**.
3. **Decomposition of `MainActivity`:** Decompose the monolithic `MainActivity.kt` (1,274 lines) into clean, focused UI view controllers (< 250 lines each).
4. **Enhanced TV Remote D-Pad Navigation:** Implement 4-way circular wrap-around navigation on `DpadPinPadView` (Left ⟷ Right edge wrapping, and Up ⟷ Down loop covering the keypad and action buttons) for twice-as-fast PIN/OTP entry with a TV remote.

---

## 2. Architecture & Module Structure

### 2.1 Build Configuration Migration
- Rename `apps/parental-control/build.gradle.kts` to `apps/parental-control/build.gradle` (Groovy DSL).
- Apply standard plugins: `id 'com.android.application'`.
- Configuration:
  ```groovy
  android {
      namespace 'vn.familytv.parentalcontrol'
      compileSdk 34

      defaultConfig {
          applicationId 'vn.familytv.parentalcontrol'
          minSdk 28
          targetSdk 34
          versionCode 3
          versionName '1.3.0'
          resConfigs 'en', 'vi'
      }

      compileOptions {
          sourceCompatibility JavaVersion.VERSION_17
          targetCompatibility JavaVersion.VERSION_17
      }

      buildTypes {
          release {
              minifyEnabled true
              shrinkResources true
              proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
              signingConfig signingConfigs.debug
          }
      }
  }

  dependencies {
      implementation project(':core:contract')
      implementation 'com.google.zxing:core:3.5.3'
      testImplementation 'junit:junit:4.13.2'
  }
  ```

### 2.2 Asset Optimization
- Replace `family_time_app_art.png` (`2064x1232`, 438 KB) with a TV-standard Leanback banner (`640x360`, MozJPEG/WebP quality 90, ~25 KB).
- Remove unused legacy images (`ic_pctv.png`, `pctv_banner.png`, `family_time_logo.jpg`).

---

## 3. Package & Class Design

### 3.1 Security Package (`vn.familytv.parentalcontrol.security`)
- **`Base32.java`:** RFC 4648 Base32 encoding/decoding without external dependencies.
- **`TotpEngine.java`:** RFC 6238 time-based one-time password verification using `javax.crypto.Mac` (`HmacSHA1`) and `otpauth://` URI generator.
- **`KeyStoreVault.java`:** AndroidKeyStore helper using `AES/GCM/NoPadding` with fallback.
- **`ParentSessionManager.java`:** 5-minute sliding parent session tracking using `AtomicLong`.
- **`RateLimiter.java`:** Exponential lockout rate limiter tracking consecutive failures.
- **`RecoveryCodeManager.java`:** Generates 6 emergency recovery codes; verifies using SHA-256 with salt.
  - Subclasses/Records: `RecoveryCodeRecord`, `GeneratedRecoverySet`.
- **`SecurityStore.java`:** Thread-safe `SharedPreferences` storage managing auth mode (`PIN` vs `TOTP`), custom PIN hash, and recovery sets.

### 3.2 Screen Time & Policy Package (`vn.familytv.parentalcontrol.screentime`)
- **`ScreenTimePolicy.java`:** Hardened whitelist packages (SmartTube, YouTube Kids, VLC, Duolingo, Khan Academy) and tracked app predicate.
- **`ScreenTimeStore.java`:** Persistent daily screen-time limits (weekday 1-4h, weekend 1-4h), used seconds, and 00:00 midnight reset check.
- **`ScreenTimeTracker.java`:** In-memory second-by-second accumulator with tick listeners.
- **`UsageTimelineAnalyzer.java`:** Queries `UsageStatsManager`, aggregates overlapping app intervals, and formats human-readable durations (`HH:mm → HH:mm`).
  - POJOs: `AppSession`, `TimelineAnalysis`.

### 3.3 Inter-Process Communication Package (`vn.familytv.parentalcontrol.ipc`)
- **`Mode.java`:** Enum representing `KID` and `PARENT`.
- **`ModeStateManager.java`:** Thread-safe Singleton managing active mode and remaining session time.
- **`ModeBroadcastSender.java`:** Dispatches `ACTION_MODE_CHANGED` broadcast to Nova TV Launcher.
- **`ModeContentProvider.java` & `StatusProvider.java`:** ContentProvider querying mode and status instantaneously without blocking UI threads.

### 3.4 Background Platform Services (`vn.familytv.parentalcontrol.platform`)
- **`ForegroundMonitorService.java`:**
  - `AccessibilityService` monitoring `TYPE_WINDOW_STATE_CHANGED`.
  - Filters foreground packages via `ForegroundEventFilter.java`.
  - 1-second `Handler` ticker evaluating active screen time.
  - Triggers `TimeLockOverlayManager.show()` and `AudioFocusInterrupter.mute()` when time expires.
- **`PermissionReader.java`:** Checks `PACKAGE_USAGE_STATS` (`AppOpsManager`) and Accessibility Service connection state.
- **`AudioFocusInterrupter.java`:** Grants/abandons audio focus using `AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE`.

---

## 4. UI Architecture & Decomposition

### 4.1 Custom Views (`vn.familytv.parentalcontrol.ui`)
- **`TvViews.java`:** Layout factory methods (`title`, `body`, `text`, `button`, `column`, `dp`). Default button width is strictly `WRAP_CONTENT` with `Gravity.CENTER_HORIZONTAL`.
- **`TimelineBarView.java`:** Custom View rendering a 24-hour visual bar (1440 minutes) on `Canvas`.
- **`DpadPinPadView.java`:** 3x4 numeric keypad with dynamic digits, slot views, and full 4-way D-Pad wrap-around navigation.
- **`QrCodeRenderer.java`:** Renders ZXing `BitMatrix` to an Android `Bitmap`.
- **`RecoveryCodesView.java`:** Displays 6 recovery codes in a 2-column rounded card layout.

### 4.2 D-Pad Circular Wrap-Around Navigation Specification
The 3x4 keypad buttons:
```
[ 1 ](id: 1)   [ 2 ](id: 2)   [ 3 ](id: 3)
[ 4 ](id: 4)   [ 5 ](id: 5)   [ 6 ](id: 6)
[ 7 ](id: 7)   [ 8 ](id: 8)   [ 9 ](id: 9)
[ ⌫ ](id: del) [ 0 ](id: 0)   [ ✔ ](id: ok)
         [ Action Button ](id: back/exit)
```

1. **Horizontal Wrap-Around (Left ⟷ Right):**
   - Button 1 `nextFocusLeftId` = Button 3; Button 3 `nextFocusRightId` = Button 1.
   - Button 4 `nextFocusLeftId` = Button 6; Button 6 `nextFocusRightId` = Button 4.
   - Button 7 `nextFocusLeftId` = Button 9; Button 9 `nextFocusRightId` = Button 7.
   - Button ⌫ `nextFocusLeftId` = Button ✔; Button ✔ `nextFocusRightId` = Button ⌫.

2. **Vertical Circular Loop (Up ⟷ Down):**
   - From Row 0 (`1, 2, 3`): pressing **UP** wraps around to the bottom Action Button (or Row 3 if no button).
   - From Row 3 (`⌫, 0, ✔`): pressing **DOWN** focuses the bottom Action Button.
   - From the bottom Action Button: pressing **DOWN** loops back up to Row 0 (`2`).
   - From the bottom Action Button: pressing **UP** focuses Row 3 (`0`).

### 4.3 Screen Decomposition (`vn.familytv.parentalcontrol.ui.screens`)
- **`KidGateView.java`:** Handles initial Parent PIN/TOTP gate when opening app in Kid Mode.
- **`ParentConfigView.java`:** Full parental control dashboard (Status card, daily limits, permission fixers, security switcher).
- **`UsageTimelineView.java`:** Visual 24-hour bar + scrollable list of today's app sessions.
- **`TotpEnrollView.java`:** QR Code display + OTP confirmation pad.
- **`ChangePinView.java`:** 6-digit PIN change keypad.
- **`MainActivity.java`:** Clean coordinator activity (~150 lines) managing parent session state, view switching, and lifecycle.

---

## 5. Verification & Test Plan

1. **Unit Testing:**
   - Migrate all 18 existing test suites to pure JUnit 4 in `src/test/java/`.
   - Add `DpadPinPadNavigationTest.java` verifying wrap-around IDs across all 12 keys.
   - Target: 100% pass rate (`./gradlew testDebugUnitTest :core:contract:test`).
2. **Packaging & Size:**
   - Verify zero Kotlin classes in DEX:
     `/opt/android-sdk/build-tools/34.0.0/dexdump -f ... | grep 'Lkotlin/'` returns 0.
   - Target APK size: `<= 200 KB`.
3. **On-TV Functional Verification:**
   - Deploy to TV (`192.168.1.7:5555`) via `./tools/deploy.sh all`.
   - Verify D-Pad wrap-around on TV remote.
   - Verify screen-time countdown, TimeLock overlay, and IPC status provider with Nova TV Launcher.
