package vn.familytv.parentalcontrol.platform;

import org.junit.Assert;
import org.junit.Test;

public class ForegroundEventFilterTest {
    @Test
    public void ignoresEventsWithoutAnIdentifiablePackage() {
        Assert.assertFalse(ForegroundEventFilter.accept(null, "android.view.View"));
        Assert.assertFalse(ForegroundEventFilter.accept("", "android.view.View"));
        Assert.assertFalse(ForegroundEventFilter.accept("  ", "android.view.View"));
    }

    @Test
    public void ignoresOwnOverlayWithoutIgnoringRealActivities() {
        Assert.assertFalse(ForegroundEventFilter.accept("vn.familytv.parentalcontrol", "android.widget.TextView"));
        Assert.assertFalse(ForegroundEventFilter.accept("vn.familytv.parentalcontrol", "vn.familytv.parentalcontrol.timelock.TimeLockOverlayLayout"));
        Assert.assertTrue(ForegroundEventFilter.accept("vn.familytv.parentalcontrol", "vn.familytv.parentalcontrol.MainActivity"));
        Assert.assertTrue(ForegroundEventFilter.accept("vn.familytv.parentalcontrol", "vn.familytv.parentalcontrol.auth.RemoteAuthActivity"));
    }

    @Test
    public void acceptsMyTvAndTizenTubeWindows() {
        Assert.assertTrue(ForegroundEventFilter.accept("vn.mytvnet.b2cott", "some.Activity"));
        Assert.assertTrue(ForegroundEventFilter.accept("io.gh.reisxd.tizentube.cobalt", "some.Activity"));
    }

    @Test
    public void ignoresSystemUiAndVolumeOverlays() {
        Assert.assertFalse(ForegroundEventFilter.accept("com.android.systemui", "android.widget.FrameLayout"));
        Assert.assertFalse(ForegroundEventFilter.accept("com.android.systemui", "com.android.systemui.volume.VolumeDialogImpl$CustomDialog"));
        Assert.assertFalse(ForegroundEventFilter.accept("com.android.systemui", "com.android.systemui.usb.tv.TvSensorPrivacyChangedActivity"));
        Assert.assertFalse(ForegroundEventFilter.accept("android", "com.android.internal.app.ResolverActivity"));
    }

    @Test
    public void ignoresToastsAndInputMethods() {
        Assert.assertFalse(ForegroundEventFilter.accept("com.android.systemui", "android.widget.Toast$TN"));
        Assert.assertFalse(ForegroundEventFilter.accept("io.gh.reisxd.tizentube.cobalt", "android.widget.Toast"));
        Assert.assertFalse(ForegroundEventFilter.accept("com.tcl.inputmethod.international", "android.inputmethodservice.SoftInputWindow"));
        Assert.assertFalse(ForegroundEventFilter.accept("com.google.android.inputmethod.latin", "com.android.inputmethod.latin.LatinIME"));
    }

    @Test
    public void ignoresTvSettingsOverlayViewsAndDialogsButAcceptsSettingsActivities() {
        Assert.assertFalse(ForegroundEventFilter.accept("com.android.tv.settings", "android.widget.FrameLayout"));
        Assert.assertFalse(ForegroundEventFilter.accept("com.android.tv.settings", "com.tcl.settings.quicksettings.QSOptionMenusDialog"));
        Assert.assertTrue(ForegroundEventFilter.accept("com.android.tv.settings", "com.android.tv.settings.MainSettings"));
        Assert.assertTrue(ForegroundEventFilter.accept("com.android.tv.settings", "com.tcl.settings.accessories.AccessoriesActivity"));
    }
}
