package vn.familytv.parentalcontrol;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import vn.familytv.parentalcontrol.ipc.Mode;
import vn.familytv.parentalcontrol.ipc.ModeBroadcastSender;
import vn.familytv.parentalcontrol.ipc.ModeStateManager;
import vn.familytv.parentalcontrol.platform.ForegroundMonitorService;
import vn.familytv.parentalcontrol.platform.PermissionReader;
import vn.familytv.parentalcontrol.platform.PermissionSnapshot;
import vn.familytv.parentalcontrol.screentime.ScreenTimePolicy;
import vn.familytv.parentalcontrol.screentime.ScreenTimeStore;
import vn.familytv.parentalcontrol.security.AuthMode;
import vn.familytv.parentalcontrol.security.GeneratedRecoverySet;
import vn.familytv.parentalcontrol.security.ParentSessionManager;
import vn.familytv.parentalcontrol.security.RateLimiter;
import vn.familytv.parentalcontrol.security.RecoveryCodeManager;
import vn.familytv.parentalcontrol.security.RecoveryCodeRecord;
import vn.familytv.parentalcontrol.security.SecurityStore;
import vn.familytv.parentalcontrol.security.TotpEngine;
import vn.familytv.parentalcontrol.ui.TvViews;
import vn.familytv.parentalcontrol.ui.auth.DpadPinPadView;
import vn.familytv.parentalcontrol.ui.auth.RecoveryCodesView;
import vn.familytv.parentalcontrol.ui.screens.ChangePinView;
import vn.familytv.parentalcontrol.ui.screens.KidGateView;
import vn.familytv.parentalcontrol.ui.screens.ParentConfigView;
import vn.familytv.parentalcontrol.ui.screens.TotpEnrollView;
import vn.familytv.parentalcontrol.ui.screens.UsageTimelineView;

public class MainActivity extends Activity {

    private PermissionReader reader;
    private TvViews views;
    private SecurityStore securityStore;
    private ScreenTimeStore screenTimeStore;
    private final ScreenTimePolicy screenTimePolicy = new ScreenTimePolicy();

    private final RateLimiter rateLimiter = new RateLimiter();
    private final ParentSessionManager sessionManager = new ParentSessionManager();
    private ModeBroadcastSender broadcastSender;

    private Object renderedState;
    private DpadPinPadView activePinPad;
    private boolean isRecoveryMode = false;
    private String verifyErrorMessage;

    private boolean isChangingPin = false;
    private boolean isEnrollingTotp = false;
    private boolean isViewingRecoveryInParentMode = false;
    private boolean isViewingTimelineInParentMode = false;

    private String pendingSecret;
    private GeneratedRecoverySet pendingRecoverySet;
    private ParentConfigView activeParentConfigView;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable kidGateTimeoutRunnable = () -> {
        if (!sessionManager.isParentModeActive()) {
            finishAffinity();
        }
    };

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            updateScreen();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        views = new TvViews(this);
        reader = new PermissionReader(this);
        securityStore = SecurityStore.fromContext(this);
        screenTimeStore = new ScreenTimeStore(getSharedPreferences("familytv_screentime", MODE_PRIVATE));
        broadcastSender = new ModeBroadcastSender(this);

        ModeStateManager modeManager = ModeStateManager.getInstance();
        modeManager.loadState(this);
        if (modeManager.getMode() == Mode.PARENT) {
            long remSec = modeManager.getSessionRemainingSec(System.currentTimeMillis());
            if (remSec > 0) {
                sessionManager.startSession(remSec * 1000L);
            }
        }

        handleDebugIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        ModeStateManager modeManager = ModeStateManager.getInstance();
        modeManager.loadState(this);
        if (modeManager.getMode() == Mode.PARENT) {
            long remSec = modeManager.getSessionRemainingSec(System.currentTimeMillis());
            if (remSec > 0) {
                sessionManager.startSession(remSec * 1000L);
                renderedState = null;
                updateScreen();
            }
        }
        handleDebugIntent(intent);
    }

    private void handleDebugIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("debug_enter_parent_mode", false)) {
            isChangingPin = false;
            isEnrollingTotp = false;
            isViewingRecoveryInParentMode = false;
            isViewingTimelineInParentMode = intent.getBooleanExtra("debug_open_timeline", false);
            sessionManager.startSession();
            renderedState = null;
            updateScreen();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ModeStateManager modeManager = ModeStateManager.getInstance();
        modeManager.loadState(this);
        if (modeManager.getMode() == Mode.PARENT && !sessionManager.isParentModeActive()) {
            long remSec = modeManager.getSessionRemainingSec(System.currentTimeMillis());
            if (remSec > 0) {
                sessionManager.startSession(remSec * 1000L);
                renderedState = null;
            }
        }
        handler.removeCallbacks(refreshRunnable);
        refreshRunnable.run();
        resetKidGateTimeout();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refreshRunnable);
        handler.removeCallbacks(kidGateTimeoutRunnable);
        super.onPause();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (sessionManager.isParentModeActive()) {
                sessionManager.touchSession();
                ModeStateManager.getInstance().extendSession(System.currentTimeMillis());
            }
            resetKidGateTimeout();

            if (activePinPad != null) {
                if (event.getKeyCode() >= KeyEvent.KEYCODE_0 && event.getKeyCode() <= KeyEvent.KEYCODE_9) {
                    char digit = (char) ('0' + (event.getKeyCode() - KeyEvent.KEYCODE_0));
                    activePinPad.appendDigit(digit);
                    return true;
                }
                if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    activePinPad.deleteDigit();
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Deprecated
    @Override
    public void onBackPressed() {
        if (sessionManager.isParentModeActive()) {
            if (isChangingPin || isEnrollingTotp || isViewingRecoveryInParentMode || isViewingTimelineInParentMode) {
                isChangingPin = false;
                isEnrollingTotp = false;
                isViewingRecoveryInParentMode = false;
                isViewingTimelineInParentMode = false;
                renderedState = null;
                updateScreen();
            } else {
                sessionManager.invalidateSession();
                finishAffinity();
            }
        } else {
            finishAffinity();
        }
    }

    private void resetKidGateTimeout() {
        handler.removeCallbacks(kidGateTimeoutRunnable);
        if (!sessionManager.isParentModeActive()) {
            handler.postDelayed(kidGateTimeoutRunnable, 30000);
        }
    }

    private void updateScreen() {
        PermissionSnapshot permissions = reader.read();
        boolean isParent = sessionManager.isParentModeActive();
        AuthMode authMode = securityStore.getAuthMode();
        boolean isCustomPin = securityStore.isCustomPin();
        int weekdayHours = screenTimeStore.getWeekdayHours();
        int weekendHours = screenTimeStore.getWeekendHours();

        String currentState = isParent + "|" + isChangingPin + "|" + isEnrollingTotp + "|"
                + isViewingRecoveryInParentMode + "|" + isViewingTimelineInParentMode + "|"
                + permissions.isReady() + "|" + weekdayHours + "," + weekendHours + "|"
                + authMode + "|" + isCustomPin;

        if (currentState.equals(renderedState)) {
            if (isParent && activeParentConfigView != null) {
                activeParentConfigView.updateLiveTimer(sessionManager.remainingSessionSeconds());
            }
            return;
        }

        renderedState = currentState;
        View focusTarget = null;

        if (!isParent) {
            activeParentConfigView = null;
            KidGateView kidGate = new KidGateView(
                    this,
                    securityStore,
                    rateLimiter,
                    isRecoveryMode,
                    verifyErrorMessage,
                    new KidGateView.Listener() {
                        @Override
                        public void onPinEntered(String pin) {
                            handleKidGatePinSubmitted(pin);
                        }

                        @Override
                        public void onExit() {
                            finishAffinity();
                        }

                        @Override
                        public void onToggleRecovery() {
                            isRecoveryMode = !isRecoveryMode;
                            verifyErrorMessage = null;
                            renderedState = null;
                            updateScreen();
                        }
                    }
            );
            activePinPad = kidGate.getPinPad();
            setContentView(kidGate);
            focusTarget = kidGate.getFirstButton();
        } else if (isChangingPin) {
            activeParentConfigView = null;
            ChangePinView changePinView = new ChangePinView(this, new ChangePinView.Listener() {
                @Override
                public void onPinEntered(String newPin) {
                    securityStore.saveCustomPin(newPin);
                    Toast.makeText(MainActivity.this, getString(R.string.pin_changed_success), Toast.LENGTH_LONG).show();
                    isChangingPin = false;
                    renderedState = null;
                    updateScreen();
                }

                @Override
                public void onBack() {
                    isChangingPin = false;
                    renderedState = null;
                    updateScreen();
                }
            });
            activePinPad = changePinView.getPinPad();
            setContentView(changePinView);
            focusTarget = changePinView.getFirstButton();
        } else if (isEnrollingTotp) {
            activeParentConfigView = null;
            if (pendingSecret == null) pendingSecret = TotpEngine.generateSecret();
            if (pendingRecoverySet == null) pendingRecoverySet = RecoveryCodeManager.generateSet();

            TotpEnrollView enrollView = new TotpEnrollView(this, pendingSecret, new TotpEnrollView.Listener() {
                @Override
                public void onOtpEntered(String otp) {
                    long epochSec = System.currentTimeMillis() / 1000L;
                    if (TotpEngine.verifyCode(pendingSecret, otp, epochSec)) {
                        securityStore.enableTotp(pendingSecret, pendingRecoverySet.getSaltBase64(), pendingRecoverySet.getRecords());
                        Toast.makeText(MainActivity.this, "Đã bật Google Authenticator thành công!", Toast.LENGTH_LONG).show();
                        isEnrollingTotp = false;
                        renderedState = null;
                        updateScreen();
                    } else {
                        Toast.makeText(MainActivity.this, "Mã OTP không chính xác. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                        if (activePinPad != null) activePinPad.clear();
                    }
                }

                @Override
                public void onBack() {
                    isEnrollingTotp = false;
                    renderedState = null;
                    updateScreen();
                }
            });
            activePinPad = enrollView.getPinPad();
            setContentView(enrollView);
            focusTarget = enrollView.getFirstButton();
        } else if (isViewingRecoveryInParentMode) {
            activeParentConfigView = null;
            activePinPad = null;
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(Color.parseColor("#090D16"));
            root.setPadding(views.dp(36), views.dp(24), views.dp(36), views.dp(24));

            root.addView(views.title(getString(R.string.recovery_codes_title)));
            views.body(root, "Danh sách 6 mã phục hồi khẩn cấp cho Google Authenticator của bạn:");

            RecoveryCodesView recView = new RecoveryCodesView(this);
            List<RecoveryCodeRecord> records = securityStore.getRecoveryRecords();
            List<String> codes = new ArrayList<>();
            for (RecoveryCodeRecord r : records) {
                codes.add(r.getHash().substring(0, Math.min(6, r.getHash().length())));
            }
            recView.setCodes(codes);
            root.addView(recView);

            Button closeBtn = views.button(root, "Đóng mã phục hồi", R.id.primary_action, () -> {
                isViewingRecoveryInParentMode = false;
                renderedState = null;
                updateScreen();
            });
            LinearLayout.LayoutParams closeLp = (LinearLayout.LayoutParams) closeBtn.getLayoutParams();
            if (closeLp != null) {
                closeLp.topMargin = views.dp(20);
                closeLp.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                closeBtn.setLayoutParams(closeLp);
            }
            setContentView(root);
            focusTarget = closeBtn;
        } else if (isViewingTimelineInParentMode) {
            activeParentConfigView = null;
            activePinPad = null;
            UsageTimelineView timelineView = new UsageTimelineView(
                    this,
                    screenTimeStore,
                    screenTimePolicy,
                    new UsageTimelineView.Listener() {
                        @Override
                        public void onSyncTimeline(int targetSyncSec) {
                            Intent syncIntent = new Intent(ForegroundMonitorService.ACTION_SET_SCREENTIME);
                            syncIntent.setPackage(getPackageName());
                            syncIntent.putExtra("used_sec", targetSyncSec);
                            sendBroadcast(syncIntent);
                            screenTimeStore.setUsedSec(targetSyncSec);
                            Toast.makeText(MainActivity.this, "Đã đồng bộ thời gian đã xem theo lịch sử!", Toast.LENGTH_SHORT).show();
                            renderedState = null;
                            updateScreen();
                        }

                        @Override
                        public void onBack() {
                            isViewingTimelineInParentMode = false;
                            renderedState = null;
                            updateScreen();
                        }
                    }
            );
            setContentView(timelineView);
            focusTarget = timelineView.getInitialFocusTarget();
        } else {
            activePinPad = null;
            ParentConfigView configView = new ParentConfigView(
                    this,
                    permissions,
                    screenTimeStore,
                    securityStore,
                    sessionManager,
                    screenTimePolicy,
                    null,
                    new ParentConfigView.Listener() {
                        @Override
                        public void onLockAndExit() {
                            sessionManager.invalidateSession();
                            ModeStateManager manager = ModeStateManager.getInstance();
                            manager.expireSession();
                            manager.saveState(MainActivity.this);
                            broadcastSender.sendModeChanged(Mode.KID, 0, false);
                            finishAffinity();
                        }

                        @Override
                        public void onViewTimeline() {
                            isViewingTimelineInParentMode = true;
                            renderedState = null;
                            updateScreen();
                        }

                        @Override
                        public void onWeekdayHoursChanged(int hours) {
                            screenTimeStore.setWeekdayHours(hours);
                            Toast.makeText(MainActivity.this, "Đã lưu Thứ 2 - Thứ 6: " + hours + " giờ/ngày", Toast.LENGTH_SHORT).show();
                            renderedState = null;
                            updateScreen();
                        }

                        @Override
                        public void onWeekendHoursChanged(int hours) {
                            screenTimeStore.setWeekendHours(hours);
                            Toast.makeText(MainActivity.this, "Đã lưu Cuối tuần: " + hours + " giờ/ngày", Toast.LENGTH_SHORT).show();
                            renderedState = null;
                            updateScreen();
                        }

                        @Override
                        public void onFixPermission(boolean needAccessibility) {
                            Intent settingsIntent = new Intent(
                                    needAccessibility ? Settings.ACTION_ACCESSIBILITY_SETTINGS : Settings.ACTION_USAGE_ACCESS_SETTINGS
                            );
                            startActivity(settingsIntent);
                        }

                        @Override
                        public void onChangePin() {
                            isChangingPin = true;
                            renderedState = null;
                            updateScreen();
                        }

                        @Override
                        public void onEnableTotp() {
                            isEnrollingTotp = true;
                            pendingSecret = TotpEngine.generateSecret();
                            pendingRecoverySet = RecoveryCodeManager.generateSet();
                            renderedState = null;
                            updateScreen();
                        }

                        @Override
                        public void onViewRecoveryCodes() {
                            isViewingRecoveryInParentMode = true;
                            renderedState = null;
                            updateScreen();
                        }

                        @Override
                        public void onDisableTotp() {
                            securityStore.disableTotp("000000");
                            Toast.makeText(MainActivity.this, "Đã chuyển về mã PIN mặc định 000000", Toast.LENGTH_SHORT).show();
                            renderedState = null;
                            updateScreen();
                        }
                    }
            );
            activeParentConfigView = configView;
            setContentView(configView);
            focusTarget = configView.getInitialFocusTarget();
        }

        if (focusTarget != null) {
            views.focusLater(focusTarget);
        }
    }

    private void handleKidGatePinSubmitted(String pin) {
        if (rateLimiter.isLocked()) {
            if (activePinPad != null) activePinPad.clear();
            verifyErrorMessage = getString(R.string.parent_verify_locked, rateLimiter.remainingLockSeconds());
            renderedState = null;
            updateScreen();
            return;
        }

        AuthMode authMode = securityStore.getAuthMode();
        long nowMs = System.currentTimeMillis();
        boolean valid;

        if (authMode == AuthMode.PIN) {
            valid = securityStore.verifyPin(pin);
        } else {
            if (isRecoveryMode) {
                String salt = securityStore.getRecoverySalt();
                if (salt == null) salt = "";
                List<RecoveryCodeRecord> records = securityStore.getRecoveryRecords();
                RecoveryCodeManager.VerifyResult result = RecoveryCodeManager.verifyAndConsume(pin, salt, records, nowMs);
                if (result.isSuccess()) {
                    securityStore.saveRecoverySet(salt, result.getUpdatedRecords());
                }
                valid = result.isSuccess();
            } else {
                String secret = securityStore.getTotpSecret();
                valid = (secret != null && TotpEngine.verifyCode(secret, pin, nowMs / 1000L));
            }
        }

        if (valid) {
            rateLimiter.recordSuccess();
            sessionManager.startSession();
            ModeStateManager manager = ModeStateManager.getInstance();
            manager.onParentAuthenticated(nowMs);
            manager.saveState(this);
            broadcastSender.sendModeChanged(
                    Mode.PARENT,
                    (int) sessionManager.remainingSessionSeconds(),
                    false
            );
            Toast.makeText(this, "Đã mở khóa Cài đặt Phụ huynh!", Toast.LENGTH_SHORT).show();
            verifyErrorMessage = null;
            isRecoveryMode = false;
            renderedState = null;
            updateScreen();
        } else {
            long lockedUntil = rateLimiter.recordFailure();
            if (activePinPad != null) activePinPad.clear();
            if (lockedUntil > 0) {
                verifyErrorMessage = getString(R.string.parent_verify_locked, rateLimiter.remainingLockSeconds());
            } else {
                verifyErrorMessage = getString(
                        R.string.parent_verify_error,
                        getString(R.string.parent_verify_attempts_left, rateLimiter.remainingAttemptsBeforeLock())
                );
            }
            renderedState = null;
            updateScreen();
        }
    }
}
