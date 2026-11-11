package com.appletv.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * tvOS 18 2-Column Launcher Settings:
 * Left Sidebar: Categories Navigation.
 * Right Pane: Dynamic Detail Settings.
 * Overlay: Pure Liquid Glass Modal Dialogs.
 */
public final class LauncherSettingsActivity extends Activity {

    private SharedPreferences choices;

    // Tabs
    private TextView tabAppearance;
    private TextView tabDisplayNight;
    private TextView tabRemotes;
    private TextView tabAppsSystem;
    private TextView categoryBadge;
    private int currentCategory = 0; // 0=Appearance, 1=DisplayNight, 2=Remotes, 3=AppsSystem

    // Panes
    private View paneAppearance;
    private View paneDisplayNight;
    private View paneRemotes;
    private View paneAppsSystem;

    // Appearance Pane Controls
    private final TextView[] columnChoices = new TextView[4];
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private int columns;
    private boolean animations;
    private boolean advancedEffects;

    // Display & Night Dimmer & Screensaver Controls
    private LinearLayout screensaverModeContainer;
    private TextView screensaverModeOff;
    private TextView screensaverModeWallpapers;
    private final List<TextView> screensaverExternalButtons = new ArrayList<>();
    private final List<ScreensaverDiscovery.ScreensaverOption> discoveredScreensavers = new ArrayList<>();
    private final TextView[] screensaverTimeoutChoices = new TextView[5];
    private final int[] screensaverTimeoutValues = {0, 2, 5, 10, 15};
    private TextView screensaverOverlayChoice;
    private TextView screensaverPreviewChoice;
    private TextView screensaverSystemSettingsChoice;

    private TextView dimmerModeOff;
    private TextView dimmerModeScheduled;
    private TextView dimmerModeAlways;
    private TextView dimmerLevel25;
    private TextView dimmerLevel50;
    private TextView dimmerLevel70;
    private int dimmerMode;
    private int dimmerLevel;

    // Remotes Pane Controls
    private TextView remoteKeysChoice;
    private TextView mappedKeysHeader;
    private LinearLayout mappedKeysList;

    // Apps & System Pane Controls
    private TextView hiddenAppsChoice;
    private TextView defaultHomeChoice;

    // Liquid Glass Modal Overlay
    private FrameLayout modalOverlay;
    private TextView modalTitle;
    private TextView modalSubtitle;
    private LinearLayout modalItemsContainer;
    private TextView modalCancelButton;
    private View previousFocusedView;
    private KeyCaptureListener keyCaptureListener;

    interface KeyCaptureListener {
        void onKeyCaptured(int keyCode);
    }

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

        dimmerMode = choices.getInt(Config.KEY_DIMMER_MODE, Config.DIMMER_MODE_SCHEDULED);
        dimmerLevel = choices.getInt(Config.KEY_DIMMER_LEVEL, Config.DEFAULT_DIMMER_LEVEL);

        bindViews();
        setupNavigation();
        setupAppearancePane();
        setupDisplayNightPane();
        setupRemotesPane();
        setupAppsSystemPane();
        setupModalOverlay();

        refresh();

        // Initial category focus
        selectCategory(0, false);
        tabAppearance.requestFocus();
    }

    private void bindViews() {
        categoryBadge = findViewById(R.id.settingsCategoryBadge);
        tabAppearance = findViewById(R.id.tabAppearance);
        tabDisplayNight = findViewById(R.id.tabDisplayNight);
        tabRemotes = findViewById(R.id.tabRemotes);
        tabAppsSystem = findViewById(R.id.tabAppsSystem);

        paneAppearance = findViewById(R.id.paneAppearance);
        paneDisplayNight = findViewById(R.id.paneDisplayNight);
        paneRemotes = findViewById(R.id.paneRemotes);
        paneAppsSystem = findViewById(R.id.paneAppsSystem);

        columnChoices[0] = findViewById(R.id.columns4);
        columnChoices[1] = findViewById(R.id.columns5);
        columnChoices[2] = findViewById(R.id.columns6);
        columnChoices[3] = findViewById(R.id.columns7);

        animationsChoice = findViewById(R.id.animationsChoice);
        advancedChoice = findViewById(R.id.advancedChoice);
        followThemeChoice = findViewById(R.id.followThemeChoice);

        dimmerModeOff = findViewById(R.id.dimmerModeOff);
        dimmerModeScheduled = findViewById(R.id.dimmerModeScheduled);
        dimmerModeAlways = findViewById(R.id.dimmerModeAlways);

        dimmerLevel25 = findViewById(R.id.dimmerLevel25);
        dimmerLevel50 = findViewById(R.id.dimmerLevel50);
        dimmerLevel70 = findViewById(R.id.dimmerLevel70);

        screensaverModeContainer = findViewById(R.id.screensaverModeContainer);
        screensaverModeOff = findViewById(R.id.screensaverModeOff);
        screensaverModeWallpapers = findViewById(R.id.screensaverModeWallpapers);

        screensaverTimeoutChoices[0] = findViewById(R.id.screensaverTimeout0);
        screensaverTimeoutChoices[1] = findViewById(R.id.screensaverTimeout2);
        screensaverTimeoutChoices[2] = findViewById(R.id.screensaverTimeout5);
        screensaverTimeoutChoices[3] = findViewById(R.id.screensaverTimeout10);
        screensaverTimeoutChoices[4] = findViewById(R.id.screensaverTimeout15);

        screensaverOverlayChoice = findViewById(R.id.screensaverOverlayChoice);
        screensaverPreviewChoice = findViewById(R.id.screensaverPreviewChoice);
        screensaverSystemSettingsChoice = findViewById(R.id.screensaverSystemSettingsChoice);

        remoteKeysChoice = findViewById(R.id.remoteKeysChoice);
        mappedKeysHeader = findViewById(R.id.mappedKeysHeader);
        mappedKeysList = findViewById(R.id.mappedKeysList);

        hiddenAppsChoice = findViewById(R.id.hiddenAppsChoice);
        defaultHomeChoice = findViewById(R.id.defaultHomeChoice);

        modalOverlay = findViewById(R.id.modalOverlay);
        modalTitle = findViewById(R.id.modalTitle);
        modalSubtitle = findViewById(R.id.modalSubtitle);
        modalItemsContainer = findViewById(R.id.modalItemsContainer);
        modalCancelButton = findViewById(R.id.modalCancelButton);
    }

    private void setupNavigation() {
        TextView[] tabs = {tabAppearance, tabDisplayNight, tabRemotes, tabAppsSystem};
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            TextView tab = tabs[i];
            tab.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    selectCategory(index, false);
                }
            });
            tab.setOnClickListener(v -> selectCategory(index, true));
            tab.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    focusActivePane();
                    return true;
                }
                return false;
            });
        }
    }

    private void selectCategory(int index, boolean focusDetail) {
        currentCategory = index;
        tabAppearance.setActivated(index == 0);
        tabDisplayNight.setActivated(index == 1);
        tabRemotes.setActivated(index == 2);
        tabAppsSystem.setActivated(index == 3);

        paneAppearance.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        paneDisplayNight.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        paneRemotes.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        paneAppsSystem.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        int[] badgeTitles = {R.string.tab_appearance, R.string.tab_display_night, R.string.tab_remotes, R.string.tab_apps_system};
        categoryBadge.setText(badgeTitles[index]);

        if (index == 2) {
            refreshMappedKeysList();
        }

        if (focusDetail) {
            focusActivePane();
        }
    }

    private void focusActivePane() {
        View target = null;
        switch (currentCategory) {
            case 0:
                target = columnChoices[Math.max(0, Math.min(columns - 4, 3))];
                break;
            case 1:
                target = screensaverModeOff != null ? screensaverModeOff : screensaverModeWallpapers;
                break;
            case 2:
                target = remoteKeysChoice;
                break;
            case 3:
                target = hiddenAppsChoice;
                break;
        }
        if (target != null) {
            target.requestFocus();
        }
    }

    private void attachBackToTabListener(View view) {
        if (view == null) return;
        view.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                View next = v.focusSearch(View.FOCUS_LEFT);
                if (next == null || isSidebarView(next)) {
                    returnToActiveTab();
                    return true;
                }
            }
            return false;
        });
    }

    private boolean isSidebarView(View v) {
        return v == tabAppearance || v == tabDisplayNight || v == tabRemotes || v == tabAppsSystem;
    }

    private void returnToActiveTab() {
        switch (currentCategory) {
            case 0: tabAppearance.requestFocus(); break;
            case 1: tabDisplayNight.requestFocus(); break;
            case 2: tabRemotes.requestFocus(); break;
            case 3: tabAppsSystem.requestFocus(); break;
        }
    }

    private void setupAppearancePane() {
        for (int i = 0; i < columnChoices.length; i++) {
            final int value = i + 4;
            TextView btn = columnChoices[i];
            attachBackToTabListener(btn);
            btn.setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                setResult(RESULT_OK);
                refresh();
            });
        }

        attachBackToTabListener(animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            setResult(RESULT_OK);
            refresh();
        });

        attachBackToTabListener(advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                showAdvancedEffectsWarningModal();
            }
        });

        attachBackToTabListener(followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit()
                    .remove("columns")
                    .remove("animations")
                    .remove("advanced_effects")
                    .remove(Config.KEY_DOCK_OVERRIDE)
                    .remove(Config.KEY_HIDDEN_OVERRIDE)
                    .remove(Config.KEY_REMOTE_OVERRIDE)
                    .remove(Config.KEY_DIMMER_MODE)
                    .remove(Config.KEY_DIMMER_LEVEL)
                    .remove(ScreensaverPreferences.MODE)
                    .remove(ScreensaverPreferences.TIMEOUT)
                    .remove(ScreensaverPreferences.OVERLAY)
                    .commit();
            Config current = Config.load(this);
            columns = current.columns;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            notifySuiteService();
            setResult(RESULT_OK);
            refresh();
        });
    }

    private void setupDisplayNightPane() {
        attachBackToTabListener(screensaverModeOff);
        screensaverModeOff.setOnClickListener(v -> {
            choices.edit().putInt(ScreensaverPreferences.MODE, ScreensaverPreferences.MODE_OFF).apply();
            refresh();
        });

        attachBackToTabListener(screensaverModeWallpapers);
        screensaverModeWallpapers.setOnClickListener(v -> {
            choices.edit().putInt(ScreensaverPreferences.MODE, ScreensaverPreferences.MODE_WALLPAPERS).apply();
            refresh();
        });

        // Dynamic discovered screensavers
        discoveredScreensavers.clear();
        discoveredScreensavers.addAll(ScreensaverDiscovery.getAvailableScreensavers(this));
        screensaverExternalButtons.clear();

        for (ScreensaverDiscovery.ScreensaverOption option : discoveredScreensavers) {
            TextView btn = new TextView(this);
            btn.setText(option.label);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
            btn.setLayoutParams(lp);
            btn.setFocusable(true);
            btn.setClickable(true);
            btn.setBackgroundResource(R.drawable.settings_choice_bg);
            btn.setTextColor(getColorStateList(R.drawable.pill_text));
            btn.setStateListAnimator(android.animation.AnimatorInflater.loadStateListAnimator(this, R.animator.tvos_focus_control));
            btn.setTextSize(13f);
            int padH = (int) (14 * getResources().getDisplayMetrics().density);
            int padV = (int) (8 * getResources().getDisplayMetrics().density);
            btn.setPadding(padH, padV, padH, padV);

            attachBackToTabListener(btn);
            btn.setOnClickListener(v -> {
                ScreensaverPreferences.setSelectedExternal(this, option.id, option.activityName);
                refresh();
            });

            screensaverModeContainer.addView(btn);
            screensaverExternalButtons.add(btn);
        }

        for (int i = 0; i < screensaverTimeoutChoices.length; i++) {
            final int timeoutVal = screensaverTimeoutValues[i];
            TextView btn = screensaverTimeoutChoices[i];
            attachBackToTabListener(btn);
            btn.setOnClickListener(v -> {
                choices.edit().putInt(ScreensaverPreferences.TIMEOUT, timeoutVal).apply();
                refresh();
            });
        }

        attachBackToTabListener(screensaverOverlayChoice);
        screensaverOverlayChoice.setOnClickListener(v -> {
            boolean next = !ScreensaverPreferences.overlay(this);
            choices.edit().putBoolean(ScreensaverPreferences.OVERLAY, next).apply();
            refresh();
        });

        attachBackToTabListener(screensaverPreviewChoice);
        screensaverPreviewChoice.setOnClickListener(v -> {
            if (ScreensaverPreferences.mode(this) == ScreensaverPreferences.MODE_OFF) {
                Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
            } else {
                ScreensaverDiscovery.launchScreensaver(this);
            }
        });

        attachBackToTabListener(screensaverSystemSettingsChoice);
        screensaverSystemSettingsChoice.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
            } catch (ActivityNotFoundException e) {
                open(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
            }
        });

        TextView[] modeButtons = {dimmerModeOff, dimmerModeScheduled, dimmerModeAlways};
        int[] modeValues = {Config.DIMMER_MODE_OFF, Config.DIMMER_MODE_SCHEDULED, Config.DIMMER_MODE_ALWAYS_ON};

        for (int i = 0; i < modeButtons.length; i++) {
            final int m = modeValues[i];
            TextView btn = modeButtons[i];
            attachBackToTabListener(btn);
            btn.setOnClickListener(v -> {
                dimmerMode = m;
                choices.edit().putInt(Config.KEY_DIMMER_MODE, m).apply();
                notifySuiteService();
                setResult(RESULT_OK);
                refresh();
            });
        }

        TextView[] levelButtons = {dimmerLevel25, dimmerLevel50, dimmerLevel70};
        int[] levelValues = {25, 50, 70};

        for (int i = 0; i < levelButtons.length; i++) {
            final int l = levelValues[i];
            TextView btn = levelButtons[i];
            attachBackToTabListener(btn);
            btn.setOnClickListener(v -> {
                dimmerLevel = l;
                choices.edit().putInt(Config.KEY_DIMMER_LEVEL, l).apply();
                notifySuiteService();
                setResult(RESULT_OK);
                refresh();
            });
        }
    }

    private void setupRemotesPane() {
        attachBackToTabListener(remoteKeysChoice);
        remoteKeysChoice.setOnClickListener(v -> showCaptureKeyModal());
    }

    private String describeKeyCode(int keyCode) {
        switch (keyCode) {
            case 83: return "Settings (#83)";
            case 178: return "Input (#178)";
            case 183: return "Red (#183)";
            case 184: return "Green (#184)";
            case 185: return "Yellow (#185)";
            case 186: return "Blue (#186)";
            case 190: return "YouTube (#190)";
            case 4001: return "Channel (#4001)";
            case 4062: return "Netflix (#4062)";
            case 4168: return "VTVgo (#4168)";
            default:
                String keyStr = KeyEvent.keyCodeToString(keyCode);
                if (keyStr != null && keyStr.startsWith("KEYCODE_")) {
                    String clean = keyStr.substring(8).replace('_', ' ');
                    return clean + " (#" + keyCode + ")";
                }
                return "Key #" + keyCode;
        }
    }

    private void refreshMappedKeysList() {
        mappedKeysList.removeAllViews();
        Config cfg = Config.load(this);
        Map<Integer, String> allKeys = new TreeMap<>(cfg.remoteKeys);

        if (mappedKeysHeader != null) {
            mappedKeysHeader.setText(allKeys.isEmpty()
                    ? getString(R.string.mapped_keys_header)
                    : getString(R.string.mapped_keys_header_count, allKeys.size()));
        }

        if (allKeys.isEmpty()) {
            LinearLayout emptyCard = new LinearLayout(this);
            emptyCard.setOrientation(LinearLayout.VERTICAL);
            emptyCard.setBackgroundResource(R.drawable.settings_choice_bg);
            emptyCard.setPadding(32, 24, 32, 24);

            TextView emptyTitle = new TextView(this);
            emptyTitle.setText(R.string.no_remote_keys);
            emptyTitle.setTextColor(getColor(R.color.tvos_label_primary));
            emptyTitle.setTextSize(14f);
            emptyTitle.setTypeface(null, Typeface.BOLD);

            TextView emptyDesc = new TextView(this);
            emptyDesc.setText(R.string.no_remote_keys_desc);
            emptyDesc.setTextColor(getColor(R.color.tvos_label_secondary));
            emptyDesc.setTextSize(12.5f);
            emptyDesc.setPadding(0, 6, 0, 0);

            emptyCard.addView(emptyTitle);
            emptyCard.addView(emptyDesc);
            mappedKeysList.addView(emptyCard);
            return;
        }

        for (Map.Entry<Integer, String> entry : allKeys.entrySet()) {
            final int code = entry.getKey();
            final String target = entry.getValue();

            View row = getLayoutInflater().inflate(R.layout.item_mapped_key, mappedKeysList, false);
            TextView badge = row.findViewById(R.id.keyBadge);
            TextView targetView = row.findViewById(R.id.keyTarget);

            badge.setText(describeKeyCode(code));
            targetView.setText(describeTarget(target));

            attachBackToTabListener(row);
            row.setOnClickListener(v -> showPickActionForKeyModal(code));
            mappedKeysList.addView(row);
        }
    }

    private void setupAppsSystemPane() {
        attachBackToTabListener(hiddenAppsChoice);
        hiddenAppsChoice.setOnClickListener(v -> showHiddenAppsModal());

        attachBackToTabListener(defaultHomeChoice);
        defaultHomeChoice.setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
    }

    private void setupModalOverlay() {
        modalCancelButton.setOnClickListener(v -> hideModalOverlay());
        modalOverlay.setOnClickListener(v -> hideModalOverlay());
    }

    // =========================================================================
    // Liquid Glass Modals (Replacing Android AlertDialog)
    // =========================================================================

    private void showModalDialog(String title, String subtitle, List<CharSequence> labels, List<Runnable> actions) {
        previousFocusedView = getCurrentFocus();
        modalTitle.setText(title);
        if (subtitle != null && !subtitle.isEmpty()) {
            modalSubtitle.setText(subtitle);
            modalSubtitle.setVisibility(View.VISIBLE);
        } else {
            modalSubtitle.setVisibility(View.GONE);
        }

        modalItemsContainer.removeAllViews();
        for (int i = 0; i < labels.size(); i++) {
            TextView item = new TextView(this, null, 0, R.style.SettingsRow);
            item.setText(labels.get(i));
            final Runnable action = (actions != null && i < actions.size()) ? actions.get(i) : null;
            item.setOnClickListener(v -> {
                hideModalOverlay();
                if (action != null) action.run();
            });
            modalItemsContainer.addView(item);
        }

        modalOverlay.setVisibility(View.VISIBLE);
        if (modalItemsContainer.getChildCount() > 0) {
            modalItemsContainer.getChildAt(0).requestFocus();
        } else {
            modalCancelButton.requestFocus();
        }
    }

    private void hideModalOverlay() {
        keyCaptureListener = null;
        modalOverlay.setVisibility(View.GONE);
        if (previousFocusedView != null) {
            previousFocusedView.requestFocus();
            previousFocusedView = null;
        }
    }

    private void showHiddenAppsModal() {
        Config cfg = Config.load(this);
        final List<String> hidden = new ArrayList<>(cfg.hiddenApps);
        if (hidden.isEmpty()) {
            Toast.makeText(this, R.string.no_hidden_apps, Toast.LENGTH_LONG).show();
            return;
        }
        PackageManager pm = getPackageManager();
        List<CharSequence> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        for (int i = 0; i < hidden.size(); i++) {
            final int index = i;
            String pkg = hidden.get(i);
            String label = pkg;
            try {
                CharSequence cs = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
                if (cs != null && cs.length() > 0) label = cs + " (" + pkg + ")";
            } catch (Exception ignored) {}
            labels.add(label);
            actions.add(() -> {
                hidden.remove(index);
                Config.saveHiddenOverride(LauncherSettingsActivity.this, hidden);
                setResult(RESULT_OK);
                refresh();
            });
        }

        showModalDialog(getString(R.string.unhide_app_title), getString(R.string.unhide_app_desc), labels, actions);
    }

    private void showCaptureKeyModal() {
        previousFocusedView = getCurrentFocus();
        modalTitle.setText(R.string.remote_key_mapper);
        modalSubtitle.setText(R.string.remote_key_press_prompt);
        modalSubtitle.setVisibility(View.VISIBLE);
        modalItemsContainer.removeAllViews();

        keyCaptureListener = keyCode -> {
            hideModalOverlay();
            showPickActionForKeyModal(keyCode);
        };

        modalOverlay.setVisibility(View.VISIBLE);
        modalCancelButton.requestFocus();
    }

    private void showPickActionForKeyModal(final int keyCode) {
        final List<String> targets = new ArrayList<>();
        final List<CharSequence> labels = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();

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
        targets.add("screensaver");
        labels.add(getString(R.string.screensaver_action));

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

        for (int i = 0; i < targets.size(); i++) {
            final String chosen = targets.get(i);
            actions.add(() -> {
                Map<Integer, String> custom = Config.loadRemoteKeysOverride(LauncherSettingsActivity.this);
                if (chosen.isEmpty()) {
                    custom.remove(keyCode);
                } else {
                    custom.put(keyCode, chosen);
                }
                Config.saveRemoteKeysOverride(LauncherSettingsActivity.this, custom);
                notifySuiteService();
                setResult(RESULT_OK);
                refreshMappedKeysList();
            });
        }

        showModalDialog(getString(R.string.remote_key_choose_action, keyCode), null, labels, actions);
    }

    private void showAdvancedEffectsWarningModal() {
        List<CharSequence> labels = Collections.singletonList(getString(R.string.enable));
        List<Runnable> actions = Collections.singletonList(() -> setAdvanced(true));
        showModalDialog(getString(R.string.advanced_effects), getString(R.string.advanced_effects_warning), labels, actions);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCaptureListener != null) {
                int keyCode = event.getKeyCode();
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    hideModalOverlay();
                    return true;
                }
                if (Config.isProtectedRemoteKey(keyCode)) {
                    Toast.makeText(this, R.string.remote_key_protected, Toast.LENGTH_SHORT).show();
                    return true;
                }
                int captured = keyCode > 0 ? keyCode : event.getScanCode();
                if (captured > 0) {
                    KeyCaptureListener listener = keyCaptureListener;
                    keyCaptureListener = null;
                    listener.onKeyCaptured(captured);
                    return true;
                }
            }

            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if (modalOverlay.getVisibility() == View.VISIBLE) {
                    hideModalOverlay();
                    return true;
                }
                View focus = getCurrentFocus();
                if (focus != null && focus != tabAppearance && focus != tabDisplayNight && focus != tabRemotes && focus != tabAppsSystem) {
                    returnToActiveTab();
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private String describeTarget(String target) {
        if (target == null) return "";
        if ("wallpaper_cycle".equals(target)) return getString(R.string.action_wallpaper_cycle);
        if ("wallpaper_picker".equals(target)) return getString(R.string.action_wallpaper_picker);
        if ("quick_settings".equals(target)) return getString(R.string.action_quick_settings);
        if ("recent_apps".equals(target)) return getString(R.string.action_recent_apps);
        if ("inputs".equals(target)) return getString(R.string.action_inputs);
        if ("screensaver".equals(target)) return getString(R.string.screensaver_action);
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
        int currentMode = ScreensaverPreferences.mode(this);
        String selectedComp = ScreensaverPreferences.selectedComponent(this);

        if (screensaverModeOff != null) {
            screensaverModeOff.setSelected(currentMode == ScreensaverPreferences.MODE_OFF);
        }
        if (screensaverModeWallpapers != null) {
            screensaverModeWallpapers.setSelected(currentMode == ScreensaverPreferences.MODE_WALLPAPERS);
        }
        for (int i = 0; i < screensaverExternalButtons.size(); i++) {
            TextView btn = screensaverExternalButtons.get(i);
            ScreensaverDiscovery.ScreensaverOption opt = discoveredScreensavers.get(i);
            btn.setSelected(currentMode == ScreensaverPreferences.MODE_EXTERNAL && opt.id.equals(selectedComp));
        }

        int currentTimeout = ScreensaverPreferences.timeout(this);
        for (int i = 0; i < screensaverTimeoutChoices.length; i++) {
            if (screensaverTimeoutChoices[i] != null) {
                screensaverTimeoutChoices[i].setSelected(currentTimeout == screensaverTimeoutValues[i]);
            }
        }

        boolean screensaverEnabled = (currentMode != ScreensaverPreferences.OFF);
        if (screensaverOverlayChoice != null) {
            boolean overlayActive = ScreensaverPreferences.overlay(this);
            screensaverOverlayChoice.setText(getString(R.string.screensaver_overlay,
                    getString(overlayActive ? R.string.on : R.string.off)));
            screensaverOverlayChoice.setEnabled(screensaverEnabled);
            screensaverOverlayChoice.setAlpha(screensaverEnabled ? 1f : 0.4f);
        }
        if (screensaverPreviewChoice != null) {
            screensaverPreviewChoice.setEnabled(screensaverEnabled);
            screensaverPreviewChoice.setAlpha(screensaverEnabled ? 1f : 0.4f);
        }

        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);

        // Night Dimmer Highlights
        dimmerModeOff.setSelected(dimmerMode == Config.DIMMER_MODE_OFF);
        dimmerModeScheduled.setSelected(dimmerMode == Config.DIMMER_MODE_SCHEDULED);
        dimmerModeAlways.setSelected(dimmerMode == Config.DIMMER_MODE_ALWAYS_ON);

        dimmerLevel25.setSelected(dimmerLevel == 25);
        dimmerLevel50.setSelected(dimmerLevel == 50);
        dimmerLevel70.setSelected(dimmerLevel == 70);

        boolean dimmerActive = (dimmerMode != Config.DIMMER_MODE_OFF);
        dimmerLevel25.setEnabled(dimmerActive);
        dimmerLevel50.setEnabled(dimmerActive);
        dimmerLevel70.setEnabled(dimmerActive);
        dimmerLevel25.setAlpha(dimmerActive ? 1f : 0.4f);
        dimmerLevel50.setAlpha(dimmerActive ? 1f : 0.4f);
        dimmerLevel70.setAlpha(dimmerActive ? 1f : 0.4f);

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
                || choices.contains(Config.KEY_REMOTE_OVERRIDE)
                || choices.contains(Config.KEY_DIMMER_MODE)
                || choices.contains(Config.KEY_DIMMER_LEVEL)
                || choices.contains(ScreensaverPreferences.MODE)
                || choices.contains(ScreensaverPreferences.TIMEOUT)
                || choices.contains(ScreensaverPreferences.OVERLAY);
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
