package vn.familytv.parentalcontrol.screentime;

import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ScreenTimeStore {
    private static final String KEY_USED_SEC = "key_used_sec";
    private static final String KEY_EXTENSION_SEC = "key_extension_sec";
    private static final String KEY_WEEKDAY_HOURS = "key_weekday_hours";
    private static final String KEY_WEEKEND_HOURS = "key_weekend_hours";
    private static final String KEY_LAST_TRACKED_DATE = "key_last_tracked_date";

    private final SharedPreferences prefs;

    public ScreenTimeStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public static String formatDate() {
        return formatDate(new Date());
    }

    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(date);
    }

    public int getWeekdayHours() {
        return prefs.getInt(KEY_WEEKDAY_HOURS, 2);
    }

    public void setWeekdayHours(int hours) {
        if (hours < 1) hours = 1;
        if (hours > 4) hours = 4;
        prefs.edit().putInt(KEY_WEEKDAY_HOURS, hours).apply();
    }

    public int getWeekendHours() {
        return prefs.getInt(KEY_WEEKEND_HOURS, 3);
    }

    public void setWeekendHours(int hours) {
        if (hours < 1) hours = 1;
        if (hours > 4) hours = 4;
        prefs.edit().putInt(KEY_WEEKEND_HOURS, hours).apply();
    }

    public int getDailyLimitHours() {
        return getDailyLimitHours(Calendar.getInstance(), new ScreenTimePolicy());
    }

    public int getDailyLimitHours(Calendar calendar, ScreenTimePolicy policy) {
        boolean isWeekend = policy.isWeekend(calendar);
        return isWeekend ? getWeekendHours() : getWeekdayHours();
    }

    public String getLastTrackedDate() {
        return prefs.getString(KEY_LAST_TRACKED_DATE, null);
    }

    public void setLastTrackedDate(String dateStr) {
        prefs.edit().putString(KEY_LAST_TRACKED_DATE, dateStr).apply();
    }

    public int getUsedSec() {
        return prefs.getInt(KEY_USED_SEC, 0);
    }

    public void setUsedSec(int sec) {
        prefs.edit().putInt(KEY_USED_SEC, Math.max(0, sec)).apply();
    }

    public int getExtensionSec() {
        return prefs.getInt(KEY_EXTENSION_SEC, 0);
    }

    public void setExtensionSec(int sec) {
        prefs.edit().putInt(KEY_EXTENSION_SEC, Math.max(0, sec)).apply();
    }

    public void addExtensionSec(int addedSec) {
        setExtensionSec(getExtensionSec() + addedSec);
    }

    public int getTotalLimitSec() {
        return getTotalLimitSec(Calendar.getInstance(), new ScreenTimePolicy());
    }

    public int getTotalLimitSec(Calendar calendar, ScreenTimePolicy policy) {
        return getDailyLimitHours(calendar, policy) * 3600 + getExtensionSec();
    }

    public int getRemainingSec() {
        return getRemainingSec(Calendar.getInstance(), new ScreenTimePolicy());
    }

    public int getRemainingSec(Calendar calendar, ScreenTimePolicy policy) {
        return Math.max(0, getTotalLimitSec(calendar, policy) - getUsedSec());
    }

    public int getRemainingSec(int defaultSec) {
        return Math.max(0, getTotalLimitSec() - getUsedSec());
    }

    public void setRemainingSec(int remainingSec) {
        int total = getTotalLimitSec();
        setUsedSec(Math.max(0, total - remainingSec));
    }

    public void addRemainingSec(int addedSec) {
        addExtensionSec(addedSec);
    }

    public boolean checkAndResetDay(Calendar calendar, ScreenTimePolicy policy) {
        String todayStr = formatDate(calendar.getTime());
        String lastDate = getLastTrackedDate();
        if (lastDate != null && todayStr.compareTo(lastDate) < 0) {
            return false;
        }
        if (!todayStr.equals(lastDate)) {
            setLastTrackedDate(todayStr);
            setUsedSec(0);
            setExtensionSec(0);
            return true;
        }
        return false;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
