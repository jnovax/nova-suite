package com.appletv.screensaver.landscape;

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

public class LandscapeInfoCardView extends LinearLayout {

    private final TextView titleViView;
    private final TextView subtitleView;
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

    public LandscapeInfoCardView(Context context) {
        this(context, null);
    }

    public LandscapeInfoCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setVisibility(GONE);

        float density = getResources().getDisplayMetrics().density;
        int padH = (int) (22 * density);
        int padV = (int) (14 * density);
        setPadding(padH, padV, padH, padV);

        // Apple TV frosted dark glass capsule
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xA6101014); // 65% dark glass
        bg.setCornerRadius(16 * density);
        bg.setStroke((int) (1 * density), 0x30FFFFFF); // Subtle bright rim
        setBackground(bg);

        // Vietnamese Primary Title
        titleViView = new TextView(context);
        titleViView.setTextColor(Color.WHITE);
        titleViView.setTextSize(18);
        titleViView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        addView(titleViView);

        // English Title + Artist + Year Subtitle
        subtitleView = new TextView(context);
        subtitleView.setTextColor(0xFFDCDCE2);
        subtitleView.setTextSize(13);
        subtitleView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        subtitleView.setPadding(0, (int) (3 * density), 0, 0);
        addView(subtitleView);
    }

    public void displayPiece(LandscapePiece piece) {
        if (piece == null) return;
        handler.removeCallbacks(fadeOutRunnable);
        animate().cancel();

        titleViView.setText(piece.titleVi);
        subtitleView.setText(String.format("%s — %s, %s", piece.titleEn, piece.artist, piece.year));

        setAlpha(0f);
        setVisibility(VISIBLE);
        animate()
                .alpha(1f)
                .setDuration(800L)
                .setListener(null)
                .start();

        handler.postDelayed(fadeOutRunnable, 5500L);
    }

    public void showBriefly() {
        handler.removeCallbacks(fadeOutRunnable);
        animate().cancel();
        setVisibility(VISIBLE);
        animate()
                .alpha(1f)
                .setDuration(400L)
                .setListener(null)
                .start();
        handler.postDelayed(fadeOutRunnable, 4000L);
    }

    public void dismiss() {
        handler.removeCallbacks(fadeOutRunnable);
        animate().cancel();
        setVisibility(GONE);
    }
}
