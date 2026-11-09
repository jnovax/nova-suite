package vn.familytv.parentalcontrol.screentime;

import org.junit.Assert;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;

public class UsageTimelineAnalyzerTest {
    private final ScreenTimePolicy policy = new ScreenTimePolicy();
    private final long dayStartMs = 1700000000000L;

    private long sec(int s) {
        return dayStartMs + s * 1000L;
    }

    @Test
    public void testExtractsSessionClosedByShutdownAndMergesShortInterruption() {
        List<RawUsageEvent> events = Arrays.asList(
                new RawUsageEvent("com.appletv.launcher", sec(90), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
                new RawUsageEvent("com.appletv.launcher", sec(100), RawUsageEvent.EVENT_ACTIVITY_PAUSED),
                new RawUsageEvent("io.gh.reisxd.tizentube.cobalt", sec(100), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
                new RawUsageEvent("com.appletv.launcher", sec(104), RawUsageEvent.EVENT_ACTIVITY_STOPPED),
                new RawUsageEvent("android", sec(7285), RawUsageEvent.EVENT_DEVICE_SHUTDOWN),

                new RawUsageEvent("android", sec(8000), RawUsageEvent.EVENT_DEVICE_STARTUP),
                new RawUsageEvent("com.appletv.launcher", sec(8002), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
                new RawUsageEvent("com.appletv.launcher", sec(8010), RawUsageEvent.EVENT_ACTIVITY_PAUSED),
                new RawUsageEvent("io.gh.reisxd.tizentube.cobalt", sec(8010), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
                new RawUsageEvent("io.gh.reisxd.tizentube.cobalt", sec(8034), RawUsageEvent.EVENT_ACTIVITY_PAUSED),
                new RawUsageEvent("com.android.tv.settings", sec(8034), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
                new RawUsageEvent("io.gh.reisxd.tizentube.cobalt", sec(8035), RawUsageEvent.EVENT_ACTIVITY_STOPPED),
                new RawUsageEvent("com.android.tv.settings", sec(8042), RawUsageEvent.EVENT_ACTIVITY_PAUSED),
                new RawUsageEvent("io.gh.reisxd.tizentube.cobalt", sec(8042), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
                new RawUsageEvent("io.gh.reisxd.tizentube.cobalt", sec(9770), RawUsageEvent.EVENT_ACTIVITY_PAUSED),
                new RawUsageEvent("com.appletv.launcher", sec(9770), RawUsageEvent.EVENT_ACTIVITY_RESUMED)
        );

        DailyUsageTimeline timeline = UsageTimelineAnalyzer.analyze(
                events,
                dayStartMs,
                sec(10000),
                policy
        );

        Assert.assertEquals(2, timeline.getSessions().size());

        AppUsageSession first = timeline.getSessions().get(0);
        Assert.assertEquals("io.gh.reisxd.tizentube.cobalt", first.getPackageName());
        Assert.assertEquals(sec(100), first.getStartMs());
        Assert.assertEquals(sec(7285), first.getEndMs());
        Assert.assertEquals(7185, first.getDurationSec());
        Assert.assertTrue(first.isTracked());

        AppUsageSession second = timeline.getSessions().get(1);
        Assert.assertEquals("io.gh.reisxd.tizentube.cobalt", second.getPackageName());
        Assert.assertEquals(sec(8010), second.getStartMs());
        Assert.assertEquals(sec(9770), second.getEndMs());
        Assert.assertEquals(1760, second.getDurationSec());
        Assert.assertTrue(second.isTracked());

        Assert.assertEquals(7185 + 1760, timeline.getTotalTrackedSec());
        Assert.assertEquals(0, timeline.getTotalUntrackedSec());
    }

    @Test
    public void testFiltersOutBriefSessionsUnder5SecondsAndClassifiesUntrackedApps() {
        List<RawUsageEvent> events = Arrays.asList(
                new RawUsageEvent("com.khtn7.tv", sec(100), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
                new RawUsageEvent("com.khtn7.tv", sec(102), RawUsageEvent.EVENT_ACTIVITY_PAUSED),

                new RawUsageEvent("com.khtn7.tv", sec(200), RawUsageEvent.EVENT_ACTIVITY_RESUMED),
                new RawUsageEvent("com.khtn7.tv", sec(320), RawUsageEvent.EVENT_ACTIVITY_PAUSED),

                new RawUsageEvent("vn.mytvnet.b2cott", sec(400), RawUsageEvent.EVENT_ACTIVITY_RESUMED)
        );

        DailyUsageTimeline timeline = UsageTimelineAnalyzer.analyze(
                events,
                dayStartMs,
                sec(500),
                policy
        );

        Assert.assertEquals(2, timeline.getSessions().size());
        Assert.assertEquals("com.khtn7.tv", timeline.getSessions().get(0).getPackageName());
        Assert.assertFalse(timeline.getSessions().get(0).isTracked());
        Assert.assertEquals(120, timeline.getSessions().get(0).getDurationSec());

        Assert.assertEquals("vn.mytvnet.b2cott", timeline.getSessions().get(1).getPackageName());
        Assert.assertTrue(timeline.getSessions().get(1).isTracked());
        Assert.assertEquals(100, timeline.getSessions().get(1).getDurationSec());

        Assert.assertEquals(100, timeline.getTotalTrackedSec());
        Assert.assertEquals(120, timeline.getTotalUntrackedSec());
    }
}
