package com.appletv.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Handles remote shortcut keys:
 * - Direct app remaps for streaming buttons (YouTube: 190, VTVgo: 4168, Netflix: 4062).
 * - Settings button (QuickSettings or System Settings).
 * - Recent Apps button (BUTTON_2 or scancode 150).
 * - Home button on remotes emitting KEYCODE_ALL_APPS.
 *
 * All navigation and volume keys strictly pass through untouched to preserve native
 * repeat acceleration.
 */
public class HomeKeyService extends AccessibilityService {

    static final String ACTION_UPDATE_SUITE = "com.appletv.launcher.action.UPDATE_SUITE";
    private int[] settingsScancodes = new int[0];
    private int[] settingsKeycodes = new int[0];
    private boolean settingsPanel;
    private Config config;
    private long lastConfigLoadTime = 0;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private android.view.View dimmerView;
    private final Runnable sleepRunnable = this::executeSleepTimer;
    private final Runnable dimmerCheckRunnable = new Runnable() {
        @Override
        public void run() {
            applyDimmer(Config.getActiveDimmerAlpha(HomeKeyService.this));
            handler.postDelayed(this, 60_000L); // Check schedule every minute
        }
    };
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
        lastConfigLoadTime = 0;
        if (Config.KEY_DIMMER_LEVEL.equals(key) || Config.KEY_DIMMER_MODE.equals(key) || Config.KEY_SLEEP_END_MS.equals(key)) {
            handler.post(this::syncSuiteState);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        settingsScancodes = getResources().getIntArray(R.array.settings_key_scancodes);
        getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(prefListener);
        getConfig();
        syncSuiteState();
        handler.removeCallbacks(dimmerCheckRunnable);
        handler.post(dimmerCheckRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_UPDATE_SUITE.equals(intent.getAction())) {
            lastConfigLoadTime = 0;
            syncSuiteState();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(prefListener);
        handler.removeCallbacks(sleepRunnable);
        applyDimmer(0);
        super.onDestroy();
    }

    private synchronized Config getConfig() {
        long now = System.currentTimeMillis();
        if (config == null || (now - lastConfigLoadTime > 4000)) {
            config = Config.load(this);
            lastConfigLoadTime = now;
            settingsKeycodes = config.settingsKeycodes;
            settingsPanel = config.settingsKeyPanel;
        }
        return config;
    }

    private boolean isSettingsKey(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_SETTINGS || event.getKeyCode() == 83) return true;
        for (int code : settingsScancodes) if (event.getScanCode() == code) return true;
        for (int code : settingsKeycodes) if (event.getKeyCode() == code) return true;
        return false;
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        android.util.Log.i("AppleTv.HomeKey", "onKeyEvent: keyCode=" + event.getKeyCode() + " scanCode=" + event.getScanCode() + " action=" + event.getAction());
        android.util.Log.e("HomeKeyTest", "key: " + event.getKeyCode() + " action: " + event.getAction());

        // 0. Never intercept navigation, volume, or power keys
        if (Config.isProtectedRemoteKey(event.getKeyCode())) {
            return false;
        }

        // 1. Direct App Shortcuts & Actions (YouTube, Netflix, VTVgo, Inputs, Wallpaper, etc.)
        Config cfg = getConfig();
        String target = cfg.remoteKeys.get(event.getKeyCode());
        if (target == null && event.getScanCode() > 0) {
            target = cfg.remoteKeys.get(event.getScanCode());
        }
        if (target != null) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                launchTarget(target);
            }
            return true;
        }

        // 2. Settings button
        if (isSettingsKey(event)) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                Intent open = new Intent(this, QuickSettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                try {
                    startActivity(open);
                } catch (Exception e) {
                    android.util.Log.e("HomeKeyService", "Failed to start settings", e);
                }
            }
            return true;
        }

        // 3. Recent Apps switcher
        if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_2 || event.getScanCode() == 150) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                Intent open = new Intent(this, RecentAppsActivity.class);
                startActivity(open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            return true;
        }

        // 4. Xiaomi Home / KEYCODE_ALL_APPS
        if (event.getKeyCode() != KeyEvent.KEYCODE_ALL_APPS) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
            Intent home = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setPackage(getPackageName())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(home);
        }
        return true;
    }

    static String getActionType(String target) {
        if (target == null) return null;
        String t = target.trim();
        if ("quick_settings".equalsIgnoreCase(t)) return "quick_settings";
        if ("recent_apps".equalsIgnoreCase(t)) return "recent_apps";
        if ("inputs".equalsIgnoreCase(t)) return "inputs";
        if ("wallpaper_cycle".equalsIgnoreCase(t)) return "wallpaper_cycle";
        if ("wallpaper_picker".equalsIgnoreCase(t)) return "wallpaper_picker";
        if ("screensaver".equalsIgnoreCase(t)) return "screensaver";
        return "app";
    }

    private void launchTarget(String target) {
        String type = getActionType(target);
        if ("screensaver".equals(type)) {
            if (ScreensaverPreferences.mode(this) != ScreensaverPreferences.OFF) {
                ScreensaverDiscovery.launchScreensaver(this);
            }
            return;
        }
        if ("quick_settings".equals(type)) {
            Intent intent = new Intent(this, QuickSettingsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try {
                startActivity(intent);
            } catch (Exception e) {
                android.util.Log.e("HomeKeyService", "Failed to start settings", e);
            }
            return;
        }

        if ("recent_apps".equals(type)) {
            Intent intent = new Intent(this, RecentAppsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try {
                startActivity(intent);
            } catch (Exception e) {
                android.util.Log.e("HomeKeyService", "Failed to start recent apps", e);
            }
            return;
        }

        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if ("inputs".equals(type)) {
            intent.setAction(MainActivity.ACTION_SHOW_INPUTS);
        } else if ("wallpaper_picker".equals(type)) {
            intent.setAction(MainActivity.ACTION_SHOW_WALLPAPERS);
        } else if ("wallpaper_cycle".equals(type)) {
            intent.setAction(MainActivity.ACTION_CYCLE_WALLPAPER);
        } else {
            intent.setAction(MainActivity.ACTION_LAUNCH_APP);
            intent.putExtra(MainActivity.EXTRA_PACKAGE_NAME, target);
        }
        try {
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("HomeKeyService", "Failed to start intent for " + target, e);
        }
    }

    private void launchPackage(String pkg) {
        try {
            Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if (intent == null) {
                intent = getPackageManager().getLaunchIntentForPackage(pkg);
            }
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(intent);
            } else {
                android.util.Log.w("HomeKeyService", "No launch intent found for package: " + pkg);
            }
        } catch (Exception e) {
            android.util.Log.e("HomeKeyService", "Failed to launch package: " + pkg, e);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    private void syncSuiteState() {
        android.content.SharedPreferences prefs = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        int dimmerLevel = Config.getActiveDimmerAlpha(this);
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
}
