package vn.familytv.parentalcontrol.security;

import org.junit.Assert;
import org.junit.Test;

public class RateLimiterTest {
    private long simulatedTime = 1000000L;
    private final RateLimiter limiter = new RateLimiter(() -> simulatedTime);

    @Test
    public void testLockoutAfter5Failures() {
        Assert.assertEquals(5, limiter.remainingAttemptsBeforeLock());
        Assert.assertFalse(limiter.isLocked());

        for (int i = 0; i < 4; i++) {
            limiter.recordFailure();
            Assert.assertFalse(limiter.isLocked());
        }
        Assert.assertEquals(1, limiter.remainingAttemptsBeforeLock());

        // 5th failure triggers 60s lock
        limiter.recordFailure();
        Assert.assertTrue(limiter.isLocked());
        Assert.assertEquals(60L, limiter.remainingLockSeconds());

        // Advance 30s
        simulatedTime += 30000L;
        Assert.assertTrue(limiter.isLocked());
        Assert.assertEquals(30L, limiter.remainingLockSeconds());

        // Advance 31s -> lock expires
        simulatedTime += 31000L;
        Assert.assertFalse(limiter.isLocked());

        // Failure immediately after lockout triggers 300s lock
        limiter.recordFailure();
        Assert.assertTrue(limiter.isLocked());
        Assert.assertEquals(300L, limiter.remainingLockSeconds());

        // Success resets everything
        limiter.recordSuccess();
        Assert.assertFalse(limiter.isLocked());
        Assert.assertEquals(5, limiter.remainingAttemptsBeforeLock());
    }
}
