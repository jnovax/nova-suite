package vn.familytv.parentalcontrol.screentime;

import org.junit.Assert;
import org.junit.Test;
import vn.familytv.parentalcontrol.ipc.Mode;

public class DebugCountdownTest {
    @Test
    public void showsSharedCountdownAndActivePackage() {
        ScreenTimeTracker tracker = new ScreenTimeTracker(new ScreenTimePolicy(1800), () -> {});
        tracker.setForegroundPackage("io.gh.reisxd.tizentube.cobalt");
        tracker.tickSecond();
        Assert.assertEquals("29:59  •  COUNTING\nio.gh.reisxd.tizentube.cobalt", tracker.debugCountdownText());

        tracker.setForegroundPackage("vn.mytvnet.b2cott");
        tracker.tickSecond();
        Assert.assertEquals("29:58  •  COUNTING\nvn.mytvnet.b2cott", tracker.debugCountdownText());
    }

    @Test
    public void explainsWhyCountdownIsPaused() {
        ScreenTimeTracker tracker = new ScreenTimeTracker(new ScreenTimePolicy(3600), () -> {});
        tracker.setForegroundPackage("com.android.systemui");
        Assert.assertEquals("1:00:00  •  UNTRACKED\ncom.android.systemui", tracker.debugCountdownText());

        tracker.setMode(Mode.PARENT);
        Assert.assertEquals("1:00:00  •  PARENT\ncom.android.systemui", tracker.debugCountdownText());
    }
}
