package vn.familytv.parentalcontrol.screentime;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UsageTimelineAnalyzer {
    private static final Set<String> SYSTEM_NOISE_PACKAGES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "android",
            "com.android.systemui",
            "com.android.tv.settings",
            "com.appletv.launcher",
            "vn.familytv.parentalcontrol",
            "vn.familytv.audit",
            "com.google.android.backdrop",
            "com.google.android.apps.tv.dreamx",
            "com.tcl.ambient",
            "com.google.android.tv.remote.service",
            "com.google.android.permissioncontroller"
    )));

    private static final Map<String, String> KNOWN_APP_LABELS;
    static {
        Map<String, String> map = new HashMap<>();
        map.put("io.gh.reisxd.tizentube.cobalt", "YouTube (TizenTube)");
        map.put("org.smarttube.stable", "YouTube (SmartTube)");
        map.put("org.smarttube.beta", "YouTube (SmartTube Beta)");
        map.put("com.google.android.youtube.tv", "YouTube");
        map.put("com.google.android.youtube.tvkids", "YouTube Kids");
        map.put("vn.mytvnet.b2cott", "MyTV");
        map.put("vn.mytvnet.b2c", "MyTV");
        map.put("vn.vnpt.media.mytv", "MyTV");
        map.put("org.courville.nova", "Nova Video Player");
        map.put("com.spotify.tv.android", "Spotify");
        map.put("com.spotify.music", "Spotify");
        map.put("com.khtn7.tv", "KHTN 7");
        KNOWN_APP_LABELS = Collections.unmodifiableMap(map);
    }

    private UsageTimelineAnalyzer() {}

    public static boolean isSystemNoisePackage(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) return true;
        String pkg = packageName.trim();
        if (SYSTEM_NOISE_PACKAGES.contains(pkg)) return true;
        if (pkg.contains(".inputmethod")) return true;
        return false;
    }

    public static DailyUsageTimeline analyze(
            List<RawUsageEvent> events,
            long dayStartMs,
            long nowMs
    ) {
        return analyze(events, dayStartMs, nowMs, new ScreenTimePolicy(), 30000L, 5);
    }

    public static DailyUsageTimeline analyze(
            List<RawUsageEvent> events,
            long dayStartMs,
            long nowMs,
            ScreenTimePolicy policy
    ) {
        return analyze(events, dayStartMs, nowMs, policy, 30000L, 5);
    }

    public static DailyUsageTimeline analyze(
            List<RawUsageEvent> events,
            long dayStartMs,
            long nowMs,
            ScreenTimePolicy policy,
            long mergeGapMs,
            int minSessionSec
    ) {
        if (nowMs <= dayStartMs) {
            return new DailyUsageTimeline(Collections.emptyList(), 0, 0);
        }

        List<RawUsageEvent> sorted = new ArrayList<>(events);
        Collections.sort(sorted, Comparator.comparingLong(RawUsageEvent::getTimestampMs));

        List<AppUsageSession> rawSessions = new ArrayList<>();
        String[] activePkgHolder = new String[]{null};
        long[] activeStartMsHolder = new long[]{0L};

        Runnable closeActive = () -> {
            String pkg = activePkgHolder[0];
            if (pkg == null) return;
            long clampedStart = Math.min(Math.max(activeStartMsHolder[0], dayStartMs), nowMs);
            long clampedEnd = Math.min(Math.max(nowMs, dayStartMs), nowMs);
        };

        for (RawUsageEvent ev : sorted) {
            long ts = ev.getTimestampMs();
            if (ts < dayStartMs || ts > nowMs) continue;

            int eventType = ev.getEventType();
            if (eventType == RawUsageEvent.EVENT_ACTIVITY_RESUMED) {
                if (activePkgHolder[0] != null && !activePkgHolder[0].equals(ev.getPackageName())) {
                    closeSession(rawSessions, activePkgHolder[0], activeStartMsHolder[0], ts, dayStartMs, nowMs, policy);
                    activePkgHolder[0] = null;
                    activeStartMsHolder[0] = 0L;
                }
                if (!isSystemNoisePackage(ev.getPackageName())) {
                    if (activePkgHolder[0] == null) {
                        activePkgHolder[0] = ev.getPackageName();
                        activeStartMsHolder[0] = ts;
                    }
                } else {
                    if (activePkgHolder[0] != null) {
                        closeSession(rawSessions, activePkgHolder[0], activeStartMsHolder[0], ts, dayStartMs, nowMs, policy);
                        activePkgHolder[0] = null;
                        activeStartMsHolder[0] = 0L;
                    }
                }
            } else if (eventType == RawUsageEvent.EVENT_ACTIVITY_PAUSED ||
                    eventType == RawUsageEvent.EVENT_ACTIVITY_STOPPED) {
                if (activePkgHolder[0] != null && activePkgHolder[0].equals(ev.getPackageName())) {
                    closeSession(rawSessions, activePkgHolder[0], activeStartMsHolder[0], ts, dayStartMs, nowMs, policy);
                    activePkgHolder[0] = null;
                    activeStartMsHolder[0] = 0L;
                }
            } else if (eventType == RawUsageEvent.EVENT_DEVICE_SHUTDOWN ||
                    eventType == RawUsageEvent.EVENT_DEVICE_STARTUP) {
                if (activePkgHolder[0] != null) {
                    closeSession(rawSessions, activePkgHolder[0], activeStartMsHolder[0], ts, dayStartMs, nowMs, policy);
                    activePkgHolder[0] = null;
                    activeStartMsHolder[0] = 0L;
                }
            }
        }

        if (activePkgHolder[0] != null) {
            closeSession(rawSessions, activePkgHolder[0], activeStartMsHolder[0], nowMs, dayStartMs, nowMs, policy);
        }

        List<AppUsageSession> merged = new ArrayList<>();
        for (AppUsageSession session : rawSessions) {
            if (merged.isEmpty()) {
                merged.add(session);
            } else {
                AppUsageSession last = merged.get(merged.size() - 1);
                if (last.getPackageName().equals(session.getPackageName()) && (session.getStartMs() - last.getEndMs()) < mergeGapMs) {
                    merged.set(merged.size() - 1, last.copyWithEndMs(Math.max(last.getEndMs(), session.getEndMs())));
                } else {
                    merged.add(session);
                }
            }
        }

        List<AppUsageSession> filtered = new ArrayList<>();
        int totalTrackedSec = 0;
        int totalUntrackedSec = 0;

        for (AppUsageSession session : merged) {
            if (session.getDurationSec() >= minSessionSec) {
                filtered.add(session);
                if (session.isTracked()) {
                    totalTrackedSec += session.getDurationSec();
                } else {
                    totalUntrackedSec += session.getDurationSec();
                }
            }
        }

        return new DailyUsageTimeline(filtered, totalTrackedSec, totalUntrackedSec);
    }

    private static void closeSession(
            List<AppUsageSession> target,
            String pkg,
            long startMs,
            long endMs,
            long dayStartMs,
            long nowMs,
            ScreenTimePolicy policy
    ) {
        if (pkg == null) return;
        long clampedStart = Math.min(Math.max(startMs, dayStartMs), nowMs);
        long clampedEnd = Math.min(Math.max(endMs, dayStartMs), nowMs);
        if (clampedEnd > clampedStart && !isSystemNoisePackage(pkg)) {
            target.add(new AppUsageSession(pkg, clampedStart, clampedEnd, policy.isTrackedPackage(pkg)));
        }
    }

    public static DailyUsageTimeline queryToday(Context context) {
        return queryToday(context, new ScreenTimePolicy(), System.currentTimeMillis());
    }

    public static DailyUsageTimeline queryToday(Context context, ScreenTimePolicy policy) {
        return queryToday(context, policy, System.currentTimeMillis());
    }

    public static DailyUsageTimeline queryToday(Context context, ScreenTimePolicy policy, long nowMs) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) {
            return new DailyUsageTimeline(Collections.emptyList(), 0, 0);
        }

        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(nowMs);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        long dayStartMs = startCal.getTimeInMillis();

        List<RawUsageEvent> rawList = new ArrayList<>();
        try {
            UsageEvents usageEvents = usm.queryEvents(dayStartMs, nowMs);
            UsageEvents.Event event = new UsageEvents.Event();
            while (usageEvents != null && usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                int type = event.getEventType();
                if (type == RawUsageEvent.EVENT_ACTIVITY_RESUMED ||
                        type == RawUsageEvent.EVENT_ACTIVITY_PAUSED ||
                        type == RawUsageEvent.EVENT_ACTIVITY_STOPPED ||
                        type == RawUsageEvent.EVENT_DEVICE_SHUTDOWN ||
                        type == RawUsageEvent.EVENT_DEVICE_STARTUP) {
                    String pkg = event.getPackageName();
                    if (pkg != null) {
                        rawList.add(new RawUsageEvent(pkg, event.getTimeStamp(), type));
                    }
                }
            }
        } catch (Exception ignored) {
            return new DailyUsageTimeline(Collections.emptyList(), 0, 0);
        }

        return analyze(rawList, dayStartMs, nowMs, policy);
    }

    public static String resolveAppName(Context context, String packageName) {
        String label = KNOWN_APP_LABELS.get(packageName);
        if (label != null) return label;
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            CharSequence appLabel = pm.getApplicationLabel(info);
            if (appLabel != null && appLabel.length() > 0) {
                return appLabel.toString();
            }
        } catch (Exception ignored) {}
        return packageName;
    }

    public static String formatDurationVi(int seconds) {
        if (seconds < 60) return seconds + "s";
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        if (h > 0 && m > 0) {
            return h + "h " + m + "p";
        } else if (h > 0) {
            return h + "h";
        } else if (s > 0 && m < 10) {
            return m + "p " + s + "s";
        } else {
            return m + " phút";
        }
    }
}
