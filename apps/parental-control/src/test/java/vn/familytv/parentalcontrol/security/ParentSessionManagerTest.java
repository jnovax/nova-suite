package vn.familytv.parentalcontrol.security;

import org.junit.Assert;
import org.junit.Test;

public class ParentSessionManagerTest {
    private long simulatedTime = 1000000L;
    private final ParentSessionManager sessionManager = new ParentSessionManager(300_000L, () -> simulatedTime);

    @Test
    public void testSessionLifecycleAndSlidingTimeout() {
        Assert.assertFalse(sessionManager.isParentModeActive());

        sessionManager.startSession();
        Assert.assertTrue(sessionManager.isParentModeActive());
        Assert.assertEquals(300L, sessionManager.remainingSessionSeconds());

        // Advance 200 seconds
        simulatedTime += 200000L;
        Assert.assertTrue(sessionManager.isParentModeActive());
        Assert.assertEquals(100L, sessionManager.remainingSessionSeconds());

        // User interacts (D-pad touch) -> extends session back to 300s
        sessionManager.touchSession();
        Assert.assertEquals(300L, sessionManager.remainingSessionSeconds());

        // Advance 301 seconds -> expires
        simulatedTime += 301000L;
        Assert.assertFalse(sessionManager.isParentModeActive());
        Assert.assertEquals(0L, sessionManager.remainingSessionSeconds());

        // Invalidate drops immediately
        sessionManager.startSession();
        Assert.assertTrue(sessionManager.isParentModeActive());
        sessionManager.invalidateSession();
        Assert.assertFalse(sessionManager.isParentModeActive());

        // Start session with custom remaining duration
        sessionManager.startSession(120_000L);
        Assert.assertTrue(sessionManager.isParentModeActive());
        Assert.assertEquals(120L, sessionManager.remainingSessionSeconds());
    }
}
