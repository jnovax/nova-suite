package vn.familytv.parentalcontrol.screentime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageTimelineAnalyzerTest {
    private val policy = ScreenTimePolicy()
    private val dayStartMs = 1_700_000_000_000L

    private fun sec(s: Int): Long = dayStartMs + s * 1000L

    @Test
    fun `extracts session closed by DEVICE_SHUTDOWN and merges short Bluetooth settings interruption`() {
        val events = listOf(
            // Session 1: 09:20:02 (t=100s) -> 11:19:47 (t=7285s), closed by DEVICE_SHUTDOWN (no PAUSED event)
            RawUsageEvent("com.appletv.launcher", sec(90), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
            RawUsageEvent("com.appletv.launcher", sec(100), RawUsageEvent.EVENT_ACTIVITY_PAUSED),
            RawUsageEvent("io.gh.reisxd.tizentube.cobalt", sec(100), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
            RawUsageEvent("com.appletv.launcher", sec(104), RawUsageEvent.EVENT_ACTIVITY_STOPPED),
            RawUsageEvent("android", sec(7285), RawUsageEvent.EVENT_DEVICE_SHUTDOWN),

            // Session 2: Reboot at t=8000s, YouTube at t=8010s, interrupted by AccessoriesActivity for 8s (8034s..8042s), then until 9770s
            RawUsageEvent("android", sec(8000), RawUsageEvent.EVENT_DEVICE_STARTUP),
            RawUsageEvent("com.appletv.launcher", sec(8002), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
            RawUsageEvent("com.appletv.launcher", sec(8010), RawUsageEvent.EVENT_ACTIVITY_PAUSED),
            RawUsageEvent("io.gh.reisxd.tizentube.cobalt", sec(8010), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
            RawUsageEvent("io.gh.reisxd.tizentube.cobalt", sec(8034), RawUsageEvent.EVENT_ACTIVITY_PAUSED),
            RawUsageEvent("com.android.tv.settings", sec(8034), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
            RawUsageEvent("io.gh.reisxd.tizentube.cobalt", sec(8035), RawUsageEvent.EVENT_ACTIVITY_STOPPED),
            RawUsageEvent("com.android.tv.settings", sec(8042), RawUsageEvent.EVENT_ACTIVITY_PAUSED),
            RawUsageEvent("io.gh.reisxd.tizentube.cobalt", sec(8042), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
            RawUsageEvent("io.gh.reisxd.tizentube.cobalt", sec(9770), RawUsageEvent.EVENT_ACTIVITY_PAUSED),
            RawUsageEvent("com.appletv.launcher", sec(9770), RawUsageEvent.EVENT_ACTIVITY_RESUMED)
        )

        val timeline = UsageTimelineAnalyzer.analyze(
            events = events,
            dayStartMs = dayStartMs,
            nowMs = sec(10000),
            policy = policy
        )

        assertEquals(2, timeline.sessions.size)

        val first = timeline.sessions[0]
        assertEquals("io.gh.reisxd.tizentube.cobalt", first.packageName)
        assertEquals(sec(100), first.startMs)
        assertEquals(sec(7285), first.endMs)
        assertEquals(7185, first.durationSec)
        assertTrue(first.isTracked)

        // Merged across the 8s AccessoriesActivity gap (< 30s)
        val second = timeline.sessions[1]
        assertEquals("io.gh.reisxd.tizentube.cobalt", second.packageName)
        assertEquals(sec(8010), second.startMs)
        assertEquals(sec(9770), second.endMs)
        assertEquals(1760, second.durationSec)
        assertTrue(second.isTracked)

        assertEquals(7185 + 1760, timeline.totalTrackedSec)
        assertEquals(0, timeline.totalUntrackedSec)
    }

    @Test
    fun `filters out brief sessions under 5 seconds and classifies untracked apps`() {
        val events = listOf(
            // Brief 2-second open of com.khtn7.tv -> should be filtered out (< 5s)
            RawUsageEvent("com.khtn7.tv", sec(100), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
            RawUsageEvent("com.khtn7.tv", sec(102), RawUsageEvent.EVENT_ACTIVITY_PAUSED),

            // 120-second session of com.khtn7.tv -> untracked user app, kept
            RawUsageEvent("com.khtn7.tv", sec(200), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
            RawUsageEvent("com.khtn7.tv", sec(320), RawUsageEvent.EVENT_ACTIVITY_PAUSED),

            // Still-open session of vn.mytvnet.b2cott from t=400s until nowMs=500s
            RawUsageEvent("vn.mytvnet.b2cott", sec(400), RawUsageEvent.EVENT_ACTIVITY_RESUMED)
        )

        val timeline = UsageTimelineAnalyzer.analyze(
            events = events,
            dayStartMs = dayStartMs,
            nowMs = sec(500),
            policy = policy
        )

        assertEquals(2, timeline.sessions.size)
        assertEquals("com.khtn7.tv", timeline.sessions[0].packageName)
        assertFalse(timeline.sessions[0].isTracked)
        assertEquals(120, timeline.sessions[0].durationSec)

        assertEquals("vn.mytvnet.b2cott", timeline.sessions[1].packageName)
        assertTrue(timeline.sessions[1].isTracked)
        assertEquals(100, timeline.sessions[1].durationSec)

        assertEquals(100, timeline.totalTrackedSec)
        assertEquals(120, timeline.totalUntrackedSec)
    }
}
