package vn.familytv.parentalcontrol.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import vn.familytv.parentalcontrol.R;

public class TvViews {
    private final Context context;

    public TvViews(Context context) {
        this.context = context;
    }

    public int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public LinearLayout column() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public TextView text(CharSequence value, float size, int color) {
        TextView tv = new TextView(context);
        tv.setText(value);
        tv.setTextSize(size);
        tv.setTextColor(context.getColor(color));
        tv.setLineSpacing(dp(3), 1f);
        return tv;
    }

    public TextView text(CharSequence value) {
        return text(value, 18f, R.color.text_secondary);
    }

    public TextView title(CharSequence value) {
        TextView tv = text(value, 32f, R.color.text_primary);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setAccessibilityHeading(true);
        tv.setPadding(0, 0, 0, dp(12));
        return tv;
    }

    public void body(LinearLayout parent, CharSequence value) {
        TextView tv = text(value);
        tv.setPadding(0, 0, 0, dp(18));
        parent.addView(tv);
    }

    public Button button(
            LinearLayout parent,
            CharSequence label,
            int viewId,
            boolean enabled,
            Runnable action
    ) {
        Button btn = new Button(context);
        btn.setId(viewId);
        btn.setText(label);
        btn.setTextSize(18f);
        btn.setAllCaps(false);
        btn.setEnabled(enabled);
        btn.setGravity(Gravity.CENTER);
        btn.setMinimumHeight(dp(56));
        btn.setPadding(dp(18), dp(12), dp(18), dp(12));
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
        btn.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(10);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        parent.addView(btn, lp);
        return btn;
    }

    public Button button(LinearLayout parent, CharSequence label, int viewId, Runnable action) {
        return button(parent, label, viewId, true, action);
    }

    public void status(LinearLayout parent, CharSequence label, CharSequence value, boolean complete) {
        LinearLayout card = column();
        card.setBackground(context.getDrawable(R.drawable.status_card));
        card.setPadding(dp(18), dp(12), dp(18), dp(12));

        card.addView(text(label, 14f, R.color.text_secondary));
        card.addView(text(value, 20f, complete ? R.color.success : R.color.warning));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = dp(10);
        parent.addView(card, lp);
    }

    public void focusLater(View view) {
        if (view != null) {
            view.post(view::requestFocus);
        }
    }
}
