package com.appletv.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScreensaverPreferencesTest {

    @Test
    public void validatesModeAndTimeout() {
        assertEquals(ScreensaverPreferences.MODE_OFF, ScreensaverPreferences.validMode(ScreensaverPreferences.MODE_OFF));
        assertEquals(ScreensaverPreferences.MODE_WALLPAPERS, ScreensaverPreferences.validMode(ScreensaverPreferences.MODE_WALLPAPERS));
        assertEquals(ScreensaverPreferences.MODE_EXTERNAL, ScreensaverPreferences.validMode(ScreensaverPreferences.MODE_EXTERNAL));
        assertEquals(ScreensaverPreferences.MODE_WALLPAPERS, ScreensaverPreferences.validMode(-1));
        assertEquals(ScreensaverPreferences.MODE_WALLPAPERS, ScreensaverPreferences.validMode(99));

        assertEquals(0, ScreensaverPreferences.validTimeout(0));
        assertEquals(2, ScreensaverPreferences.validTimeout(2));
        assertEquals(5, ScreensaverPreferences.validTimeout(5));
        assertEquals(10, ScreensaverPreferences.validTimeout(10));
        assertEquals(15, ScreensaverPreferences.validTimeout(15));
        assertEquals(5, ScreensaverPreferences.validTimeout(99));
    }

    @Test
    public void idleMustHaveBothEnabledModeAndTimeout() {
        assertEquals(0L, ScreensaverPreferences.idleDelayMillis(ScreensaverPreferences.MODE_OFF, 5));
        assertEquals(0L, ScreensaverPreferences.idleDelayMillis(ScreensaverPreferences.MODE_WALLPAPERS, 0));
        assertEquals(120000L, ScreensaverPreferences.idleDelayMillis(ScreensaverPreferences.MODE_WALLPAPERS, 2));
        assertEquals(300000L, ScreensaverPreferences.idleDelayMillis(ScreensaverPreferences.MODE_EXTERNAL, 5));
    }

    @Test
    public void componentStringParsing() {
        String comp = "com.appletv.screensaver.flipclock/.FlipClockDreamService";
        assertEquals("com.appletv.screensaver.flipclock", ScreensaverPreferences.extractPackage(comp));
        assertEquals(".FlipClockDreamService", ScreensaverPreferences.extractClass(comp));

        assertNull(ScreensaverPreferences.extractPackage(null));
        assertNull(ScreensaverPreferences.extractPackage("invalidString"));
    }

    @Test
    public void remoteMapperRecognizesScreensaverAction() {
        assertEquals("screensaver", HomeKeyService.getActionType("screensaver"));
    }
}
