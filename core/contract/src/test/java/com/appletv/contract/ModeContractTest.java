package com.appletv.contract;

import org.junit.Assert;
import org.junit.Test;

public class ModeContractTest {
    @Test
    public void testContractConstants() {
        Assert.assertEquals("KID", ModeContract.MODE_KID);
        Assert.assertEquals("PARENT", ModeContract.MODE_PARENT);
        Assert.assertEquals("vn.familytv.parentalcontrol", ModeContract.TARGET_PACKAGE);
        Assert.assertEquals("vn.familytv.parentalcontrol.ACTION_MODE_CHANGED", ModeContract.ACTION_MODE_CHANGED);
        Assert.assertEquals("vn.familytv.parentalcontrol.ACTION_AUTHENTICATE", ModeContract.ACTION_AUTHENTICATE);
        Assert.assertEquals("vn.familytv.parentalcontrol.ACTION_LOCK_KID", ModeContract.ACTION_LOCK_KID);
        Assert.assertEquals("open_settings", ModeContract.EXTRA_OPEN_SETTINGS);
        Assert.assertEquals("extra_mode", ModeContract.EXTRA_MODE);
        Assert.assertEquals("extra_allowed_packages", ModeContract.EXTRA_ALLOWED_PACKAGES);
        Assert.assertEquals("extra_remaining_seconds", ModeContract.EXTRA_REMAINING_SECONDS);
    }
}
