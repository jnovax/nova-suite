# Zero-Bloat Practical TV Suite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the 5 ultra-lightweight practical TV features (Long-Press Tile Menu for Dock/Hide, Enhanced Control Center with Sleep Timer & Night Dimmer, Auto USB Top-Bar Pill, and Interactive Remote Key Mapper & Hidden Apps Manager) with strict Kid Mode protection and < 15 KB APK overhead.

**Architecture:** Extend `Config` and `SharedPreferences("launcher_settings")` to persist on-TV overrides (`dock_apps_override`, `hidden_apps_override`, `remote_keys_override`, `night_dimmer_level`, `sleep_timer_end_ms`). Leverage the existing `HomeKeyService` (`AccessibilityService`) to render the zero-permission `TYPE_ACCESSIBILITY_OVERLAY` Night Dimmer and execute the Sleep Timer standby (`GLOBAL_ACTION_LOCK_SCREEN`). Reuse `MainActivity.glassPopup` for the Long-Press Tile Context Menu and the Top-Bar USB Dropdown.

**Tech Stack:** Pure Android SDK (API 28–34), Java 8, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-09-practical-tv-suite-design.md`

## Global Constraints

- Zero third-party libraries added to `apps/launcher`.
- All layout customization actions (Long-Press Tile Context Menu, Pin/Unpin/Move Dock, Hide/Unhide App, Remote Key Mapper, Launcher Settings) MUST be strictly blocked when `ModeManager.isKidMode(ctx)` is `true`.
- Navigation and volume keys (`DPAD_*`, `ENTER`, `BACK`, `HOME`, `VOLUME_*`, `POWER`) must NEVER be intercepted or remapped in `HomeKeyService` to preserve Linux `InputDispatcher` hardware repeat rate.
- Environment variables for Gradle commands: `export JAVA_HOME=/opt/jdk17 ANDROID_HOME=/opt/android-sdk PATH="/opt/jdk17/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/cmdline-tools/latest/bin:$PATH"`.

---

### Task 1: Storage Overrides & Pure Helper Logic in `Config`

**Files:**
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/Config.java`
- Create: `apps/launcher/src/test/java/com/appletv/launcher/PracticalSuiteTest.java`

**Interfaces:**
- Produces in `Config`:
  - `static final String KEY_DOCK_OVERRIDE = "dock_apps_override"`
  - `static final String KEY_HIDDEN_OVERRIDE = "hidden_apps_override"`
  - `static final String KEY_REMOTE_OVERRIDE = "remote_keys_override"`
  - `static final String KEY_DIMMER_LEVEL = "night_dimmer_level"`
  - `static final String KEY_SLEEP_END_MS = "sleep_timer_end_ms"`
  - `static List<String> parseCsvList(String csv)`
  - `static String toCsvList(List<String> items)`
  - `static List<String> pinToDock(List<String> currentDock, String pkg, int maxColumns)`
  - `static List<String> unpinFromDock(List<String> currentDock, String pkg)`
  - `static List<String> moveInDock(List<String> currentDock, String pkg, int delta)`
  - `static boolean canHidePackage(String pkg, String ownPkg)`
  - `static int nextSleepTimerMinutes(int currentMinutes)`
  - `static int nextNightDimmerLevel(int currentLevel)`
  - `static boolean isProtectedRemoteKey(int keyCode)`
  - `static void saveDockOverride(Context ctx, List<String> dock)`
  - `static void saveHiddenOverride(Context ctx, List<String> hidden)`
  - `static void saveRemoteKeysOverride(Context ctx, Map<Integer, String> customKeys)`
  - `static Map<Integer, String> loadRemoteKeysOverride(Context ctx)`

- [ ] **Step 1: Write the failing unit tests in `PracticalSuiteTest.java`**

```java
package com.appletv.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PracticalSuiteTest {

    @Test
    public void csvListRoundTripPreservesOrderAndIgnoresBlanks() {
        List<String> input = Arrays.asList("app.one", "app.two", "app.three");
        String csv = Config.toCsvList(input);
        assertEquals("app.one,app.two,app.three", csv);
        assertEquals(input, Config.parseCsvList(csv));
        assertEquals(Collections.emptyList(), Config.parseCsvList(" , , "));
    }

    @Test
    public void pinAndUnpinDockRespectsMaxColumns() {
        List<String> dock = Arrays.asList("a.pkg", "b.pkg");
        List<String> pinned = Config.pinToDock(dock, "c.pkg", 4);
        assertEquals(Arrays.asList("a.pkg", "b.pkg", "c.pkg"), pinned);

        // Cannot exceed maxColumns (3)
        List<String> capped = Config.pinToDock(pinned, "d.pkg", 3);
        assertEquals(Arrays.asList("a.pkg", "b.pkg", "c.pkg"), capped);

        List<String> unpinned = Config.unpinFromDock(pinned, "b.pkg");
        assertEquals(Arrays.asList("a.pkg", "c.pkg"), unpinned);
    }

    @Test
    public void moveInDockSwapsLeftAndRightSafely() {
        List<String> dock = Arrays.asList("a.pkg", "b.pkg", "c.pkg");
        assertEquals(Arrays.asList("b.pkg", "a.pkg", "c.pkg"), Config.moveInDock(dock, "b.pkg", -1));
        assertEquals(Arrays.asList("a.pkg", "c.pkg", "b.pkg"), Config.moveInDock(dock, "b.pkg", 1));
        // Out of bounds moves leave list unchanged
        assertEquals(dock, Config.moveInDock(dock, "a.pkg", -1));
        assertEquals(dock, Config.moveInDock(dock, "c.pkg", 1));
    }

    @Test
    public void canHidePackageBlocksLauncherAndParentalControl() {
        assertFalse(Config.canHidePackage("com.appletv.launcher", "com.appletv.launcher"));
        assertFalse(Config.canHidePackage("vn.familytv.parentalcontrol", "com.appletv.launcher"));
        assertTrue(Config.canHidePackage("org.courville.nova", "com.appletv.launcher"));
    }

    @Test
    public void cyclesSleepTimerAndNightDimmerCorrectly() {
        assertEquals(15, Config.nextSleepTimerMinutes(0));
        assertEquals(30, Config.nextSleepTimerMinutes(15));
        assertEquals(60, Config.nextSleepTimerMinutes(30));
        assertEquals(90, Config.nextSleepTimerMinutes(60));
        assertEquals(0, Config.nextSleepTimerMinutes(90));

        assertEquals(25, Config.nextNightDimmerLevel(0));
        assertEquals(50, Config.nextNightDimmerLevel(25));
        assertEquals(70, Config.nextNightDimmerLevel(50));
        assertEquals(0, Config.nextNightDimmerLevel(70));
    }

    @Test
    public void protectsNavigationAndVolumeKeysFromRemapping() {
        assertTrue(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_DPAD_UP));
        assertTrue(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_DPAD_CENTER));
        assertTrue(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_BACK));
        assertTrue(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_HOME));
        assertTrue(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_VOLUME_UP));
        assertTrue(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_VOLUME_DOWN));
        assertTrue(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_POWER));

        assertFalse(Config.isProtectedRemoteKey(190));  // YouTube
        assertFalse(Config.isProtectedRemoteKey(4062)); // Netflix
        assertFalse(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_PROG_RED));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :apps:launcher:testDebugUnitTest --tests com.appletv.launcher.PracticalSuiteTest`
Expected: FAIL with compilation errors (`cannot find symbol Config.toCsvList`, etc.).

- [ ] **Step 3: Implement storage overrides and helpers in `Config.java`**

Add constants, override loading in `Config.load(Context)`, reset cleanup in `syncThemeChoices`, and helper methods to `apps/launcher/src/main/java/com/appletv/launcher/Config.java`:

```java
    static final String KEY_DOCK_OVERRIDE = "dock_apps_override";
    static final String KEY_HIDDEN_OVERRIDE = "hidden_apps_override";
    static final String KEY_REMOTE_OVERRIDE = "remote_keys_override";
    static final String KEY_DIMMER_LEVEL = "night_dimmer_level";
    static final String KEY_SLEEP_END_MS = "sleep_timer_end_ms";
```

In `Config.load(Context ctx)` right after `advancedEffects = choices.getBoolean("advanced_effects", advancedEffects);`:
```java
        if (choices.contains(KEY_DOCK_OVERRIDE)) {
            dock = parseCsvList(choices.getString(KEY_DOCK_OVERRIDE, ""));
        }
        if (choices.contains(KEY_HIDDEN_OVERRIDE)) {
            hidden = parseCsvList(choices.getString(KEY_HIDDEN_OVERRIDE, ""));
        }
        if (choices.contains(KEY_REMOTE_OVERRIDE)) {
            try {
                JSONObject rkJson = new JSONObject();
                rkJson.put("remote_keys", new JSONObject(choices.getString(KEY_REMOTE_OVERRIDE, "{}")));
                parseRemoteKeys(rkJson, remoteKeys);
            } catch (Exception ignored) {}
        }
```

Add the static helper methods to `Config.java`:
```java
    static List<String> parseCsvList(String csv) {
        if (csv == null || csv.trim().isEmpty()) return new ArrayList<>();
        String[] parts = csv.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty() && !out.contains(trimmed)) out.add(trimmed);
        }
        return out;
    }

    static String toCsvList(List<String> items) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (item == null || item.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(item.trim());
        }
        return sb.toString();
    }

    static List<String> pinToDock(List<String> currentDock, String pkg, int maxColumns) {
        List<String> next = new ArrayList<>(currentDock != null ? currentDock : Collections.emptyList());
        if (pkg == null || pkg.isEmpty() || next.contains(pkg) || next.size() >= maxColumns) return next;
        next.add(pkg);
        return next;
    }

    static List<String> unpinFromDock(List<String> currentDock, String pkg) {
        List<String> next = new ArrayList<>(currentDock != null ? currentDock : Collections.emptyList());
        next.remove(pkg);
        return next;
    }

    static List<String> moveInDock(List<String> currentDock, String pkg, int delta) {
        List<String> next = new ArrayList<>(currentDock != null ? currentDock : Collections.emptyList());
        int idx = next.indexOf(pkg);
        int target = idx + delta;
        if (idx < 0 || target < 0 || target >= next.size()) return next;
        Collections.swap(next, idx, target);
        return next;
    }

    static boolean canHidePackage(String pkg, String ownPkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        if (pkg.equals(ownPkg)) return false;
        if (ModeManager.isFamilyTimePackage(pkg)) return false;
        return true;
    }

    static int nextSleepTimerMinutes(int currentMinutes) {
        if (currentMinutes <= 0) return 15;
        if (currentMinutes <= 15) return 30;
        if (currentMinutes <= 30) return 60;
        if (currentMinutes <= 60) return 90;
        return 0;
    }

    static int nextNightDimmerLevel(int currentLevel) {
        if (currentLevel <= 0) return 25;
        if (currentLevel <= 25) return 50;
        if (currentLevel <= 50) return 70;
        return 0;
    }

    static boolean isProtectedRemoteKey(int keyCode) {
        switch (keyCode) {
            case android.view.KeyEvent.KEYCODE_DPAD_UP:
            case android.view.KeyEvent.KEYCODE_DPAD_DOWN:
            case android.view.KeyEvent.KEYCODE_DPAD_LEFT:
            case android.view.KeyEvent.KEYCODE_DPAD_RIGHT:
            case android.view.KeyEvent.KEYCODE_DPAD_CENTER:
            case android.view.KeyEvent.KEYCODE_ENTER:
            case android.view.KeyEvent.KEYCODE_NUMPAD_ENTER:
            case android.view.KeyEvent.KEYCODE_BACK:
            case android.view.KeyEvent.KEYCODE_HOME:
            case android.view.KeyEvent.KEYCODE_VOLUME_UP:
            case android.view.KeyEvent.KEYCODE_VOLUME_DOWN:
            case android.view.KeyEvent.KEYCODE_VOLUME_MUTE:
            case android.view.KeyEvent.KEYCODE_POWER:
            case android.view.KeyEvent.KEYCODE_SLEEP:
            case android.view.KeyEvent.KEYCODE_WAKEUP:
                return true;
            default:
                return keyCode <= 0;
        }
    }

    static void saveDockOverride(Context ctx, List<String> dock) {
        ctx.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_DOCK_OVERRIDE, toCsvList(dock)).apply();
    }

    static void saveHiddenOverride(Context ctx, List<String> hidden) {
        ctx.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_HIDDEN_OVERRIDE, toCsvList(hidden)).apply();
    }

    static Map<Integer, String> loadRemoteKeysOverride(Context ctx) {
        Map<Integer, String> out = new HashMap<>();
        SharedPreferences prefs = ctx.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_REMOTE_OVERRIDE)) return out;
        try {
            JSONObject rkJson = new JSONObject();
            rkJson.put("remote_keys", new JSONObject(prefs.getString(KEY_REMOTE_OVERRIDE, "{}")));
            parseRemoteKeys(rkJson, out);
        } catch (Exception ignored) {}
        return out;
    }

    static void saveRemoteKeysOverride(Context ctx, Map<Integer, String> map) {
        JSONObject obj = new JSONObject();
        if (map != null) {
            for (Map.Entry<Integer, String> e : map.entrySet()) {
                try {
                    if (e.getValue() != null && !e.getValue().trim().isEmpty()) {
                        obj.put(String.valueOf(e.getKey()), e.getValue().trim());
                    }
                } catch (Exception ignored) {}
            }
        }
        ctx.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_REMOTE_OVERRIDE, obj.toString()).apply();
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :apps:launcher:testDebugUnitTest --tests com.appletv.launcher.PracticalSuiteTest`
Expected: PASS (6 tests passing).

- [ ] **Step 5: Commit**

```bash
git add apps/launcher/src/main/java/com/appletv/launcher/Config.java apps/launcher/src/test/java/com/appletv/launcher/PracticalSuiteTest.java
git commit -m "feat(launcher): add Config overrides and pure helpers for practical TV suite"
```

---

### Task 2: Long-Press `OK` Context Menu on App Tiles & Kid Mode Lock

**Files:**
- Modify: `apps/launcher/src/main/res/values/strings.xml`
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/AppAdapter.java`
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/MainActivity.java`

**Interfaces:**
- Consumes from `Config`: `pinToDock`, `unpinFromDock`, `moveInDock`, `canHidePackage`, `saveDockOverride`, `saveHiddenOverride`
- Modifies `AppAdapter.TileListener`: adds `void onTileLongPressed(int position, View card)`

- [ ] **Step 1: Add UI strings in `apps/launcher/src/main/res/values/strings.xml`**

Add before `</resources>`:
```xml
    <string name="pin_to_dock">Pin to Dock</string>
    <string name="unpin_from_dock">Unpin from Dock</string>
    <string name="move_left">Move Left</string>
    <string name="move_right">Move Right</string>
    <string name="hide_app">Hide App</string>
    <string name="app_info">App Info</string>
    <string name="usb">USB</string>
    <string name="usb_open_player">Open Video Player (Nova)</string>
    <string name="usb_storage_settings">Storage &amp; Safely Eject</string>
    <string name="sleep_timer_state">Sleep Timer: %1$s</string>
    <string name="sleep_timer_remaining">%1$d min</string>
    <string name="night_dimmer_state">Night Dimmer: %1$s</string>
    <string name="bluetooth_audio">Bluetooth &amp; Audio</string>
    <string name="start_screensaver">Start Screensaver</string>
    <string name="hidden_apps_state">Hidden apps: %1$d</string>
    <string name="no_hidden_apps">No apps are hidden. Hold OK on any app on the home screen to hide it.</string>
    <string name="unhide_app_title">Select an app to unhide</string>
    <string name="remote_key_mapper">Remote button mapper</string>
    <string name="remote_key_add">+ Map a remote button…</string>
    <string name="remote_key_press_prompt">Press any shortcut button on your remote now\n(Press Back to cancel)</string>
    <string name="remote_key_protected">Navigation, Volume and Power keys cannot be remapped.</string>
    <string name="remote_key_choose_action">Button #%1$d — Choose action</string>
    <string name="action_wallpaper_cycle">Cycle 4K Wallpaper</string>
    <string name="action_wallpaper_picker">Open 4K Wallpaper Picker</string>
    <string name="action_quick_settings">Open Quick Settings</string>
    <string name="action_recent_apps">Open Recent Apps Switcher</string>
    <string name="action_inputs">Open TV Inputs Menu</string>
    <string name="action_open_app">Open App: %1$s</string>
    <string name="action_clear_mapping">Clear mapping</string>
```

- [ ] **Step 2: Update `AppAdapter.java` to delegate long-press to `TileListener` and expose current dock/entry info**

In `apps/launcher/src/main/java/com/appletv/launcher/AppAdapter.java`:
1. Add `void onTileLongPressed(int position, View card);` to `interface TileListener`.
2. Add helper methods in `AppAdapter`:
```java
    Entry entryAt(int position) {
        return position >= 0 && position < items.size() ? items.get(position) : null;
    }

    List<String> currentDockPackages() {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < docked && i < items.size(); i++) {
            Entry e = items.get(i);
            if (e.kind != Entry.EMPTY) out.add(e.pkg);
        }
        return out;
    }
```
3. In `onBindViewHolder`, replace `h.card.setOnLongClickListener(...)` with:
```java
        h.card.setOnLongClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (listener != null && pos != RecyclerView.NO_POSITION) {
                listener.onTileLongPressed(pos, v);
            }
            return true;
        });
```

- [ ] **Step 3: Implement `onTileLongPressed` context menu in `MainActivity.java`**

In `apps/launcher/src/main/java/com/appletv/launcher/MainActivity.java`:
```java
    @Override
    public void onTileLongPressed(int position, View card) {
        // Strictly block tile customization and App Info in Kid Mode
        if (ModeManager.isKidMode(this)) return;
        AppAdapter.Entry entry = adapter.entryAt(position);
        if (entry == null || entry.kind == AppAdapter.Entry.EMPTY || entry.kind == AppAdapter.Entry.HOME_SETTINGS) return;

        View panel = getLayoutInflater().inflate(R.layout.popup_inputs, root, false);
        LinearLayout rows = panel.findViewById(R.id.rows);
        PopupWindow popup = glassPopup(panel, (int) (210 * density));

        List<String> dockPkgs = adapter.currentDockPackages();
        int dockIdx = dockPkgs.indexOf(entry.pkg);
        if (dockIdx >= 0) {
            if (dockIdx > 0) {
                addSettingsMenuRow(rows, popup, R.string.move_left, () -> {
                    Config.saveDockOverride(this, Config.moveInDock(dockPkgs, entry.pkg, -1));
                    applyConfigAndReload();
                });
            }
            if (dockIdx < dockPkgs.size() - 1) {
                addSettingsMenuRow(rows, popup, R.string.move_right, () -> {
                    Config.saveDockOverride(this, Config.moveInDock(dockPkgs, entry.pkg, 1));
                    applyConfigAndReload();
                });
            }
            addSettingsMenuRow(rows, popup, R.string.unpin_from_dock, () -> {
                Config.saveDockOverride(this, Config.unpinFromDock(dockPkgs, entry.pkg));
                applyConfigAndReload();
            });
        } else if (dockPkgs.size() < COLUMNS) {
            addSettingsMenuRow(rows, popup, R.string.pin_to_dock, () -> {
                Config.saveDockOverride(this, Config.pinToDock(dockPkgs, entry.pkg, COLUMNS));
                applyConfigAndReload();
            });
        }

        if (Config.canHidePackage(entry.pkg, getPackageName())) {
            addSettingsMenuRow(rows, popup, R.string.hide_app, () -> {
                List<String> nextHidden = new ArrayList<>(hiddenApps);
                if (!nextHidden.contains(entry.pkg)) nextHidden.add(entry.pkg);
                Config.saveHiddenOverride(this, nextHidden);
                if (dockIdx >= 0) {
                    Config.saveDockOverride(this, Config.unpinFromDock(dockPkgs, entry.pkg));
                }
                applyConfigAndReload();
            });
        }

        addSettingsMenuRow(rows, popup, R.string.app_info, () -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + entry.pkg));
            open(i);
        });

        panel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        popup.setWidth(Math.max((int) (210 * density), panel.getMeasuredWidth()));
        dropIn(popup, panel, card, Gravity.START);
        if (rows.getChildCount() > 0) rows.getChildAt(0).requestFocus();
    }

    private void applyConfigAndReload() {
        Config cfg = Config.load(this);
        hiddenApps = new HashSet<>(cfg.hiddenApps);
        adapter = new AppAdapter(this, this, COLUMNS, cfg);
        grid.setAdapter(adapter);
        reloadTiles();
    }
```
Also guard `showSettingsMenu()` in `MainActivity.java` so long-pressing Settings in `Kid Mode` does not open `LauncherSettingsActivity`:
```java
        if (ModeManager.isKidMode(this)) return;
```

- [ ] **Step 4: Run unit tests to verify compilation and existing tests pass**

Run: `./gradlew :apps:launcher:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add apps/launcher/src/main/res/values/strings.xml apps/launcher/src/main/java/com/appletv/launcher/AppAdapter.java apps/launcher/src/main/java/com/appletv/launcher/MainActivity.java
git commit -m "feat(launcher): add tvOS long-press tile context menu with Kid Mode protection"
```

---

### Task 3: Enhanced Control Center (`QuickSettingsActivity`) & `HomeKeyService` Night Dimmer / Sleep Timer

**Files:**
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/HomeKeyService.java`
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/QuickSettingsActivity.java`

**Interfaces:**
- Produces in `HomeKeyService`: `static final String ACTION_UPDATE_SUITE = "com.appletv.launcher.action.UPDATE_SUITE"`
- Consumes from `Config`: `KEY_DIMMER_LEVEL`, `KEY_SLEEP_END_MS`, `nextSleepTimerMinutes`, `nextNightDimmerLevel`

- [ ] **Step 1: Add Night Dimmer overlay & Sleep Timer execution to `HomeKeyService.java`**

Update `apps/launcher/src/main/java/com/appletv/launcher/HomeKeyService.java`:
- Add fields:
```java
    static final String ACTION_UPDATE_SUITE = "com.appletv.launcher.action.UPDATE_SUITE";
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private android.view.View dimmerView;
    private final Runnable sleepRunnable = this::executeSleepTimer;
```
- Call `syncSuiteState()` in `onServiceConnected()` and override `onStartCommand`:
```java
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_UPDATE_SUITE.equals(intent.getAction())) {
            lastConfigLoadTime = 0;
            syncSuiteState();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void syncSuiteState() {
        android.content.SharedPreferences prefs = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        int dimmerLevel = prefs.getInt(Config.KEY_DIMMER_LEVEL, 0);
        applyDimmer(dimmerLevel);

        handler.removeCallbacks(sleepRunnable);
        long endMs = prefs.getLong(Config.KEY_SLEEP_END_MS, 0L);
        long remaining = endMs - System.currentTimeMillis();
        if (endMs > 0 && remaining > 0) {
            handler.postDelayed(sleepRunnable, remaining);
        } else if (endMs > 0) {
            prefs.edit().putLong(Config.KEY_SLEEP_END_MS, 0L).apply();
        }
    }

    private void applyDimmer(int level) {
        android.view.WindowManager wm = (android.view.WindowManager) getSystemService(WINDOW_SERVICE);
        if (wm == null) return;
        if (level <= 0) {
            if (dimmerView != null) {
                try { wm.removeView(dimmerView); } catch (Exception ignored) {}
                dimmerView = null;
            }
            return;
        }
        float alpha = Math.max(0f, Math.min(0.85f, level / 100f));
        if (dimmerView == null) {
            dimmerView = new android.view.View(this);
            dimmerView.setBackgroundColor(0xFF000000);
            dimmerView.setAlpha(alpha);
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    android.graphics.PixelFormat.TRANSLUCENT);
            try {
                wm.addView(dimmerView, lp);
            } catch (Exception e) {
                dimmerView = null;
            }
        } else {
            dimmerView.setAlpha(alpha);
        }
    }

    private void executeSleepTimer() {
        getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE)
                .edit().putLong(Config.KEY_SLEEP_END_MS, 0L).apply();
        try {
            android.media.AudioManager am = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            if (am != null) {
                am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP));
                am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_STOP));
            }
        } catch (Exception ignored) {}
        try {
            Intent home = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setPackage(getPackageName())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        } catch (Exception ignored) {}
        handler.postDelayed(() -> {
            boolean locked = false;
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                try {
                    locked = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
                } catch (Exception ignored) {}
            }
            if (!locked) {
                try {
                    Intent dream = new Intent(Intent.ACTION_MAIN)
                            .setClassName("com.android.systemui", "com.android.systemui.Somnambulator")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dream);
                } catch (Exception ignored) {}
            }
        }, 600);
    }
```

- [ ] **Step 2: Upgrade `QuickSettingsActivity.java` with Sleep Timer, Night Dimmer, Bluetooth & Screensaver**

Update `apps/launcher/src/main/java/com/appletv/launcher/QuickSettingsActivity.java`:
- Add interactive rows for Sleep Timer (cycles 0/15/30/60/90 min in-place and notifies `HomeKeyService`), Night Dimmer (cycles 0/25/50/70% in-place and notifies `HomeKeyService`), Bluetooth & Audio (`Settings.ACTION_BLUETOOTH_SETTINGS` fallback `Settings.ACTION_SOUND_SETTINGS`), Start Screensaver (`com.android.systemui.Somnambulator`), Wallpaper, Display, Launcher Settings (only when `!ModeManager.isKidMode(this)`), and TV System Settings.

- [ ] **Step 3: Run unit tests to verify everything compiles and passes**

Run: `./gradlew :apps:launcher:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add apps/launcher/src/main/java/com/appletv/launcher/HomeKeyService.java apps/launcher/src/main/java/com/appletv/launcher/QuickSettingsActivity.java
git commit -m "feat(launcher): add Sleep Timer, Night Dimmer, Bluetooth and Screensaver to Control Center"
```

---

### Task 4: Auto `[ 💾 USB ]` Pill on Top Bar (`MainActivity`)

**Files:**
- Create: `apps/launcher/src/main/res/drawable/ic_usb.xml`
- Modify: `apps/launcher/src/main/res/layout/activity_main.xml`
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/MainActivity.java`

- [ ] **Step 1: Create `ic_usb.xml` vector icon and add `btnUsb` to `activity_main.xml`**

Create `apps/launcher/src/main/res/drawable/ic_usb.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="14dp"
    android:height="14dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M15,7v4h1v2h-3V5h2l-3,-4 -3,4h2v8H8v-2.07c0.7,-0.37 1.2,-1.08 1.2,-1.93 0,-1.21 -0.99,-2.2 -2.2,-2.2 -1.21,0 -2.2,0.99 -2.2,2.2 0,0.85 0.5,1.56 1.2,1.93V13c0,1.11 0.89,2 2,2h3v3.05c-0.71,0.37 -1.2,1.1 -1.2,1.95 0,1.22 0.99,2.2 2.2,2.2 1.21,0 2.2,-0.98 2.2,-2.2 0,-0.85 -0.49,-1.58 -1.2,-1.95V15h3c1.11,0 2,-0.89 2,-2v-2h1V7h-4z" />
</vector>
```

Add `btnUsb` right before `btnInputs` in `apps/launcher/src/main/res/layout/activity_main.xml`:
```xml
                    <TextView
                        android:id="@+id/btnUsb"
                        style="@style/TopButton"
                        android:text="@string/usb"
                        android:drawableStart="@drawable/ic_usb"
                        android:visibility="gone"
                        android:nextFocusDown="@+id/grid" />
```

- [ ] **Step 2: Implement USB detection, broadcast receiver, and 2-row dropdown in `MainActivity.java`**

In `MainActivity.java`:
1. Bind `btnUsb = findViewById(R.id.btnUsb);` and wire `btnUsb.setOnClickListener(v -> showUsbMenu());`.
2. Include `btnUsb` in `topBarButtons()` focus navigation list when visible.
3. Implement `hasMountedUsbDrive()` using `StorageManager.getStorageVolumes()` (`v.isRemovable() && Environment.MEDIA_MOUNTED.equals(v.getState())`) and register a `BroadcastReceiver` for `ACTION_MEDIA_MOUNTED`, `ACTION_MEDIA_UNMOUNTED`, `ACTION_MEDIA_EJECT`, `ACTION_MEDIA_REMOVED` with scheme `"file"` in `onStart()` / `onStop()`.
4. Implement `showUsbMenu()` opening a `glassPopup` under `btnUsb` with:
   - Row 1: `R.string.usb_open_player` -> launches `org.courville.nova` (or fallback video/file manager or `Settings.ACTION_INTERNAL_STORAGE_SETTINGS`).
   - Row 2: `R.string.usb_storage_settings` -> launches `new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)`.

- [ ] **Step 3: Run unit tests to verify build**

Run: `./gradlew :apps:launcher:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add apps/launcher/src/main/res/drawable/ic_usb.xml apps/launcher/src/main/res/layout/activity_main.xml apps/launcher/src/main/java/com/appletv/launcher/MainActivity.java
git commit -m "feat(launcher): add auto-appearing USB top-bar pill with Nova player & safe eject menu"
```

---

### Task 5: Hidden Apps Manager & Interactive Remote Key Mapper in `LauncherSettingsActivity`

**Files:**
- Modify: `apps/launcher/src/main/res/layout/activity_launcher_settings.xml`
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/LauncherSettingsActivity.java`

- [ ] **Step 1: Add `hiddenAppsChoice` and `remoteKeysChoice` rows to `activity_launcher_settings.xml`**

Add before `followThemeChoice` in `apps/launcher/src/main/res/layout/activity_launcher_settings.xml`:
```xml
                <TextView
                    android:id="@+id/hiddenAppsChoice"
                    style="@style/SettingsRow"
                    android:layout_marginTop="16dp" />

                <TextView
                    android:id="@+id/remoteKeysChoice"
                    style="@style/SettingsRow"
                    android:layout_marginTop="4dp"
                    android:text="@string/remote_key_mapper" />
```

- [ ] **Step 2: Implement Hidden Apps dialog, Interactive Remote Key Mapper, and Kid Mode guard in `LauncherSettingsActivity.java`**

In `LauncherSettingsActivity.java`:
1. At the top of `onCreate`: `if (ModeManager.isKidMode(this)) { finish(); return; }`.
2. Wire `hiddenAppsChoice` to display count of hidden apps and open `showHiddenAppsDialog()` to unhide any selected app with 1 click.
3. Wire `remoteKeysChoice` to open `showRemoteKeyMapperDialog()`:
   - Lists current mappings + `"+ Map a remote button…"`.
   - Pressing `"+ Map a remote button…"` opens a key-capture dialog (`setOnKeyListener`) that rejects `Config.isProtectedRemoteKey(code)` and captures any valid shortcut key/scancode, then opens `showPickActionForKeyDialog(capturedCode)` allowing the user to assign `wallpaper_cycle`, `wallpaper_picker`, `quick_settings`, `recent_apps`, `inputs`, any installed Leanback app, or clear the mapping.
   - Saves via `Config.saveRemoteKeysOverride(this, map)` and notifies `HomeKeyService` immediately.
4. Update `followThemeChoice` and `hasOnTvChoices()` to also clear `Config.KEY_DOCK_OVERRIDE`, `Config.KEY_HIDDEN_OVERRIDE`, and `Config.KEY_REMOTE_OVERRIDE`.

- [ ] **Step 3: Run full test suite & assemble release APK**

Run: `./gradlew testDebugUnitTest assembleRelease`
Expected: PASS, APK generated at `apps/launcher/build/outputs/apk/release/app-release.apk` with size < 235 KB.

- [ ] **Step 4: Commit**

```bash
git add apps/launcher/src/main/res/layout/activity_launcher_settings.xml apps/launcher/src/main/java/com/appletv/launcher/LauncherSettingsActivity.java
git commit -m "feat(launcher): add Hidden Apps manager and Interactive Remote Key Mapper in settings"
```
