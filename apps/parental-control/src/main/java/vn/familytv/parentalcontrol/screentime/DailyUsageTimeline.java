package vn.familytv.parentalcontrol.screentime;

import java.util.List;

public final class DailyUsageTimeline {
    private final List<AppUsageSession> sessions;
    private final int totalTrackedSec;
    private final int totalUntrackedSec;

    public DailyUsageTimeline(List<AppUsageSession> sessions, int totalTrackedSec, int totalUntrackedSec) {
        this.sessions = sessions;
        this.totalTrackedSec = totalTrackedSec;
        this.totalUntrackedSec = totalUntrackedSec;
    }

    public List<AppUsageSession> getSessions() {
        return sessions;
    }

    public int getTotalTrackedSec() {
        return totalTrackedSec;
    }

    public int getTotalUntrackedSec() {
        return totalUntrackedSec;
    }
}
