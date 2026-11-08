package vn.familytv.parentalcontrol.platform

/**
 * Filters Accessibility TYPE_WINDOW_STATE_CHANGED events so transient system overlays,
 * volume dialogs, toasts, input methods, and our own overlays do not overwrite the
 * active foreground application package.
 */
internal object ForegroundEventFilter {
    private const val OWN_PACKAGE = "vn.familytv.parentalcontrol"
    private const val TV_SETTINGS_PACKAGE = "com.android.tv.settings"

    private val ownActivities = setOf(
        "$OWN_PACKAGE.MainActivity",
        "$OWN_PACKAGE.auth.RemoteAuthActivity"
    )

    private val ignoredPackages = setOf(
        "android",
        "com.android.systemui"
    )

    fun accept(packageName: String?, className: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val pkg = packageName.trim()
        val cls = className?.trim().orEmpty()

        if (pkg in ignoredPackages) return false
        if (pkg.contains(".inputmethod") || cls == "android.inputmethodservice.SoftInputWindow") return false
        if (cls.contains("Toast") || cls.contains("TransientNotification")) return false

        if (pkg == OWN_PACKAGE) {
            return cls in ownActivities
        }

        if (pkg == TV_SETTINGS_PACKAGE) {
            if (cls.startsWith("android.widget.") || cls.startsWith("android.view.") || cls.endsWith("Dialog")) {
                return false
            }
        }

        return true
    }
}
