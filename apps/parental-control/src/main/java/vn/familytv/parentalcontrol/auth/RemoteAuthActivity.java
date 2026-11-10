package vn.familytv.parentalcontrol.auth;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import vn.familytv.parentalcontrol.R;
import vn.familytv.parentalcontrol.ipc.Mode;
import vn.familytv.parentalcontrol.ipc.ModeBroadcastSender;
import vn.familytv.parentalcontrol.ipc.ModeStateManager;
import vn.familytv.parentalcontrol.security.AuthMode;
import vn.familytv.parentalcontrol.security.RateLimiter;
import vn.familytv.parentalcontrol.security.RecoveryCodeManager;
import vn.familytv.parentalcontrol.security.RecoveryCodeRecord;
import vn.familytv.parentalcontrol.security.SecurityStore;
import vn.familytv.parentalcontrol.security.TotpEngine;
import vn.familytv.parentalcontrol.ui.TvViews;
import vn.familytv.parentalcontrol.ui.auth.DpadPinPadView;

public class RemoteAuthActivity extends Activity {
    public static final String ACTION_AUTHENTICATE = "vn.familytv.parentalcontrol.ACTION_AUTHENTICATE";
    public static final RateLimiter rateLimiter = new RateLimiter();

    private SecurityStore securityStore;
    private ModeBroadcastSender broadcastSender;
    private TvViews views;
    private DpadPinPadView pinPad;
    private boolean isRecoveryMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ModeStateManager.getInstance().setAuthInProgress(true);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(220, 10, 14, 24)));
        }

        securityStore = SecurityStore.fromContext(this);
        broadcastSender = new ModeBroadcastSender(this);
        views = new TvViews(this);

        renderAuthUi();
    }

    private void renderAuthUi() {
        AuthMode authMode = securityStore.getAuthMode();
        boolean isCustomPin = securityStore.isCustomPin();
        String secret = securityStore.getTotpSecret();

        if (authMode == AuthMode.TOTP && (secret == null || secret.isEmpty())) {
            Toast.makeText(this, "Chưa thiết lập bảo vệ phụ huynh trên FamilyTime", Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(views.dp(36), views.dp(16), views.dp(36), views.dp(16));

        ImageView logoImg = new ImageView(this);
        logoImg.setImageResource(R.drawable.family_time_app_art);
        logoImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(views.dp(74), views.dp(44));
        logoLp.gravity = Gravity.CENTER_HORIZONTAL;
        logoLp.bottomMargin = views.dp(6);
        root.addView(logoImg, logoLp);

        TextView title = new TextView(this);
        title.setText(getString(R.string.kid_gate_title));
        title.setTextSize(22f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, views.dp(4));
        root.addView(title);

        TextView sub = new TextView(this);
        if (authMode == AuthMode.PIN && !isCustomPin) {
            sub.setText(getString(R.string.default_pin_hint));
            sub.setTextSize(15f);
            sub.setTextColor(Color.parseColor("#F59E0B"));
            sub.setTypeface(Typeface.DEFAULT_BOLD);
        } else if (authMode == AuthMode.PIN) {
            sub.setText("Nhập mã PIN 6 số của phụ huynh để mở khóa:");
            sub.setTextSize(14f);
            sub.setTextColor(Color.LTGRAY);
        } else if (isRecoveryMode) {
            sub.setText(getString(R.string.enter_recovery_body));
            sub.setTextSize(14f);
            sub.setTextColor(Color.parseColor("#F59E0B"));
        } else {
            sub.setText("Nhập mã 6 chữ số từ ứng dụng Google Authenticator:");
            sub.setTextSize(14f);
            sub.setTextColor(Color.LTGRAY);
        }
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, views.dp(8));
        root.addView(sub);

        pinPad = new DpadPinPadView(this, pin -> {
            long nowSec = System.currentTimeMillis() / 1000L;
            if (rateLimiter.isLocked()) {
                long waitSec = rateLimiter.remainingLockSeconds();
                Toast.makeText(RemoteAuthActivity.this, "Thử sai nhiều lần! Vui lòng chờ " + waitSec + " giây", Toast.LENGTH_SHORT).show();
                pinPad.clear();
                return;
            }

            boolean valid;
            if (authMode == AuthMode.PIN) {
                valid = securityStore.verifyPin(pin);
            } else {
                if (isRecoveryMode) {
                    String salt = securityStore.getRecoverySalt();
                    if (salt == null) salt = "";
                    List<RecoveryCodeRecord> records = securityStore.getRecoveryRecords();
                    RecoveryCodeManager.VerifyResult result = RecoveryCodeManager.verifyAndConsume(
                            pin, salt, records, System.currentTimeMillis()
                    );
                    if (result.isSuccess()) {
                        securityStore.saveRecoverySet(salt, result.getUpdatedRecords());
                    }
                    valid = result.isSuccess();
                } else {
                    valid = secret != null && TotpEngine.verifyCode(secret, pin, nowSec);
                }
            }

            if (valid) {
                rateLimiter.recordSuccess();
                ModeStateManager manager = ModeStateManager.getInstance();
                long nowMs = System.currentTimeMillis();
                manager.onParentAuthenticated(nowMs);
                manager.saveState(RemoteAuthActivity.this);
                broadcastSender.sendModeChanged(
                        Mode.PARENT,
                        manager.getSessionRemainingSec(nowMs),
                        manager.isQuizActive()
                );
                Toast.makeText(RemoteAuthActivity.this, "Đã mở khóa Chế độ Phụ huynh!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                if (getIntent().getBooleanExtra(com.appletv.contract.ModeContract.EXTRA_OPEN_SETTINGS, false)) {
                    Intent openApp = new Intent(RemoteAuthActivity.this, vn.familytv.parentalcontrol.MainActivity.class);
                    openApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(openApp);
                }
                finish();
            } else {
                rateLimiter.recordFailure();
                pinPad.clear();
                String msg = (authMode == AuthMode.PIN) ? "Mã PIN không chính xác" : "Mã xác thực không chính xác";
                Toast.makeText(RemoteAuthActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(pinPad);

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_HORIZONTAL);
        actionRow.setPadding(0, views.dp(8), 0, 0);

        Button backBtn = views.button(actionRow, getString(R.string.back_action), R.id.primary_action, () -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        backBtn.setTextSize(13f);
        backBtn.setMinimumHeight(views.dp(34));
        backBtn.setPadding(views.dp(14), views.dp(4), views.dp(14), views.dp(4));
        LinearLayout.LayoutParams backLp = (LinearLayout.LayoutParams) backBtn.getLayoutParams();
        if (backLp != null) {
            backLp.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            backLp.topMargin = 0;
            backBtn.setLayoutParams(backLp);
        }

        if (authMode == AuthMode.TOTP) {
            String recoveryBtnText = isRecoveryMode ? "Dùng mã TOTP" : getString(R.string.use_recovery_code);
            Button recBtn = views.button(actionRow, recoveryBtnText, R.id.recovery_action, () -> {
                isRecoveryMode = !isRecoveryMode;
                renderAuthUi();
            });
            recBtn.setTextSize(13f);
            recBtn.setMinimumHeight(views.dp(34));
            recBtn.setPadding(views.dp(14), views.dp(4), views.dp(14), views.dp(4));
            LinearLayout.LayoutParams recLp = (LinearLayout.LayoutParams) recBtn.getLayoutParams();
            if (recLp != null) {
                recLp.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                recLp.setMarginStart(views.dp(12));
                recLp.topMargin = 0;
                recBtn.setLayoutParams(recLp);
            }
        }

        root.addView(actionRow);
        setContentView(root);

        pinPad.setBottomActionButton(backBtn);
        pinPad.requestFocus();
        if (pinPad.getFirstButton() != null) {
            pinPad.getFirstButton().requestFocus();
        }
    }

    @Override
    protected void onDestroy() {
        ModeStateManager.getInstance().setAuthInProgress(false);
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (pinPad != null) {
                if (event.getKeyCode() >= KeyEvent.KEYCODE_0 && event.getKeyCode() <= KeyEvent.KEYCODE_9) {
                    char digit = (char) ('0' + (event.getKeyCode() - KeyEvent.KEYCODE_0));
                    pinPad.appendDigit(digit);
                    return true;
                }
                if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    pinPad.deleteDigit();
                    return true;
                }
            }
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Deprecated
    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
