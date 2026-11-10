package vn.familytv.parentalcontrol.platform;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ForegroundEventFilter {
    private static final String OWN_PACKAGE = "vn.familytv.parentalcontrol";
    private static final String TV_SETTINGS_PACKAGE = "com.android.tv.settings";

    private static final Set<String> OWN_ACTIVITIES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            OWN_PACKAGE + ".MainActivity",
            OWN_PACKAGE + ".auth.RemoteAuthActivity"
    )));

    private static final Set<String> IGNORED_PACKAGES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "android",
            "com.android.systemui"
    )));

    private ForegroundEventFilter() {}

    public static boolean accept(String packageName, String className) {
        if (packageName == null || packageName.trim().isEmpty()) return false;
        String pkg = packageName.trim();
        String cls = className != null ? className.trim() : "";

        if (IGNORED_PACKAGES.contains(pkg)) return false;
        if (pkg.contains(".inputmethod") || "android.inputmethodservice.SoftInputWindow".equals(cls)) return false;
        if (cls.contains("Toast") || cls.contains("TransientNotification")) return false;

        if (OWN_PACKAGE.equals(pkg)) {
            return OWN_ACTIVITIES.contains(cls);
        }

        if (TV_SETTINGS_PACKAGE.equals(pkg)) {
            if (cls.startsWith("android.widget.") || cls.startsWith("android.view.") || cls.endsWith("Dialog")) {
                return false;
            }
        }

        return true;
    }
}
