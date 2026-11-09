package vn.familytv.parentalcontrol.ipc;

import android.database.Cursor;
import org.junit.Assert;
import org.junit.Test;

public class ModeContentProviderTest {
    @Test
    public void testProviderCursorColumnsAndValues() {
        Cursor cursor = ModeContentProvider.buildCursor(
                Mode.PARENT,
                300,
                true,
                600
        );
        Assert.assertNotNull(cursor);

        String[] expectedColumns = new String[]{
                ModeContentProvider.COL_CURRENT_MODE,
                ModeContentProvider.COL_SESSION_REMAINING_SEC,
                ModeContentProvider.COL_IS_QUIZ_ACTIVE,
                ModeContentProvider.COL_SCREENTIME_REMAINING_SEC
        };
        Assert.assertArrayEquals(expectedColumns, cursor.getColumnNames());
        Assert.assertEquals(4, cursor.getColumnCount());

        Assert.assertEquals("current_mode", cursor.getColumnName(0));
        Assert.assertEquals("session_remaining_sec", cursor.getColumnName(1));
        Assert.assertEquals("is_quiz_active", cursor.getColumnName(2));
        Assert.assertEquals("screentime_remaining_sec", cursor.getColumnName(3));

        cursor.moveToFirst();
        Assert.assertEquals("PARENT", cursor.getString(0));
        Assert.assertEquals(300, cursor.getInt(1));
        Assert.assertEquals(1, cursor.getInt(2));
        Assert.assertEquals(600, cursor.getInt(3));
    }

    @Test
    public void testProviderKidModeDefaults() {
        Cursor cursor = ModeContentProvider.buildCursor(
                Mode.KID,
                0,
                false,
                900
        );
        Assert.assertNotNull(cursor);
        cursor.moveToFirst();
        Assert.assertEquals("KID", cursor.getString(0));
        Assert.assertEquals(0, cursor.getInt(1));
        Assert.assertEquals(0, cursor.getInt(2));
        Assert.assertEquals(900, cursor.getInt(3));
    }
}
