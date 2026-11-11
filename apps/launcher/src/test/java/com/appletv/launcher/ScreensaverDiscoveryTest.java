package com.appletv.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScreensaverDiscoveryTest {

    @Test
    public void testScreensaverOptionModel() {
        ScreensaverDiscovery.ScreensaverOption opt = new ScreensaverDiscovery.ScreensaverOption(
                "flipclock",
                "FlipClock",
                "com.appletv.screensaver.flipclock",
                "com.appletv.screensaver.flipclock.FlipClockDreamService",
                "com.appletv.screensaver.flipclock.FlipClockActivity",
                null,
                false
        );

        assertEquals("flipclock", opt.id);
        assertEquals("FlipClock", opt.label);
        assertEquals("com.appletv.screensaver.flipclock", opt.packageName);
        assertEquals("com.appletv.screensaver.flipclock.FlipClockDreamService", opt.serviceName);
        assertEquals("com.appletv.screensaver.flipclock.FlipClockActivity", opt.activityName);
        assertFalse(opt.isInternal);
    }

    @Test
    public void testInternalWallpaperOption() {
        ScreensaverDiscovery.ScreensaverOption opt = ScreensaverDiscovery.ScreensaverOption.createWallpapersOption("4K Wallpapers");
        assertTrue(opt.isInternal);
        assertEquals(ScreensaverDiscovery.ID_WALLPAPERS, opt.id);
    }

    @Test
    public void testOffOption() {
        ScreensaverDiscovery.ScreensaverOption opt = ScreensaverDiscovery.ScreensaverOption.createOffOption("Off");
        assertTrue(opt.isInternal);
        assertEquals(ScreensaverDiscovery.ID_OFF, opt.id);
    }
}
