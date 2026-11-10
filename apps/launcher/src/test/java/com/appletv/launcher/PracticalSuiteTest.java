package com.appletv.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PracticalSuiteTest {

    @Test
    public void csvListRoundTripPreservesOrderAndIgnoresBlanks() {
        List<String> input = Arrays.asList("app.one", "app.two", "app.three");
        String csv = Config.toCsvList(input);
        assertEquals("app.one,app.two,app.three", csv);
        assertEquals(input, Config.parseCsvList(csv));
        assertEquals(Collections.emptyList(), Config.parseCsvList(" , , "));
    }

    @Test
    public void pinAndUnpinDockRespectsMaxColumns() {
        List<String> dock = Arrays.asList("a.pkg", "b.pkg");
        List<String> pinned = Config.pinToDock(dock, "c.pkg", 4);
        assertEquals(Arrays.asList("a.pkg", "b.pkg", "c.pkg"), pinned);

        // Cannot exceed maxColumns (3)
        List<String> capped = Config.pinToDock(pinned, "d.pkg", 3);
        assertEquals(Arrays.asList("a.pkg", "b.pkg", "c.pkg"), capped);

        List<String> unpinned = Config.unpinFromDock(pinned, "b.pkg");
        assertEquals(Arrays.asList("a.pkg", "c.pkg"), unpinned);
    }

    @Test
    public void moveInDockSwapsLeftAndRightSafely() {
        List<String> dock = Arrays.asList("a.pkg", "b.pkg", "c.pkg");
        assertEquals(Arrays.asList("b.pkg", "a.pkg", "c.pkg"), Config.moveInDock(dock, "b.pkg", -1));
        assertEquals(Arrays.asList("a.pkg", "c.pkg", "b.pkg"), Config.moveInDock(dock, "b.pkg", 1));
        // Out of bounds moves leave list unchanged
        assertEquals(dock, Config.moveInDock(dock, "a.pkg", -1));
        assertEquals(dock, Config.moveInDock(dock, "c.pkg", 1));
    }

    @Test
    public void canHidePackageBlocksLauncherAndParentalControl() {
        assertFalse(Config.canHidePackage("com.appletv.launcher", "com.appletv.launcher"));
        assertFalse(Config.canHidePackage("vn.familytv.parentalcontrol", "com.appletv.launcher"));
        assertTrue(Config.canHidePackage("org.courville.nova", "com.appletv.launcher"));
    }

    @Test
    public void cyclesSleepTimerAndNightDimmerCorrectly() {
        assertEquals(15, Config.nextSleepTimerMinutes(0));
        assertEquals(30, Config.nextSleepTimerMinutes(15));
        assertEquals(60, Config.nextSleepTimerMinutes(30));
        assertEquals(90, Config.nextSleepTimerMinutes(60));
        assertEquals(0, Config.nextSleepTimerMinutes(90));

        assertEquals(25, Config.nextNightDimmerLevel(0));
        assertEquals(50, Config.nextNightDimmerLevel(25));
        assertEquals(70, Config.nextNightDimmerLevel(50));
        assertEquals(0, Config.nextNightDimmerLevel(70));
    }

    @Test
    public void evaluatesNightDimmerScheduleCorrectly() {
        // Mode OFF always returns false
        assertFalse(Config.isDimmerTime(Config.DIMMER_MODE_OFF, 22, 21, 6));
        assertFalse(Config.isDimmerTime(Config.DIMMER_MODE_OFF, 2, 21, 6));
        assertFalse(Config.isDimmerTime(Config.DIMMER_MODE_OFF, 14, 21, 6));

        // Mode ALWAYS_ON always returns true
        assertTrue(Config.isDimmerTime(Config.DIMMER_MODE_ALWAYS_ON, 14, 21, 6));
        assertTrue(Config.isDimmerTime(Config.DIMMER_MODE_ALWAYS_ON, 22, 21, 6));

        // Mode SCHEDULED overnight (21:00 -> 06:00)
        assertFalse(Config.isDimmerTime(Config.DIMMER_MODE_SCHEDULED, 20, 21, 6));
        assertTrue(Config.isDimmerTime(Config.DIMMER_MODE_SCHEDULED, 21, 21, 6));
        assertTrue(Config.isDimmerTime(Config.DIMMER_MODE_SCHEDULED, 23, 21, 6));
        assertTrue(Config.isDimmerTime(Config.DIMMER_MODE_SCHEDULED, 0, 21, 6));
        assertTrue(Config.isDimmerTime(Config.DIMMER_MODE_SCHEDULED, 5, 21, 6));
        assertFalse(Config.isDimmerTime(Config.DIMMER_MODE_SCHEDULED, 6, 21, 6));
        assertFalse(Config.isDimmerTime(Config.DIMMER_MODE_SCHEDULED, 12, 21, 6));
    }

    @Test
    public void protectsNavigationAndVolumeKeysFromRemapping() {
        assertTrue(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_DPAD_UP));
        assertTrue(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_DPAD_CENTER));
        assertTrue(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_BACK));
        assertTrue(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_HOME));
        assertTrue(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_VOLUME_UP));
        assertTrue(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_VOLUME_DOWN));
        assertTrue(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_POWER));

        assertFalse(Config.isProtectedRemoteKey(190));  // YouTube
        assertFalse(Config.isProtectedRemoteKey(4062)); // Netflix
        assertFalse(Config.isProtectedRemoteKey(KeyEvent.KEYCODE_PROG_RED));
    }
}
