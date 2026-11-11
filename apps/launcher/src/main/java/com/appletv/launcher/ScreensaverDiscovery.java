package com.appletv.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public final class ScreensaverDiscovery {
    private static final String TAG = "ScreensaverDiscovery";

    public static final String ID_OFF = "OFF";
    public static final String ID_WALLPAPERS = "WALLPAPERS";

    public static final class ScreensaverOption {
        public final String id;
        public final String label;
        public final String packageName;
        public final String serviceName;
        public final String activityName;
        public final Drawable icon;
        public final boolean isInternal;

        public ScreensaverOption(String id, String label, String packageName, String serviceName,
                                 String activityName, Drawable icon, boolean isInternal) {
            this.id = id;
            this.label = label;
            this.packageName = packageName;
            this.serviceName = serviceName;
            this.activityName = activityName;
            this.icon = icon;
            this.isInternal = isInternal;
        }

        public static ScreensaverOption createOffOption(String label) {
            return new ScreensaverOption(ID_OFF, label, null, null, null, null, true);
        }

        public static ScreensaverOption createWallpapersOption(String label) {
            return new ScreensaverOption(ID_WALLPAPERS, label, null, null, null, null, true);
        }
    }

    private ScreensaverDiscovery() {}

    public static List<ScreensaverOption> getAvailableScreensavers(Context context) {
        List<ScreensaverOption> options = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        Intent dreamIntent = new Intent("android.service.dreams.DreamService");
        List<ResolveInfo> services = pm.queryIntentServices(dreamIntent, PackageManager.GET_META_DATA);

        if (services != null) {
            for (ResolveInfo ri : services) {
                if (ri.serviceInfo == null) continue;
                String pkg = ri.serviceInfo.packageName;
                String serviceClass = ri.serviceInfo.name;

                // Skip launcher's internal ClockDream if present
                if (context.getPackageName().equals(pkg)) continue;

                String label = ri.loadLabel(pm).toString();
                Drawable icon = ri.loadIcon(pm);

                String id = pkg + "/" + serviceClass;
                String activityClass = findCompanionActivity(pm, pkg);

                options.add(new ScreensaverOption(id, label, pkg, serviceClass, activityClass, icon, false));
            }
        }

        return options;
    }

    public static String findCompanionActivity(PackageManager pm, String packageName) {
        try {
            // 1. Look for custom screensaver activity category
            Intent customIntent = new Intent(Intent.ACTION_MAIN)
                    .addCategory("com.appletv.launcher.category.SCREENSAVER")
                    .setPackage(packageName);
            List<ResolveInfo> customActivities = pm.queryIntentActivities(customIntent, 0);
            if (customActivities != null && !customActivities.isEmpty()) {
                return customActivities.get(0).activityInfo.name;
            }

            // 2. Look for Leanback or standard launcher activity
            Intent launchIntent = pm.getLeanbackLaunchIntentForPackage(packageName);
            if (launchIntent == null) {
                launchIntent = pm.getLaunchIntentForPackage(packageName);
            }
            if (launchIntent != null && launchIntent.getComponent() != null) {
                return launchIntent.getComponent().getClassName();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed finding companion activity for " + packageName + ": " + e.getMessage());
        }
        return null;
    }

    public static boolean isPackageInstalled(Context context, String packageName) {
        if (packageName == null) return false;
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void launchScreensaver(Context context) {
        int mode = ScreensaverPreferences.mode(context);
        if (mode == ScreensaverPreferences.MODE_OFF) return;

        if (mode == ScreensaverPreferences.MODE_WALLPAPERS) {
            launchInternalWallpapers(context);
            return;
        }

        // Mode is EXTERNAL
        String component = ScreensaverPreferences.selectedComponent(context);
        String activity = ScreensaverPreferences.selectedActivity(context);
        String pkg = ScreensaverPreferences.extractPackage(component);

        if (pkg != null && isPackageInstalled(context, pkg)) {
            if (activity != null) {
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(pkg, activity));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return;
                } catch (Exception e) {
                    Log.w(TAG, "Failed launching screensaver activity " + activity + ", fallback to Somnambulator: " + e.getMessage());
                }
            }

            // Fallback to system dream trigger
            try {
                Intent dreamIntent = new Intent(Intent.ACTION_MAIN);
                dreamIntent.addCategory("android.intent.category.DREAM");
                dreamIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(dreamIntent);
                return;
            } catch (Exception e) {
                Log.w(TAG, "Failed launching system dream: " + e.getMessage());
            }
        }

        // Fallback gracefully to internal wallpapers if uninstalled or failed
        Log.w(TAG, "External screensaver unavailable, falling back to 4K Wallpapers");
        launchInternalWallpapers(context);
    }

    private static void launchInternalWallpapers(Context context) {
        try {
            Intent intent = new Intent(context, ScreenSaverActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch internal ScreenSaverActivity", e);
        }
    }
}
