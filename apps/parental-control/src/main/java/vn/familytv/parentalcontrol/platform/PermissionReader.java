package vn.familytv.parentalcontrol.platform;

import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Process;
import android.provider.Settings;

public class PermissionReader {
    private final Context context;

    public PermissionReader(Context context) {
        this.context = context;
    }

    @SuppressWarnings("deprecation")
    public PermissionSnapshot read() {
        ContentResolver resolver = context.getContentResolver();
        ComponentName target = new ComponentName(context, ForegroundMonitorService.class);

        int a11yInt = Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0);
        String enabledServices = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean accessibilityEnabled = false;
        if (a11yInt == 1 && enabledServices != null) {
            String[] parts = enabledServices.split(":");
            for (String part : parts) {
                if (target.equals(ComponentName.unflattenFromString(part))) {
                    accessibilityEnabled = true;
                    break;
                }
            }
        }

        AppOpsManager ops = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        boolean usageAccess = false;
        if (ops != null) {
            int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
            usageAccess = (mode == AppOpsManager.MODE_ALLOWED);
        }

        return new PermissionSnapshot(accessibilityEnabled, ForegroundMonitorService.connected, usageAccess);
    }
}
