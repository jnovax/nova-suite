package vn.familytv.parentalcontrol.platform;

public final class PermissionSnapshot {
    private final boolean accessibilityEnabled;
    private final boolean serviceConnected;
    private final boolean usageAccess;

    public PermissionSnapshot(boolean accessibilityEnabled, boolean serviceConnected, boolean usageAccess) {
        this.accessibilityEnabled = accessibilityEnabled;
        this.serviceConnected = serviceConnected;
        this.usageAccess = usageAccess;
    }

    public boolean isAccessibilityEnabled() {
        return accessibilityEnabled;
    }

    public boolean isServiceConnected() {
        return serviceConnected;
    }

    public boolean isUsageAccess() {
        return usageAccess;
    }

    public AccessibilityState getAccessibility() {
        if (!accessibilityEnabled) {
            return AccessibilityState.DISABLED;
        } else if (serviceConnected) {
            return AccessibilityState.CONNECTED;
        } else {
            return AccessibilityState.ENABLED_DISCONNECTED;
        }
    }

    public boolean isReady() {
        return getAccessibility() == AccessibilityState.CONNECTED && usageAccess;
    }

    public boolean getReady() {
        return isReady();
    }
}
