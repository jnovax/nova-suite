package com.appletv.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView hiddenAppsChoice;
    private TextView remoteKeysChoice;
    private TextView followThemeChoice;
    private int columns;
    private boolean animations;
    private boolean advancedEffects;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (ModeManager.isKidMode(this)) {
            finish();
            return;
        }
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        columns = config.columns;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                setResult(RESULT_OK);
                refresh();
            });
        }
        // Keep the numeric choices increasing in the Hebrew reading direction. Reorder the
        // actual views so D-pad focus follows their positions without changing the saved values.
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            LinearLayout row = (LinearLayout) columnChoices[0].getParent();
            row.removeAllViews();
            for (int i = columnChoices.length - 1; i >= 0; i--) row.addView(columnChoices[i]);
        }
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            setResult(RESULT_OK);
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        hiddenAppsChoice = findViewById(R.id.hiddenAppsChoice);
        if (hiddenAppsChoice != null) {
            hiddenAppsChoice.setOnClickListener(v -> showHiddenAppsDialog());
        }
        remoteKeysChoice = findViewById(R.id.remoteKeysChoice);
        if (remoteKeysChoice != null) {
            remoteKeysChoice.setOnClickListener(v -> showRemoteKeyMapperDialog());
        }
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit()
                    .remove("columns")
                    .remove("animations")
                    .remove("advanced_effects")
                    .remove(Config.KEY_DOCK_OVERRIDE)
                    .remove(Config.KEY_HIDDEN_OVERRIDE)
                    .remove(Config.KEY_REMOTE_OVERRIDE)
                    .commit();
            Config current = Config.load(this);
            columns = current.columns;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            notifySuiteService();
            setResult(RESULT_OK);
            refresh();
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(new Intent(Settings.ACTION_SETTINGS)));
        refresh();
        columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    private void showHiddenAppsDialog() {
        Config cfg = Config.load(this);
        final List<String> hidden = new ArrayList<>(cfg.hiddenApps);
        if (hidden.isEmpty()) {
            Toast.makeText(this, R.string.no_hidden_apps, Toast.LENGTH_LONG).show();
            return;
        }
        PackageManager pm = getPackageManager();
        CharSequence[] labels = new CharSequence[hidden.size()];
        for (int i = 0; i < hidden.size(); i++) {
            String pkg = hidden.get(i);
            String label = pkg;
            try {
                CharSequence cs = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
                if (cs != null && cs.length() > 0) label = cs + " (" + pkg + ")";
            } catch (Exception ignored) {}
            labels[i] = label;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.unhide_app_title)
                .setItems(labels, (dialog, which) -> {
                    hidden.remove(which);
                    Config.saveHiddenOverride(this, hidden);
                    setResult(RESULT_OK);
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showRemoteKeyMapperDialog() {
        Config cfg = Config.load(this);
        final Map<Integer, String> allKeys = new TreeMap<>(cfg.remoteKeys);
        final List<Integer> codes = new ArrayList<>(allKeys.keySet());
        CharSequence[] items = new CharSequence[codes.size() + 1];
        items[0] = getString(R.string.remote_key_add);
        for (int i = 0; i < codes.size(); i++) {
            int code = codes.get(i);
            items[i + 1] = "Key #" + code + "  \u2192  " + describeTarget(allKeys.get(code));
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.remote_key_mapper)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        showCaptureKeyDialog();
                    } else {
                        showPickActionForKeyDialog(codes.get(which - 1));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showCaptureKeyDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.remote_key_mapper)
                .setMessage(R.string.remote_key_press_prompt)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                d.dismiss();
                return true;
            }
            if (Config.isProtectedRemoteKey(keyCode)) {
                Toast.makeText(this, R.string.remote_key_protected, Toast.LENGTH_SHORT).show();
                return true;
            }
            int captured = keyCode > 0 ? keyCode : event.getScanCode();
            if (captured <= 0) return true;
            d.dismiss();
            showPickActionForKeyDialog(captured);
            return true;
        });
        dialog.show();
    }

    private void showPickActionForKeyDialog(final int keyCode) {
        final List<String> targets = new ArrayList<>();
        final List<CharSequence> labels = new ArrayList<>();

        targets.add("wallpaper_cycle");
        labels.add(getString(R.string.action_wallpaper_cycle));
        targets.add("wallpaper_picker");
        labels.add(getString(R.string.action_wallpaper_picker));
        targets.add("quick_settings");
        labels.add(getString(R.string.action_quick_settings));
        targets.add("recent_apps");
        labels.add(getString(R.string.action_recent_apps));
        targets.add("inputs");
        labels.add(getString(R.string.action_inputs));

        PackageManager pm = getPackageManager();
        Intent probe = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(probe, 0);
        Collections.sort(apps, (a, b) -> String.valueOf(a.loadLabel(pm)).compareToIgnoreCase(String.valueOf(b.loadLabel(pm))));
        for (ResolveInfo ri : apps) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || targets.contains(pkg)) continue;
            String appName = AppAdapter.resolveAppLabel(pkg, String.valueOf(ri.loadLabel(pm)));
            targets.add(pkg);
            labels.add(getString(R.string.action_open_app, appName));
        }
        targets.add("");
        labels.add(getString(R.string.action_clear_mapping));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.remote_key_choose_action, keyCode))
                .setItems(labels.toArray(new CharSequence[0]), (d, which) -> {
                    Map<Integer, String> custom = Config.loadRemoteKeysOverride(this);
                    String chosen = targets.get(which);
                    if (chosen.isEmpty()) {
                        custom.remove(keyCode);
                    } else {
                        custom.put(keyCode, chosen);
                    }
                    Config.saveRemoteKeysOverride(this, custom);
                    notifySuiteService();
                    setResult(RESULT_OK);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String describeTarget(String target) {
        if (target == null) return "";
        if ("wallpaper_cycle".equals(target)) return getString(R.string.action_wallpaper_cycle);
        if ("wallpaper_picker".equals(target)) return getString(R.string.action_wallpaper_picker);
        if ("quick_settings".equals(target)) return getString(R.string.action_quick_settings);
        if ("recent_apps".equals(target)) return getString(R.string.action_recent_apps);
        if ("inputs".equals(target)) return getString(R.string.action_inputs);
        try {
            PackageManager pm = getPackageManager();
            CharSequence cs = pm.getApplicationLabel(pm.getApplicationInfo(target, 0));
            if (cs != null && cs.length() > 0) return AppAdapter.resolveAppLabel(target, cs.toString());
        } catch (Exception ignored) {}
        return target;
    }

    private void notifySuiteService() {
        try {
            startService(new Intent(this, HomeKeyService.class)
                    .setAction(HomeKeyService.ACTION_UPDATE_SUITE));
        } catch (Exception ignored) {}
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        setResult(RESULT_OK);
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        if (hiddenAppsChoice != null) {
            int hiddenCount = Config.load(this).hiddenApps.size();
            hiddenAppsChoice.setText(getString(R.string.hidden_apps_state, hiddenCount));
        }
        followThemeChoice.setSelected(!hasOnTvChoices());
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains("animations")
                || choices.contains("advanced_effects")
                || choices.contains(Config.KEY_DOCK_OVERRIDE)
                || choices.contains(Config.KEY_HIDDEN_OVERRIDE)
                || choices.contains(Config.KEY_REMOTE_OVERRIDE);
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
