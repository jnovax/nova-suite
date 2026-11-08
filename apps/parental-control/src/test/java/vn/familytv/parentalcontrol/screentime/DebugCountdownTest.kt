package vn.familytv.parentalcontrol.screentime

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.familytv.parentalcontrol.ipc.Mode

class DebugCountdownTest {
    @Test fun showsSharedCountdownAndActivePackage() {
        val tracker = ScreenTimeTracker(ScreenTimePolicy(defaultLimitSec = 1800)) {}
        tracker.setForegroundPackage("io.gh.reisxd.tizentube.cobalt")
        tracker.tickSecond()
        assertEquals("29:59  •  COUNTING\nio.gh.reisxd.tizentube.cobalt", tracker.debugCountdownText())
        tracker.setForegroundPackage("vn.mytvnet.b2cott")
        tracker.tickSecond()
        assertEquals("29:58  •  COUNTING\nvn.mytvnet.b2cott", tracker.debugCountdownText())
    }

    @Test fun explainsWhyCountdownIsPaused() {
        val tracker = ScreenTimeTracker(ScreenTimePolicy(defaultLimitSec = 3600)) {}
        tracker.setForegroundPackage("com.android.systemui")
        assertEquals("1:00:00  •  UNTRACKED\ncom.android.systemui", tracker.debugCountdownText())
        tracker.setMode(Mode.PARENT)
        assertEquals("1:00:00  •  PARENT\ncom.android.systemui", tracker.debugCountdownText())
    }
}
