package vn.familytv.parentalcontrol.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import com.appletv.contract.StatusProviderContract;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import vn.familytv.parentalcontrol.ipc.Mode;
import vn.familytv.parentalcontrol.ipc.ModeStateManager;

public class StatusProvider extends ContentProvider {
    public ModeStateManager stateManager = ModeStateManager.instance;

    public static final String AUTHORITY = StatusProviderContract.AUTHORITY;
    public static final String STATUS_PATH = StatusProviderContract.STATUS_PATH;
    public static final String CONTENT_URI_STRING = StatusProviderContract.CONTENT_URI_STRING;

    public static final Uri CONTENT_URI;
    static {
        Uri uri;
        try {
            uri = Uri.parse(CONTENT_URI_STRING);
        } catch (Throwable ignored) {
            uri = Uri.EMPTY;
        }
        CONTENT_URI = uri;
    }

    public static final long DEFAULT_SESSION_LIMIT_SECONDS = 900L;
    public static final String DEFAULT_WHITELIST_PACKAGES =
            "com.google.android.youtube.tvkids,org.smarttube.stable,org.schabi.newpipe,org.videolan.vlc,org.khanacademy.android,com.duolingo,vn.familytv.parentalcontrol";

    public static final String[] COLUMNS = new String[]{
            StatusProviderContract.COLUMN_CURRENT_MODE,
            StatusProviderContract.COLUMN_REMAINING_SECONDS,
            StatusProviderContract.COLUMN_SESSION_LIMIT_SECONDS,
            StatusProviderContract.COLUMN_WHITELIST_PACKAGES
    };

    public static Cursor buildCursor(Mode mode) {
        return buildCursor(mode != null ? mode.name() : "KID", DEFAULT_SESSION_LIMIT_SECONDS, DEFAULT_SESSION_LIMIT_SECONDS, DEFAULT_WHITELIST_PACKAGES, null);
    }

    public static Cursor buildCursor(Mode mode, long remainingSeconds) {
        return buildCursor(mode != null ? mode.name() : "KID", remainingSeconds, DEFAULT_SESSION_LIMIT_SECONDS, DEFAULT_WHITELIST_PACKAGES, null);
    }

    public static Cursor buildCursor(Mode mode, long remainingSeconds, long sessionLimitSeconds, String whitelistPackages) {
        return buildCursor(mode != null ? mode.name() : "KID", remainingSeconds, sessionLimitSeconds, whitelistPackages, null);
    }

    public static Cursor buildCursor(Mode mode, long remainingSeconds, long sessionLimitSeconds, String whitelistPackages, String[] projection) {
        return buildCursor(mode != null ? mode.name() : "KID", remainingSeconds, sessionLimitSeconds, whitelistPackages, projection);
    }

    public static Cursor buildCursor(String mode, long remainingSeconds, long sessionLimitSeconds, String whitelistPackages, String[] projection) {
        String[] requestedCols = (projection != null && projection.length > 0) ? projection : COLUMNS;
        Object[] values = new Object[requestedCols.length];

        for (int i = 0; i < requestedCols.length; i++) {
            String col = requestedCols[i];
            if (StatusProviderContract.COLUMN_CURRENT_MODE.equals(col)) {
                values[i] = mode;
            } else if (StatusProviderContract.COLUMN_REMAINING_SECONDS.equals(col)) {
                values[i] = remainingSeconds;
            } else if (StatusProviderContract.COLUMN_SESSION_LIMIT_SECONDS.equals(col)) {
                values[i] = sessionLimitSeconds;
            } else if (StatusProviderContract.COLUMN_WHITELIST_PACKAGES.equals(col)) {
                values[i] = whitelistPackages;
            } else {
                values[i] = "";
            }
        }

        try {
            MatrixCursor cursor = new MatrixCursor(requestedCols);
            cursor.addRow(values);
            return cursor;
        } catch (Throwable ignored) {
            return createStatusCursor(requestedCols, values);
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    public Cursor queryStatus() {
        return queryStatus(null);
    }

    public Cursor queryStatus(String[] projection) {
        ModeStateManager manager = stateManager != null ? stateManager : ModeStateManager.instance;
        if (manager == ModeStateManager.instance && getContext() != null) {
            manager.loadState(getContext());
        }
        Mode mode = manager.getMode();
        long remainingSeconds = manager.getScreenTimeRemainingSec();
        return buildCursor(mode, remainingSeconds, DEFAULT_SESSION_LIMIT_SECONDS, DEFAULT_WHITELIST_PACKAGES, projection);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return queryStatus(projection);
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
                                return vi instanceof Number ? ((Number) vi).intValue() : Integer.parseInt(vi != null ? vi.toString() : "0");
                            case "getLong":
                                Object vl = vals[(Integer) args[0]];
                                return vl instanceof Number ? ((Number) vl).longValue() : Long.parseLong(vl != null ? vl.toString() : "0");
                            case "isNull":
                                return vals[(Integer) args[0]] == null;
                            case "close":
                                return null;
                            case "isClosed":
                                return false;
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "equals":
                                return args != null && args[0] == proxy;
                            default:
                                return null;
                        }
                    }
                }
        );
    }
}
