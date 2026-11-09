package vn.familytv.parentalcontrol.screentime;

import android.app.usage.UsageEvents;

public final class RawUsageEvent {
    public static final int EVENT_ACTIVITY_RESUMED = UsageEvents.Event.ACTIVITY_RESUMED;
    public static final int EVENT_ACTIVITY_PAUSED = UsageEvents.Event.ACTIVITY_PAUSED;
    public static final int EVENT_ACTIVITY_STOPPED = UsageEvents.Event.ACTIVITY_STOPPED;
    public static final int EVENT_DEVICE_SHUTDOWN = UsageEvents.Event.DEVICE_SHUTDOWN;
    public static final int EVENT_DEVICE_STARTUP = UsageEvents.Event.DEVICE_STARTUP;

    private final String packageName;
    private final long timestampMs;
    private final int eventType;

    public RawUsageEvent(String packageName, long timestampMs, int eventType) {
        this.packageName = packageName;
        this.timestampMs = timestampMs;
        this.eventType = eventType;
    }

    public String getPackageName() {
        return packageName;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public int getEventType() {
        return eventType;
    }
}
