package com.appletv.launcher;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class ModeUpdateTest {
    private final Set<String> allowed = new HashSet<>(Arrays.asList("video", "music"));

    @Test public void repeatedParentStateDoesNotRebuildAndStealFocus() {
        assertFalse(ModeManager.shouldReloadApps("PARENT", "PARENT", null, null));
    }

    @Test public void actualModeChangeRebuilds() {
        assertTrue(ModeManager.shouldReloadApps("KID", "PARENT", allowed, null));
        assertTrue(ModeManager.shouldReloadApps("PARENT", "KID", allowed, null));
    }

    @Test public void kidWhitelistChangesRebuildButOmittedOrEquivalentListDoesNot() {
        assertTrue(ModeManager.shouldReloadApps("KID", "KID", allowed, new HashSet<>(Arrays.asList("video"))));
        assertFalse(ModeManager.shouldReloadApps("KID", "KID", allowed, null));
        assertFalse(ModeManager.shouldReloadApps("KID", "KID", allowed, new HashSet<>(allowed)));
        assertFalse(ModeManager.shouldReloadApps("KID", "KID", null, new HashSet<>()));
    }

    @Test public void parentWhitelistUpdateDoesNotChangeVisibleApps() {
        assertFalse(ModeManager.shouldReloadApps("PARENT", "PARENT", allowed, new HashSet<>()));
    }
}
