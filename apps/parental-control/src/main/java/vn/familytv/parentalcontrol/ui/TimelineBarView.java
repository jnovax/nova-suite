package vn.familytv.parentalcontrol.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import java.util.List;
import java.util.Locale;
import vn.familytv.parentalcontrol.screentime.AppUsageSession;

/**
 * Renders a 24-hour horizontal timeline bar (00:00 -> 24:00) highlighting
 * tracked (amber) and untracked (cyan/green) app sessions across the day.
 */
public class TimelineBarView extends View {
    private final List<AppUsageSession> sessions;
    private final long dayStartMs;
    private final float density;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackedSessionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint untrackedSessionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF barRect = new RectF();
    private final RectF segRect = new RectF();

    public TimelineBarView(Context context, List<AppUsageSession> sessions, long dayStartMs) {
        super(context);
        this.sessions = sessions;
        this.dayStartMs = dayStartMs;
        this.density = context.getResources().getDisplayMetrics().density;

        trackPaint.setColor(Color.parseColor("#1E293B"));
        trackPaint.setStyle(Paint.Style.FILL);

        trackedSessionPaint.setColor(Color.parseColor("#F59E0B"));
        trackedSessionPaint.setStyle(Paint.Style.FILL);

        untrackedSessionPaint.setColor(Color.parseColor("#38BDF8"));
        untrackedSessionPaint.setStyle(Paint.Style.FILL);

        tickPaint.setColor(Color.parseColor("#475569"));
        tickPaint.setStrokeWidth(dp(1f));

        labelPaint.setColor(Color.parseColor("#94A3B8"));
        labelPaint.setTextSize(dp(11f));
    }

    private float dp(float v) {
        return v * density;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) dp(46f);
        int w = resolveSize((int) dp(400f), widthMeasureSpec);
        int h = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = (float) getWidth();
        float barTop = dp(4f);
        float barHeight = dp(20f);
        float barBottom = barTop + barHeight;
        float radius = dp(6f);

        barRect.set(0f, barTop, w, barBottom);
        canvas.drawRoundRect(barRect, radius, radius, trackPaint);

        long dayDurationMs = 24L * 3600L * 1000L;
        if (sessions != null) {
            for (AppUsageSession session : sessions) {
                float startFrac = Math.max(0f, Math.min(1f, (float) (session.getStartMs() - dayStartMs) / dayDurationMs));
                float endFrac = Math.max(0f, Math.min(1f, (float) (session.getEndMs() - dayStartMs) / dayDurationMs));
                float left = startFrac * w;
                float right = Math.min(w, Math.max(left + dp(3f), endFrac * w));
                segRect.set(left, barTop, right, barBottom);
                Paint paint = session.isTracked() ? trackedSessionPaint : untrackedSessionPaint;
                canvas.drawRoundRect(segRect, dp(2f), dp(2f), paint);
            }
        }

        int[] hours = new int[]{0, 6, 12, 18, 24};
        for (int hr : hours) {
            float frac = hr / 24f;
            float x = Math.max(dp(1f), Math.min(w - dp(1f), frac * w));
            canvas.drawLine(x, barTop, x, barBottom + dp(4f), tickPaint);

            String label = String.format(Locale.US, "%02d:00", hr);
            float textWidth = labelPaint.measureText(label);
            float textX;
            if (hr == 0) {
                textX = 0f;
            } else if (hr == 24) {
                textX = w - textWidth;
            } else {
                textX = x - textWidth / 2f;
            }
            canvas.drawText(label, textX, barBottom + dp(17f), labelPaint);
        }
    }
}
