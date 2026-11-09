package vn.familytv.parentalcontrol.security;

public class ParentSessionManager {
    public interface TimeProvider {
        long now();
    }

    private final long sessionDurationMs;
    private final TimeProvider timeProvider;
    private long sessionExpiresEpochMs = 0L;

    public ParentSessionManager() {
        this(300_000L, System::currentTimeMillis);
    }

    public ParentSessionManager(long sessionDurationMs, TimeProvider timeProvider) {
        this.sessionDurationMs = sessionDurationMs;
        this.timeProvider = timeProvider;
    }

    public void startSession() {
        startSession(sessionDurationMs);
    }

    public void startSession(long durationMs) {
        sessionExpiresEpochMs = timeProvider.now() + durationMs;
    }

    public void touchSession() {
        if (isParentModeActive()) {
            sessionExpiresEpochMs = timeProvider.now() + sessionDurationMs;
        }
    }

    public void invalidateSession() {
        sessionExpiresEpochMs = 0L;
    }

    public boolean isParentModeActive() {
        return timeProvider.now() < sessionExpiresEpochMs;
    }

    public long remainingSessionSeconds() {
        long diff = sessionExpiresEpochMs - timeProvider.now();
        return diff > 0 ? (diff + 999) / 1000 : 0L;
    }
}
