package com.appletv.screensaver.flipclock;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.Calendar;
import java.util.Random;

/**
 * Pixel-accurate, hardware-accelerated Fliqlo Split-Flap Clock screensaver.
 * Mimics mechanical flip-clock physics with 3D Camera rotation, dynamic flap shading,
 * and periodic OLED micro-drift for burn-in protection.
 */
public class FliqloClockView extends View {

    private static final long FLIP_DURATION = 650L;
    private static final long DRIFT_INTERVAL = 300_000L; // Every 5 minutes
    private static final float MAX_DRIFT_DP = 15f;

    // Time state
    private String currentHour = "";
    private String currentMinute = "";
    private String currentAmPm = "";
    private boolean is24Hour;

    // Flip card animators & state
    private final FlipCard hoursCard = new FlipCard();
    private final FlipCard minutesCard = new FlipCard();

    // Micro-drift for anti-burn-in
    private float driftOffsetX = 0f;
    private float driftOffsetY = 0f;
    private ValueAnimator driftAnimator;

    // Ticking Handler
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;

    // Pre-allocated graphic objects (Zero allocations in onDraw)
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint amPmPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF cardRect = new RectF();
    private final RectF topHalfRect = new RectF();
    private final RectF bottomHalfRect = new RectF();
    private final Path clipPath = new Path();
    private final Rect textBounds = new Rect();

    private final Camera camera = new Camera();
    private final Matrix matrix = new Matrix();

    private final Runnable timeTickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            updateTime(true);
            long now = System.currentTimeMillis();
            long delay = 60_000L - (now % 60_000L) + 50L;
            handler.postDelayed(this, delay);
        }
    };

    private final Runnable driftRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            startDriftAnimation();
            handler.postDelayed(this, DRIFT_INTERVAL);
        }
    };

    public FliqloClockView(Context context) {
        this(context, null);
    }

    public FliqloClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setBackgroundColor(0xFF000000); // True OLED black
        setLayerType(LAYER_TYPE_HARDWARE, null);

        cardPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFFF0F0F5);
        textPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        amPmPaint.setColor(0xFF8E8E93);
        amPmPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        amPmPaint.setTextAlign(Paint.Align.LEFT);

        slitPaint.setColor(0xFF000000);
        slitPaint.setStyle(Paint.Style.FILL);

        shadowPaint.setStyle(Paint.Style.FILL);

        is24Hour = DateFormat.is24HourFormat(getContext());
        updateTime(false);
    }

    public void start() {
        if (running) return;
        running = true;
        is24Hour = DateFormat.is24HourFormat(getContext());
        updateTime(false);

        handler.removeCallbacks(timeTickRunnable);
        long now = System.currentTimeMillis();
        long delay = 60_000L - (now % 60_000L) + 50L;
        handler.postDelayed(timeTickRunnable, delay);

        handler.removeCallbacks(driftRunnable);
        handler.postDelayed(driftRunnable, DRIFT_INTERVAL);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(timeTickRunnable);
        handler.removeCallbacks(driftRunnable);
        if (driftAnimator != null) {
            driftAnimator.cancel();
        }
        hoursCard.cancel();
        minutesCard.cancel();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    private void updateTime(boolean animate) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        FliqloTimeHelper.TimeData data = FliqloTimeHelper.format(hour, minute, is24Hour);

        String newHour = data.hourStr;
        String newMinute = data.minStr;
        currentAmPm = data.amPm;

        if (currentHour.isEmpty()) {
            currentHour = newHour;
            hoursCard.displayedValue = newHour;
            hoursCard.targetValue = newHour;
        } else if (!currentHour.equals(newHour)) {
            if (animate) {
                hoursCard.flipTo(newHour);
            } else {
                hoursCard.displayedValue = newHour;
                hoursCard.targetValue = newHour;
            }
            currentHour = newHour;
        }

        if (currentMinute.isEmpty()) {
            currentMinute = newMinute;
            minutesCard.displayedValue = newMinute;
            minutesCard.targetValue = newMinute;
        } else if (!currentMinute.equals(newMinute)) {
            if (animate) {
                minutesCard.flipTo(newMinute);
            } else {
                minutesCard.displayedValue = newMinute;
                minutesCard.targetValue = newMinute;
            }
            currentMinute = newMinute;
        }

        invalidate();
    }

    private void startDriftAnimation() {
        float density = getResources().getDisplayMetrics().density;
        float maxPx = MAX_DRIFT_DP * density;
        Random rng = new Random();
        final float targetX = (rng.nextFloat() * 2f - 1f) * maxPx;
        final float targetY = (rng.nextFloat() * 2f - 1f) * (maxPx * 0.75f);
        final float startX = driftOffsetX;
        final float startY = driftOffsetY;

        if (driftAnimator != null) driftAnimator.cancel();
        driftAnimator = ValueAnimator.ofFloat(0f, 1f);
        driftAnimator.setDuration(4000L);
        driftAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        driftAnimator.addUpdateListener(anim -> {
            float frac = anim.getAnimatedFraction();
            driftOffsetX = startX + (targetX - startX) * frac;
            driftOffsetY = startY + (targetY - startY) * frac;
            invalidate();
        });
        driftAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float cardWidth = height * 0.44f;
        float cardHeight = cardWidth * 1.20f;
        float cardGap = cardWidth * 0.08f;
        float cornerRadius = cardWidth * 0.08f;
        float slitHeight = Math.max(2.5f, 2f * getResources().getDisplayMetrics().density);

        float totalWidth = cardWidth * 2 + cardGap;
        float startX = (width - totalWidth) / 2f + driftOffsetX;
        float startY = (height - cardHeight) / 2f + driftOffsetY;

        textPaint.setTextSize(cardHeight * 0.72f);
        amPmPaint.setTextSize(cardHeight * 0.09f);

        // Hours Card
        cardRect.set(startX, startY, startX + cardWidth, startY + cardHeight);
        drawFlipCard(canvas, hoursCard, cardRect, cornerRadius, slitHeight, currentAmPm);

        // Minutes Card
        float minStartX = startX + cardWidth + cardGap;
        cardRect.set(minStartX, startY, minStartX + cardWidth, startY + cardHeight);
        drawFlipCard(canvas, minutesCard, cardRect, cornerRadius, slitHeight, null);
    }

    private void drawFlipCard(Canvas canvas, FlipCard card, RectF rect, float cornerRadius, float slitHeight, String amPm) {
        float midY = rect.centerY();
        float halfSlit = slitHeight / 2f;

        topHalfRect.set(rect.left, rect.top, rect.right, midY - halfSlit);
        bottomHalfRect.set(rect.left, midY + halfSlit, rect.right, rect.bottom);

        textPaint.getTextBounds("88", 0, 2, textBounds);
        float textY = midY - textBounds.exactCenterY();

        if (!card.isFlipping) {
            drawCardHalf(canvas, topHalfRect, rect, cornerRadius, card.displayedValue, textY, true, amPm);
            drawCardHalf(canvas, bottomHalfRect, rect, cornerRadius, card.displayedValue, textY, false, null);
        } else {
            float progress = card.flipProgress;
            if (progress <= 0.5f) {
                drawCardHalf(canvas, topHalfRect, rect, cornerRadius, card.targetValue, textY, true, amPm);
                drawCardHalf(canvas, bottomHalfRect, rect, cornerRadius, card.displayedValue, textY, false, null);

                float angle = -180f * progress;
                canvas.save();
                camera.save();
                camera.setLocation(0, 0, -8f * getResources().getDisplayMetrics().density);
                camera.rotateX(angle);
                camera.getMatrix(matrix);
                camera.restore();

                matrix.preTranslate(-rect.centerX(), -midY);
                matrix.postTranslate(rect.centerX(), midY);
                canvas.concat(matrix);

                drawCardHalf(canvas, topHalfRect, rect, cornerRadius, card.displayedValue, textY, true, amPm);

                int shadowAlpha = (int) (170f * (progress / 0.5f));
                shadowPaint.setColor(0xFF000000);
                shadowPaint.setAlpha(shadowAlpha);
                canvas.drawRect(topHalfRect, shadowPaint);

                canvas.restore();
            } else {
                drawCardHalf(canvas, topHalfRect, rect, cornerRadius, card.targetValue, textY, true, amPm);
                drawCardHalf(canvas, bottomHalfRect, rect, cornerRadius, card.displayedValue, textY, false, null);

                float angle = 90f - 180f * (progress - 0.5f);
                canvas.save();
                camera.save();
                camera.setLocation(0, 0, -8f * getResources().getDisplayMetrics().density);
                camera.rotateX(angle);
                camera.getMatrix(matrix);
                camera.restore();

                matrix.preTranslate(-rect.centerX(), -midY);
                matrix.postTranslate(rect.centerX(), midY);
                canvas.concat(matrix);

                drawCardHalf(canvas, bottomHalfRect, rect, cornerRadius, card.targetValue, textY, false, null);

                int shadowAlpha = (int) (170f * (1f - (progress - 0.5f) / 0.5f));
                shadowPaint.setColor(0xFF000000);
                shadowPaint.setAlpha(shadowAlpha);
                canvas.drawRect(bottomHalfRect, shadowPaint);

                canvas.restore();
            }
        }

        canvas.drawRect(rect.left, midY - halfSlit, rect.right, midY + halfSlit, slitPaint);
    }

    private void drawCardHalf(Canvas canvas, RectF halfRect, RectF fullRect, float cornerRadius,
                              String text, float textY, boolean isTop, String amPm) {
        canvas.save();
        clipPath.reset();
        clipPath.addRoundRect(fullRect, cornerRadius, cornerRadius, Path.Direction.CW);
        canvas.clipPath(clipPath);
        canvas.clipRect(halfRect);

        cardPaint.setColor(isTop ? 0xFF242426 : 0xFF1F1F21);
        canvas.drawRoundRect(fullRect, cornerRadius, cornerRadius, cardPaint);

        canvas.drawText(text, fullRect.centerX(), textY, textPaint);

        if (isTop && amPm != null && !amPm.isEmpty()) {
            float amPmX = fullRect.left + (fullRect.width() * 0.08f);
            float amPmY = fullRect.top + (fullRect.height() * 0.16f);
            canvas.drawText(amPm, amPmX, amPmY, amPmPaint);
        }

        canvas.restore();
    }

    private class FlipCard {
        String displayedValue = "";
        String targetValue = "";
        boolean isFlipping = false;
        float flipProgress = 0f;
        ValueAnimator animator;

        void flipTo(String nextValue) {
            if (nextValue == null || nextValue.equals(displayedValue)) return;
            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
            targetValue = nextValue;
            isFlipping = true;
            flipProgress = 0f;

            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(FLIP_DURATION);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                flipProgress = (float) animation.getAnimatedValue();
                invalidate();
            });
            animator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    displayedValue = targetValue;
                    isFlipping = false;
                    flipProgress = 0f;
                    invalidate();
                }
            });
            animator.start();
        }

        void cancel() {
            if (animator != null) animator.cancel();
            isFlipping = false;
            flipProgress = 0f;
        }
    }
}
