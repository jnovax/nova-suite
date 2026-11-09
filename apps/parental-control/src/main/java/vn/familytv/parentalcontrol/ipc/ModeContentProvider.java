package vn.familytv.parentalcontrol.ipc;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class ModeContentProvider extends ContentProvider {
    public static final String AUTHORITY = "vn.familytv.parentalcontrol.provider";
    public static final Uri CONTENT_URI;
    static {
        Uri uri;
        try {
            uri = Uri.parse("content://" + AUTHORITY + "/status");
        } catch (RuntimeException ignored) {
            uri = Uri.EMPTY;
        }
        CONTENT_URI = uri;
    }

    public static final String COL_CURRENT_MODE = "current_mode";
    public static final String COL_SESSION_REMAINING_SEC = "session_remaining_sec";
    public static final String COL_IS_QUIZ_ACTIVE = "is_quiz_active";
    public static final String COL_SCREENTIME_REMAINING_SEC = "screentime_remaining_sec";

    public static final String[] COLUMNS = new String[]{
            COL_CURRENT_MODE,
            COL_SESSION_REMAINING_SEC,
            COL_IS_QUIZ_ACTIVE,
            COL_SCREENTIME_REMAINING_SEC
    };

    public static Cursor buildCursor(Mode mode, int sessionRemainingSec, boolean isQuizActive, int screenTimeRemainingSec) {
        Object[] values = new Object[]{
                mode != null ? mode.name() : "KID",
                sessionRemainingSec,
                isQuizActive ? 1 : 0,
                screenTimeRemainingSec
        };
        try {
            MatrixCursor cursor = new MatrixCursor(COLUMNS);
            cursor.addRow(values);
            return cursor;
        } catch (RuntimeException ignored) {
            return createStatusCursor(COLUMNS, values);
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        ModeStateManager manager = ModeStateManager.instance;
        long now = System.currentTimeMillis();
        return buildCursor(
                manager.getMode(),
                manager.getSessionRemainingSec(now),
                manager.isQuizActive(),
                manager.getScreenTimeRemainingSec()
        );
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd.familytv.status";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private static Cursor createStatusCursor(final String[] cols, final Object[] vals) {
        return (Cursor) Proxy.newProxyInstance(
                Cursor.class.getClassLoader(),
                new Class<?>[]{Cursor.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        switch (name) {
                            case "getColumnNames":
                                return cols;
                            case "getColumnName":
                                return cols[(Integer) args[0]];
                            case "getColumnCount":
                                return cols.length;
                            case "getColumnIndex":
                                String colName = (String) args[0];
                                for (int i = 0; i < cols.length; i++) {
                                    if (cols[i].equals(colName)) return i;
                                }
                                return -1;
                            case "getColumnIndexOrThrow":
                                String reqCol = (String) args[0];
                                for (int i = 0; i < cols.length; i++) {
                                    if (cols[i].equals(reqCol)) return i;
                                }
                                throw new IllegalArgumentException("Column not found: " + reqCol);
                            case "getCount":
                                return 1;
                            case "moveToFirst":
                                return true;
                            case "moveToNext":
                                return false;
                            case "getString":
                                Object v = vals[(Integer) args[0]];
                                return v != null ? v.toString() : null;
                            case "getInt":
                                Object vi = vals[(Integer) args[0]];
                                return vi instanceof Number ? ((Number) vi).intValue() : Integer.parseInt(vi.toString());
                            case "getLong":
                                Object vl = vals[(Integer) args[0]];
                                return vl instanceof Number ? ((Number) vl).longValue() : Long.parseLong(vl.toString());
                            case "close":
                                return null;
                            case "isClosed":
                                return false;
                            default:
                                return null;
                        }
                    }
                }
        );
    }
}
