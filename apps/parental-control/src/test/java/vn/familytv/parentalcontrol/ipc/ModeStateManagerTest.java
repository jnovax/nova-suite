package vn.familytv.parentalcontrol.ipc;

import org.junit.Assert;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModeStateManagerTest {
    @Test
    public void testDefaultModeIsKid() {
        ModeStateManager manager = new ModeStateManager(300);
        Assert.assertEquals(Mode.KID, manager.getMode());
        Assert.assertEquals(0, manager.getSessionRemainingSec(1000L));
        Assert.assertFalse(manager.isQuizActive());
    }

    @Test
    public void testParentAuthenticationStarts5MinSession() {
        ModeStateManager manager = new ModeStateManager(300);
        long now = 100000L;
        manager.onParentAuthenticated(now);

        Assert.assertEquals(Mode.PARENT, manager.getMode());
        Assert.assertEquals(300, manager.getSessionRemainingSec(now));

        // Check sliding extension
        manager.extendSession(now + 60000L);
        Assert.assertEquals(300, manager.getSessionRemainingSec(now + 60000L));

        // Fast forward 300s -> expires to KID
        manager.tick(now + 361000L);
        Assert.assertEquals(Mode.KID, manager.getMode());
        Assert.assertEquals(0, manager.getSessionRemainingSec(now + 361000L));
    }

    @Test
    public void testScreenOffImmediatelyLocksToKidMode() {
        ModeStateManager manager = new ModeStateManager(300);
        manager.onParentAuthenticated(100000L);
        Assert.assertEquals(Mode.PARENT, manager.getMode());

        manager.onScreenOff();
        Assert.assertEquals(Mode.KID, manager.getMode());
    }

    @Test
    public void testQuizActiveFlag() {
        ModeStateManager manager = new ModeStateManager(300);
        manager.setQuizActive(true);
        Assert.assertTrue(manager.isQuizActive());
        manager.setQuizActive(false);
        Assert.assertFalse(manager.isQuizActive());
    }

    @Test
    public void testScreenTimeRemaining() {
        ModeStateManager manager = new ModeStateManager(300);
        Assert.assertEquals(900, manager.getScreenTimeRemainingSec());
        manager.setScreenTimeRemainingSec(450);
        Assert.assertEquals(450, manager.getScreenTimeRemainingSec());
    }

    @Test
    public void testModeChangeListener() {
        ModeStateManager manager = new ModeStateManager(300);
        List<Mode> transitions = new ArrayList<>();
        ModeStateManager.ModeListener listener = transitions::add;
        manager.addListener(listener);

        manager.onParentAuthenticated(100000L);
        manager.expireSession();

        Assert.assertEquals(Arrays.asList(Mode.PARENT, Mode.KID), transitions);

        // Test removeListener
        manager.removeListener(listener);
        manager.onParentAuthenticated(200000L);
        Assert.assertEquals(2, transitions.size());
    }

    @Test
    public void testAuthInProgressFlag() {
        ModeStateManager manager = new ModeStateManager(300);
        Assert.assertFalse(manager.isAuthInProgress);
        manager.isAuthInProgress = true;
        Assert.assertTrue(manager.isAuthInProgress);
        manager.isAuthInProgress = false;
        Assert.assertFalse(manager.isAuthInProgress);
    }

    @Test
    public void testResetForTesting() {
        ModeStateManager manager = ModeStateManager.instance;
        manager.onParentAuthenticated(100000L);
        manager.setQuizActive(true);
        manager.isAuthInProgress = true;
        manager.setScreenTimeRemainingSec(100);

        manager.resetForTesting();
        Assert.assertEquals(Mode.KID, manager.getMode());
        Assert.assertFalse(manager.isQuizActive());
        Assert.assertFalse(manager.isAuthInProgress);
        Assert.assertEquals(900, manager.getScreenTimeRemainingSec());
        Assert.assertEquals(0, manager.getSessionRemainingSec(100000L));
    }
}
