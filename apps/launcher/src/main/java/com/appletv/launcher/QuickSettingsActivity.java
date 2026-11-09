package com.appletv.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The panel the remote's Settings button opens when {@code settings_key_panel} is on (HomeKeyService):
 * a few glass rows under the Settings pill, over Home or whatever app is playing. Translucent and in a
 * task of its own, so the app behind keeps running and Back returns to it. Pressing Settings again
 * closes the panel.
 */
public final class QuickSettingsActivity extends Activity {

    private SharedPreferences prefs;
    private TextView sleepTitle;
    private TextView dimmerTitle;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_quick_settings);
        prefs = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        findViewById(R.id.scrim).setOnClickListener(v -> finish());
        LinearLayout rows = findViewById(R.id.rows);

        // 1. Sleep Timer (in-place cycle: Off -> 15m -> 30m -> 60m -> 90m -> Off)
        sleepTitle = addToggleRow(rows, R.drawable.ic_settings, formatSleepTimerText(), this::cycleSleepTimer);

        // 2. Night Dimmer (in-place cycle: Off -> 25% -> 50% -> 70% -> Off)
        dimmerTitle = addToggleRow(rows, R.drawable.ic_picture, formatNightDimmerText(), this::cycleNightDimmer);

        // 3. Bluetooth & Audio shortcut
        addActionRow(rows, R.drawable.ic_settings, getString(R.string.bluetooth_audio), this::openBluetoothSettings);

        // 4. Start Screensaver immediately
        addActionRow(rows, R.drawable.ic_picture, getString(R.string.start_screensaver), this::startScreensaver);

        // 5. 4K Wallpaper Picker
        addRow(rows, R.drawable.ic_picture, R.string.wallpaper, 0,
                new Intent(this, MainActivity.class).setAction(MainActivity.ACTION_SHOW_WALLPAPERS));

        // 6. Display / Picture Settings
        addRow(rows, R.drawable.ic_picture, R.string.quick_picture, 0, new Intent(Settings.ACTION_DISPLAY_SETTINGS));

        // 7. Launcher Settings (Parent Mode only)
        if (!ModeManager.isKidMode(this)) {
            addRow(rows, R.drawable.ic_home_settings, R.string.launcher_settings, 0,
                    new Intent(this, MainActivity.class).setAction(MainActivity.ACTION_LAUNCHER_SETTINGS));
        }

        // 8. System Settings
        addRow(rows, R.drawable.ic_settings, R.string.tv_settings, 0, new Intent(Settings.ACTION_SETTINGS));

        if (rows.getChildCount() > 0) {
            rows.getChildAt(0).requestFocus();
        }
    }

    private int currentSleepMinutes() {
        long endMs = prefs.getLong(Config.KEY_SLEEP_END_MS, 0L);
        long remMs = endMs - System.currentTimeMillis();
        if (endMs <= 0 || remMs <= 0) return 0;
        return (int) ((remMs + 59_999L) / 60_000L);
    }

    private String formatSleepTimerText() {
        int mins = currentSleepMinutes();
        String state = mins <= 0
                ? getString(R.string.off)
                : getString(R.string.sleep_timer_remaining, mins);
        return getString(R.string.sleep_timer_state, state);
    }

    private void cycleSleepTimer() {
        int nextMins = Config.nextSleepTimerMinutes(currentSleepMinutes());
        long endMs = nextMins <= 0 ? 0L : System.currentTimeMillis() + nextMins * 60_000L;
        prefs.edit().putLong(Config.KEY_SLEEP_END_MS, endMs).apply();
        if (sleepTitle != null) sleepTitle.setText(formatSleepTimerText());
        notifySuiteService();
    }

    private String formatNightDimmerText() {
        int level = prefs.getInt(Config.KEY_DIMMER_LEVEL, 0);
        String state = level <= 0 ? getString(R.string.off) : (level + "%");
        return getString(R.string.night_dimmer_state, state);
    }

    private void cycleNightDimmer() {
        int current = prefs.getInt(Config.KEY_DIMMER_LEVEL, 0);
        int next = Config.nextNightDimmerLevel(current);
        prefs.edit().putInt(Config.KEY_DIMMER_LEVEL, next).apply();
        if (dimmerTitle != null) dimmerTitle.setText(formatNightDimmerText());
        notifySuiteService();
    }

    private void notifySuiteService() {
        try {
            startService(new Intent(this, HomeKeyService.class)
                    .setAction(HomeKeyService.ACTION_UPDATE_SUITE));
        } catch (Exception ignored) {}
    }

    private void openBluetoothSettings() {
        Intent[] candidates = new Intent[]{
                new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                new Intent(Settings.ACTION_SOUND_SETTINGS),
                new Intent(Settings.ACTION_SETTINGS)
        };
        for (Intent i : candidates) {
            try {
                startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (Exception ignored) {}
        }
        Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
    }

    private void startScreensaver() {
        try {
            Intent dream = new Intent(Intent.ACTION_MAIN)
                    .setClassName("com.android.systemui", "com.android.systemui.Somnambulator")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dream);
        } catch (Exception e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
    }

    /** Settings pressed again while the panel is up. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
    }

    /** Anything else taking the screen (Home, an app, a screensaver) closes the panel. */
    @Override
    protected void onPause() {
        super.onPause();
        finish();
        overridePendingTransition(0, 0);
    }

    private TextView addToggleRow(LinearLayout rows, int icon, String text, Runnable onToggle) {
        View row = getLayoutInflater().inflate(R.layout.item_input, rows, false);
        ((ImageView) row.findViewById(R.id.icon)).setImageResource(icon);
        TextView titleView = row.findViewById(R.id.title);
        titleView.setText(text);
        row.findViewById(R.id.dot).setVisibility(View.GONE);
        row.findViewById(R.id.status).setVisibility(View.GONE);
        row.setOnClickListener(v -> onToggle.run());
        rows.addView(row);
        return titleView;
    }

    private void addActionRow(LinearLayout rows, int icon, String text, Runnable action) {
        View row = getLayoutInflater().inflate(R.layout.item_input, rows, false);
        ((ImageView) row.findViewById(R.id.icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.title)).setText(text);
        row.findViewById(R.id.dot).setVisibility(View.GONE);
        row.findViewById(R.id.status).setVisibility(View.GONE);
        row.setOnClickListener(v -> {
            action.run();
            finish();
        });
        rows.addView(row);
    }

    private void addRow(LinearLayout rows, int icon, int title, int status, Intent target) {
        View row = getLayoutInflater().inflate(R.layout.item_input, rows, false);
        ((ImageView) row.findViewById(R.id.icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.title)).setText(title);
        row.findViewById(R.id.dot).setVisibility(View.GONE);
        TextView hint = row.findViewById(R.id.status);
        if (status != 0) hint.setText(status);
        else hint.setVisibility(View.GONE);
        row.setOnClickListener(v -> {
            try {
                startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
            }
            finish();
        });
        rows.addView(row);
    }
}
