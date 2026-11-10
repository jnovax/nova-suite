package vn.familytv.parentalcontrol.ipc;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModeStateManager {
    public interface ModeListener {
        void onModeChanged(Mode mode);
    }

    private static final String PREFS_NAME = "family_time_mode_state";
    private static final String KEY_CURRENT_MODE = "saved_current_mode";
    private static final String KEY_EXPIRY_MS = "saved_expiry_ms";

    private final int sessionDurationSec;
    private Mode currentMode = Mode.KID;
    private long expiryTimeMs = 0L;
    private boolean isQuizActive = false;
    private int screenTimeRemainingSec = 900;
    private final CopyOnWriteArrayList<ModeListener> listeners = new CopyOnWriteArrayList<>();

    public volatile boolean isAuthInProgress = false;

    public boolean isAuthInProgress() {
        return isAuthInProgress;
    }

    public void setAuthInProgress(boolean authInProgress) {
        this.isAuthInProgress = authInProgress;
    }

    public ModeStateManager() {
        this(1800);
    }

    public ModeStateManager(int sessionDurationSec) {
        this.sessionDurationSec = sessionDurationSec;
    }

    public synchronized void loadState(Context context) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String saved = prefs.getString(KEY_CURRENT_MODE, null);
            long savedExpiry = prefs.getLong(KEY_EXPIRY_MS, 0L);
            long now = System.currentTimeMillis();
            if (Mode.PARENT.name().equals(saved) && now < savedExpiry) {
                currentMode = Mode.PARENT;
                expiryTimeMs = savedExpiry;
            }
        } catch (Exception ignored) {}
    }

    public synchronized void saveState(Context context) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_CURRENT_MODE, currentMode.name())
                    .putLong(KEY_EXPIRY_MS, expiryTimeMs)
                    .apply();
        } catch (Exception ignored) {}
    }

    public synchronized Mode getMode() {
        return currentMode;
    }

    public synchronized void onParentAuthenticated(long nowMs) {
        currentMode = Mode.PARENT;
        expiryTimeMs = nowMs + (sessionDurationSec * 1000L);
        isQuizActive = false;
        notifyListeners(Mode.PARENT);
    }

    public synchronized void extendSession(long nowMs) {
        if (currentMode == Mode.PARENT) {
            expiryTimeMs = nowMs + (sessionDurationSec * 1000L);
        }
    }

    public synchronized void tick(long nowMs) {
        if (currentMode == Mode.PARENT && nowMs >= expiryTimeMs) {
            expireSession();
        }
    }

    public synchronized void expireSession() {
        if (currentMode == Mode.PARENT) {
            currentMode = Mode.KID;
            expiryTimeMs = 0L;
            notifyListeners(Mode.KID);
        }
    }

    public synchronized void onScreenOff() {
        expireSession();
    }

    public synchronized void setQuizActive(boolean active) {
        this.isQuizActive = active;
    }

    public synchronized boolean isQuizActive() {
        return isQuizActive;
    }

    public synchronized int getSessionRemainingSec(long nowMs) {
        if (currentMode != Mode.PARENT) return 0;
        long diff = expiryTimeMs - nowMs;
        return diff > 0 ? (int) ((diff + 999) / 1000) : 0;
    }

    public synchronized void setScreenTimeRemainingSec(int sec) {
        this.screenTimeRemainingSec = sec;
    }

    public synchronized int getScreenTimeRemainingSec() {
        return screenTimeRemainingSec;
    }

    public void addListener(ModeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ModeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public synchronized void resetForTesting() {
        currentMode = Mode.KID;
        expiryTimeMs = 0L;
        isQuizActive = false;
        isAuthInProgress = false;
        screenTimeRemainingSec = 900;
        listeners.clear();
    }

    private void notifyListeners(Mode mode) {
        for (ModeListener listener : listeners) {
            listener.onModeChanged(mode);
        }
    }

    public static final ModeStateManager instance = new ModeStateManager();

    public static ModeStateManager getInstance() {
        return instance;
    }
}
