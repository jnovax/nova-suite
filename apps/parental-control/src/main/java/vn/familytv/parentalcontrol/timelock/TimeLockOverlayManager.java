package vn.familytv.parentalcontrol.timelock;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.WindowManager;
import java.util.function.Consumer;
import vn.familytv.parentalcontrol.MainActivity;
import vn.familytv.parentalcontrol.screentime.ScreenTimeStore;
import vn.familytv.parentalcontrol.security.SecurityStore;

public class TimeLockOverlayManager {
    private static final String TAG = "FamilyTime_TimeLock";

    private final AccessibilityService service;
    private final ScreenTimeStore screenTimeStore;
    private final SecurityStore securityStore;
    private final WindowManager windowManager;
    private TimeLockOverlayLayout overlayLayout;

    public TimeLockOverlayManager(
            AccessibilityService service,
            ScreenTimeStore screenTimeStore,
            SecurityStore securityStore
    ) {
        this.service = service;
        this.screenTimeStore = screenTimeStore;
        this.securityStore = securityStore;
        this.windowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
    }

    public boolean isShowing() {
        return overlayLayout != null;
    }

    public void showExtensionSelector() {
        if (overlayLayout != null) {
            overlayLayout.showExtensionSelectorView();
        }
    }

    @SuppressWarnings("deprecation")
    public void showTimeLock(
            int todayHours,
            Consumer<Integer> onExtended,
            Runnable onOpenParentConfig,
            Runnable onGoHome
    ) {
        if (overlayLayout != null) return;

        TimeLockOverlayLayout panel = new TimeLockOverlayLayout(
                service,
                securityStore,
                addedSec -> {
                    dismiss();
                    if (onExtended != null) {
                        onExtended.accept(addedSec);
                    }
                },
                () -> {
                    dismiss();
                    if (onOpenParentConfig != null) {
                        onOpenParentConfig.run();
                    } else {
                        Intent intent = new Intent(service, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        service.startActivity(intent);
                    }
                },
                () -> {
                    dismiss();
                    if (onGoHome != null) {
                        onGoHome.run();
                    } else {
                        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                        homeIntent.addCategory(Intent.CATEGORY_HOME);
                        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        service.startActivity(homeIntent);
                    }
                }
        );
        panel.setTodayLimitHours(todayHours);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
        );
        params.setTitle("FamilyTime TimeLock");

        try {
            windowManager.addView(panel, params);
            overlayLayout = panel;
            Log.i(TAG, "TIMELOCK_OVERLAY_SHOWN");
        } catch (Exception e) {
            Log.e(TAG, "TIMELOCK_OVERLAY_FAILED", e);
        }
    }

    public void dismiss() {
        if (overlayLayout != null) {
            try {
                windowManager.removeView(overlayLayout);
                Log.i(TAG, "TIMELOCK_OVERLAY_DISMISSED");
            } catch (Exception e) {
                Log.e(TAG, "TIMELOCK_DISMISS_FAILED", e);
            }
            overlayLayout = null;
        }
    }
}
