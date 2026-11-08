package vn.familytv.parentalcontrol.screentime

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

data class RawUsageEvent(
    val packageName: String,
    val timestampMs: Long,
    val eventType: Int
) {
    companion object {
        const val EVENT_ACTIVITY_RESUMED = UsageEvents.Event.ACTIVITY_RESUMED
        const val EVENT_ACTIVITY_PAUSED = UsageEvents.Event.ACTIVITY_PAUSED
        const val EVENT_ACTIVITY_STOPPED = UsageEvents.Event.ACTIVITY_STOPPED
        const val EVENT_DEVICE_SHUTDOWN = UsageEvents.Event.DEVICE_SHUTDOWN
        const val EVENT_DEVICE_STARTUP = UsageEvents.Event.DEVICE_STARTUP
    }
}

data class AppUsageSession(
    val packageName: String,
    val startMs: Long,
    val endMs: Long,
    val isTracked: Boolean
) {
    val durationSec: Int
        get() = ((endMs - startMs) / 1000L).toInt().coerceAtLeast(0)
}

data class DailyUsageTimeline(
    val sessions: List<AppUsageSession>,
    val totalTrackedSec: Int,
    val totalUntrackedSec: Int
)

object UsageTimelineAnalyzer {
    private val SYSTEM_NOISE_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.tv.settings",
        "com.appletv.launcher",
        "vn.familytv.parentalcontrol",
        "vn.familytv.audit",
        "com.google.android.backdrop",
        "com.google.android.apps.tv.dreamx",
        "com.tcl.ambient",
        "com.google.android.tv.remote.service",
        "com.google.android.permissioncontroller"
    )

    private val KNOWN_APP_LABELS = mapOf(
        "io.gh.reisxd.tizentube.cobalt" to "YouTube (TizenTube)",
        "org.smarttube.stable" to "YouTube (SmartTube)",
        "org.smarttube.beta" to "YouTube (SmartTube Beta)",
        "com.google.android.youtube.tv" to "YouTube",
        "com.google.android.youtube.tvkids" to "YouTube Kids",
        "vn.mytvnet.b2cott" to "MyTV",
        "vn.mytvnet.b2c" to "MyTV",
        "vn.vnpt.media.mytv" to "MyTV",
        "org.courville.nova" to "Nova Video Player",
        "com.spotify.tv.android" to "Spotify",
        "com.spotify.music" to "Spotify",
        "com.khtn7.tv" to "KHTN 7"
    )

    fun isSystemNoisePackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return true
        val pkg = packageName.trim()
        if (pkg in SYSTEM_NOISE_PACKAGES) return true
        if (pkg.contains(".inputmethod")) return true
        return false
    }

    fun analyze(
        events: List<RawUsageEvent>,
        dayStartMs: Long,
        nowMs: Long,
        policy: ScreenTimePolicy = ScreenTimePolicy(),
        mergeGapMs: Long = 30_000L,
        minSessionSec: Int = 5
    ): DailyUsageTimeline {
        if (nowMs <= dayStartMs) {
            return DailyUsageTimeline(emptyList(), 0, 0)
        }

        val sorted = events.sortedBy { it.timestampMs }
        val rawSessions = mutableListOf<AppUsageSession>()

        var activePkg: String? = null
        var activeStartMs: Long = 0L

        fun closeActive(atMs: Long) {
            val pkg = activePkg ?: return
            val clampedStart = activeStartMs.coerceIn(dayStartMs, nowMs)
            val clampedEnd = atMs.coerceIn(dayStartMs, nowMs)
            if (clampedEnd > clampedStart && !isSystemNoisePackage(pkg)) {
                rawSessions.add(
                    AppUsageSession(
                        packageName = pkg,
                        startMs = clampedStart,
                        endMs = clampedEnd,
                        isTracked = policy.isTrackedPackage(pkg)
                    )
                )
            }
            activePkg = null
            activeStartMs = 0L
        }

        for (ev in sorted) {
            val ts = ev.timestampMs
            if (ts < dayStartMs || ts > nowMs) continue

            when (ev.eventType) {
                RawUsageEvent.EVENT_ACTIVITY_RESUMED -> {
                    if (activePkg != null && activePkg != ev.packageName) {
                        closeActive(ts)
                    }
                    if (!isSystemNoisePackage(ev.packageName)) {
                        if (activePkg == null) {
                            activePkg = ev.packageName
                            activeStartMs = ts
                        }
                    } else {
                        closeActive(ts)
                    }
                }
                RawUsageEvent.EVENT_ACTIVITY_PAUSED,
                RawUsageEvent.EVENT_ACTIVITY_STOPPED -> {
                    if (activePkg != null && activePkg == ev.packageName) {
                        closeActive(ts)
                    }
                }
                RawUsageEvent.EVENT_DEVICE_SHUTDOWN,
                RawUsageEvent.EVENT_DEVICE_STARTUP -> {
                    closeActive(ts)
                }
            }
        }

        // Close still-active session at nowMs
        if (activePkg != null) {
            closeActive(nowMs)
        }

        // Merge consecutive sessions of the same package separated by < mergeGapMs
        val merged = mutableListOf<AppUsageSession>()
        for (session in rawSessions) {
            val last = merged.lastOrNull()
            if (last != null && last.packageName == session.packageName && (session.startMs - last.endMs) < mergeGapMs) {
                merged[merged.lastIndex] = last.copy(endMs = maxOf(last.endMs, session.endMs))
            } else {
                merged.add(session)
            }
        }

        // Filter out brief sessions < minSessionSec
        val filtered = merged.filter { it.durationSec >= minSessionSec }

        val totalTrackedSec = filtered.filter { it.isTracked }.sumOf { it.durationSec }
        val totalUntrackedSec = filtered.filter { !it.isTracked }.sumOf { it.durationSec }

        return DailyUsageTimeline(
            sessions = filtered,
            totalTrackedSec = totalTrackedSec,
            totalUntrackedSec = totalUntrackedSec
        )
    }

    fun queryToday(
        context: Context,
        policy: ScreenTimePolicy = ScreenTimePolicy(),
        nowMs: Long = System.currentTimeMillis()
    ): DailyUsageTimeline {
        val usm = context.getSystemService(UsageStatsManager::class.java)
            ?: return DailyUsageTimeline(emptyList(), 0, 0)

        val startCal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStartMs = startCal.timeInMillis

        val rawList = mutableListOf<RawUsageEvent>()
        try {
            val usageEvents = usm.queryEvents(dayStartMs, nowMs)
            val event = UsageEvents.Event()
            while (usageEvents != null && usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val type = event.eventType
                if (
                    type == RawUsageEvent.EVENT_ACTIVITY_RESUMED ||
                    type == RawUsageEvent.EVENT_ACTIVITY_PAUSED ||
                    type == RawUsageEvent.EVENT_ACTIVITY_STOPPED ||
                    type == RawUsageEvent.EVENT_DEVICE_SHUTDOWN ||
                    type == RawUsageEvent.EVENT_DEVICE_STARTUP
                ) {
                    val pkg = event.packageName ?: continue
                    rawList.add(RawUsageEvent(pkg, event.timeStamp, type))
                }
            }
        } catch (_: Exception) {
            return DailyUsageTimeline(emptyList(), 0, 0)
        }

        return analyze(
            events = rawList,
            dayStartMs = dayStartMs,
            nowMs = nowMs,
            policy = policy
        )
    }

    fun resolveAppName(context: Context, packageName: String): String {
        KNOWN_APP_LABELS[packageName]?.let { return it }
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString().takeIf { it.isNotBlank() } ?: packageName
        } catch (_: Exception) {
            packageName
        }
    }

    fun formatDurationVi(seconds: Int): String {
        if (seconds < 60) return "${seconds}s"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}p"
            h > 0 -> "${h}h"
            s > 0 && m < 10 -> "${m}p ${s}s"
            else -> "${m} phút"
        }
    }
}
