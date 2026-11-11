package com.appletv.screensaver.flipclock;

import java.util.Locale;

public final class FliqloTimeHelper {

    public static final class TimeData {
        public final String hourStr;
        public final String minStr;
        public final String amPm;

        public TimeData(String hourStr, String minStr, String amPm) {
            this.hourStr = hourStr;
            this.minStr = minStr;
            this.amPm = amPm;
        }
    }

    private FliqloTimeHelper() {}

    public static TimeData format(int hourOfDay, int minute, boolean is24Hour) {
        String hourStr;
        String amPm = "";

        if (is24Hour) {
            hourStr = String.format(Locale.US, "%02d", hourOfDay);
        } else {
            int h12 = hourOfDay % 12;
            if (h12 == 0) h12 = 12;
            hourStr = String.format(Locale.US, "%02d", h12);
            amPm = (hourOfDay >= 12) ? "PM" : "AM";
        }

        String minStr = String.format(Locale.US, "%02d", minute);
        return new TimeData(hourStr, minStr, amPm);
    }
}
