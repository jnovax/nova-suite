package vn.familytv.parentalcontrol.timelock;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.function.Consumer;
import vn.familytv.parentalcontrol.R;
import vn.familytv.parentalcontrol.security.AuthMode;
import vn.familytv.parentalcontrol.security.RateLimiter;
import vn.familytv.parentalcontrol.security.RecoveryCodeManager;
import vn.familytv.parentalcontrol.security.RecoveryCodeRecord;
import vn.familytv.parentalcontrol.security.SecurityStore;
import vn.familytv.parentalcontrol.security.TotpEngine;
import vn.familytv.parentalcontrol.ui.TvViews;
import vn.familytv.parentalcontrol.ui.auth.DpadPinPadView;

public class TimeLockOverlayLayout extends FrameLayout {

    private final TvViews views;
    private final SecurityStore securityStore;
    private final Consumer<Integer> onExtended;
    private final Runnable onOpenParentConfig;
    private final Runnable onGoHome;
    private final RateLimiter rateLimiter = new RateLimiter();

    private final LinearLayout container;
    private DpadPinPadView activePinPad;
    private int todayLimitHours = 2;

    public TimeLockOverlayLayout(
            Context context,
            SecurityStore securityStore,
            Consumer<Integer> onExtended,
            Runnable onOpenParentConfig,
            Runnable onGoHome
    ) {
        super(context);
        this.securityStore = securityStore;
        this.onExtended = onExtended;
        this.onOpenParentConfig = onOpenParentConfig;
        this.onGoHome = onGoHome;
        this.views = new TvViews(context);

        setBackgroundColor(Color.parseColor("#F2090D16")); // Deep dark frosted overlay
        setFocusable(true);
        setFocusableInTouchMode(true);

        container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(views.dp(48), views.dp(36), views.dp(48), views.dp(36));

        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        addView(container, lp);

        showNoticeView();
    }

    public void setTodayLimitHours(int hours) {
        this.todayLimitHours = hours;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
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
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if (onGoHome != null) {
                    onGoHome.run();
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public void showNoticeView() {
        activePinPad = null;
        container.removeAllViews();

        TextView iconTv = new TextView(getContext());
        iconTv.setText("⏰ 🔇");
        iconTv.setTextSize(44f);
        iconTv.setGravity(Gravity.CENTER);
        container.addView(iconTv);

        TextView titleTv = views.title(getContext().getString(R.string.timelock_title));
        titleTv.setGravity(Gravity.CENTER);
        titleTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        titleTv.setPadding(0, views.dp(10), 0, views.dp(8));
        container.addView(titleTv);

        TextView bodyTv = views.text(
                getContext().getString(R.string.timelock_body, todayLimitHours),
                18f,
                R.color.text_secondary
        );
        bodyTv.setGravity(Gravity.CENTER);
        bodyTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        bodyTv.setPadding(0, 0, 0, views.dp(28));
        container.addView(bodyTv);

        LinearLayout actionsRow = new LinearLayout(getContext());
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.CENTER);

        Button unlockBtn = views.button(
                actionsRow,
                getContext().getString(R.string.timelock_parent_unlock),
                R.id.primary_action,
                this::showParentAuthView
        );
        LinearLayout.LayoutParams unlockLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        unlockLp.topMargin = 0;
        unlockBtn.setLayoutParams(unlockLp);
        unlockBtn.setNextFocusRightId(R.id.continue_action);

        Button homeBtn = views.button(
                actionsRow,
                getContext().getString(R.string.timelock_go_home),
                R.id.continue_action,
                () -> {
                    if (onGoHome != null) onGoHome.run();
                }
        );
        LinearLayout.LayoutParams homeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        homeLp.topMargin = 0;
        homeLp.setMarginStart(views.dp(16));
        homeBtn.setLayoutParams(homeLp);
        homeBtn.setNextFocusLeftId(R.id.primary_action);

        container.addView(actionsRow);
        views.focusLater(unlockBtn);
    }

    private void showParentAuthView() {
        container.removeAllViews();

        TextView titleTv = views.title(getContext().getString(R.string.kid_gate_title));
        titleTv.setGravity(Gravity.CENTER);
        titleTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        container.addView(titleTv);

        AuthMode authMode = securityStore.getAuthMode();
        boolean isCustomPin = securityStore.isCustomPin();

        String bodyText = (authMode == AuthMode.TOTP)
                ? "Nhập 6 chữ số mã OTP Google Authenticator (hoặc mã phục hồi) để mở khóa:"
                : "Nhập mã PIN 6 số của phụ huynh để mở khóa gia hạn thời gian:";

        TextView bodyTv = views.text(bodyText, 16f, R.color.text_secondary);
        bodyTv.setGravity(Gravity.CENTER);
        bodyTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        bodyTv.setPadding(0, views.dp(4), 0, views.dp(12));
        container.addView(bodyTv);

        TextView statusTv = views.text("", 16f, R.color.warning);
        statusTv.setGravity(Gravity.CENTER);
        statusTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        statusTv.setPadding(0, 0, 0, views.dp(8));
        statusTv.setVisibility(View.GONE);
        container.addView(statusTv);

        DpadPinPadView pinPad = new DpadPinPadView(getContext(), enteredPin -> handlePinSubmitted(enteredPin, statusTv));
        activePinPad = pinPad;
        container.addView(pinPad);

        if (authMode == AuthMode.PIN && !isCustomPin) {
            TextView hintTv = views.text(getContext().getString(R.string.default_pin_hint), 15f, R.color.accent_cyan);
            hintTv.setGravity(Gravity.CENTER);
            hintTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            hintTv.setPadding(0, views.dp(10), 0, 0);
            container.addView(hintTv);
        }

        Button backBtn = views.button(container, getContext().getString(R.string.back_action), R.id.back_action, this::showNoticeView);
        backBtn.setTextSize(14f);
        backBtn.setMinimumHeight(views.dp(40));
        backBtn.setPadding(views.dp(24), views.dp(8), views.dp(24), views.dp(8));
        LinearLayout.LayoutParams backLp = (LinearLayout.LayoutParams) backBtn.getLayoutParams();
        if (backLp != null) {
            backLp.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            backLp.topMargin = views.dp(14);
            backLp.gravity = Gravity.CENTER_HORIZONTAL;
            backBtn.setLayoutParams(backLp);
        }

        pinPad.setBottomActionButton(backBtn);
        views.focusLater(pinPad.getFirstButton());
    }

    private void handlePinSubmitted(String pin, TextView statusTv) {
        if (rateLimiter.isLocked()) {
            if (activePinPad != null) activePinPad.clear();
            statusTv.setText(getContext().getString(R.string.parent_verify_locked, rateLimiter.remainingLockSeconds()));
            statusTv.setVisibility(View.VISIBLE);
            return;
        }

        AuthMode authMode = securityStore.getAuthMode();
        long nowMs = System.currentTimeMillis();

        boolean valid;
        if (authMode == AuthMode.PIN) {
            valid = securityStore.verifyPin(pin);
        } else {
            long epochSeconds = nowMs / 1000L;
            String secret = securityStore.getTotpSecret();
            if (secret == null) secret = "";
            boolean isValid = TotpEngine.verifyCode(secret, pin, epochSeconds);
            if (!isValid) {
                String salt = securityStore.getRecoverySalt();
                if (salt == null) salt = "";
                List<RecoveryCodeRecord> records = securityStore.getRecoveryRecords();
                RecoveryCodeManager.VerifyResult result = RecoveryCodeManager.verifyAndConsume(pin, salt, records, nowMs);
                if (result.isSuccess()) {
                    securityStore.saveRecoverySet(salt, result.getUpdatedRecords());
                    isValid = true;
                }
            }
            valid = isValid;
        }

        if (valid) {
            rateLimiter.recordSuccess();
            showExtensionSelectorView();
        } else {
            long lockedUntil = rateLimiter.recordFailure();
            if (activePinPad != null) activePinPad.clear();
            statusTv.setVisibility(View.VISIBLE);
            if (lockedUntil > 0) {
                statusTv.setText(getContext().getString(R.string.parent_verify_locked, rateLimiter.remainingLockSeconds()));
                if (activePinPad != null) activePinPad.setKeypadEnabled(false);
            } else {
                statusTv.setText(getContext().getString(
                        R.string.parent_verify_error,
                        getContext().getString(R.string.parent_verify_attempts_left, rateLimiter.remainingAttemptsBeforeLock())
                ));
            }
        }
    }

    public void showExtensionSelectorView() {
        activePinPad = null;
        container.removeAllViews();

        TextView titleTv = views.title(getContext().getString(R.string.extension_title));
        titleTv.setGravity(Gravity.CENTER);
        titleTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        container.addView(titleTv);

        TextView bodyTv = views.text(
                getContext().getString(R.string.extension_body),
                16f,
                R.color.text_secondary
        );
        bodyTv.setGravity(Gravity.CENTER);
        bodyTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        bodyTv.setPadding(0, views.dp(4), 0, views.dp(24));
        container.addView(bodyTv);

        LinearLayout grid = new LinearLayout(getContext());
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setGravity(Gravity.CENTER);
        grid.setLayoutParams(new LinearLayout.LayoutParams(views.dp(640), LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout row1 = new LinearLayout(getContext());
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER);
        row1.setPadding(0, 0, 0, views.dp(14));
        row1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button btn15m = views.button(row1, "+ 15 Phút", R.id.primary_action, () -> {
            if (onExtended != null) onExtended.accept(900);
        });
        LinearLayout.LayoutParams lp15m = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp15m.topMargin = 0;
        btn15m.setLayoutParams(lp15m);
        btn15m.setNextFocusRightId(R.id.continue_action);
        btn15m.setNextFocusDownId(R.id.unlock_action);

        Button btn30m = views.button(row1, "+ 30 Phút", R.id.continue_action, () -> {
            if (onExtended != null) onExtended.accept(1800);
        });
        LinearLayout.LayoutParams lp30m = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp30m.topMargin = 0;
        lp30m.setMarginStart(views.dp(16));
        btn30m.setLayoutParams(lp30m);
        btn30m.setNextFocusLeftId(R.id.primary_action);
        btn30m.setNextFocusDownId(R.id.lock_action);

        grid.addView(row1);

        LinearLayout row2 = new LinearLayout(getContext());
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER);
        row2.setPadding(0, 0, 0, views.dp(20));
        row2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button btn1h = views.button(row2, "+ 1 Giờ", R.id.unlock_action, () -> {
            if (onExtended != null) onExtended.accept(3600);
        });
        LinearLayout.LayoutParams lp1h = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp1h.topMargin = 0;
        btn1h.setLayoutParams(lp1h);
        btn1h.setNextFocusUpId(R.id.primary_action);
        btn1h.setNextFocusRightId(R.id.lock_action);
        btn1h.setNextFocusDownId(R.id.recovery_action);

        Button btnAllDay = views.button(row2, "Xem hết hôm nay", R.id.lock_action, () -> {
            if (onExtended != null) onExtended.accept(86400);
        });
        LinearLayout.LayoutParams lpAllDay = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lpAllDay.topMargin = 0;
        lpAllDay.setMarginStart(views.dp(16));
        btnAllDay.setLayoutParams(lpAllDay);
        btnAllDay.setNextFocusUpId(R.id.continue_action);
        btnAllDay.setNextFocusLeftId(R.id.unlock_action);
        btnAllDay.setNextFocusDownId(R.id.recovery_action);

        grid.addView(row2);
        container.addView(grid);

        Button configBtn = views.button(container, "Vào trang Cấu hình đầy đủ", R.id.recovery_action, () -> {
            if (onOpenParentConfig != null) onOpenParentConfig.run();
        });
        LinearLayout.LayoutParams lpConfig = new LinearLayout.LayoutParams(views.dp(360), LinearLayout.LayoutParams.WRAP_CONTENT);
        lpConfig.topMargin = views.dp(12);
        configBtn.setLayoutParams(lpConfig);
        configBtn.setNextFocusUpId(R.id.unlock_action);

        views.focusLater(btn15m);
    }
}
