# Fliqlo Flip Clock Screensaver Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the TV screensaver clock mode into an authentic, pixel-accurate clone of the iconic Fliqlo flip clock (https://fliqlo.com/screensaver/) with 3D mechanical split-flap card animation, subtle OLED micro-drift anti-burn-in protection, and 12h/24h support.

**Architecture:** A high-performance, lightweight pure Java custom `View` (`FliqloClockView`) rendering dual mechanical split-flap cards (Hours and Minutes) via hardware-accelerated `Canvas` and `android.graphics.Camera` 3D perspective rotation matrix. Smooth 650ms gravity ease animation on minute change with dynamic lighting/shadows, zero heap allocations in `onDraw()`, and 6-minute interval micro-drift.

**Tech Stack:** Android SDK (Java 17), `android.graphics.Camera`, `Matrix`, `ValueAnimator`, `AccelerateDecelerateInterpolator`, Leanback / Android TV DreamService.

---

### Task 1: Time Model & Flip State Unit Tests
- Create unit tests for Fliqlo time extraction (12h format with AM/PM, 24h format, digit split, change detection).
- Verify state transitions when minutes or hours change.
- **Files:** `apps/launcher/src/test/java/com/appletv/launcher/FliqloTimeTest.java`

### Task 2: Implement Pure Java `FliqloClockView`
- Create `FliqloClockView.java`:
  - Geometry: Dual flip cards (Hours left, Minutes right) with rounded corners, horizontal split gap, AM/PM label.
  - 3D Projection: Two-phase split-flap folding using `android.graphics.Camera.rotateX()`:
    - Phase 1 (0° to 90°): Old top flap folds downward with dynamic shadow darkening.
    - Phase 2 (90° to 0°): New bottom flap lands with deceleration and shadow clearing.
  - OLED Anti-Burn-In: Periodic micro-drift (±12–16px) every 6 minutes using subtle translation animation.
  - Zero-allocation `onDraw()` loop with pre-allocated `Path`, `Paint`, `RectF`, `Camera`, `Matrix`.
- **Files:** `apps/launcher/src/main/java/com/appletv/launcher/FliqloClockView.java`

### Task 3: Integrate into `ScreensaverView` & `ClockDream`
- Update `ScreensaverView.java`:
  - When `selectedMode == ScreensaverPreferences.CLOCK`, embed and activate `FliqloClockView`.
  - Ensure clean lifecycle management (`start()`, `stop()`, detachment cleanup).
- Update `ScreenSaverActivity.java` and `ClockDream.java` to support full interactive preview and dream transitions.
- **Files:**
  - `apps/launcher/src/main/java/com/appletv/launcher/ScreensaverView.java`

### Task 4: Verification & Emulator Validation
- Build with `./gradlew assembleRelease` and run unit tests.
- Deploy to Android TV Emulator `emulator-5554` via `./tools/deploy.sh launcher`.
- Start Clock Screensaver preview via `am start -n com.appletv.launcher/.ScreenSaverActivity`.
- Take screenshots and verify visual fidelity, 3D split-flap cards, and typography.
