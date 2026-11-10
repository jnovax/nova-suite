package vn.familytv.parentalcontrol.ui.auth;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import vn.familytv.parentalcontrol.R;

public class DpadPinPadView extends LinearLayout {

    @FunctionalInterface
    public interface OnPinCompleteListener {
        void onPinComplete(String pin);
    }

    private final float density;
    private final OnPinCompleteListener onPinComplete;
    private final StringBuilder pinBuilder = new StringBuilder();
    private final List<TextView> slotViews = new ArrayList<>();
    private final GridLayout grid;
    private int bottomActionViewId = View.NO_ID;

    public DpadPinPadView(Context context, OnPinCompleteListener onPinComplete) {
        super(context);
        this.density = context.getResources().getDisplayMetrics().density;
        this.onPinComplete = onPinComplete;

        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout slotsRow = new LinearLayout(context);
        slotsRow.setOrientation(HORIZONTAL);
        slotsRow.setGravity(Gravity.CENTER);
        slotsRow.setPadding(0, 0, 0, dp(10));

        for (int i = 0; i < 6; i++) {
            TextView slot = new TextView(context);
            slot.setText("");
            slot.setTextSize(20f);
            slot.setTextColor(Color.WHITE);
            slot.setGravity(Gravity.CENTER);
            slot.setBackground(createSlotBackground(false));
            LayoutParams lp = new LayoutParams(dp(40), dp(44));
            lp.setMargins(dp(3), 0, dp(3), 0);
            slot.setLayoutParams(lp);

            slotViews.add(slot);
            slotsRow.addView(slot);
        }

        LayoutParams slotsLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        slotsLp.gravity = Gravity.CENTER_HORIZONTAL;
        addView(slotsRow, slotsLp);

        grid = new GridLayout(context);
        grid.setColumnCount(3);
        grid.setRowCount(4);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);

        String[][] buttonLabels = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"⌫", "0", "✔"}
        };
        int[][] buttonIds = {
                {R.id.pin_btn_1, R.id.pin_btn_2, R.id.pin_btn_3},
                {R.id.pin_btn_4, R.id.pin_btn_5, R.id.pin_btn_6},
                {R.id.pin_btn_7, R.id.pin_btn_8, R.id.pin_btn_9},
                {R.id.pin_btn_del, R.id.pin_btn_0, R.id.pin_btn_ok}
        };

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                String btnText = buttonLabels[r][c];
                int idRes = buttonIds[r][c];

                Button btn = new Button(context);
                btn.setId(idRes);
                btn.setText(btnText);
                btn.setTextSize(18f);
                btn.setAllCaps(false);
                btn.setGravity(Gravity.CENTER);
                btn.setBackground(context.getDrawable(R.drawable.tv_button));
                btn.setTextColor(new ColorStateList(
                        new int[][]{
                                new int[]{-android.R.attr.state_enabled},
                                new int[]{android.R.attr.state_focused},
                                new int[]{}
                        },
                        new int[]{
                                context.getColor(R.color.text_disabled),
                                context.getColor(R.color.focus_text),
                                context.getColor(R.color.text_primary)
                        }
                ));

                btn.setOnClickListener(v -> {
                    if ("⌫".equals(btnText)) {
                        deleteDigit();
                    } else if ("✔".equals(btnText)) {
                        if (pinBuilder.length() == 6 && this.onPinComplete != null) {
                            this.onPinComplete.onPinComplete(pinBuilder.toString());
                        }
                    } else if (btnText.length() == 1) {
                        appendDigit(btnText.charAt(0));
                    }
                });

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = dp(88);
                lp.height = dp(44);
                lp.setMargins(dp(3), dp(3), dp(3), dp(3));
                btn.setLayoutParams(lp);

                grid.addView(btn);
            }
        }

        LayoutParams gridLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        gridLp.gravity = Gravity.CENTER_HORIZONTAL;
        addView(grid, gridLp);

        applyNavigationRules();
    }

    private int dp(int v) {
        return (int) (v * density + 0.5f);
    }

    private GradientDrawable createSlotBackground(boolean filled) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(dp(8));
        if (filled) {
            gd.setColor(Color.parseColor("#1D4ED8"));
            gd.setStroke(dp(2), Color.parseColor("#60A5FA"));
        } else {
            gd.setColor(Color.parseColor("#1E293B"));
            gd.setStroke(dp(1), Color.parseColor("#475569"));
        }
        return gd;
    }

    public void setBottomActionViewId(int viewId) {
        this.bottomActionViewId = viewId;
        applyNavigationRules();
    }

    public void setBottomActionButton(View actionButton) {
        if (actionButton != null) {
            if (actionButton.getId() == View.NO_ID) {
                actionButton.setId(View.generateViewId());
            }
            this.bottomActionViewId = actionButton.getId();
            actionButton.setNextFocusUpId(R.id.pin_btn_0);
            actionButton.setNextFocusDownId(R.id.pin_btn_2);
        } else {
            this.bottomActionViewId = View.NO_ID;
        }
        applyNavigationRules();
    }

    /**
     * Configures 4-way circular wrap-around navigation across all keypad buttons:
     * - Horizontal: Left wraps to Right; Right wraps to Left for each row.
     * - Vertical: Up wraps to bottom action (or bottom row); Down goes to bottom action (or top row).
     */
    private void applyNavigationRules() {
        int[] allIds = {
                R.id.pin_btn_1, R.id.pin_btn_2, R.id.pin_btn_3,
                R.id.pin_btn_4, R.id.pin_btn_5, R.id.pin_btn_6,
                R.id.pin_btn_7, R.id.pin_btn_8, R.id.pin_btn_9,
                R.id.pin_btn_del, R.id.pin_btn_0, R.id.pin_btn_ok
        };
        for (int btnId : allIds) {
            DpadPinPadNavRules.FocusRule rule = DpadPinPadNavRules.calculateRule(btnId, bottomActionViewId);
            configureButton(btnId, rule.leftId, rule.rightId, rule.upId, rule.downId);
        }
    }

    private void configureButton(int btnId, int leftId, int rightId, int upId, int downId) {
        View btn = findViewById(btnId);
        if (btn != null) {
            btn.setNextFocusLeftId(leftId);
            btn.setNextFocusRightId(rightId);
            btn.setNextFocusUpId(upId);
            btn.setNextFocusDownId(downId);
        }
    }

    public void appendDigit(char digit) {
        if (pinBuilder.length() < 6 && Character.isDigit(digit)) {
            pinBuilder.append(digit);
            updateSlots();
            if (pinBuilder.length() == 6 && onPinComplete != null) {
                onPinComplete.onPinComplete(pinBuilder.toString());
            }
        }
    }

    public void deleteDigit() {
        if (pinBuilder.length() > 0) {
            pinBuilder.deleteCharAt(pinBuilder.length() - 1);
            updateSlots();
        }
    }

    public void clear() {
        pinBuilder.setLength(0);
        updateSlots();
    }

    public String currentPin() {
        return pinBuilder.toString();
    }

    public View getFirstButton() {
        return grid.getChildAt(0);
    }

    public Button getButton(int id) {
        return findViewById(id);
    }

    private void updateSlots() {
        for (int i = 0; i < 6; i++) {
            TextView slot = slotViews.get(i);
            if (i < pinBuilder.length()) {
                slot.setText(String.valueOf(pinBuilder.charAt(i)));
                slot.setBackground(createSlotBackground(true));
            } else {
                slot.setText("");
                slot.setBackground(createSlotBackground(false));
            }
        }
    }

    public void setKeypadEnabled(boolean enabled) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            grid.getChildAt(i).setEnabled(enabled);
        }
    }
}
