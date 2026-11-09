package vn.familytv.parentalcontrol.ipc;

import android.content.Intent;
import org.junit.Assert;
import org.junit.Test;

public class ModeBroadcastSenderTest {
    @Test
    public void testBuildBroadcastIntent() {
        Intent intent = ModeBroadcastSender.createIntent(
                Mode.PARENT,
                180,
                true
        );
        Assert.assertEquals("vn.familytv.parentalcontrol.ACTION_MODE_CHANGED", intent.getAction());
        Assert.assertEquals("PARENT", intent.getStringExtra("extra_mode"));
        Assert.assertEquals(180, intent.getIntExtra("extra_session_remaining_sec", -1));
        Assert.assertTrue(intent.getBooleanExtra("extra_is_quiz_active", false));
    }
}
