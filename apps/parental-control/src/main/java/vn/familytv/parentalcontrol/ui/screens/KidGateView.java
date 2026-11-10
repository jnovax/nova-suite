package vn.familytv.parentalcontrol.ui.screens;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import vn.familytv.parentalcontrol.R;
import vn.familytv.parentalcontrol.security.AuthMode;
import vn.familytv.parentalcontrol.security.RateLimiter;
import vn.familytv.parentalcontrol.security.SecurityStore;
import vn.familytv.parentalcontrol.ui.TvViews;
import vn.familytv.parentalcontrol.ui.auth.DpadPinPadView;

public class KidGateView extends LinearLayout {

    public interface Listener {
        void onPinEntered(String pin);
        void onExit();
        void onToggleRecovery();
    }

    private final DpadPinPadView pinPad;
    private final TextView statusTv;

    public KidGateView(
            Context context,
            SecurityStore securityStore,
            RateLimiter rateLimiter,
            boolean isRecoveryMode,
            String verifyErrorMessage,
            Listener listener
    ) {
        super(context);
        TvViews views = new TvViews(context);

        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setBackgroundColor(Color.parseColor("#090D16"));
        setPadding(views.dp(48), views.dp(20), views.dp(48), views.dp(20));

        ImageView logoImg = new ImageView(context);
        logoImg.setImageResource(R.drawable.family_time_app_art);
        logoImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LayoutParams logoLp = new LayoutParams(views.dp(84), views.dp(50));
        logoLp.gravity = Gravity.CENTER_HORIZONTAL;
        logoLp.bottomMargin = views.dp(8);
        addView(logoImg, logoLp);

        AuthMode authMode = securityStore.getAuthMode();
        boolean isCustomPin = securityStore.isCustomPin();

        String titleText;
        if (authMode == AuthMode.TOTP) {
            titleText = isRecoveryMode
                    ? context.getString(R.string.enter_recovery_title)
                    : "Xác thực Google Authenticator";
        } else {
            titleText = context.getString(R.string.kid_gate_title);
        }

        TextView titleTv = views.title(titleText);
        titleTv.setTextSize(20f);
        titleTv.setGravity(Gravity.CENTER_HORIZONTAL);
        titleTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        titleTv.setPadding(0, 0, 0, views.dp(2));
        addView(titleTv);

        if (authMode == AuthMode.PIN && !isCustomPin) {
            TextView hintTv = views.text(context.getString(R.string.default_pin_hint), 15f, R.color.accent_amber);
            hintTv.setGravity(Gravity.CENTER_HORIZONTAL);
            hintTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            hintTv.setTypeface(Typeface.DEFAULT_BOLD);
            hintTv.setPadding(0, 0, 0, views.dp(6));
            addView(hintTv);
        } else {
            String bodyText;
            if (authMode == AuthMode.TOTP) {
                bodyText = isRecoveryMode
                        ? context.getString(R.string.enter_recovery_body)
                        : "Nhập mã 6 chữ số từ ứng dụng Google Authenticator để mở khóa:";
            } else {
                bodyText = "Nhập mã PIN 6 số của phụ huynh để mở phần Cài đặt:";
            }
            TextView bodyTv = views.text(bodyText, 13f, R.color.text_secondary);
            bodyTv.setGravity(Gravity.CENTER_HORIZONTAL);
            bodyTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            bodyTv.setPadding(0, 0, 0, views.dp(6));
            addView(bodyTv);
        }

        statusTv = views.text("", 14f, R.color.warning);
        statusTv.setGravity(Gravity.CENTER_HORIZONTAL);
        statusTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        statusTv.setPadding(0, 0, 0, views.dp(6));
        statusTv.setVisibility(View.GONE);
        addView(statusTv);

        if (rateLimiter.isLocked()) {
            long sec = rateLimiter.remainingLockSeconds();
            statusTv.setText(context.getString(R.string.parent_verify_locked, sec));
            statusTv.setVisibility(View.VISIBLE);
        } else if (verifyErrorMessage != null && !verifyErrorMessage.isEmpty()) {
            statusTv.setText(verifyErrorMessage);
            statusTv.setVisibility(View.VISIBLE);
        }

        pinPad = new DpadPinPadView(context, enteredPin -> {
            if (listener != null) listener.onPinEntered(enteredPin);
        });
        pinPad.setKeypadEnabled(!rateLimiter.isLocked());
        addView(pinPad);

        LinearLayout actionRow = new LinearLayout(context);
        actionRow.setOrientation(HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_HORIZONTAL);
        actionRow.setPadding(0, views.dp(8), 0, 0);

        Button exitBtn = views.button(actionRow, context.getString(R.string.kid_gate_exit), R.id.primary_action, () -> {
            if (listener != null) listener.onExit();
        });
        exitBtn.setTextSize(13f);
        exitBtn.setMinimumHeight(views.dp(34));
        exitBtn.setPadding(views.dp(14), views.dp(4), views.dp(14), views.dp(4));
        LayoutParams exitLp = (LayoutParams) exitBtn.getLayoutParams();
        if (exitLp != null) {
            exitLp.width = LayoutParams.WRAP_CONTENT;
            exitLp.topMargin = 0;
            exitBtn.setLayoutParams(exitLp);
        }

        if (authMode == AuthMode.TOTP) {
            String switchModeLabel = isRecoveryMode ? "Dùng mã OTP Authenticator" : context.getString(R.string.use_recovery_code);
            Button switchBtn = views.button(actionRow, switchModeLabel, R.id.recovery_action, () -> {
                if (listener != null) listener.onToggleRecovery();
            });
            switchBtn.setTextSize(13f);
            switchBtn.setMinimumHeight(views.dp(34));
            switchBtn.setPadding(views.dp(14), views.dp(4), views.dp(14), views.dp(4));
            LayoutParams switchLp = (LayoutParams) switchBtn.getLayoutParams();
            if (switchLp != null) {
                switchLp.width = LayoutParams.WRAP_CONTENT;
                switchLp.topMargin = 0;
                switchLp.setMarginStart(views.dp(12));
                switchBtn.setLayoutParams(switchLp);
            }
        }

        addView(actionRow);
        pinPad.setBottomActionButton(exitBtn);
    }

    public DpadPinPadView getPinPad() {
        return pinPad;
    }

    public View getFirstButton() {
        return pinPad.getFirstButton();
    }
}
