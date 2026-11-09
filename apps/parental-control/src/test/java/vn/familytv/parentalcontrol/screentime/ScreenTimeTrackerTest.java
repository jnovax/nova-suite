package vn.familytv.parentalcontrol.screentime;

import org.junit.Assert;
import org.junit.Test;
import vn.familytv.parentalcontrol.ipc.Mode;

public class ScreenTimeTrackerTest {
    private boolean limitReached = false;
    private final ScreenTimePolicy policy = new ScreenTimePolicy(10);
    private final ScreenTimeTracker tracker = new ScreenTimeTracker(policy, () -> limitReached = true);

    @Test
    public void testDoesNotTickWhenScreenIsOff() {
        tracker.setScreenOn(false);
        tracker.setMode(Mode.KID);
        tracker.setForegroundPackage("com.google.android.youtube.tv");

        tracker.tickSecond();
        Assert.assertEquals(10, tracker.getRemainingSec());
        Assert.assertFalse(limitReached);
    }

    @Test
    public void testDoesNotTickWhenDreamingOrScreenSaverIsActive() {
        tracker.setScreenOn(true);
        tracker.setDreaming(true);
        tracker.setMode(Mode.KID);
        tracker.setForegroundPackage("com.google.android.youtube.tv");

        tracker.tickSecond();
        Assert.assertEquals(10, tracker.getRemainingSec());
        Assert.assertFalse(limitReached);

        // Exit screensaver
        tracker.setDreaming(false);
        tracker.tickSecond();
        Assert.assertEquals(9, tracker.getRemainingSec());
    }

    @Test
    public void testDoesNotTickWhenInParentMode() {
        tracker.setScreenOn(true);
        tracker.setMode(Mode.PARENT);
        tracker.setForegroundPackage("com.google.android.youtube.tv");

        tracker.tickSecond();
        Assert.assertEquals(10, tracker.getRemainingSec());
        Assert.assertFalse(limitReached);
    }

    @Test
    public void testDoesNotTickOnLauncherOrFamilyTime() {
        tracker.setScreenOn(true);
        tracker.setMode(Mode.KID);

        tracker.setForegroundPackage("com.appletv.launcher");
        tracker.tickSecond();
        Assert.assertEquals(10, tracker.getRemainingSec());

        tracker.setForegroundPackage("vn.familytv.parentalcontrol");
        tracker.tickSecond();
        Assert.assertEquals(10, tracker.getRemainingSec());
        Assert.assertFalse(limitReached);
    }

    @Test
    public void testDoesNotTickOnScreensaverOrAmbientPackages() {
        tracker.setScreenOn(true);
        tracker.setMode(Mode.KID);

        tracker.setForegroundPackage("com.google.android.backdrop");
        tracker.tickSecond();
        Assert.assertEquals(10, tracker.getRemainingSec());

        tracker.setForegroundPackage("com.tcl.ambient");
        tracker.tickSecond();
        Assert.assertEquals(10, tracker.getRemainingSec());

        tracker.setForegroundPackage("com.android.systemui");
        tracker.tickSecond();
        Assert.assertEquals(10, tracker.getRemainingSec());
        Assert.assertFalse(limitReached);
    }

    @Test
    public void testDoesNotTickOnUntrackedApps() {
        tracker.setScreenOn(true);
        tracker.setMode(Mode.KID);

        tracker.setForegroundPackage("com.netflix.ninja");
        tracker.tickSecond();
        Assert.assertEquals(10, tracker.getRemainingSec());

        tracker.setForegroundPackage("com.vocatv.app");
        tracker.tickSecond();
        Assert.assertEquals(10, tracker.getRemainingSec());
        Assert.assertFalse(limitReached);
    }

    @Test
    public void testTicksOnYouTubeAndReachesLimit() {
        tracker.setScreenOn(true);
        tracker.setMode(Mode.KID);
        tracker.setForegroundPackage("org.smarttube.stable");

        for (int i = 0; i < 9; i++) {
            tracker.tickSecond();
            Assert.assertFalse(limitReached);
        }
        Assert.assertEquals(1, tracker.getRemainingSec());

        // 10th tick reaches limit
        tracker.tickSecond();
        Assert.assertEquals(0, tracker.getRemainingSec());
        Assert.assertTrue(limitReached);

        // Subsequent ticks do not trigger negative values
        tracker.tickSecond();
        Assert.assertEquals(0, tracker.getRemainingSec());
        Assert.assertEquals(10, tracker.getUsedSec());
    }

    @Test
    public void testHomeNavigationSuppressesTransientEvents() {
        long now = 1000000L;
        tracker.onNavigatingHome(now, 2500L);

        // Transient tracked package event while returning home is suppressed
        boolean updated = tracker.updateForegroundPackage("org.smarttube.stable", now + 500L);
        Assert.assertFalse(updated);
        Assert.assertNull(tracker.getForegroundPackage());

        // But returning home to launcher is accepted immediately
        boolean homeUpdated = tracker.updateForegroundPackage("com.appletv.launcher", now + 1000L);
        Assert.assertTrue(homeUpdated);
        Assert.assertEquals("com.appletv.launcher", tracker.getForegroundPackage());
    }
}
