package com.appletv.screensaver.flipclock;

import org.junit.Test;
import static org.junit.Assert.*;

public class FliqloTimeHelperTest {

    @Test
    public void test24HourFormat() {
        FliqloTimeHelper.TimeData data = FliqloTimeHelper.format(14, 5, true);
        assertEquals("14", data.hourStr);
        assertEquals("05", data.minStr);
        assertEquals("", data.amPm);
    }

    @Test
    public void test24HourMidnight() {
        FliqloTimeHelper.TimeData data = FliqloTimeHelper.format(0, 0, true);
        assertEquals("00", data.hourStr);
        assertEquals("00", data.minStr);
        assertEquals("", data.amPm);
    }

    @Test
    public void test12HourMidnightIs12AM() {
        FliqloTimeHelper.TimeData data = FliqloTimeHelper.format(0, 30, false);
        assertEquals("12", data.hourStr);
        assertEquals("30", data.minStr);
        assertEquals("AM", data.amPm);
    }

    @Test
    public void test12HourNoonIs12PM() {
        FliqloTimeHelper.TimeData data = FliqloTimeHelper.format(12, 15, false);
        assertEquals("12", data.hourStr);
        assertEquals("15", data.minStr);
        assertEquals("PM", data.amPm);
    }

    @Test
    public void test12HourAfternoon() {
        FliqloTimeHelper.TimeData data = FliqloTimeHelper.format(23, 59, false);
        assertEquals("11", data.hourStr);
        assertEquals("59", data.minStr);
        assertEquals("PM", data.amPm);
    }

    @Test
    public void test12HourMorning() {
        FliqloTimeHelper.TimeData data = FliqloTimeHelper.format(9, 7, false);
        assertEquals("09", data.hourStr);
        assertEquals("07", data.minStr);
        assertEquals("AM", data.amPm);
    }
}
