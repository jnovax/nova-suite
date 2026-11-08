package vn.familytv.parentalcontrol.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundEventFilterTest {
    @Test fun ignoresEventsWithoutAnIdentifiablePackage() {
        assertFalse(ForegroundEventFilter.accept(null, "android.view.View"))
        assertFalse(ForegroundEventFilter.accept("", "android.view.View"))
        assertFalse(ForegroundEventFilter.accept("  ", "android.view.View"))
    }

    @Test fun ignoresOwnOverlayWithoutIgnoringRealActivities() {
        assertFalse(ForegroundEventFilter.accept("vn.familytv.parentalcontrol", "android.widget.TextView"))
        assertFalse(ForegroundEventFilter.accept("vn.familytv.parentalcontrol", "vn.familytv.parentalcontrol.timelock.TimeLockOverlayLayout"))
        assertTrue(ForegroundEventFilter.accept("vn.familytv.parentalcontrol", "vn.familytv.parentalcontrol.MainActivity"))
        assertTrue(ForegroundEventFilter.accept("vn.familytv.parentalcontrol", "vn.familytv.parentalcontrol.auth.RemoteAuthActivity"))
    }

    @Test fun acceptsMyTvAndTizenTubeWindows() {
        assertTrue(ForegroundEventFilter.accept("vn.mytvnet.b2cott", "some.Activity"))
        assertTrue(ForegroundEventFilter.accept("io.gh.reisxd.tizentube.cobalt", "some.Activity"))
    }

    @Test fun ignoresSystemUiAndVolumeOverlays() {
        assertFalse(ForegroundEventFilter.accept("com.android.systemui", "android.widget.FrameLayout"))
        assertFalse(ForegroundEventFilter.accept("com.android.systemui", "com.android.systemui.volume.VolumeDialogImpl\$CustomDialog"))
        assertFalse(ForegroundEventFilter.accept("com.android.systemui", "com.android.systemui.usb.tv.TvSensorPrivacyChangedActivity"))
        assertFalse(ForegroundEventFilter.accept("android", "com.android.internal.app.ResolverActivity"))
    }

    @Test fun ignoresToastsAndInputMethods() {
        assertFalse(ForegroundEventFilter.accept("com.android.systemui", "android.widget.Toast\$TN"))
        assertFalse(ForegroundEventFilter.accept("io.gh.reisxd.tizentube.cobalt", "android.widget.Toast"))
        assertFalse(ForegroundEventFilter.accept("com.tcl.inputmethod.international", "android.inputmethodservice.SoftInputWindow"))
        assertFalse(ForegroundEventFilter.accept("com.google.android.inputmethod.latin", "com.android.inputmethod.latin.LatinIME"))
    }

    @Test fun ignoresTvSettingsOverlayViewsAndDialogsButAcceptsSettingsActivities() {
        assertFalse(ForegroundEventFilter.accept("com.android.tv.settings", "android.widget.FrameLayout"))
        assertFalse(ForegroundEventFilter.accept("com.android.tv.settings", "com.tcl.settings.quicksettings.QSOptionMenusDialog"))
        assertTrue(ForegroundEventFilter.accept("com.android.tv.settings", "com.android.tv.settings.MainSettings"))
        assertTrue(ForegroundEventFilter.accept("com.android.tv.settings", "com.tcl.settings.accessories.AccessoriesActivity"))
    }
}
