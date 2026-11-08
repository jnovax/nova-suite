package vn.familytv.parentalcontrol.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import vn.familytv.parentalcontrol.screentime.AppUsageSession

/**
 * Renders a 24-hour horizontal timeline bar (00:00 -> 24:00) highlighting
 * tracked (amber) and untracked (cyan/green) app sessions across the day.
 */
class TimelineBarView(
    context: Context,
    private val sessions: List<AppUsageSession>,
    private val dayStartMs: Long
) : View(context) {

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E293B")
        style = Paint.Style.FILL
    }

    private val trackedSessionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F59E0B")
        style = Paint.Style.FILL
    }

    private val untrackedSessionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        style = Paint.Style.FILL
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#475569")
        strokeWidth = dp(1f)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = dp(11f)
    }

    private val barRect = RectF()
    private val segRect = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = dp(46f).toInt()
        val w = resolveSize(dp(400f).toInt(), widthMeasureSpec)
        val h = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val barTop = dp(4f)
        val barHeight = dp(20f)
        val barBottom = barTop + barHeight
        val radius = dp(6f)

        barRect.set(0f, barTop, w, barBottom)
        canvas.drawRoundRect(barRect, radius, radius, trackPaint)

        val dayDurationMs = 24L * 3600L * 1000L
        for (session in sessions) {
            val startFrac = ((session.startMs - dayStartMs).toFloat() / dayDurationMs).coerceIn(0f, 1f)
            val endFrac = ((session.endMs - dayStartMs).toFloat() / dayDurationMs).coerceIn(0f, 1f)
            val left = startFrac * w
            val right = maxOf(left + dp(3f), endFrac * w).coerceAtMost(w)
            segRect.set(left, barTop, right, barBottom)
            val paint = if (session.isTracked) trackedSessionPaint else untrackedSessionPaint
            canvas.drawRoundRect(segRect, dp(2f), dp(2f), paint)
        }

        val hours = listOf(0, 6, 12, 18, 24)
        for (hr in hours) {
            val frac = hr / 24f
            val x = (frac * w).coerceIn(dp(1f), w - dp(1f))
            canvas.drawLine(x, barTop, x, barBottom + dp(4f), tickPaint)

            val label = "%02d:00".format(hr)
            val textWidth = labelPaint.measureText(label)
            val textX = when (hr) {
                0 -> 0f
                24 -> w - textWidth
                else -> x - textWidth / 2f
            }
            canvas.drawText(label, textX, barBottom + dp(17f), labelPaint)
        }
    }
}
