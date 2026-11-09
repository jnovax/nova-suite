package vn.familytv.parentalcontrol.screentime;

import vn.familytv.parentalcontrol.ipc.Mode;

public class ScreenTimeTracker {
    public interface LimitCallback {
        void onLimitReached();
    }

    private final ScreenTimePolicy policy;
    private final LimitCallback onLimitReached;

    private boolean isScreenOn = true;
    private boolean isDreaming = false;
    private Mode currentMode = Mode.KID;
    private String foregroundPackage = null;
    private long navigatingHomeUntilMs = 0L;
    private int usedSec = 0;
    private int totalLimitSec;

    public ScreenTimeTracker(LimitCallback onLimitReached) {
        this(new ScreenTimePolicy(), onLimitReached);
    }

    public ScreenTimeTracker(ScreenTimePolicy policy, LimitCallback onLimitReached) {
        this.policy = policy != null ? policy : new ScreenTimePolicy();
        this.onLimitReached = onLimitReached;
        this.totalLimitSec = this.policy.getDefaultLimitSec();
    }

    public ScreenTimePolicy getPolicy() {
        return policy;
    }

    public synchronized void setScreenOn(boolean isOn) {
        this.isScreenOn = isOn;
    }

    public synchronized boolean isScreenOn() {
        return isScreenOn;
    }

    public synchronized void setDreaming(boolean dreaming) {
        this.isDreaming = dreaming;
    }

    public synchronized boolean isDreaming() {
        return isDreaming;
    }

    public synchronized void setMode(Mode mode) {
        this.currentMode = mode;
    }

    public synchronized Mode getMode() {
        return currentMode;
    }

    public synchronized void setForegroundPackage(String pkg) {
        this.foregroundPackage = pkg;
    }

    public synchronized void onNavigatingHome() {
        onNavigatingHome(System.currentTimeMillis(), 2500L);
    }

    public synchronized void onNavigatingHome(long nowMs, long guardDurationMs) {
        this.foregroundPackage = null;
        this.navigatingHomeUntilMs = nowMs + guardDurationMs;
    }

    public synchronized boolean updateForegroundPackage(String pkg) {
        return updateForegroundPackage(pkg, System.currentTimeMillis());
    }

    public synchronized boolean updateForegroundPackage(String pkg, long nowMs) {
        if (pkg != null && nowMs < navigatingHomeUntilMs) {
            if (policy.isTrackedPackage(pkg)) {
                return false;
            }
            navigatingHomeUntilMs = 0L;
        }
        this.foregroundPackage = pkg;
        return true;
    }

    public synchronized String getForegroundPackage() {
        return foregroundPackage;
    }

    public synchronized void setTotalLimitSec(int limit) {
        this.totalLimitSec = limit;
    }

    public synchronized int getTotalLimitSec() {
        return totalLimitSec;
    }

    public synchronized int getUsedSec() {
        return usedSec;
    }

    public synchronized void setUsedSec(int used) {
        this.usedSec = Math.max(0, used);
    }

    public synchronized int getRemainingSec() {
        return Math.max(0, totalLimitSec - usedSec);
    }

    public synchronized String debugCountdownText() {
        int remaining = getRemainingSec();
        int hours = remaining / 3600;
        String time = hours > 0
                ? String.format("%d:%02d:%02d", hours, (remaining / 60) % 60, remaining % 60)
                : String.format("%02d:%02d", remaining / 60, remaining % 60);

        String status;
        if (!isScreenOn) {
            status = "SCREEN OFF";
        } else if (isDreaming) {
            status = "DREAMING";
        } else if (currentMode != Mode.KID) {
            status = "PARENT";
        } else if (remaining == 0) {
            status = "LIMIT REACHED";
        } else if (!policy.isTrackedPackage(foregroundPackage)) {
            status = "UNTRACKED";
        } else {
            status = "COUNTING";
        }
        return time + "  •  " + status + "\n" + (foregroundPackage != null ? foregroundPackage : "(no package)");
    }

    public synchronized void tickSecond() {
        if (!isScreenOn || isDreaming || currentMode != Mode.KID) return;
        if (foregroundPackage == null) return;
        if (!policy.isTrackedPackage(foregroundPackage)) return;

        if (getRemainingSec() > 0) {
            usedSec++;
            if (getRemainingSec() == 0 && onLimitReached != null) {
                onLimitReached.onLimitReached();
            }
        }
    }

    public synchronized void reset() {
        usedSec = 0;
    }

    public synchronized void restoreState(int remainingSec) {
        if (remainingSec > totalLimitSec - usedSec) {
            totalLimitSec = usedSec + remainingSec;
        } else {
            usedSec = Math.max(0, totalLimitSec - remainingSec);
        }
    }

    public synchronized void restoreUsedAndLimit(int used, int limit) {
        this.totalLimitSec = limit;
        this.usedSec = Math.max(0, used);
    }
}
