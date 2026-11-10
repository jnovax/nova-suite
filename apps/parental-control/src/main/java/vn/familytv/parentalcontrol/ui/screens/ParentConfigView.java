package vn.familytv.parentalcontrol.ui.screens;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Calendar;
import java.util.List;
import vn.familytv.parentalcontrol.R;
import vn.familytv.parentalcontrol.platform.AccessibilityState;
import vn.familytv.parentalcontrol.platform.PermissionSnapshot;
import vn.familytv.parentalcontrol.screentime.ScreenTimePolicy;
import vn.familytv.parentalcontrol.screentime.ScreenTimeStore;
import vn.familytv.parentalcontrol.security.AuthMode;
import vn.familytv.parentalcontrol.security.ParentSessionManager;
import vn.familytv.parentalcontrol.security.SecurityStore;
import vn.familytv.parentalcontrol.ui.TvViews;

public class ParentConfigView extends LinearLayout {

    public interface Listener {
        void onLockAndExit();
        void onViewTimeline();
        void onWeekdayHoursChanged(int hours);
        void onWeekendHoursChanged(int hours);
        void onFixPermission(boolean needAccessibility);
        void onChangePin();
        void onEnableTotp();
        void onViewRecoveryCodes();
        void onDisableTotp();
    }

    private final TextView liveTimerView;
    private final Button lockExitBtn;
    private final View initialFocusTarget;

    public ParentConfigView(
            Context context,
            PermissionSnapshot permissions,
            ScreenTimeStore screenTimeStore,
            SecurityStore securityStore,
            ParentSessionManager sessionManager,
            ScreenTimePolicy screenTimePolicy,
            Integer oldFocusId,
            Listener listener
    ) {
        super(context);
        TvViews views = new TvViews(context);

        setOrientation(VERTICAL);
        setBackgroundColor(Color.parseColor("#090D16"));
        setPadding(views.dp(36), views.dp(20), views.dp(36), views.dp(20));

        // Top Header Bar
        LinearLayout topBar = new LinearLayout(context);
        topBar.setOrientation(HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, views.dp(10));

        ImageView logoImg = new ImageView(context);
        logoImg.setImageResource(R.drawable.family_time_app_art);
        logoImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LayoutParams logoLp = new LayoutParams(views.dp(67), views.dp(40));
        logoLp.gravity = Gravity.CENTER_VERTICAL;
        logoLp.setMarginEnd(views.dp(14));
        topBar.addView(logoImg, logoLp);

        LinearLayout titlesCol = views.column();
        TextView titleTv = views.title(context.getString(R.string.parent_config_title));
        titleTv.setTextSize(20f);
        titleTv.setPadding(0, 0, 0, views.dp(2));
        titlesCol.addView(titleTv);

        long remSec = sessionManager.remainingSessionSeconds();
        long min = remSec / 60;
        long sec = remSec % 60;
        liveTimerView = views.text(context.getString(R.string.parent_session_remaining, min, sec), 13f, R.color.accent_amber);
        titlesCol.addView(liveTimerView);
        topBar.addView(titlesCol, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        lockExitBtn = views.button(topBar, context.getString(R.string.lock_and_exit), R.id.lock_action, () -> {
            if (listener != null) listener.onLockAndExit();
        });
        lockExitBtn.setTextSize(14f);
        lockExitBtn.setMinimumHeight(views.dp(38));
        lockExitBtn.setPadding(views.dp(14), views.dp(6), views.dp(14), views.dp(6));
        lockExitBtn.setNextFocusDownId(R.id.view_timeline_action);
        LayoutParams lockLp = (LayoutParams) lockExitBtn.getLayoutParams();
        if (lockLp != null) {
            lockLp.topMargin = 0;
            lockLp.width = LayoutParams.WRAP_CONTENT;
            lockExitBtn.setLayoutParams(lockLp);
        }

        addView(topBar);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        addView(scroll, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(VERTICAL);
        scroll.addView(content);

        // 1. Status Card
        LinearLayout statusCard = new LinearLayout(context);
        statusCard.setOrientation(HORIZONTAL);
        statusCard.setGravity(Gravity.CENTER_VERTICAL);
        statusCard.setBackground(context.getDrawable(R.drawable.status_card));
        statusCard.setPadding(views.dp(16), views.dp(10), views.dp(16), views.dp(10));

        LinearLayout statusInfoCol = views.column();
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        boolean isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY);
        String dayName;
        if (isWeekend) {
            dayName = (dayOfWeek == Calendar.SATURDAY) ? "Thứ Bảy" : "Chủ Nhật";
        } else {
            switch (dayOfWeek) {
                case Calendar.MONDAY: dayName = "Thứ Hai"; break;
                case Calendar.TUESDAY: dayName = "Thứ Ba"; break;
                case Calendar.WEDNESDAY: dayName = "Thứ Tư"; break;
                case Calendar.THURSDAY: dayName = "Thứ Năm"; break;
                default: dayName = "Thứ Sáu"; break;
            }
        }
        int currentLimitH = isWeekend ? screenTimeStore.getWeekendHours() : screenTimeStore.getWeekdayHours();
        statusInfoCol.addView(views.text("Hôm nay (" + dayName + ") • Giới hạn " + currentLimitH + " Giờ", 14f, R.color.accent_cyan));

        int usedSec = screenTimeStore.getUsedSec();
        int usedMin = usedSec / 60;
        int totalLimitSec = screenTimeStore.getTotalLimitSec(cal, screenTimePolicy);
        int remainingSec = Math.max(0, totalLimitSec - usedSec);
        int remMin = remainingSec / 60;
        String timerSummary = "Đã xem: " + usedMin + " phút  |  Còn lại: " + remMin + " phút  (Tự động đặt lại lúc 00:00)";
        TextView summaryTv = views.text(timerSummary, 16f, R.color.success);
        summaryTv.setTypeface(Typeface.DEFAULT_BOLD);
        summaryTv.setPadding(0, views.dp(4), 0, 0);
        statusInfoCol.addView(summaryTv);

        statusCard.addView(statusInfoCol, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        Button viewTimelineBtn = views.button(statusCard, "📊 Xem lịch sử hôm nay", R.id.view_timeline_action, () -> {
            if (listener != null) listener.onViewTimeline();
        });
        viewTimelineBtn.setTextSize(13f);
        viewTimelineBtn.setMinimumHeight(views.dp(38));
        viewTimelineBtn.setPadding(views.dp(14), views.dp(6), views.dp(14), views.dp(6));
        viewTimelineBtn.setNextFocusUpId(R.id.lock_action);
        viewTimelineBtn.setNextFocusDownId(R.id.weekday_4h);
        LayoutParams timelineLp = (LayoutParams) viewTimelineBtn.getLayoutParams();
        if (timelineLp != null) {
            timelineLp.width = LayoutParams.WRAP_CONTENT;
            timelineLp.topMargin = 0;
            timelineLp.setMarginStart(views.dp(12));
            viewTimelineBtn.setLayoutParams(timelineLp);
        }

        LayoutParams statusCardLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        statusCardLp.bottomMargin = views.dp(8);
        content.addView(statusCard, statusCardLp);

        // 2. Settings Card
        LinearLayout configCard = views.column();
        configCard.setBackground(context.getDrawable(R.drawable.status_card));
        configCard.setPadding(views.dp(16), views.dp(10), views.dp(16), views.dp(10));

        TextView configTitle = views.text("Cài đặt giới hạn thời gian xem TV", 16f, R.color.text_primary);
        configTitle.setTypeface(Typeface.DEFAULT_BOLD);
        configCard.addView(configTitle);

        if (!permissions.isReady()) {
            TextView warnTv = views.text(context.getString(R.string.permissions_required_for_screentime), 13f, R.color.warning);
            warnTv.setPadding(0, views.dp(4), 0, views.dp(8));
            configCard.addView(warnTv);
        } else {
            TextView descTv = views.text("Khi hết thời lượng, TV sẽ tự động hiển thị màn hình khóa và ngắt toàn bộ âm thanh.", 13f, R.color.text_secondary);
            descTv.setPadding(0, views.dp(2), 0, views.dp(6));
            configCard.addView(descTv);
        }

        int weekdayHours = screenTimeStore.getWeekdayHours();
        int weekendHours = screenTimeStore.getWeekendHours();

        TextView weekdayTitle = views.text("Ngày trong tuần (Thứ 2 – Thứ 6)", 13f, R.color.accent_cyan);
        weekdayTitle.setPadding(0, views.dp(2), 0, views.dp(4));
        configCard.addView(weekdayTitle);

        LinearLayout weekdayRow = new LinearLayout(context);
        weekdayRow.setOrientation(HORIZONTAL);
        weekdayRow.setPadding(0, views.dp(4), 0, views.dp(10));

        int[] weekdayIds = {R.id.weekday_1h, R.id.weekday_2h, R.id.weekday_3h, R.id.weekday_4h};
        int[] weekendIds = {R.id.weekend_1h, R.id.weekend_2h, R.id.weekend_3h, R.id.weekend_4h};

        for (int h = 1; h <= 4; h++) {
            final int hours = h;
            boolean isSelected = (h == weekdayHours);
            String label = isSelected ? "✓ " + h + " Giờ (Đang chọn)" : h + " Giờ";
            Button btn = views.button(weekdayRow, label, weekdayIds[h - 1], permissions.isReady(), () -> {
                if (listener != null) listener.onWeekdayHoursChanged(hours);
            });
            btn.setTextSize(14f);
            btn.setMinimumHeight(views.dp(40));
            btn.setPadding(views.dp(8), views.dp(6), views.dp(8), views.dp(6));
            if (!permissions.isReady()) {
                btn.setAlpha(0.4f);
            }
            btn.setNextFocusUpId(h == 4 ? R.id.view_timeline_action : R.id.lock_action);
            btn.setNextFocusDownId(weekendIds[h - 1]);

            LayoutParams btnLp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            btnLp.topMargin = 0;
            if (h > 1) btnLp.setMarginStart(views.dp(8));
            btn.setLayoutParams(btnLp);
        }
        configCard.addView(weekdayRow);

        TextView weekendTitle = views.text("Cuối tuần (Thứ 7 & Chủ Nhật)", 13f, R.color.accent_cyan);
        weekendTitle.setPadding(0, views.dp(2), 0, views.dp(4));
        configCard.addView(weekendTitle);

        LinearLayout weekendRow = new LinearLayout(context);
        weekendRow.setOrientation(HORIZONTAL);
        weekendRow.setPadding(0, views.dp(4), 0, views.dp(4));

        for (int h = 1; h <= 4; h++) {
            final int hours = h;
            boolean isSelected = (h == weekendHours);
            String label = isSelected ? "✓ " + h + " Giờ (Đang chọn)" : h + " Giờ";
            Button btn = views.button(weekendRow, label, weekendIds[h - 1], permissions.isReady(), () -> {
                if (listener != null) listener.onWeekendHoursChanged(hours);
            });
            btn.setTextSize(14f);
            btn.setMinimumHeight(views.dp(40));
            btn.setPadding(views.dp(8), views.dp(6), views.dp(8), views.dp(6));
            if (!permissions.isReady()) {
                btn.setAlpha(0.4f);
            }
            btn.setNextFocusUpId(weekdayIds[h - 1]);
            btn.setNextFocusDownId(!permissions.isReady() && h <= 2 ? R.id.permission_fix_action : R.id.change_pin_action);

            LayoutParams btnLp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            btnLp.topMargin = 0;
            if (h > 1) btnLp.setMarginStart(views.dp(8));
            btn.setLayoutParams(btnLp);
        }
        configCard.addView(weekendRow);

        LayoutParams configCardLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        configCardLp.bottomMargin = views.dp(8);
        content.addView(configCard, configCardLp);

        // 3. Compact Bottom System & Security Row
        LinearLayout bottomRow = new LinearLayout(context);
        bottomRow.setOrientation(HORIZONTAL);

        // Left Card: System Status
        LinearLayout cardLeft = views.column();
        cardLeft.setBackground(context.getDrawable(R.drawable.status_card));
        cardLeft.setPadding(views.dp(14), views.dp(8), views.dp(14), views.dp(8));
        cardLeft.addView(views.text(context.getString(R.string.system_status_title), 12f, R.color.text_secondary));

        LinearLayout statusRow = new LinearLayout(context);
        statusRow.setOrientation(HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setMinimumHeight(views.dp(34));

        if (permissions.isReady()) {
            statusRow.addView(
                    views.text(context.getString(R.string.system_status_ready), 14f, R.color.success),
                    new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            );
        } else {
            statusRow.addView(
                    views.text(context.getString(R.string.system_status_unready), 14f, R.color.warning),
                    new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            );
            Button fixBtn = views.button(statusRow, context.getString(R.string.fix_permission_action), R.id.permission_fix_action, () -> {
                if (listener != null) {
                    listener.onFixPermission(permissions.getAccessibility() != AccessibilityState.CONNECTED);
                }
            });
            fixBtn.setTextSize(13f);
            fixBtn.setMinimumHeight(views.dp(34));
            fixBtn.setPadding(views.dp(12), views.dp(4), views.dp(12), views.dp(4));
            fixBtn.setNextFocusUpId(R.id.weekend_1h);
            fixBtn.setNextFocusRightId(R.id.change_pin_action);
            LayoutParams fixLp = (LayoutParams) fixBtn.getLayoutParams();
            if (fixLp != null) {
                fixLp.width = LayoutParams.WRAP_CONTENT;
                fixLp.topMargin = 0;
                fixBtn.setLayoutParams(fixLp);
            }
        }
        cardLeft.addView(statusRow);
        LayoutParams leftLp = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        leftLp.setMarginEnd(views.dp(8));
        bottomRow.addView(cardLeft, leftLp);

        // Right Card: Security Settings
        LinearLayout cardRight = views.column();
        cardRight.setBackground(context.getDrawable(R.drawable.status_card));
        cardRight.setPadding(views.dp(14), views.dp(8), views.dp(14), views.dp(8));
        cardRight.addView(views.text(context.getString(R.string.security_card_title), 12f, R.color.text_secondary));

        LinearLayout secRow = new LinearLayout(context);
        secRow.setOrientation(HORIZONTAL);
        secRow.setGravity(Gravity.CENTER_VERTICAL);
        secRow.setMinimumHeight(views.dp(34));

        AuthMode authMode = securityStore.getAuthMode();
        if (authMode == AuthMode.PIN) {
            String pinStatusLabel = securityStore.isCustomPin() ? "Mã PIN riêng" : "Mã PIN mặc định (000000)";
            secRow.addView(views.text(pinStatusLabel, 13f, R.color.success), new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

            Button changePinBtn = views.button(secRow, context.getString(R.string.change_pin_action), R.id.change_pin_action, () -> {
                if (listener != null) listener.onChangePin();
            });
            changePinBtn.setTextSize(13f);
            changePinBtn.setMinimumHeight(views.dp(34));
            changePinBtn.setPadding(views.dp(10), views.dp(4), views.dp(10), views.dp(4));
            changePinBtn.setNextFocusUpId(R.id.weekend_3h);
            changePinBtn.setNextFocusRightId(R.id.enable_google_auth_action);
            if (!permissions.isReady()) {
                changePinBtn.setNextFocusLeftId(R.id.permission_fix_action);
            }
            LayoutParams cpLp = (LayoutParams) changePinBtn.getLayoutParams();
            if (cpLp != null) {
                cpLp.width = LayoutParams.WRAP_CONTENT;
                cpLp.topMargin = 0;
                cpLp.setMarginEnd(views.dp(6));
                changePinBtn.setLayoutParams(cpLp);
            }

            Button enableTotpBtn = views.button(secRow, context.getString(R.string.enable_google_auth_action), R.id.enable_google_auth_action, () -> {
                if (listener != null) listener.onEnableTotp();
            });
            enableTotpBtn.setTextSize(13f);
            enableTotpBtn.setMinimumHeight(views.dp(34));
            enableTotpBtn.setPadding(views.dp(10), views.dp(4), views.dp(10), views.dp(4));
            enableTotpBtn.setNextFocusUpId(R.id.weekend_4h);
            enableTotpBtn.setNextFocusLeftId(R.id.change_pin_action);
            LayoutParams etLp = (LayoutParams) enableTotpBtn.getLayoutParams();
            if (etLp != null) {
                etLp.width = LayoutParams.WRAP_CONTENT;
                etLp.topMargin = 0;
                enableTotpBtn.setLayoutParams(etLp);
            }
        } else {
            secRow.addView(views.text(context.getString(R.string.security_totp_mode), 13f, R.color.success), new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

            Button viewRecBtn = views.button(secRow, context.getString(R.string.view_recovery_codes), R.id.recovery_action, () -> {
                if (listener != null) listener.onViewRecoveryCodes();
            });
            viewRecBtn.setTextSize(13f);
            viewRecBtn.setMinimumHeight(views.dp(34));
            viewRecBtn.setPadding(views.dp(10), views.dp(4), views.dp(10), views.dp(4));
            viewRecBtn.setNextFocusUpId(R.id.weekend_3h);
            viewRecBtn.setNextFocusRightId(R.id.disable_google_auth_action);
            if (!permissions.isReady()) {
                viewRecBtn.setNextFocusLeftId(R.id.permission_fix_action);
            }
            LayoutParams vrLp = (LayoutParams) viewRecBtn.getLayoutParams();
            if (vrLp != null) {
                vrLp.width = LayoutParams.WRAP_CONTENT;
                vrLp.topMargin = 0;
                vrLp.setMarginEnd(views.dp(6));
                viewRecBtn.setLayoutParams(vrLp);
            }

            Button disableTotpBtn = views.button(secRow, context.getString(R.string.disable_google_auth_action), R.id.disable_google_auth_action, () -> {
                if (listener != null) listener.onDisableTotp();
            });
            disableTotpBtn.setTextSize(13f);
            disableTotpBtn.setMinimumHeight(views.dp(34));
            disableTotpBtn.setPadding(views.dp(10), views.dp(4), views.dp(10), views.dp(4));
            disableTotpBtn.setNextFocusUpId(R.id.weekend_4h);
            disableTotpBtn.setNextFocusLeftId(R.id.recovery_action);
            LayoutParams dtLp = (LayoutParams) disableTotpBtn.getLayoutParams();
            if (dtLp != null) {
                dtLp.width = LayoutParams.WRAP_CONTENT;
                dtLp.topMargin = 0;
                disableTotpBtn.setLayoutParams(dtLp);
            }
        }

        cardRight.addView(secRow);
        LayoutParams rightLp = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        rightLp.setMarginStart(views.dp(8));
        bottomRow.addView(cardRight, rightLp);

        content.addView(bottomRow, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        int selectedWeekdayId = weekdayIds[Math.max(1, Math.min(4, screenTimeStore.getWeekdayHours())) - 1];
        View preserved = (oldFocusId != null) ? findViewById(oldFocusId) : null;
        if (preserved != null && preserved.isEnabled() && preserved.isFocusable()) {
            initialFocusTarget = preserved;
        } else if (permissions.isReady()) {
            initialFocusTarget = findViewById(selectedWeekdayId);
        } else {
            View fixBtn = findViewById(R.id.permission_fix_action);
            initialFocusTarget = (fixBtn != null) ? fixBtn : lockExitBtn;
        }
    }

    public void updateLiveTimer(long remainingSec) {
        if (liveTimerView != null) {
            long min = remainingSec / 60;
            long sec = remainingSec % 60;
            liveTimerView.setText(getContext().getString(R.string.parent_session_remaining, min, sec));
        }
    }

    public View getInitialFocusTarget() {
        return initialFocusTarget != null ? initialFocusTarget : lockExitBtn;
    }
}
