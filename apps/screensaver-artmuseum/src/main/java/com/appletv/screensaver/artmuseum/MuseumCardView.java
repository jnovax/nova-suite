package com.appletv.screensaver.artmuseum;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MuseumCardView extends LinearLayout {

    private final TextView titleView;
    private final TextView subtitleView;
    private final TextView museumView;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable fadeOutRunnable = new Runnable() {
        @Override
        public void run() {
            animate()
                    .alpha(0f)
                    .setDuration(1200L)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            setVisibility(GONE);
                        }
                    })
                    .start();
        }
    };

    public MuseumCardView(Context context) {
        this(context, null);
    }

    public MuseumCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);

        float density = getResources().getDisplayMetrics().density;
        int padH = (int) (20 * density);
        int padV = (int) (14 * density);
        setPadding(padH, padV, padH, padV);

        // Frosted glass dark capsule background
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xA0101014); // 62% alpha dark glass
        bg.setCornerRadius(16 * density);
        bg.setStroke((int) (1 * density), 0x30FFFFFF); // Subtle glass rim
        setBackground(bg);

        // Title View
        titleView = new TextView(context);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(18);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        addView(titleView);

        // Subtitle View (Artist, Year)
        subtitleView = new TextView(context);
        subtitleView.setTextColor(0xFFDCDCE0);
        subtitleView.setTextSize(14);
        subtitleView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        subtitleView.setPadding(0, (int) (2 * density), 0, 0);
        addView(subtitleView);

        // Museum View
        museumView = new TextView(context);
        museumView.setTextColor(0xFF9E9EA6);
        museumView.setTextSize(12);
        museumView.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
        museumView.setPadding(0, (int) (2 * density), 0, 0);
        addView(museumView);
    }

    public void displayPiece(ArtPiece piece) {
        if (piece == null) return;
        handler.removeCallbacks(fadeOutRunnable);
        animate().cancel();

        titleView.setText(piece.title);
        subtitleView.setText(String.format("%s, %s", piece.artist, piece.year));
        museumView.setText(piece.museum);

        setAlpha(0f);
        setVisibility(VISIBLE);
        animate()
                .alpha(1f)
                .setDuration(800L)
                .setListener(null)
                .start();

        handler.postDelayed(fadeOutRunnable, 6000L);
    }

    public void dismiss() {
        handler.removeCallbacks(fadeOutRunnable);
        animate().cancel();
        setVisibility(GONE);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(fadeOutRunnable);
        super.onDetachedFromWindow();
    }
}
