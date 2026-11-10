package vn.familytv.parentalcontrol.ui.auth;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import vn.familytv.parentalcontrol.R;

public class DpadPinPadNavigationTest {

    @Test
    public void horizontalWrapAround_row0() {
        DpadPinPadNavRules.FocusRule r1 = DpadPinPadNavRules.calculateRule(R.id.pin_btn_1, 0);
        assertEquals(R.id.pin_btn_3, r1.leftId);
        assertEquals(R.id.pin_btn_2, r1.rightId);

        DpadPinPadNavRules.FocusRule r2 = DpadPinPadNavRules.calculateRule(R.id.pin_btn_2, 0);
        assertEquals(R.id.pin_btn_1, r2.leftId);
        assertEquals(R.id.pin_btn_3, r2.rightId);

        DpadPinPadNavRules.FocusRule r3 = DpadPinPadNavRules.calculateRule(R.id.pin_btn_3, 0);
        assertEquals(R.id.pin_btn_2, r3.leftId);
        assertEquals(R.id.pin_btn_1, r3.rightId);
    }

    @Test
    public void horizontalWrapAround_row1() {
        DpadPinPadNavRules.FocusRule r4 = DpadPinPadNavRules.calculateRule(R.id.pin_btn_4, 0);
        assertEquals(R.id.pin_btn_6, r4.leftId);
        assertEquals(R.id.pin_btn_5, r4.rightId);

        DpadPinPadNavRules.FocusRule r5 = DpadPinPadNavRules.calculateRule(R.id.pin_btn_5, 0);
        assertEquals(R.id.pin_btn_4, r5.leftId);
        assertEquals(R.id.pin_btn_6, r5.rightId);

        DpadPinPadNavRules.FocusRule r6 = DpadPinPadNavRules.calculateRule(R.id.pin_btn_6, 0);
        assertEquals(R.id.pin_btn_5, r6.leftId);
        assertEquals(R.id.pin_btn_4, r6.rightId);
    }

    @Test
    public void horizontalWrapAround_row2() {
        DpadPinPadNavRules.FocusRule r7 = DpadPinPadNavRules.calculateRule(R.id.pin_btn_7, 0);
        assertEquals(R.id.pin_btn_9, r7.leftId);
        assertEquals(R.id.pin_btn_8, r7.rightId);

        DpadPinPadNavRules.FocusRule r8 = DpadPinPadNavRules.calculateRule(R.id.pin_btn_8, 0);
        assertEquals(R.id.pin_btn_7, r8.leftId);
        assertEquals(R.id.pin_btn_9, r8.rightId);

        DpadPinPadNavRules.FocusRule r9 = DpadPinPadNavRules.calculateRule(R.id.pin_btn_9, 0);
        assertEquals(R.id.pin_btn_8, r9.leftId);
        assertEquals(R.id.pin_btn_7, r9.rightId);
    }

    @Test
    public void horizontalWrapAround_row3() {
        DpadPinPadNavRules.FocusRule rDel = DpadPinPadNavRules.calculateRule(R.id.pin_btn_del, 0);
        assertEquals(R.id.pin_btn_ok, rDel.leftId);
        assertEquals(R.id.pin_btn_0, rDel.rightId);

        DpadPinPadNavRules.FocusRule r0 = DpadPinPadNavRules.calculateRule(R.id.pin_btn_0, 0);
        assertEquals(R.id.pin_btn_del, r0.leftId);
        assertEquals(R.id.pin_btn_ok, r0.rightId);

        DpadPinPadNavRules.FocusRule rOk = DpadPinPadNavRules.calculateRule(R.id.pin_btn_ok, 0);
        assertEquals(R.id.pin_btn_0, rOk.leftId);
        assertEquals(R.id.pin_btn_del, rOk.rightId);
    }

    @Test
    public void verticalWrapAround_withoutActionButton() {
        // Up from Row 0 wraps to Row 3
        assertEquals(R.id.pin_btn_del, DpadPinPadNavRules.calculateRule(R.id.pin_btn_1, 0).upId);
        assertEquals(R.id.pin_btn_0, DpadPinPadNavRules.calculateRule(R.id.pin_btn_2, 0).upId);
        assertEquals(R.id.pin_btn_ok, DpadPinPadNavRules.calculateRule(R.id.pin_btn_3, 0).upId);

        // Down from Row 3 wraps to Row 0
        assertEquals(R.id.pin_btn_1, DpadPinPadNavRules.calculateRule(R.id.pin_btn_del, 0).downId);
        assertEquals(R.id.pin_btn_2, DpadPinPadNavRules.calculateRule(R.id.pin_btn_0, 0).downId);
        assertEquals(R.id.pin_btn_3, DpadPinPadNavRules.calculateRule(R.id.pin_btn_ok, 0).downId);

        // Interior vertical steps
        assertEquals(R.id.pin_btn_4, DpadPinPadNavRules.calculateRule(R.id.pin_btn_1, 0).downId);
        assertEquals(R.id.pin_btn_1, DpadPinPadNavRules.calculateRule(R.id.pin_btn_4, 0).upId);
        assertEquals(R.id.pin_btn_7, DpadPinPadNavRules.calculateRule(R.id.pin_btn_4, 0).downId);
        assertEquals(R.id.pin_btn_4, DpadPinPadNavRules.calculateRule(R.id.pin_btn_7, 0).upId);
        assertEquals(R.id.pin_btn_del, DpadPinPadNavRules.calculateRule(R.id.pin_btn_7, 0).downId);
        assertEquals(R.id.pin_btn_7, DpadPinPadNavRules.calculateRule(R.id.pin_btn_del, 0).upId);
    }

    @Test
    public void verticalWrapAround_withActionButton() {
        int actionBtnId = 99999;

        // Up from Row 0 directs to action button
        assertEquals(actionBtnId, DpadPinPadNavRules.calculateRule(R.id.pin_btn_1, actionBtnId).upId);
        assertEquals(actionBtnId, DpadPinPadNavRules.calculateRule(R.id.pin_btn_2, actionBtnId).upId);
        assertEquals(actionBtnId, DpadPinPadNavRules.calculateRule(R.id.pin_btn_3, actionBtnId).upId);

        // Down from Row 3 directs to action button
        assertEquals(actionBtnId, DpadPinPadNavRules.calculateRule(R.id.pin_btn_del, actionBtnId).downId);
        assertEquals(actionBtnId, DpadPinPadNavRules.calculateRule(R.id.pin_btn_0, actionBtnId).downId);
        assertEquals(actionBtnId, DpadPinPadNavRules.calculateRule(R.id.pin_btn_ok, actionBtnId).downId);

        // Interior buttons keep their vertical links
        assertEquals(R.id.pin_btn_2, DpadPinPadNavRules.calculateRule(R.id.pin_btn_5, actionBtnId).upId);
        assertEquals(R.id.pin_btn_8, DpadPinPadNavRules.calculateRule(R.id.pin_btn_5, actionBtnId).downId);
    }
}
