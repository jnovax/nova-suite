package com.appletv.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The launcher's settings: the values in res/values/config.xml, with an optional overlay read from
 * {@code config.json} in the app's external files directory
 * ({@code /sdcard/Android/data/com.appletv.launcher/files/}). Every key in the JSON is optional and an
 * unknown or badly typed key is ignored, so a theme can change the dock and the look without a rebuild:
 * Dynamic theme overlays can be placed there to customize the dock and layout.
 *
 * Read once in {@link MainActivity#onCreate}; nothing here touches the network.
 */
final class Config {
    private static final String TAG = "AppleTv.Config";
    static final String FILE_NAME = "config.json";
    static final String WALLPAPER_NAME = "wallpaper.jpg";
    static final String SETTINGS_NAME = "launcher_settings";
    static final String KEY_DOCK_OVERRIDE = "dock_apps_override";
    static final String KEY_HIDDEN_OVERRIDE = "hidden_apps_override";
    static final String KEY_REMOTE_OVERRIDE = "remote_keys_override";
    static final String KEY_DIMMER_LEVEL = "night_dimmer_level";
    static final String KEY_DIMMER_MODE = "night_dimmer_mode";
    static final String KEY_SLEEP_END_MS = "sleep_timer_end_ms";

    static final int DIMMER_MODE_OFF = 0;
    static final int DIMMER_MODE_SCHEDULED = 1;
    static final int DIMMER_MODE_ALWAYS_ON = 2;
    static final int DIMMER_DEFAULT_START_HOUR = 21; // 21:00 (9 PM)
    static final int DIMMER_DEFAULT_END_HOUR = 6;    // 06:00 (6 AM)
    static final int DEFAULT_DIMMER_LEVEL = 50;
    private static final String THEME_REVISION_NAME = "theme_revision";
    private static final String THEME_SIGNATURE_KEY = "theme_signature";

    final List<String> dockApps;
    /** Grid apps fill the dock slots dock_apps leaves free (config.xml, fill_dock); on by default, false leaves the slots empty. */
    final boolean fillDock;
    final List<String> hiddenApps;
    final String liveTvPackage;
    final String liveTvAction;
    final String sourcePanelService;
    final String sourcePanelAction;
    final String inputAction;
    final int columns;
    final boolean animations;
    final boolean advancedEffects;
    final boolean ambientGlow;
    final int rowsBelowDock;
    final boolean shamsiDate;
    final boolean moduleCards;
    /** Calmer motion for weak GPUs (config.xml, reduced_motion): stiff spring, short fades, no replay. */
    final boolean reducedMotion;
    final String vpnApp;
    final boolean showHomeSettingsShortcut;
    /** Background colour behind everything, #RRGGBB; black unless a theme says otherwise. */
    final int backgroundColor;
    /** Time-of-day backdrop, 0 to 100 (config.xml, sky_intensity); 0 keeps the plain background. */
    final int skyIntensity;
    /** Remote keycodes that open Settings through HomeKeyService (config.xml, settings_key_keycodes). */
    final int[] settingsKeycodes;
    /** The Settings button opens the quick settings panel instead of the TV's Settings (settings_key_panel). */
    final boolean settingsKeyPanel;
    /** Full-screen picture behind the launcher, or null for the plain colour. */
    final File wallpaper;
    final int dockPaddingTop;
    final Map<Integer, String> remoteKeys;
    /** Whether the overlay file was present and parsed. */
    final boolean overlayApplied;
    /** Festive skin from a theme (festival, festival_until, greeting, accent_color); {@link Festival#NONE} if none. */
    final Festival festival;

    private Config(List<String> dockApps, boolean fillDock, List<String> hiddenApps, String liveTvPackage,
                   String liveTvAction, String sourcePanelService, String sourcePanelAction, String inputAction,
                   int columns, int rowsBelowDock, boolean animations, boolean advancedEffects, boolean ambientGlow,
                   boolean shamsiDate, boolean moduleCards, boolean reducedMotion, String vpnApp,
                   boolean showHomeSettingsShortcut, int backgroundColor, int skyIntensity,
                   int[] settingsKeycodes, boolean settingsKeyPanel, File wallpaper, int dockPaddingTop,
                   Map<Integer, String> remoteKeys, boolean overlayApplied,
                   Festival festival) {
        this.dockApps = Collections.unmodifiableList(dockApps);
        this.fillDock = fillDock;
        this.hiddenApps = Collections.unmodifiableList(hiddenApps);
        this.liveTvPackage = liveTvPackage;
        this.liveTvAction = liveTvAction;
        this.sourcePanelService = sourcePanelService;
        this.sourcePanelAction = sourcePanelAction;
        this.inputAction = inputAction;
        this.columns = columns;
        this.rowsBelowDock = rowsBelowDock;
        this.animations = animations;
        this.advancedEffects = advancedEffects;
        this.ambientGlow = ambientGlow;
        this.shamsiDate = shamsiDate;
        this.moduleCards = moduleCards;
        this.reducedMotion = reducedMotion;
        this.vpnApp = vpnApp;
        this.showHomeSettingsShortcut = showHomeSettingsShortcut;
        this.backgroundColor = backgroundColor;
        this.skyIntensity = skyIntensity;
        this.settingsKeycodes = settingsKeycodes;
        this.settingsKeyPanel = settingsKeyPanel;
        this.wallpaper = wallpaper;
        this.dockPaddingTop = dockPaddingTop;
        this.remoteKeys = Collections.unmodifiableMap(remoteKeys);
        this.overlayApplied = overlayApplied;
        this.festival = festival;
    }

    /** The folder the theme tools push into; null when external storage is unavailable. */
    static File dir(Context ctx) {
        return ctx.getExternalFilesDir(null);
    }

    static Config load(Context ctx) {
        Resources res = ctx.getResources();
        List<String> dock = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.dock_apps)));
        boolean fillDock = res.getBoolean(R.bool.fill_dock);
        List<String> hidden = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.hidden_apps)));
        String liveTv = res.getString(R.string.live_tv_package).trim();
        String liveTvAction = res.getString(R.string.live_tv_action).trim();
        String panelService = res.getString(R.string.source_panel_service).trim();
        String panelAction = res.getString(R.string.source_panel_action).trim();
        String inputAction = res.getString(R.string.input_action).trim();
        int columns = res.getInteger(R.integer.columns);
        int rowsBelow = res.getInteger(R.integer.rows_below_dock);
        boolean animations = res.getBoolean(R.bool.animations);
        boolean advancedEffects = res.getBoolean(R.bool.advanced_effects);
        boolean ambientGlow = res.getBoolean(R.bool.ambient_glow);
        boolean shamsi = res.getBoolean(R.bool.shamsi_date);
        boolean moduleCards = res.getBoolean(R.bool.module_cards);
        boolean reducedMotion = res.getBoolean(R.bool.reduced_motion);
        String vpn = res.getString(R.string.vpn_app).trim();
        boolean homeSettings = res.getBoolean(R.bool.show_home_settings_shortcut);
        int background = 0xFF000000;
        int skyIntensity = res.getInteger(R.integer.sky_intensity);
        int[] settingsKeycodes = res.getIntArray(R.array.settings_key_keycodes);
        boolean settingsKeyPanel = res.getBoolean(R.bool.settings_key_panel);
        int dockPaddingTop = -1;
        boolean applied = false;

        Map<Integer, String> remoteKeys = new HashMap<>();
        // Default remap for TCL remote app buttons
        remoteKeys.put(190, "io.gh.reisxd.tizentube.cobalt"); // YouTube -> TizenTube
        remoteKeys.put(4168, "vn.mytvnet.b2cott");             // VTVgo -> MyTV
        remoteKeys.put(4062, "org.courville.nova");            // Netflix -> Nova Video Player
        remoteKeys.put(178, "inputs");                         // Change Input -> HDMI / Inputs Menu
        remoteKeys.put(4001, "wallpaper_cycle");               // Change Channel -> Cycle Wallpaper
        remoteKeys.put(83, "quick_settings");                  // Settings key -> Quick Settings Panel

        File dir = dir(ctx);
        File file = dir != null ? new File(dir, FILE_NAME) : null;
        JSONObject json = read(file);
        if (json != null) {
            applied = true;
            dock = stringList(json, "dock_apps", dock);
            fillDock = bool(json, "fill_dock", fillDock);
            hidden = stringList(json, "hidden_apps", hidden);
            liveTv = string(json, "live_tv_package", liveTv);
            liveTvAction = string(json, "live_tv_action", liveTvAction);
            panelService = string(json, "source_panel_service", panelService);
            panelAction = string(json, "source_panel_action", panelAction);
            inputAction = string(json, "input_action", inputAction);
            columns = integer(json, "columns", columns, 1, 8);
            rowsBelow = integer(json, "rows_below_dock", rowsBelow, 0, 3);
            dockPaddingTop = integer(json, "dock_padding_top", dockPaddingTop, -1, 2000);
            animations = bool(json, "animations", animations);
            advancedEffects = bool(json, "advanced_effects", advancedEffects);
            ambientGlow = bool(json, "ambient_glow", ambientGlow);
            shamsi = bool(json, "shamsi_date", shamsi);
            moduleCards = bool(json, "module_cards", moduleCards);
            reducedMotion = bool(json, "reduced_motion", reducedMotion);
            vpn = string(json, "vpn_app", vpn);
            homeSettings = bool(json, "show_home_settings_shortcut", homeSettings);
            background = color(json, "background_color", background);
            skyIntensity = integer(json, "sky_intensity", skyIntensity, 0, 100);
            settingsKeycodes = intArray(json, "settings_key_keycodes", settingsKeycodes);
            settingsKeyPanel = bool(json, "settings_key_panel", settingsKeyPanel);
            parseRemoteKeys(json, remoteKeys);
        }
        // A theme push or reset invalidates choices made on the TV. On first use, remember the
        // current theme without clearing choices from an older launcher version.
        SharedPreferences choices = ctx.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
        syncThemeChoices(choices, dir, file);
        int chosenColumns = choices.getInt("columns", columns);
        if (chosenColumns >= 4 && chosenColumns <= 7) columns = chosenColumns;
        animations = choices.getBoolean("animations", animations);
        advancedEffects = choices.getBoolean("advanced_effects", advancedEffects);
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
        File wallpaper = dir != null ? new File(dir, WALLPAPER_NAME) : null;
        if (wallpaper != null && !(wallpaper.isFile() && wallpaper.length() > 0)) wallpaper = null;
        // A theme can also switch the wallpaper off while the file is still there.
        if (json != null && json.has("wallpaper") && !bool(json, "wallpaper", true)) wallpaper = null;
        // A festive skin comes only from a theme; config.xml has no festival.
        Festival festival = json == null ? Festival.NONE : Festival.parse(string(json, "festival", ""),
                string(json, "festival_until", ""), string(json, "greeting", ""), string(json, "accent_color", ""));

        return new Config(dock, fillDock, hidden, liveTv, liveTvAction, panelService, panelAction, inputAction,
                Math.max(1, columns), Math.max(0, rowsBelow), animations, advancedEffects, ambientGlow,
                shamsi, moduleCards, reducedMotion, vpn, homeSettings, background,
                Math.max(0, Math.min(100, skyIntensity)), settingsKeycodes, settingsKeyPanel, wallpaper, dockPaddingTop,
                remoteKeys, applied, festival);
    }

    private static void syncThemeChoices(SharedPreferences choices, File dir, File config) {
        File revision = dir == null ? null : new File(dir, THEME_REVISION_NAME);
        String token = revisionToken(revision);
        String signature = fileStamp(config) + ":" + token;
        String seen = choices.getString(THEME_SIGNATURE_KEY, null);
        if (seen == null) {
            // Existing preferences survive an app upgrade, unless the first launch follows an
            // explicit theme push or reset by the updated tools.
            SharedPreferences.Editor editor = choices.edit();
            if (!"absent".equals(token)) editor.remove("columns").remove("animations")
                    .remove("advanced_effects");
            editor.putString(THEME_SIGNATURE_KEY, signature).commit();
        } else if (!seen.equals(signature)) {
            choices.edit().remove("columns").remove("animations").remove("advanced_effects")
                    .putString(THEME_SIGNATURE_KEY, signature).commit();
        }
    }

    private static String fileStamp(File file) {
        return file != null && file.isFile() ? file.length() + "@" + file.lastModified() : "absent";
    }

    private static String revisionToken(File file) {
        if (file == null || !file.isFile() || file.length() > 64) return "absent";
        try (InputStream in = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int n = in.read(bytes);
            return n > 0 ? new String(bytes, 0, n, StandardCharsets.US_ASCII) : "absent";
        } catch (Exception ignored) {
            return "absent";
        }
    }

    /** Reads and parses the overlay; any problem means "no overlay", never a crash on the home screen. */
    private static JSONObject read(File file) {
        if (file == null || !file.isFile()) return null;
        if (file.length() > 64 * 1024) {
            Log.w(TAG, "ignoring " + file + ": larger than 64 KB");
            return null;
        }
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int n = 0, r;
            while (n < buf.length && (r = in.read(buf, n, buf.length - n)) > 0) n += r;
            String text = new String(buf, 0, n, StandardCharsets.UTF_8);
            if (text.startsWith("\uFEFF")) text = text.substring(1);   // Windows editors add a BOM
            return new JSONObject(text);
        } catch (Exception e) {
            Log.w(TAG, "ignoring " + file + ": " + e.getMessage());
            return null;
        }
    }

    private static String string(JSONObject o, String key, String fallback) {
        Object v = o.opt(key);
        return v instanceof String ? ((String) v).trim() : fallback;
    }

    private static boolean bool(JSONObject o, String key, boolean fallback) {
        Object v = o.opt(key);
        return v instanceof Boolean ? (Boolean) v : fallback;
    }

    private static int integer(JSONObject o, String key, int fallback, int min, int max) {
        Object v = o.opt(key);
        if (!(v instanceof Number)) return fallback;
        int i = ((Number) v).intValue();
        return i < min || i > max ? fallback : i;
    }

    private static int color(JSONObject o, String key, int fallback) {
        Object v = o.opt(key);
        if (!(v instanceof String)) return fallback;
        try {
            int c = Color.parseColor(((String) v).trim());
            return c | 0xFF000000;   // always opaque: the window behind is black anyway
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /** Positive keycodes only (vendor remotes use codes in the thousands); a list with anything else in it is ignored as a whole. */
    private static int[] intArray(JSONObject o, String key, int[] fallback) {
        Object v = o.opt(key);
        if (!(v instanceof JSONArray)) return fallback;
        JSONArray a = (JSONArray) v;
        int[] out = new int[a.length()];
        for (int i = 0; i < a.length(); i++) {
            Object item = a.opt(i);
            if (!(item instanceof Number)) return fallback;
            out[i] = ((Number) item).intValue();
            if (out[i] < 1) return fallback;
        }
        return out;
    }

    private static List<String> stringList(JSONObject o, String key, List<String> fallback) {
        Object v = o.opt(key);
        if (!(v instanceof JSONArray)) return fallback;
        JSONArray a = (JSONArray) v;
        List<String> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            Object item = a.opt(i);
            if (item instanceof String && !((String) item).trim().isEmpty()) out.add(((String) item).trim());
        }
        return out;
    }

    static void parseRemoteKeys(JSONObject json, Map<Integer, String> out) {
        if (json == null) return;
        JSONObject rk = json.optJSONObject("remote_keys");
        if (rk == null) return;
        java.util.Iterator<String> it = rk.keys();
        while (it.hasNext()) {
            String k = it.next();
            try {
                int code = Integer.parseInt(k);
                String val = rk.optString(k, "").trim();
                if (!val.isEmpty()) {
                    out.put(code, val);
                }
            } catch (NumberFormatException ignored) {}
        }
    }

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

    /**
     * Determines whether Night Dimmer should be active given the current mode, schedule, and current hour.
     * @param mode 0=Off, 1=Scheduled (overnight startHour to endHour), 2=Always On
     * @param hourOfDay 0-23
     * @param startHour e.g. 21 (9 PM)
     * @param endHour e.g. 6 (6 AM)
     * @return true if dimmer overlay should be active
     */
    static boolean isDimmerTime(int mode, int hourOfDay, int startHour, int endHour) {
        if (mode == DIMMER_MODE_ALWAYS_ON) return true;
        if (mode != DIMMER_MODE_SCHEDULED) return false;
        if (startHour > endHour) {
            // Overnight interval (e.g. 21:00 to 06:00)
            return hourOfDay >= startHour || hourOfDay < endHour;
        } else if (startHour < endHour) {
            // Daytime interval
            return hourOfDay >= startHour && hourOfDay < endHour;
        } else {
            return false;
        }
    }

    /**
     * Resolves the active dimmer opacity level (0, 25, 50, 70) based on user settings and current time.
     */
    static int getActiveDimmerAlpha(Context ctx) {
        android.content.SharedPreferences prefs = ctx.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
        int mode = prefs.getInt(KEY_DIMMER_MODE, DIMMER_MODE_SCHEDULED);
        int level = prefs.getInt(KEY_DIMMER_LEVEL, DEFAULT_DIMMER_LEVEL);
        if (level <= 0) level = DEFAULT_DIMMER_LEVEL;
        int currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (isDimmerTime(mode, currentHour, DIMMER_DEFAULT_START_HOUR, DIMMER_DEFAULT_END_HOUR)) {
            return level;
        }
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
}
