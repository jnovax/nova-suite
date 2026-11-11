package com.appletv.launcher;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

final class ScreensaverPreferences {
    private static final String TAG = "ScreensaverPrefs";

    static final String MODE = "screensaver_mode";
    static final String TIMEOUT = "screensaver_timeout_mins";
    static final String OVERLAY = "screensaver_show_overlay";
    static final String COMPONENT = "screensaver_component";
    static final String ACTIVITY = "screensaver_activity";

    static final int MODE_OFF = 0;
    static final int MODE_WALLPAPERS = 1;
    static final int MODE_EXTERNAL = 2;

    // Backwards compatibility aliases
    static final int OFF = MODE_OFF;
    static final int CLOCK = MODE_WALLPAPERS;
    static final int WALLPAPERS = MODE_WALLPAPERS;

    private ScreensaverPreferences() {}

    static int validMode(int value) {
        if (value >= MODE_OFF && value <= MODE_EXTERNAL) return value;
        return MODE_WALLPAPERS;
    }

    static int validTimeout(int value) {
        switch (value) {
            case 0: case 2: case 5: case 10: case 15: return value;
            default: return 5;
        }
    }

    static long idleDelayMillis(int mode, int minutes) {
        return validMode(mode) == MODE_OFF ? 0 : validTimeout(minutes) * 60000L;
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
    }

    static int mode(Context context) {
        return validMode(prefs(context).getInt(MODE, MODE_WALLPAPERS));
    }

    static void setMode(Context context, int mode) {
        prefs(context).edit().putInt(MODE, validMode(mode)).apply();
    }

    static int timeout(Context context) {
        return validTimeout(prefs(context).getInt(TIMEOUT, 5));
    }

    static void setTimeout(Context context, int timeout) {
        prefs(context).edit().putInt(TIMEOUT, validTimeout(timeout)).apply();
    }

    static boolean overlay(Context context) {
        return prefs(context).getBoolean(OVERLAY, true);
    }

    static void setOverlay(Context context, boolean overlay) {
        prefs(context).edit().putBoolean(OVERLAY, overlay).apply();
    }

    static String selectedComponent(Context context) {
        return prefs(context).getString(COMPONENT, null);
    }

    static String selectedActivity(Context context) {
        return prefs(context).getString(ACTIVITY, null);
    }

    static void setSelectedExternal(Context context, String component, String activity) {
        prefs(context).edit()
                .putInt(MODE, MODE_EXTERNAL)
                .putString(COMPONENT, component)
                .putString(ACTIVITY, activity)
                .apply();
        syncSecureSettings(context, component);
    }

    static void syncSecureSettings(Context context, String component) {
        if (component == null || component.trim().isEmpty()) return;
        try {
            ContentResolver cr = context.getContentResolver();
            Settings.Secure.putString(cr, "screensaver_components", component);
            Settings.Secure.putInt(cr, "screensaver_enabled", 1);
        } catch (SecurityException e) {
            Log.w(TAG, "Cannot write screensaver_components (requires WRITE_SECURE_SETTINGS): " + e.getMessage());
        } catch (Exception e) {
            Log.w(TAG, "Failed to sync screensaver to system settings: " + e.getMessage());
        }
    }

    static String extractPackage(String component) {
        if (component == null || !component.contains("/")) return null;
        String[] parts = component.split("/");
        return parts.length > 0 && !parts[0].trim().isEmpty() ? parts[0].trim() : null;
    }

    static String extractClass(String component) {
        if (component == null || !component.contains("/")) return null;
        String[] parts = component.split("/");
        return parts.length > 1 ? parts[1].trim() : null;
    }
}
