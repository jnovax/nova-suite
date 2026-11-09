package vn.familytv.parentalcontrol;

import org.junit.Assert;
import org.junit.Test;
import java.util.Calendar;
import vn.familytv.parentalcontrol.ipc.Mode;
import vn.familytv.parentalcontrol.ipc.ModeStateManager;
import vn.familytv.parentalcontrol.screentime.ScreenTimePolicy;
import vn.familytv.parentalcontrol.screentime.ScreenTimeTracker;

public class EndToEndM3IntegrationTest {
    @Test
    public void testFullDailyScreenTimeLimitAndExtensionFlow() {
        boolean[] audioMuted = new boolean[]{false};
        boolean[] overlayShown = new boolean[]{false};

        ModeStateManager modeManager = new ModeStateManager(300);
        ScreenTimePolicy policy = new ScreenTimePolicy(3);
        ScreenTimeTracker tracker = new ScreenTimeTracker(policy, () -> {
            audioMuted[0] = true;
            overlayShown[0] = true;
            modeManager.setQuizActive(true);
        });

        // 1. Kid watches TV on 3rd-party app
        tracker.setScreenOn(true);
        tracker.setMode(Mode.KID);
        tracker.setForegroundPackage("com.google.android.youtube.tv");

        tracker.tickSecond();
        tracker.tickSecond();
        Assert.assertFalse(audioMuted[0]);
        Assert.assertFalse(overlayShown[0]);

        // 2. 3rd second hits daily limit
        tracker.tickSecond();
        Assert.assertTrue(audioMuted[0]);
        Assert.assertTrue(overlayShown[0]);
        Assert.assertTrue(modeManager.isQuizActive());

        // 3. Parent unlocks and grants 30-minute extension (1800 seconds)
        int extensionSec = 1800;
        int currentRem = Math.max(0, tracker.getRemainingSec());
        int newRemaining = currentRem + extensionSec;
        tracker.restoreState(newRemaining);

        audioMuted[0] = false;
        overlayShown[0] = false;
        modeManager.setQuizActive(false);

        Assert.assertEquals(1800, tracker.getRemainingSec());
        Assert.assertFalse(audioMuted[0]);
        Assert.assertFalse(overlayShown[0]);
        Assert.assertFalse(modeManager.isQuizActive());

        // Kid continues watching: tick 10 seconds
        for (int i = 0; i < 10; i++) {
            tracker.tickSecond();
        }
        Assert.assertEquals(1790, tracker.getRemainingSec());
    }

    @Test
    public void testWeekdayAndWeekendPolicyCalculations() {
        ScreenTimePolicy policy = new ScreenTimePolicy(2, 3);

        // Monday (Weekday)
        Calendar monday = Calendar.getInstance();
        monday.set(2026, Calendar.NOVEMBER, 2); // Monday
        Assert.assertFalse(policy.isWeekend(monday));
        Assert.assertEquals(7200, policy.getLimitSecForDay(policy.isWeekend(monday), 2, 3));

        // Saturday (Weekend)
        Calendar saturday = Calendar.getInstance();
        saturday.set(2026, Calendar.NOVEMBER, 7); // Saturday
        Assert.assertTrue(policy.isWeekend(saturday));
        Assert.assertEquals(10800, policy.getLimitSecForDay(policy.isWeekend(saturday), 2, 3));

        // Boundary checks (1 to 4 hours)
        Assert.assertEquals(3600, policy.getLimitSecForDay(false, 0, 3)); // coerced to 1h
        Assert.assertEquals(14400, policy.getLimitSecForDay(false, 5, 3)); // coerced to 4h
    }

    @Test
    public void testParentModeSuspendsScreenTimeDeduction() {
        ModeStateManager modeManager = new ModeStateManager(300);
        ScreenTimePolicy policy = new ScreenTimePolicy(10);
        boolean[] limitHit = new boolean[]{false};
        ScreenTimeTracker tracker = new ScreenTimeTracker(policy, () -> {
            limitHit[0] = true;
        });

        tracker.setScreenOn(true);
        tracker.setForegroundPackage("com.google.android.youtube.tv");

        // In kid mode, 5 seconds elapsed
        tracker.setMode(modeManager.getMode());
        for (int i = 0; i < 5; i++) {
            tracker.tickSecond();
        }
        Assert.assertEquals(5, tracker.getRemainingSec());

        // Parent authenticates
        modeManager.onParentAuthenticated(1000L);
        tracker.setMode(modeManager.getMode());
        Assert.assertEquals(Mode.PARENT, modeManager.getMode());

        // In parent mode, ticking doesn't reduce kid screen time
        for (int i = 0; i < 5; i++) {
            tracker.tickSecond();
        }
        Assert.assertEquals(5, tracker.getRemainingSec());
        Assert.assertFalse(limitHit[0]);

        // Parent session expires after 300s
        modeManager.tick(1000L + 300000L);
        Assert.assertEquals(Mode.KID, modeManager.getMode());
        tracker.setMode(modeManager.getMode());

        // Now ticking resumes in Kid mode
        for (int i = 0; i < 5; i++) {
            tracker.tickSecond();
        }
        Assert.assertEquals(0, tracker.getRemainingSec());
        Assert.assertTrue(limitHit[0]);
    }
}
