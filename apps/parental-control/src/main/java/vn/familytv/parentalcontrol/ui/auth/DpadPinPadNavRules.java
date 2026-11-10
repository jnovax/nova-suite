package vn.familytv.parentalcontrol.ui.auth;

import android.view.View;
import vn.familytv.parentalcontrol.R;

/**
 * Calculates 4-way circular wrap-around navigation rules for the 3x4 numeric keypad.
 */
public final class DpadPinPadNavRules {

    public static final class FocusRule {
        public final int leftId;
        public final int rightId;
        public final int upId;
        public final int downId;

        public FocusRule(int leftId, int rightId, int upId, int downId) {
            this.leftId = leftId;
            this.rightId = rightId;
            this.upId = upId;
            this.downId = downId;
        }
    }

    private DpadPinPadNavRules() {}

    public static FocusRule calculateRule(int btnId, int bottomActionViewId) {
        boolean hasAction = (bottomActionViewId != View.NO_ID && bottomActionViewId != 0);
        int targetAction = hasAction ? bottomActionViewId : View.NO_ID;

        if (btnId == R.id.pin_btn_1) {
            return new FocusRule(
                    R.id.pin_btn_3,
                    R.id.pin_btn_2,
                    targetAction != View.NO_ID ? targetAction : R.id.pin_btn_del,
                    R.id.pin_btn_4
            );
        } else if (btnId == R.id.pin_btn_2) {
            return new FocusRule(
                    R.id.pin_btn_1,
                    R.id.pin_btn_3,
                    targetAction != View.NO_ID ? targetAction : R.id.pin_btn_0,
                    R.id.pin_btn_5
            );
        } else if (btnId == R.id.pin_btn_3) {
            return new FocusRule(
                    R.id.pin_btn_2,
                    R.id.pin_btn_1,
                    targetAction != View.NO_ID ? targetAction : R.id.pin_btn_ok,
                    R.id.pin_btn_6
            );
        } else if (btnId == R.id.pin_btn_4) {
            return new FocusRule(R.id.pin_btn_6, R.id.pin_btn_5, R.id.pin_btn_1, R.id.pin_btn_7);
        } else if (btnId == R.id.pin_btn_5) {
            return new FocusRule(R.id.pin_btn_4, R.id.pin_btn_6, R.id.pin_btn_2, R.id.pin_btn_8);
        } else if (btnId == R.id.pin_btn_6) {
            return new FocusRule(R.id.pin_btn_5, R.id.pin_btn_4, R.id.pin_btn_3, R.id.pin_btn_9);
        } else if (btnId == R.id.pin_btn_7) {
            return new FocusRule(R.id.pin_btn_9, R.id.pin_btn_8, R.id.pin_btn_4, R.id.pin_btn_del);
        } else if (btnId == R.id.pin_btn_8) {
            return new FocusRule(R.id.pin_btn_7, R.id.pin_btn_9, R.id.pin_btn_5, R.id.pin_btn_0);
        } else if (btnId == R.id.pin_btn_9) {
            return new FocusRule(R.id.pin_btn_8, R.id.pin_btn_7, R.id.pin_btn_6, R.id.pin_btn_ok);
        } else if (btnId == R.id.pin_btn_del) {
            return new FocusRule(
                    R.id.pin_btn_ok,
                    R.id.pin_btn_0,
                    R.id.pin_btn_7,
                    targetAction != View.NO_ID ? targetAction : R.id.pin_btn_1
            );
        } else if (btnId == R.id.pin_btn_0) {
            return new FocusRule(
                    R.id.pin_btn_del,
                    R.id.pin_btn_ok,
                    R.id.pin_btn_8,
                    targetAction != View.NO_ID ? targetAction : R.id.pin_btn_2
            );
        } else if (btnId == R.id.pin_btn_ok) {
            return new FocusRule(
                    R.id.pin_btn_0,
                    R.id.pin_btn_del,
                    R.id.pin_btn_9,
                    targetAction != View.NO_ID ? targetAction : R.id.pin_btn_3
            );
        }
        return new FocusRule(View.NO_ID, View.NO_ID, View.NO_ID, View.NO_ID);
    }
}
