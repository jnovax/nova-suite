package vn.familytv.parentalcontrol.provider;

import android.database.Cursor;
import com.appletv.contract.StatusProviderContract;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import vn.familytv.parentalcontrol.ipc.Mode;
import vn.familytv.parentalcontrol.ipc.ModeStateManager;

public class StatusProviderTest {
    private ModeStateManager stateManager;
    private StatusProvider provider;

    @Before
    public void setUp() {
        stateManager = new ModeStateManager(300);
        stateManager.resetForTesting();
        provider = new StatusProvider();
        provider.stateManager = this.stateManager;
    }

    @Test
    public void testContractColumnsAlignment() {
        Cursor cursor = StatusProvider.buildCursor(
                Mode.KID,
                600L,
                900L,
                "com.pkg.one,com.pkg.two"
        );
        Assert.assertNotNull(cursor);

        String[] expectedColumns = new String[]{
                StatusProviderContract.COLUMN_CURRENT_MODE,
                StatusProviderContract.COLUMN_REMAINING_SECONDS,
                StatusProviderContract.COLUMN_SESSION_LIMIT_SECONDS,
                StatusProviderContract.COLUMN_WHITELIST_PACKAGES
        };
        Assert.assertArrayEquals(expectedColumns, cursor.getColumnNames());
        Assert.assertEquals(4, cursor.getColumnCount());
        Assert.assertEquals(1, cursor.getCount());

        Assert.assertEquals(StatusProviderContract.COLUMN_CURRENT_MODE, cursor.getColumnName(0));
        Assert.assertEquals(StatusProviderContract.COLUMN_REMAINING_SECONDS, cursor.getColumnName(1));
        Assert.assertEquals(StatusProviderContract.COLUMN_SESSION_LIMIT_SECONDS, cursor.getColumnName(2));
        Assert.assertEquals(StatusProviderContract.COLUMN_WHITELIST_PACKAGES, cursor.getColumnName(3));
    }

    @Test
    public void testKidModeValuesAndColumnIndices() {
        Cursor cursor = StatusProvider.buildCursor(
                Mode.KID,
                420L,
                900L,
                "com.google.android.youtube.tvkids,org.smarttube.stable"
        );

        Assert.assertTrue(cursor.moveToFirst());

        int modeIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_CURRENT_MODE);
        int remIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_REMAINING_SECONDS);
        int limitIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_SESSION_LIMIT_SECONDS);
        int whiteIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_WHITELIST_PACKAGES);

        Assert.assertEquals("KID", cursor.getString(modeIdx));
        Assert.assertEquals(420L, cursor.getLong(remIdx));
        Assert.assertEquals(420, cursor.getInt(remIdx));
        Assert.assertEquals(900L, cursor.getLong(limitIdx));
        Assert.assertEquals(900, cursor.getInt(limitIdx));
        Assert.assertTrue(cursor.getString(whiteIdx).contains("org.smarttube.stable"));
        Assert.assertFalse(cursor.moveToNext());
        cursor.close();
    }

    @Test
    public void testParentModeValues() {
        Cursor cursor = StatusProvider.buildCursor(
                Mode.PARENT,
                900L
        );

        Assert.assertTrue(cursor.moveToFirst());
        int modeIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_CURRENT_MODE);
        int remIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_REMAINING_SECONDS);

        Assert.assertEquals("PARENT", cursor.getString(modeIdx));
        Assert.assertEquals(900L, cursor.getLong(remIdx));
    }

    @Test
    public void testDefaultValues() {
        Cursor cursor = StatusProvider.buildCursor(Mode.KID);
        Assert.assertTrue(cursor.moveToFirst());

        int remIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_REMAINING_SECONDS);
        int limitIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_SESSION_LIMIT_SECONDS);
        int whiteIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_WHITELIST_PACKAGES);

        Assert.assertEquals(900L, cursor.getLong(remIdx));
        Assert.assertEquals(900L, cursor.getLong(limitIdx));
        Assert.assertTrue(cursor.getString(whiteIdx).contains("vn.familytv.parentalcontrol"));
    }

    @Test
    public void testProviderQueryConnectsToModeStateManager() {
        stateManager.setScreenTimeRemainingSec(540);

        Cursor cursor = provider.queryStatus();
        Assert.assertNotNull(cursor);
        Assert.assertTrue(cursor.moveToFirst());

        int modeIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_CURRENT_MODE);
        int remIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_REMAINING_SECONDS);
        int limitIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_SESSION_LIMIT_SECONDS);

        Assert.assertEquals("KID", cursor.getString(modeIdx));
        Assert.assertEquals(540L, cursor.getLong(remIdx));
        Assert.assertEquals(900L, cursor.getLong(limitIdx));
    }

    @Test
    public void testProviderQueryWhenParentAuthenticated() {
        stateManager.onParentAuthenticated(System.currentTimeMillis());
        stateManager.setScreenTimeRemainingSec(300);

        Cursor cursor = provider.queryStatus();
        Assert.assertTrue(cursor.moveToFirst());

        int modeIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_CURRENT_MODE);
        int remIdx = cursor.getColumnIndexOrThrow(StatusProviderContract.COLUMN_REMAINING_SECONDS);

        Assert.assertEquals("PARENT", cursor.getString(modeIdx));
        Assert.assertEquals(300L, cursor.getLong(remIdx));
    }

    @Test
    public void testQueryWithSpecificProjection() {
        stateManager.setScreenTimeRemainingSec(180);

        String[] projection = new String[]{StatusProviderContract.COLUMN_REMAINING_SECONDS};
        Cursor cursor = provider.queryStatus(projection);

        Assert.assertNotNull(cursor);
        Assert.assertEquals(1, cursor.getColumnCount());
        Assert.assertEquals(StatusProviderContract.COLUMN_REMAINING_SECONDS, cursor.getColumnName(0));
        Assert.assertTrue(cursor.moveToFirst());
        Assert.assertEquals(180L, cursor.getLong(0));
        Assert.assertEquals(0, cursor.getColumnIndex(StatusProviderContract.COLUMN_REMAINING_SECONDS));
        Assert.assertEquals(-1, cursor.getColumnIndex(StatusProviderContract.COLUMN_CURRENT_MODE));
    }

    @Test
    public void testColumnNotFoundThrowsException() {
        Cursor cursor = StatusProvider.buildCursor(Mode.KID);
        try {
            cursor.getColumnIndexOrThrow("non_existent_column");
            Assert.fail("Expected IllegalArgumentException for unknown column");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
