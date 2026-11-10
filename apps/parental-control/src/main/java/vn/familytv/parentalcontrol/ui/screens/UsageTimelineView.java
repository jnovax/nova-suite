package vn.familytv.parentalcontrol.ui.screens;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import vn.familytv.parentalcontrol.R;
import vn.familytv.parentalcontrol.screentime.AppUsageSession;
import vn.familytv.parentalcontrol.screentime.DailyUsageTimeline;
import vn.familytv.parentalcontrol.screentime.ScreenTimePolicy;
import vn.familytv.parentalcontrol.screentime.ScreenTimeStore;
import vn.familytv.parentalcontrol.screentime.UsageTimelineAnalyzer;
import vn.familytv.parentalcontrol.ui.TimelineBarView;
import vn.familytv.parentalcontrol.ui.TvViews;

public class UsageTimelineView extends LinearLayout {

    public interface Listener {
        void onSyncTimeline(int targetSyncSec);
        void onBack();
    }

    private final Button initialFocusTarget;

    public UsageTimelineView(
            Context context,
            ScreenTimeStore screenTimeStore,
            ScreenTimePolicy screenTimePolicy,
            Listener listener
    ) {
        super(context);
        TvViews views = new TvViews(context);

        long nowMs = System.currentTimeMillis();
        DailyUsageTimeline timeline = UsageTimelineAnalyzer.queryToday(context, screenTimePolicy, nowMs);

        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(nowMs);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        long dayStartMs = startCal.getTimeInMillis();

        String dateLabel = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date(nowMs));
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.US);

        setOrientation(VERTICAL);
        setBackgroundColor(Color.parseColor("#090D16"));
        setPadding(views.dp(36), views.dp(20), views.dp(36), views.dp(20));

        // Header Row
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, views.dp(10));

        LinearLayout titleCol = views.column();
        TextView titleTv = views.title("Lịch sử sử dụng hôm nay (" + dateLabel + ")");
        titleTv.setTextSize(20f);
        titleTv.setPadding(0, 0, 0, views.dp(2));
        titleCol.addView(titleTv);

        String summaryText = "Tổng thời gian App tính giờ: " + UsageTimelineAnalyzer.formatDurationVi(timeline.getTotalTrackedSec())
                + "   •   App không tính giờ: " + UsageTimelineAnalyzer.formatDurationVi(timeline.getTotalUntrackedSec());
        titleCol.addView(views.text(summaryText, 13f, R.color.accent_cyan));
        headerRow.addView(titleCol, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        int totalLimitSec = screenTimeStore.getTotalLimitSec(startCal, screenTimePolicy);
        int targetSyncSec = Math.min(timeline.getTotalTrackedSec(), totalLimitSec);
        int currentUsedSec = screenTimeStore.getUsedSec();

        if (Math.abs(targetSyncSec - currentUsedSec) >= 60) {
            Button syncBtn = views.button(
                    headerRow,
                    "Đồng bộ bộ đếm (" + UsageTimelineAnalyzer.formatDurationVi(timeline.getTotalTrackedSec()) + ")",
                    R.id.sync_timeline_action,
                    () -> {
                        if (listener != null) listener.onSyncTimeline(targetSyncSec);
                    }
            );
            syncBtn.setTextSize(13f);
            syncBtn.setMinimumHeight(views.dp(38));
            syncBtn.setPadding(views.dp(12), views.dp(6), views.dp(12), views.dp(6));
            LayoutParams syncLp = (LayoutParams) syncBtn.getLayoutParams();
            if (syncLp != null) {
                syncLp.width = LayoutParams.WRAP_CONTENT;
                syncLp.topMargin = 0;
                syncLp.setMarginEnd(views.dp(10));
                syncBtn.setLayoutParams(syncLp);
            }
        }

        Button backBtn = views.button(headerRow, "Quay lại", R.id.back_action, () -> {
            if (listener != null) listener.onBack();
        });
        backBtn.setTextSize(13f);
        backBtn.setMinimumHeight(views.dp(38));
        backBtn.setPadding(views.dp(14), views.dp(6), views.dp(14), views.dp(6));
        LayoutParams backLp = (LayoutParams) backBtn.getLayoutParams();
        if (backLp != null) {
            backLp.width = LayoutParams.WRAP_CONTENT;
            backLp.topMargin = 0;
            backBtn.setLayoutParams(backLp);
        }
        initialFocusTarget = backBtn;
        addView(headerRow);

        // 24-Hour Timeline Bar Card
        LinearLayout barCard = views.column();
        barCard.setBackground(context.getDrawable(R.drawable.status_card));
        barCard.setPadding(views.dp(16), views.dp(10), views.dp(16), views.dp(10));

        LinearLayout legendRow = new LinearLayout(context);
        legendRow.setOrientation(HORIZONTAL);
        legendRow.setGravity(Gravity.CENTER_VERTICAL);
        legendRow.setPadding(0, 0, 0, views.dp(4));

        TextView legendTitle = views.text("Trục thời gian 24 giờ", 13f, R.color.text_primary);
        legendTitle.setTypeface(Typeface.DEFAULT_BOLD);
        legendRow.addView(legendTitle, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        TextView legendAmber = views.text("■ Tính giờ (Giải trí)", 12f, R.color.accent_amber);
        legendAmber.setPadding(0, 0, views.dp(14), 0);
        legendRow.addView(legendAmber);

        TextView legendCyan = views.text("■ Không tính giờ (Học tập / Khác)", 12f, R.color.accent_cyan);
        legendRow.addView(legendCyan);

        barCard.addView(legendRow);
        barCard.addView(
                new TimelineBarView(context, timeline.getSessions(), dayStartMs),
                new LayoutParams(LayoutParams.MATCH_PARENT, views.dp(46))
        );

        LayoutParams barCardLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        barCardLp.bottomMargin = views.dp(8);
        addView(barCard, barCardLp);

        // Chronological Sessions List Card
        LinearLayout listCard = views.column();
        listCard.setBackground(context.getDrawable(R.drawable.status_card));
        listCard.setPadding(views.dp(16), views.dp(10), views.dp(16), views.dp(10));

        TextView listTitle = views.text("Chi tiết các phiên mở ứng dụng (" + timeline.getSessions().size() + " phiên)", 14f, R.color.text_primary);
        listTitle.setTypeface(Typeface.DEFAULT_BOLD);
        listTitle.setPadding(0, 0, 0, views.dp(6));
        listCard.addView(listTitle);

        ScrollView scroll = new ScrollView(context);
        scroll.setFocusable(true);
        scroll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout sessionList = views.column();
        List<AppUsageSession> sessions = timeline.getSessions();
        if (sessions.isEmpty()) {
            TextView emptyTv = views.text("Chưa ghi nhận phiên sử dụng ứng dụng nào trong ngày hôm nay.", 14f, R.color.text_secondary);
            emptyTv.setPadding(0, views.dp(8), 0, views.dp(8));
            sessionList.addView(emptyTv);
        } else {
            for (int idx = 0; idx < sessions.size(); idx++) {
                AppUsageSession session = sessions.get(idx);
                LinearLayout row = new LinearLayout(context);
                row.setOrientation(HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(views.dp(10), views.dp(7), views.dp(10), views.dp(7));
                row.setBackgroundColor((idx % 2 == 0) ? Color.parseColor("#111827") : Color.parseColor("#0B1120"));

                String startStr = timeFmt.format(new Date(session.getStartMs()));
                String endStr = timeFmt.format(new Date(session.getEndMs()));
                TextView rangeTv = views.text(startStr + " → " + endStr, 14f, R.color.accent_cyan);
                rangeTv.setTypeface(Typeface.MONOSPACE);
                row.addView(rangeTv, new LayoutParams(views.dp(130), LayoutParams.WRAP_CONTENT));

                String appLabel = UsageTimelineAnalyzer.resolveAppName(context, session.getPackageName());
                TextView appTv = views.text(appLabel, 14f, R.color.text_primary);
                appTv.setTypeface(Typeface.DEFAULT_BOLD);
                row.addView(appTv, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

                TextView durTv = views.text(
                        UsageTimelineAnalyzer.formatDurationVi(session.getDurationSec()),
                        14f,
                        session.isTracked() ? R.color.accent_amber : R.color.text_secondary
                );
                durTv.setGravity(Gravity.END);
                durTv.setPadding(views.dp(8), 0, views.dp(14), 0);
                row.addView(durTv, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                String tagText = session.isTracked() ? "Tính giờ" : "Không tính giờ";
                int tagColor = session.isTracked() ? R.color.accent_amber : R.color.accent_cyan;
                TextView tagTv = views.text(tagText, 12f, tagColor);
                tagTv.setGravity(Gravity.END);
                row.addView(tagTv, new LayoutParams(views.dp(100), LayoutParams.WRAP_CONTENT));

                LayoutParams rowLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                rowLp.bottomMargin = views.dp(3);
                sessionList.addView(row, rowLp);
            }
        }

        scroll.addView(sessionList);
        listCard.addView(scroll);
        addView(listCard, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
    }

    public View getInitialFocusTarget() {
        return initialFocusTarget;
    }
}
