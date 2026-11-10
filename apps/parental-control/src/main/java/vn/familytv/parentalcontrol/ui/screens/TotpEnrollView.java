package vn.familytv.parentalcontrol.ui.screens;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import vn.familytv.parentalcontrol.R;
import vn.familytv.parentalcontrol.security.TotpEngine;
import vn.familytv.parentalcontrol.ui.TvViews;
import vn.familytv.parentalcontrol.ui.auth.DpadPinPadView;
import vn.familytv.parentalcontrol.ui.auth.QrCodeRenderer;

public class TotpEnrollView extends LinearLayout {

    public interface Listener {
        void onOtpEntered(String otp);
        void onBack();
    }

    private final DpadPinPadView pinPad;
    private final TextView statusTv;

    public TotpEnrollView(Context context, String secret, Listener listener) {
        super(context);
        TvViews views = new TvViews(context);

        setOrientation(VERTICAL);
        setBackgroundColor(Color.parseColor("#090D16"));
        setPadding(views.dp(36), views.dp(20), views.dp(36), views.dp(20));

        TextView titleTv = views.title("Nâng cấp: Bật Google Authenticator");
        titleTv.setTextSize(22f);
        titleTv.setPadding(0, 0, 0, views.dp(8));
        addView(titleTv);

        TextView descTv = views.text(
                "Quét mã QR bằng ứng dụng Google Authenticator trên điện thoại, sau đó nhập mã OTP 6 số để kích hoạt:",
                14f,
                R.color.text_secondary
        );
        descTv.setPadding(0, 0, 0, views.dp(12));
        addView(descTv);

        LinearLayout twoCol = new LinearLayout(context);
        twoCol.setOrientation(HORIZONTAL);
        twoCol.setGravity(Gravity.CENTER_VERTICAL);

        // Left Col: QR Code
        LinearLayout leftCol = views.column();
        leftCol.setGravity(Gravity.CENTER_HORIZONTAL);
        leftCol.setPadding(views.dp(16), views.dp(12), views.dp(16), views.dp(12));
        leftCol.setBackground(context.getDrawable(R.drawable.status_card));

        String uri = TotpEngine.generateOtpAuthUri(secret, "TCL TV", "FamilyTime");
        Bitmap qrBitmap = QrCodeRenderer.renderQrBitmap(uri, views.dp(180));
        ImageView qrView = new ImageView(context);
        qrView.setImageBitmap(qrBitmap);
        qrView.setBackground(new ColorDrawable(Color.WHITE));
        qrView.setPadding(views.dp(8), views.dp(8), views.dp(8), views.dp(8));
        leftCol.addView(qrView, new LayoutParams(views.dp(180), views.dp(180)));

        TextView secretTv = views.text("Mã khóa: " + secret, 12f, R.color.accent_cyan);
        secretTv.setPadding(0, views.dp(8), 0, 0);
        leftCol.addView(secretTv);

        LayoutParams leftLp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        leftLp.setMarginEnd(views.dp(16));
        twoCol.addView(leftCol, leftLp);

        // Right Col: PIN Pad
        LinearLayout rightCol = views.column();
        rightCol.setGravity(Gravity.CENTER_HORIZONTAL);

        statusTv = views.text("", 14f, R.color.warning);
        statusTv.setVisibility(View.GONE);
        statusTv.setPadding(0, 0, 0, views.dp(6));
        rightCol.addView(statusTv);

        pinPad = new DpadPinPadView(context, enteredOtp -> {
            if (listener != null) listener.onOtpEntered(enteredOtp);
        });
        rightCol.addView(pinPad);

        twoCol.addView(rightCol, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.2f));
        addView(twoCol);

        Button backBtn = views.button(this, context.getString(R.string.back_action), R.id.back_action, () -> {
            if (listener != null) listener.onBack();
        });
        backBtn.setTextSize(14f);
        backBtn.setMinimumHeight(views.dp(40));
        backBtn.setPadding(views.dp(24), views.dp(8), views.dp(24), views.dp(8));
        LayoutParams backLp = (LayoutParams) backBtn.getLayoutParams();
        if (backLp != null) {
            backLp.topMargin = views.dp(14);
            backLp.width = LayoutParams.WRAP_CONTENT;
            backLp.gravity = Gravity.CENTER_HORIZONTAL;
            backBtn.setLayoutParams(backLp);
        }

        pinPad.setBottomActionButton(backBtn);
    }

    public void showError(String message) {
        statusTv.setText(message);
        statusTv.setVisibility(View.VISIBLE);
        pinPad.clear();
    }

    public DpadPinPadView getPinPad() {
        return pinPad;
    }

    public View getFirstButton() {
        return pinPad.getFirstButton();
    }
}
