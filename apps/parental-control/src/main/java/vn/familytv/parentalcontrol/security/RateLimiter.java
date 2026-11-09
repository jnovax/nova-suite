package vn.familytv.parentalcontrol.security;

public class RateLimiter {
    public interface TimeProvider {
        long now();
    }

    private final TimeProvider timeProvider;
    private int consecutiveFailures = 0;
    private long lockedUntilEpochMs = 0L;

    public RateLimiter() {
        this(System::currentTimeMillis);
    }

    public RateLimiter(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public long getLockedUntilEpochMs() {
        return lockedUntilEpochMs;
    }

    public boolean isLocked() {
        return timeProvider.now() < lockedUntilEpochMs;
    }

    public long remainingLockSeconds() {
        long diff = lockedUntilEpochMs - timeProvider.now();
        return diff > 0 ? (diff + 999) / 1000 : 0L;
    }

    public int remainingAttemptsBeforeLock() {
        if (isLocked()) return 0;
        int rem = 5 - (consecutiveFailures % 5);
        if (rem < 1) rem = 1;
        if (rem > 5) rem = 5;
        return rem;
    }

    public long recordFailure() {
        consecutiveFailures++;
        long now = timeProvider.now();
        if (consecutiveFailures >= 5) {
            long lockoutDurationMs = (consecutiveFailures == 5) ? 60_000L : 300_000L;
            lockedUntilEpochMs = now + lockoutDurationMs;
            return lockedUntilEpochMs;
        }
        return 0L;
    }

    public void recordSuccess() {
        consecutiveFailures = 0;
        lockedUntilEpochMs = 0L;
    }

    public void restore(int failures, long lockedUntil) {
        this.consecutiveFailures = failures;
        this.lockedUntilEpochMs = lockedUntil;
    }
}
