package vn.familytv.parentalcontrol.platform;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import java.util.Calendar;
import vn.familytv.parentalcontrol.MainActivity;
import vn.familytv.parentalcontrol.audio.AudioFocusInterrupter;
import vn.familytv.parentalcontrol.ipc.Mode;
import vn.familytv.parentalcontrol.ipc.ModeBroadcastSender;
import vn.familytv.parentalcontrol.ipc.ModeStateManager;
import vn.familytv.parentalcontrol.screentime.ScreenTimePolicy;
import vn.familytv.parentalcontrol.screentime.ScreenTimeStore;
import vn.familytv.parentalcontrol.screentime.ScreenTimeTracker;
import vn.familytv.parentalcontrol.security.SecurityStore;
import vn.familytv.parentalcontrol.timelock.TimeLockOverlayManager;

public class ForegroundMonitorService extends AccessibilityService {
    public static final String TAG = "FAMILY_TV";
    private static final String PREFS_NAME = "familytv_screentime";
    public static final String ACTION_SET_SCREENTIME = "vn.familytv.parentalcontrol.ACTION_SET_SCREENTIME";
    public static final String ACTION_TRIGGER_TIMELOCK = "vn.familytv.parentalcontrol.ACTION_TRIGGER_TIMELOCK";
    public static final String ACTION_TRIGGER_QUIZ = "vn.familytv.parentalcontrol.ACTION_TRIGGER_QUIZ";

    public static boolean connected = false;

    private ScreenTimeStore screenTimeStore;
    private SecurityStore securityStore;
    private ScreenTimePolicy policy;
    private ScreenTimeTracker tracker;
    private AudioFocusInterrupter audioFocusInterrupter;
    private ModeBroadcastSender modeBroadcastSender;
    private TimeLockOverlayManager timeLockOverlayManager;

    private final ModeStateManager modeManager = ModeStateManager.instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isReceiverRegistered = false;
    private boolean isDebugReceiverRegistered = false;
    private String currentForegroundPackage = null;
    private int lastSavedUsedSec = -1;
    private TextView debugCountdown = null;

    private final BroadcastReceiver debugReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();
            if (ACTION_SET_SCREENTIME.equals(action)) {
                if (tracker != null && screenTimeStore != null) {
                    Calendar calendar = Calendar.getInstance();
                    int totalLimit = screenTimeStore.getTotalLimitSec(calendar, policy);
                    int targetUsed;
                    int remaining;
                    if (intent.hasExtra("used_min")) {
                        targetUsed = intent.getIntExtra("used_min", 0) * 60;
                        remaining = Math.max(0, totalLimit - targetUsed);
                    } else if (intent.hasExtra("used_sec")) {
                        targetUsed = Math.max(0, intent.getIntExtra("used_sec", 0));
                        remaining = Math.max(0, totalLimit - targetUsed);
                    } else {
                        remaining = intent.getIntExtra("remaining_sec", 5);
                        targetUsed = Math.max(0, totalLimit - remaining);
                    }
                    Log.i(TAG, "DEBUG_SET_SCREENTIME used=" + targetUsed + ", remaining=" + remaining);
                    tracker.restoreUsedAndLimit(targetUsed, totalLimit);
                    modeManager.setScreenTimeRemainingSec(remaining);
                    lastSavedUsedSec = targetUsed;
                    screenTimeStore.setUsedSec(targetUsed);

                    if (remaining > 0 && timeLockOverlayManager != null && timeLockOverlayManager.isShowing()) {
                        timeLockOverlayManager.dismiss();
                        audioFocusInterrupter.releaseFocus();
                        modeManager.setQuizActive(false);
                    } else if (remaining <= 0 && modeManager.getMode() == Mode.KID) {
                        onLimitReached();
                    }
                    broadcastState();
                }
            } else if (ACTION_TRIGGER_TIMELOCK.equals(action) || ACTION_TRIGGER_QUIZ.equals(action)) {
                boolean showExtension = intent.getBooleanExtra("show_extension", false);
                Log.i(TAG, "DEBUG_TRIGGER_TIMELOCK showExtension=" + showExtension);
                onLimitReached();
                if (showExtension && timeLockOverlayManager != null) {
                    timeLockOverlayManager.showExtensionSelector();
                }
            }
        }
    };

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();
            switch (action) {
                case Intent.ACTION_SCREEN_OFF:
                    Log.i(TAG, "SCREEN_OFF");
                    if (tracker != null) {
                        tracker.setScreenOn(false);
                    }
                    modeManager.onScreenOff();
                    if (screenTimeStore != null && tracker != null) {
                        lastSavedUsedSec = tracker.getUsedSec();
                        screenTimeStore.setUsedSec(lastSavedUsedSec);
                    }
                    if (timeLockOverlayManager != null && timeLockOverlayManager.isShowing()) {
                        timeLockOverlayManager.dismiss();
                        audioFocusInterrupter.releaseFocus();
                        modeManager.setQuizActive(false);
                    }
                    broadcastState();
                    break;
                case Intent.ACTION_SCREEN_ON:
                    Log.i(TAG, "SCREEN_ON");
                    if (tracker != null) {
                        tracker.setScreenOn(true);
                    }
                    broadcastState();
                    break;
                case Intent.ACTION_DREAMING_STARTED:
                    Log.i(TAG, "DREAMING_STARTED");
                    if (tracker != null) {
                        tracker.setDreaming(true);
                    }
                    if (screenTimeStore != null && tracker != null) {
                        lastSavedUsedSec = tracker.getUsedSec();
                        screenTimeStore.setUsedSec(lastSavedUsedSec);
                    }
                    if (timeLockOverlayManager != null && timeLockOverlayManager.isShowing()) {
                        timeLockOverlayManager.dismiss();
                        audioFocusInterrupter.releaseFocus();
                        modeManager.setQuizActive(false);
                    }
                    broadcastState();
                    break;
                case Intent.ACTION_DREAMING_STOPPED:
                    Log.i(TAG, "DREAMING_STOPPED");
                    if (tracker != null) {
                        tracker.setDreaming(false);
                    }
                    broadcastState();
                    break;
            }
        }
    };

    private final ModeStateManager.ModeListener modeListener = mode -> {
        if (tracker != null) {
            tracker.setMode(mode);
        }
        if (mode == Mode.PARENT) {
            if (audioFocusInterrupter != null) {
                audioFocusInterrupter.releaseFocus();
            }
            if (timeLockOverlayManager != null) {
                timeLockOverlayManager.dismiss();
            }
            modeManager.setQuizActive(false);
        }
        broadcastState();
    };

    private final Runnable tickerRunnable = new Runnable() {
        @Override
        public void run() {
            long nowMs = System.currentTimeMillis();
            modeManager.tick(nowMs);

            Calendar calendar = Calendar.getInstance();
            if (screenTimeStore != null && screenTimeStore.checkAndResetDay(calendar, policy)) {
                int newLimit = screenTimeStore.getTotalLimitSec(calendar, policy);
                if (tracker != null) {
                    tracker.restoreUsedAndLimit(0, newLimit);
                }
                modeManager.setScreenTimeRemainingSec(newLimit);
                lastSavedUsedSec = 0;
                if (timeLockOverlayManager != null && timeLockOverlayManager.isShowing()) {
                    timeLockOverlayManager.dismiss();
                    audioFocusInterrupter.releaseFocus();
                    modeManager.setQuizActive(false);
                }
                broadcastState();
            } else if (screenTimeStore != null && tracker != null) {
                int currentTotalLimit = screenTimeStore.getTotalLimitSec(calendar, policy);
                tracker.setTotalLimitSec(currentTotalLimit);
            }

            if (tracker != null) {
                tracker.setMode(modeManager.getMode());
                tracker.tickSecond();
                int remaining = tracker.getRemainingSec();
                int used = tracker.getUsedSec();
                modeManager.setScreenTimeRemainingSec(remaining);
                if (debugCountdown != null) {
                    debugCountdown.setText(tracker.debugCountdownText());
                }

                if (used != lastSavedUsedSec && used % 10 == 0 && screenTimeStore != null) {
                    lastSavedUsedSec = used;
                    screenTimeStore.setUsedSec(used);
                }

                if (remaining <= 0 && modeManager.getMode() == Mode.KID &&
                        (timeLockOverlayManager == null || !timeLockOverlayManager.isShowing()) &&
                        !tracker.isDreaming()) {
                    String pkg = currentForegroundPackage;
                    if (pkg != null && policy.isTrackedPackage(pkg)) {
                        onLimitReached();
                    }
                }
            }

            handler.postDelayed(this, 1000L);
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        screenTimeStore = new ScreenTimeStore(prefs);
        securityStore = SecurityStore.fromContext(this);
        policy = new ScreenTimePolicy();
        audioFocusInterrupter = new AudioFocusInterrupter(this);
        modeBroadcastSender = new ModeBroadcastSender(this);
        timeLockOverlayManager = new TimeLockOverlayManager(this, screenTimeStore, securityStore);

        tracker = new ScreenTimeTracker(policy, this::onLimitReached);

        Calendar calendar = Calendar.getInstance();
        int totalDailyLimit = screenTimeStore.getTotalLimitSec(calendar, policy);
        int savedUsedSec = screenTimeStore.getUsedSec();
        tracker.restoreUsedAndLimit(savedUsedSec, totalDailyLimit);
        int initialRemaining = tracker.getRemainingSec();
        modeManager.setScreenTimeRemainingSec(initialRemaining);
        lastSavedUsedSec = savedUsedSec;

        modeManager.addListener(modeListener);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_DREAMING_STARTED);
        filter.addAction(Intent.ACTION_DREAMING_STOPPED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, filter);
        }
        isReceiverRegistered = true;

        IntentFilter debugFilter = new IntentFilter();
        debugFilter.addAction(ACTION_SET_SCREENTIME);
        debugFilter.addAction(ACTION_TRIGGER_TIMELOCK);
        debugFilter.addAction(ACTION_TRIGGER_QUIZ);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(debugReceiver, debugFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(debugReceiver, debugFilter);
        }
        isDebugReceiverRegistered = true;

        handler.post(tickerRunnable);
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        connected = true;
        Log.i(TAG, "MONITOR_CONNECTED");
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0 && debugCountdown == null && tracker != null) {
            TextView label = new TextView(this);
            label.setTextColor(Color.WHITE);
            label.setBackgroundColor(0xCC000000);
            label.setTextSize(10f);
            label.setGravity(Gravity.RIGHT);
            label.setPadding(4, 2, 4, 2);
            label.setText(tracker.debugCountdownText());

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.RIGHT;
            params.x = 0;
            params.y = 0;
            try {
                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                if (wm != null) {
                    wm.addView(label, params);
                    debugCountdown = label;
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "DEBUG_COUNTDOWN_FAILED", e);
            }
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence rawPkg = event.getPackageName();
            CharSequence rawCls = event.getClassName();
            String pkg = rawPkg != null ? rawPkg.toString() : null;
            String windowClass = rawCls != null ? rawCls.toString() : null;

            if (!ForegroundEventFilter.accept(pkg, windowClass)) {
                Log.d(TAG, "FOREGROUND_IGNORED package=" + pkg + " class=" + windowClass);
                return;
            }
            if (tracker != null && !tracker.updateForegroundPackage(pkg)) {
                Log.d(TAG, "FOREGROUND_SUPPRESSED_GO_HOME package=" + pkg + " class=" + windowClass);
                return;
            }
            Log.d(TAG, "FOREGROUND package=" + pkg);
            currentForegroundPackage = pkg;
            if (pkg != null && policy != null && tracker != null && timeLockOverlayManager != null) {
                if (policy.isTrackedPackage(pkg)) {
                    if (tracker.getRemainingSec() <= 0 && modeManager.getMode() == Mode.KID &&
                            !tracker.isDreaming() && !timeLockOverlayManager.isShowing()) {
                        onLimitReached();
                    }
                } else if (timeLockOverlayManager.isShowing()) {
                    timeLockOverlayManager.dismiss();
                    audioFocusInterrupter.releaseFocus();
                    modeManager.setQuizActive(false);
                    broadcastState();
                }
            }
        }
    }

    private void onLimitReached() {
        Log.i(TAG, "SCREEN_TIME_LIMIT_REACHED");
        showTimeLockOverlay();
    }

    private void showTimeLockOverlay() {
        if (timeLockOverlayManager == null || timeLockOverlayManager.isShowing() || (tracker != null && tracker.isDreaming())) return;
        audioFocusInterrupter.acquireFocus();
        modeManager.setQuizActive(true);
        Calendar calendar = Calendar.getInstance();
        int todayHours = screenTimeStore.getDailyLimitHours(calendar, policy);
        timeLockOverlayManager.showTimeLock(
                todayHours,
                addedSec -> {
                    audioFocusInterrupter.releaseFocus();
                    screenTimeStore.addExtensionSec(addedSec);
                    int newLimit = screenTimeStore.getTotalLimitSec(calendar, policy);
                    tracker.setTotalLimitSec(newLimit);
                    int newRemaining = tracker.getRemainingSec();
                    modeManager.setScreenTimeRemainingSec(newRemaining);
                    modeManager.setQuizActive(false);
                    broadcastState();
                },
                () -> {
                    audioFocusInterrupter.releaseFocus();
                    modeManager.setQuizActive(false);
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                },
                () -> {
                    currentForegroundPackage = null;
                    if (tracker != null) {
                        tracker.onNavigatingHome();
                    }
                    audioFocusInterrupter.releaseFocus();
                    timeLockOverlayManager.dismiss();
                    modeManager.setQuizActive(false);
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                }
        );
        broadcastState();
    }

    private void broadcastState() {
        if (modeBroadcastSender == null) return;
        long now = System.currentTimeMillis();
        boolean isQuiz = timeLockOverlayManager != null && timeLockOverlayManager.isShowing();
        modeBroadcastSender.sendModeChanged(
                modeManager.getMode(),
                modeManager.getSessionRemainingSec(now),
                isQuiz
        );
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "MONITOR_INTERRUPTED");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        connected = false;
        Log.i(TAG, "MONITOR_UNBOUND");
        removeDebugCountdown();
        return super.onUnbind(intent);
    }

    private void removeDebugCountdown() {
        if (debugCountdown != null) {
            try {
                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                if (wm != null) {
                    wm.removeView(debugCountdown);
                }
            } catch (Exception ignored) {}
            debugCountdown = null;
        }
    }

    @Override
    public void onDestroy() {
        connected = false;
        Log.i(TAG, "MONITOR_DESTROYED");

        handler.removeCallbacks(tickerRunnable);
        removeDebugCountdown();

        if (isReceiverRegistered) {
            try { unregisterReceiver(screenReceiver); } catch (Exception ignored) {}
            isReceiverRegistered = false;
        }

        if (isDebugReceiverRegistered) {
            try { unregisterReceiver(debugReceiver); } catch (Exception ignored) {}
            isDebugReceiverRegistered = false;
        }

        modeManager.removeListener(modeListener);

        if (timeLockOverlayManager != null) {
            timeLockOverlayManager.dismiss();
        }

        if (audioFocusInterrupter != null) {
            audioFocusInterrupter.releaseFocus();
        }

        if (screenTimeStore != null && tracker != null) {
            screenTimeStore.setUsedSec(tracker.getUsedSec());
        }

        super.onDestroy();
    }
}
