package vn.familytv.parentalcontrol.ui.screens;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import vn.familytv.parentalcontrol.R;
import vn.familytv.parentalcontrol.ui.TvViews;
import vn.familytv.parentalcontrol.ui.auth.DpadPinPadView;

public class ChangePinView extends LinearLayout {

    public interface Listener {
        void onPinEntered(String newPin);
        void onBack();
    }

    private final DpadPinPadView pinPad;

    public ChangePinView(Context context, Listener listener) {
        super(context);
        TvViews views = new TvViews(context);

        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setBackgroundColor(Color.parseColor("#090D16"));
        setPadding(views.dp(48), views.dp(24), views.dp(48), views.dp(24));

        TextView titleTv = views.title(context.getString(R.string.enter_new_pin_title));
        titleTv.setGravity(Gravity.CENTER_HORIZONTAL);
        titleTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        addView(titleTv);

        views.body(this, context.getString(R.string.enter_new_pin_body));

        pinPad = new DpadPinPadView(context, enteredPin -> {
            if (enteredPin.length() == 6 && listener != null) {
                listener.onPinEntered(enteredPin);
            }
        });
        addView(pinPad);

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

    public DpadPinPadView getPinPad() {
        return pinPad;
    }

    public View getFirstButton() {
        return pinPad.getFirstButton();
    }
}
