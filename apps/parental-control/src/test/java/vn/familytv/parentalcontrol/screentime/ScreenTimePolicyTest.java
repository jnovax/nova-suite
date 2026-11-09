package vn.familytv.parentalcontrol.screentime;

import org.junit.Assert;
import org.junit.Test;

public class ScreenTimePolicyTest {
    private final ScreenTimePolicy policy = new ScreenTimePolicy();

    @Test
    public void testYouTubePackagesAreTracked() {
        Assert.assertTrue(policy.isTrackedPackage("io.gh.reisxd.tizentube.cobalt"));
        Assert.assertTrue(policy.isTrackedPackage("org.smarttube.stable"));
        Assert.assertTrue(policy.isTrackedPackage("org.smarttube.beta"));
        Assert.assertTrue(policy.isTrackedPackage("com.teamsmart.videomanager.tv"));
        Assert.assertTrue(policy.isTrackedPackage("com.liskovsoft.smarttubetv.beta"));
        Assert.assertTrue(policy.isTrackedPackage("com.liskovsoft.videomanager"));
        Assert.assertTrue(policy.isTrackedPackage("com.google.android.youtube.tv"));
        Assert.assertTrue(policy.isTrackedPackage("com.google.android.youtube.tvkids"));
        Assert.assertTrue(policy.isTrackedPackage("com.google.android.youtube"));
        Assert.assertTrue(policy.isTrackedPackage("org.schabi.newpipe"));
    }

    @Test
    public void testMyTVPackagesAreTracked() {
        Assert.assertTrue(policy.isTrackedPackage("vn.mytvnet.b2cott"));
        Assert.assertTrue(policy.isTrackedPackage("vn.mytvnet.b2c"));
        Assert.assertTrue(policy.isTrackedPackage("vn.vnpt.media.mytv"));
    }

    @Test
    public void testNovaVideoPlayerIsTracked() {
        Assert.assertTrue(policy.isTrackedPackage("org.courville.nova"));
    }

    @Test
    public void testSpotifyPackagesAreTracked() {
        Assert.assertTrue(policy.isTrackedPackage("com.spotify.tv.android"));
        Assert.assertTrue(policy.isTrackedPackage("com.spotify.music"));
    }

    @Test
    public void testScreenSaverAndAmbientPackagesAreNotTracked() {
        Assert.assertFalse(policy.isTrackedPackage("com.google.android.backdrop"));
        Assert.assertFalse(policy.isTrackedPackage("com.google.android.apps.tv.dreamx"));
        Assert.assertFalse(policy.isTrackedPackage("com.tcl.ambient"));
        Assert.assertFalse(policy.isTrackedPackage("com.android.systemui"));
    }

    @Test
    public void testLauncherAndSettingsAndOtherAppsAreNotTracked() {
        Assert.assertFalse(policy.isTrackedPackage("com.appletv.launcher"));
        Assert.assertFalse(policy.isTrackedPackage("vn.familytv.parentalcontrol"));
        Assert.assertFalse(policy.isTrackedPackage("com.android.tv.settings"));
        Assert.assertFalse(policy.isTrackedPackage("com.netflix.ninja"));
        Assert.assertFalse(policy.isTrackedPackage("com.vocatv.app"));
        Assert.assertFalse(policy.isTrackedPackage(null));
        Assert.assertFalse(policy.isTrackedPackage(""));
        Assert.assertFalse(policy.isTrackedPackage("   "));
    }
}
