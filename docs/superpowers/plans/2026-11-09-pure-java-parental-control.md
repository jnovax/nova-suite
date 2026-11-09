# Pure Java Parental Control & TV D-Pad Wrap-Around Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Completely migrate `apps/parental-control` from Kotlin to 100% Pure Java 17, decompose `MainActivity` into focused screen controllers, implement 4-way D-Pad circular wrap-around navigation on `DpadPinPadView`, optimize Leanback image assets, and eliminate the Kotlin Gradle plugin/compiler daemon to achieve an ultra-lightweight release APK under 200 KB.

**Architecture:** Bottom-up layered migration starting from independent business logic (Security, Screentime, IPC) verified with JUnit 4 tests, through background platform services and custom views, to UI screen decomposition and Groovy build script conversion.

**Tech Stack:** Java 17, Android SDK 34 (minSdk 28), Android TV Leanback, ZXing Core 3.5.3 (pure Java), JUnit 4, R8 ProGuard Minifier & Resource Shrinker.

**Spec:** `docs/superpowers/specs/2026-11-09-pure-java-parental-control-design.md`

## Global Constraints
- Target Application: `apps/parental-control` (`vn.familytv.parentalcontrol`).
- Java Version: Pure Java 17 (`sourceCompatibility = JavaVersion.VERSION_17`, `targetCompatibility = JavaVersion.VERSION_17`).
- Zero Kotlin dependencies or runtime remaining in final APK (`dexdump` must find 0 classes under `Lkotlin/`).
- Zero external UI libraries: no AndroidX Fragment, no Jetpack Compose, no ViewModel/Lifecycle dependencies.
- Button layout standard: All single buttons must default to `WRAP_CONTENT` with `Gravity.CENTER_HORIZONTAL`.
- D-Pad wrap-around: Horizontal Left ⟷ Right edges wrap; Vertical Up ⟷ Down loops through keypad and bottom action button.
- Target Release APK size: `<= 200 KB` (optimized from 3.4 MB / 530 KB).

---

### Task 1: Security Package Java Migration & Unit Tests

**Files:**
- Create:
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/security/Base32.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/security/TotpEngine.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/security/KeyStoreVault.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/security/ParentSessionManager.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/security/RateLimiter.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/security/RecoveryCodeRecord.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/security/GeneratedRecoverySet.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/security/RecoveryCodeManager.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/security/AuthMode.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/security/SecurityStore.java`
- Delete:
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/security/*.kt`
- Test:
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/security/Base32Test.java`
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/security/TotpEngineTest.java`
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/security/ParentSessionManagerTest.java`
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/security/RateLimiterTest.java`
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/security/RecoveryCodeManagerTest.java`
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/security/SecurityStoreTest.java`
  - Delete `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/security/*.kt`

**Interfaces:**
- Consumes: Standard Android `SharedPreferences`, `java.security`, `javax.crypto`.
- Produces: `SecurityStore`, `TotpEngine`, `RecoveryCodeManager`, `RateLimiter`, `ParentSessionManager`.

- [ ] **Step 1: Write Java Security unit tests**
  Port `Base32Test`, `TotpEngineTest`, `ParentSessionManagerTest`, `RateLimiterTest`, `RecoveryCodeManagerTest`, `SecurityStoreTest` to Java JUnit 4.
- [ ] **Step 2: Implement Java Security classes**
  Implement `Base32.java`, `TotpEngine.java`, `KeyStoreVault.java`, `ParentSessionManager.java`, `RateLimiter.java`, `RecoveryCodeRecord.java`, `GeneratedRecoverySet.java`, `RecoveryCodeManager.java`, `AuthMode.java`, `SecurityStore.java`.
- [ ] **Step 3: Remove Kotlin security files and run tests**
  Remove `.kt` files in `security/` (main and test) and run:
  ```bash
  export JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk PATH="/opt/jdk17/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/cmdline-tools/latest/bin:$PATH"
  ./gradlew :apps:parental-control:testDebugUnitTest --tests "vn.familytv.parentalcontrol.security.*"
  ```
  Expected: PASS all security tests.
- [ ] **Step 4: Commit**
  ```bash
  git add apps/parental-control/src/main/java/vn/familytv/parentalcontrol/security/ apps/parental-control/src/test/java/vn/familytv/parentalcontrol/security/
  git commit -m "refactor(parental-control): migrate security package to pure Java"
  ```

---

### Task 2: Screen Time & Policy Package Java Migration & Unit Tests

**Files:**
- Create:
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/screentime/ScreenTimePolicy.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/screentime/ScreenTimeStore.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/screentime/ScreenTimeTracker.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/screentime/AppSession.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/screentime/TimelineAnalysis.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/screentime/UsageTimelineAnalyzer.java`
- Delete:
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/screentime/*.kt`
- Test:
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/screentime/ScreenTimePolicyTest.java`
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/screentime/ScreenTimeStoreTest.java`
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/screentime/ScreenTimeTrackerTest.java`
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/screentime/UsageTimelineAnalyzerTest.java`
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/screentime/DebugCountdownTest.java`
  - Delete `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/screentime/*.kt`

**Interfaces:**
- Consumes: `SecurityStore`, Android `UsageStatsManager`, `Calendar`.
- Produces: `ScreenTimeStore`, `ScreenTimePolicy`, `ScreenTimeTracker`, `UsageTimelineAnalyzer`.

- [ ] **Step 1: Write Java Screentime unit tests**
  Port all 5 screentime unit test classes to Java JUnit 4.
- [ ] **Step 2: Implement Java Screentime classes**
  Implement `ScreenTimePolicy.java`, `ScreenTimeStore.java`, `ScreenTimeTracker.java`, `AppSession.java`, `TimelineAnalysis.java`, `UsageTimelineAnalyzer.java`.
- [ ] **Step 3: Remove Kotlin screentime files and run tests**
  Remove `.kt` files in `screentime/` and run:
  ```bash
  export JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk PATH="/opt/jdk17/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/cmdline-tools/latest/bin:$PATH"
  ./gradlew :apps:parental-control:testDebugUnitTest --tests "vn.familytv.parentalcontrol.screentime.*"
  ```
  Expected: PASS all screentime tests.
- [ ] **Step 4: Commit**
  ```bash
  git add apps/parental-control/src/main/java/vn/familytv/parentalcontrol/screentime/ apps/parental-control/src/test/java/vn/familytv/parentalcontrol/screentime/
  git commit -m "refactor(parental-control): migrate screentime package to pure Java"
  ```

---

### Task 3: IPC & Provider Package Java Migration & Unit Tests

**Files:**
- Create:
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ipc/Mode.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ipc/ModeStateManager.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ipc/ModeBroadcastSender.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ipc/ModeContentProvider.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/provider/StatusProvider.java`
- Delete:
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ipc/*.kt`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/provider/*.kt`
- Test:
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/ipc/ModeStateManagerTest.java`
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/ipc/ModeBroadcastSenderTest.java`
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/ipc/ModeContentProviderTest.java`
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/provider/StatusProviderTest.java`
  - Delete old test `.kt` files.

**Interfaces:**
- Consumes: `:core:contract` (`ModeContract`, `StatusProviderContract`), `ScreenTimeStore`.
- Produces: ContentProviders and ModeStateManager.

- [ ] **Step 1: Write Java IPC and Provider unit tests**
  Port test classes to Java JUnit 4.
- [ ] **Step 2: Implement Java IPC and Provider classes**
  Implement `Mode.java`, `ModeStateManager.java`, `ModeBroadcastSender.java`, `ModeContentProvider.java`, and `StatusProvider.java`.
- [ ] **Step 3: Remove Kotlin IPC/provider files and run tests**
  ```bash
  export JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk PATH="/opt/jdk17/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/cmdline-tools/latest/bin:$PATH"
  ./gradlew :apps:parental-control:testDebugUnitTest --tests "vn.familytv.parentalcontrol.ipc.*" --tests "vn.familytv.parentalcontrol.provider.*"
  ```
  Expected: PASS all tests.
- [ ] **Step 4: Commit**
  ```bash
  git add apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ipc/ apps/parental-control/src/main/java/vn/familytv/parentalcontrol/provider/ apps/parental-control/src/test/java/vn/familytv/parentalcontrol/
  git commit -m "refactor(parental-control): migrate IPC and StatusProvider to pure Java"
  ```

---

### Task 4: Background Service & Audio/Platform Java Migration

**Files:**
- Create:
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/audio/AudioFocusInterrupter.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/platform/ForegroundEventFilter.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/platform/PermissionReader.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/platform/PermissionSnapshot.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/platform/AccessibilityState.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/platform/ForegroundMonitorService.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/onboarding/OnboardingState.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/onboarding/OnboardingStore.java`
- Delete:
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/audio/*.kt`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/platform/*.kt`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/onboarding/*.kt`
- Test:
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/platform/ForegroundEventFilterTest.java`
  - `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/onboarding/OnboardingStateTest.java`
  - Delete old test `.kt` files.

**Interfaces:**
- Consumes: Android `AccessibilityService`, `AudioManager`, `AppOpsManager`.
- Produces: `ForegroundMonitorService`, `AudioFocusInterrupter`, `PermissionReader`.

- [ ] **Step 1: Write Java Platform/Audio unit tests**
  Port `ForegroundEventFilterTest` and `OnboardingStateTest` to Java JUnit 4.
- [ ] **Step 2: Implement Java Platform/Audio/Onboarding classes**
  Implement `AudioFocusInterrupter.java`, `ForegroundEventFilter.java`, `PermissionReader.java`, `PermissionSnapshot.java`, `AccessibilityState.java`, `ForegroundMonitorService.java`, `OnboardingState.java`, `OnboardingStore.java`.
- [ ] **Step 3: Remove Kotlin files and run tests**
  ```bash
  export JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk PATH="/opt/jdk17/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/cmdline-tools/latest/bin:$PATH"
  ./gradlew :apps:parental-control:testDebugUnitTest --tests "vn.familytv.parentalcontrol.platform.*" --tests "vn.familytv.parentalcontrol.onboarding.*"
  ```
  Expected: PASS all tests.
- [ ] **Step 4: Commit**
  ```bash
  git add apps/parental-control/src/main/java/vn/familytv/parentalcontrol/platform/ apps/parental-control/src/main/java/vn/familytv/parentalcontrol/audio/ apps/parental-control/src/main/java/vn/familytv/parentalcontrol/onboarding/ apps/parental-control/src/test/java/vn/familytv/parentalcontrol/
  git commit -m "refactor(parental-control): migrate background monitor service and audio to pure Java"
  ```

---

### Task 5: Custom Views & 4-Way D-Pad Wrap-Around Navigation

**Files:**
- Create:
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ui/TvViews.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ui/TimelineBarView.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ui/auth/QrCodeRenderer.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ui/auth/RecoveryCodesView.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ui/auth/DpadPinPadView.java`
- Delete:
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ui/*.kt`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ui/auth/*.kt`
- Test:
  - Create: `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/ui/auth/DpadPinPadNavigationTest.java`

**Interfaces:**
- Consumes: ZXing Core (`MultiFormatWriter`), standard Android Canvas & Layout.
- Produces: `TvViews`, `DpadPinPadView` (with 4-way circular wrap-around focus IDs), `TimelineBarView`.

- [ ] **Step 1: Write DpadPinPadNavigationTest**
  Write tests in `DpadPinPadNavigationTest.java` verifying:
  - Button 1 `nextFocusLeftId == R.id.pin_btn_3`, Button 3 `nextFocusRightId == R.id.pin_btn_1`.
  - Button 4 `nextFocusLeftId == R.id.pin_btn_6`, Button 6 `nextFocusRightId == R.id.pin_btn_4`.
  - Button 7 `nextFocusLeftId == R.id.pin_btn_9`, Button 9 `nextFocusRightId == R.id.pin_btn_7`.
  - Button ⌫ `nextFocusLeftId == R.id.pin_btn_ok`, Button ✔ `nextFocusRightId == R.id.pin_btn_del`.
  - Row 0 buttons wrap UP to Row 3 or bottom action ID.
- [ ] **Step 2: Implement Java UI views and DpadPinPadView with wrap-around**
  Implement `TvViews.java`, `TimelineBarView.java`, `QrCodeRenderer.java`, `RecoveryCodesView.java`, and `DpadPinPadView.java`.
- [ ] **Step 3: Remove Kotlin UI files and run tests**
  ```bash
  export JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk PATH="/opt/jdk17/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/cmdline-tools/latest/bin:$PATH"
  ./gradlew :apps:parental-control:testDebugUnitTest --tests "vn.familytv.parentalcontrol.ui.auth.DpadPinPadNavigationTest"
  ```
  Expected: PASS navigation test.
- [ ] **Step 4: Commit**
  ```bash
  git add apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ui/ apps/parental-control/src/test/java/vn/familytv/parentalcontrol/ui/
  git commit -m "feat(parental-control): implement Java custom views and 4-way D-Pad wrap-around navigation"
  ```

---

### Task 6: TimeLock Overlay, Screen Decomposition & MainActivity

**Files:**
- Create:
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/timelock/TimeLockOverlayLayout.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/timelock/TimeLockOverlayManager.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/auth/RemoteAuthActivity.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ui/screens/KidGateView.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ui/screens/ParentConfigView.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ui/screens/UsageTimelineView.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ui/screens/TotpEnrollView.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/ui/screens/ChangePinView.java`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/MainActivity.java`
- Delete:
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/timelock/*.kt`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/auth/*.kt`
  - `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/MainActivity.kt`
- Test:
  - Port `EndToEndM3IntegrationTest.kt` to `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/EndToEndM3IntegrationTest.java`

**Interfaces:**
- Consumes: All UI Views, Security, Screentime, IPC components.
- Produces: `MainActivity`, `RemoteAuthActivity`, `TimeLockOverlayManager`.

- [ ] **Step 1: Implement TimeLockOverlayLayout and TimeLockOverlayManager in Java**
  Implement `TimeLockOverlayLayout.java` and `TimeLockOverlayManager.java` with D-pad wrapping and centered buttons.
- [ ] **Step 2: Implement screen controllers and MainActivity**
  Implement `KidGateView.java`, `ParentConfigView.java`, `UsageTimelineView.java`, `TotpEnrollView.java`, `ChangePinView.java`, and decomposed `MainActivity.java`.
- [ ] **Step 3: Implement RemoteAuthActivity and port integration test**
  Implement `RemoteAuthActivity.java` and port `EndToEndM3IntegrationTest.java`.
- [ ] **Step 4: Remove remaining `.kt` files and run all tests**
  Ensure no `.kt` files remain under `apps/parental-control/src/`.
  ```bash
  export JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk PATH="/opt/jdk17/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/cmdline-tools/latest/bin:$PATH"
  ./gradlew :apps:parental-control:testDebugUnitTest
  ```
  Expected: PASS all unit tests in parental-control.
- [ ] **Step 5: Commit**
  ```bash
  git add apps/parental-control/src/
  git commit -m "refactor(parental-control): decompose MainActivity and complete Java migration of all screens"
  ```

---

### Task 7: Build Script Migration (Groovy `build.gradle`), Asset Optimization & TV Verification

**Files:**
- Create:
  - `apps/parental-control/build.gradle` (Groovy DSL)
- Delete:
  - `apps/parental-control/build.gradle.kts`
- Modify:
  - Optimize `apps/parental-control/src/main/res/drawable-nodpi/family_time_app_art.png` to TV Leanback banner dimensions (`640x360`, ~25 KB).
  - Remove unused legacy images: `ic_pctv.png`, `pctv_banner.png`, `family_time_logo.jpg`.

- [ ] **Step 1: Optimize Leanback banner and clean legacy images**
  Resize `family_time_app_art.png` using ImageMagick / ffmpeg / mozjpeg to 640x360 (~25 KB) and delete unused images.
- [ ] **Step 2: Convert build script to pure Groovy and remove Kotlin plugin**
  Create `apps/parental-control/build.gradle` without `org.jetbrains.kotlin.android`. Delete `apps/parental-control/build.gradle.kts`.
- [ ] **Step 3: Clean build, run full test suite, and verify zero Kotlin in APK**
  Run:
  ```bash
  export JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk PATH="/opt/jdk17/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/cmdline-tools/latest/bin:$PATH"
  ./gradlew testDebugUnitTest :core:contract:test assembleRelease --rerun-tasks
  ```
  Verify zero Kotlin classes in DEX:
  ```bash
  /opt/android-sdk/build-tools/34.0.0/dexdump -f apps/parental-control/build/outputs/apk/release/parental-control-release.apk | grep 'Class descriptor' | grep -c 'Lkotlin/' || true
  ```
  Expected: `0`
  Verify APK size:
  ```bash
  ls -lh apps/parental-control/build/outputs/apk/release/parental-control-release.apk
  ```
  Expected: `<= 200 KB`.
- [ ] **Step 4: Deploy to TV and perform on-device verification**
  Run:
  ```bash
  ./tools/deploy.sh all 192.168.1.7:5555
  ```
  Verify on TV:
  - Remote Left/Right wrapping on PIN pad.
  - Remote Up/Down looping between keypad and action buttons.
  - Screen limits change and persistence.
  - Timeline 24h bar display.
- [ ] **Step 5: Commit and tag**
  ```bash
  git add apps/parental-control/
  git commit -m "perf(parental-control): finalize pure Java build, optimize assets, and achieve <200KB release APK"
  ```
