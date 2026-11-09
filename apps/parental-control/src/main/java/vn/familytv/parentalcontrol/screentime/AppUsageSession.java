package vn.familytv.parentalcontrol.screentime;

import java.util.Objects;

public final class AppUsageSession {
    private final String packageName;
    private final long startMs;
    private final long endMs;
    private final boolean isTracked;

    public AppUsageSession(String packageName, long startMs, long endMs, boolean isTracked) {
        this.packageName = packageName;
        this.startMs = startMs;
        this.endMs = endMs;
        this.isTracked = isTracked;
    }

    public String getPackageName() {
        return packageName;
    }

    public long getStartMs() {
        return startMs;
    }

    public long getEndMs() {
        return endMs;
    }

    public boolean isTracked() {
        return isTracked;
    }

    public int getDurationSec() {
        return Math.max(0, (int) ((endMs - startMs) / 1000L));
    }

    public AppUsageSession copy(long startMs, long endMs) {
        return new AppUsageSession(this.packageName, startMs, endMs, this.isTracked);
    }

    public AppUsageSession copyWithEndMs(long endMs) {
        return new AppUsageSession(this.packageName, this.startMs, endMs, this.isTracked);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppUsageSession that = (AppUsageSession) o;
        return startMs == that.startMs &&
                endMs == that.endMs &&
                isTracked == that.isTracked &&
                Objects.equals(packageName, that.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, startMs, endMs, isTracked);
    }

    @Override
    public String toString() {
        return "AppUsageSession(packageName=" + packageName + ", startMs=" + startMs + ", endMs=" + endMs + ", isTracked=" + isTracked + ")";
    }
}
