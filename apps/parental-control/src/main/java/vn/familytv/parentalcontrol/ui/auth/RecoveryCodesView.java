package vn.familytv.parentalcontrol.ui.auth;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class RecoveryCodesView extends LinearLayout {
    private final float density;
    private final GridLayout grid;

    public RecoveryCodesView(Context context) {
        super(context);
        this.density = context.getResources().getDisplayMetrics().density;

        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);

        grid = new GridLayout(context);
        grid.setColumnCount(2);
        grid.setRowCount(3);
        addView(grid);
    }

    private int dp(int v) {
        return (int) (v * density + 0.5f);
    }

    public void setCodes(List<String> codes) {
        grid.removeAllViews();
        if (codes == null) return;
        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            String formatted = (code.length() == 6)
                    ? code.substring(0, 3) + " " + code.substring(3)
                    : code;

            TextView tv = new TextView(getContext());
            tv.setText((i + 1) + ".  " + formatted);
            tv.setTextSize(22f);
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setTextColor(Color.parseColor("#60A5FA"));
            tv.setBackgroundColor(Color.parseColor("#1E293B"));
            tv.setPadding(dp(16), dp(10), dp(16), dp(10));

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.setMargins(dp(8), dp(6), dp(8), dp(6));
            tv.setLayoutParams(lp);

            grid.addView(tv);
        }
    }
}
