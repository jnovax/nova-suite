package vn.familytv.parentalcontrol.screentime;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ScreenTimePolicy {
    public static final Set<String> DEFAULT_TRACKED_PACKAGES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "io.gh.reisxd.tizentube.cobalt",
            "org.smarttube.stable",
            "org.smarttube.beta",
            "com.teamsmart.videomanager.tv",
            "com.liskovsoft.smarttubetv.beta",
            "com.liskovsoft.videomanager",
            "com.google.android.youtube.tv",
            "com.google.android.youtube.tvkids",
            "com.google.android.youtube",
            "org.schabi.newpipe",
            "vn.mytvnet.b2cott",
            "vn.mytvnet.b2c",
            "vn.vnpt.media.mytv",
            "org.courville.nova",
            "com.spotify.tv.android",
            "com.spotify.music"
    )));

    public static final Set<String> DEFAULT_EXCLUDED_PACKAGES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "com.appletv.launcher",
            "vn.familytv.parentalcontrol",
            "vn.familytv.audit",
            "com.android.tv.settings",
            "com.android.systemui",
            "com.google.android.backdrop",
            "com.google.android.apps.tv.dreamx",
            "com.tcl.ambient"
    )));

    private final int defaultWeekdayHours;
    private final int defaultWeekendHours;
    private final int defaultLimitSec;
    private final Set<String> trackedPackages;
    private final Set<String> excludedPackages;

    public ScreenTimePolicy() {
        this(2, 3, 7200, DEFAULT_TRACKED_PACKAGES, DEFAULT_EXCLUDED_PACKAGES);
    }

    public ScreenTimePolicy(int defaultLimitSec) {
        this(2, 3, defaultLimitSec, DEFAULT_TRACKED_PACKAGES, DEFAULT_EXCLUDED_PACKAGES);
    }

    public ScreenTimePolicy(int defaultWeekdayHours, int defaultWeekendHours) {
        this(defaultWeekdayHours, defaultWeekendHours, 7200, DEFAULT_TRACKED_PACKAGES, DEFAULT_EXCLUDED_PACKAGES);
    }

    public ScreenTimePolicy(int defaultWeekdayHours, int defaultWeekendHours, int defaultLimitSec,
                            Set<String> trackedPackages, Set<String> excludedPackages) {
        this.defaultWeekdayHours = defaultWeekdayHours;
        this.defaultWeekendHours = defaultWeekendHours;
        this.defaultLimitSec = defaultLimitSec;
        this.trackedPackages = trackedPackages != null ? trackedPackages : DEFAULT_TRACKED_PACKAGES;
        this.excludedPackages = excludedPackages != null ? excludedPackages : DEFAULT_EXCLUDED_PACKAGES;
    }

    public int getDefaultWeekdayHours() {
        return defaultWeekdayHours;
    }

    public int getDefaultWeekendHours() {
        return defaultWeekendHours;
    }

    public int getDefaultLimitSec() {
        return defaultLimitSec;
    }

    public Set<String> getTrackedPackages() {
        return trackedPackages;
    }

    public Set<String> getExcludedPackages() {
        return excludedPackages;
    }

    public boolean isTrackedPackage(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) return false;
        String pkg = packageName.trim();
        if (trackedPackages.contains(pkg)) return true;
        if (pkg.startsWith("vn.mytvnet.")) return true;
        if (pkg.startsWith("com.spotify.")) return true;
        return false;
    }

    public boolean isWeekend() {
        return isWeekend(Calendar.getInstance());
    }

    public boolean isWeekend(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY;
    }

    public int getLimitSecForDay(boolean isWeekend) {
        return getLimitSecForDay(isWeekend, defaultWeekdayHours, defaultWeekendHours);
    }

    public int getLimitSecForDay(boolean isWeekend, int weekdayHours, int weekendHours) {
        int hours = isWeekend ? weekendHours : weekdayHours;
        if (hours < 1) hours = 1;
        if (hours > 4) hours = 4;
        return hours * 3600;
    }
}
