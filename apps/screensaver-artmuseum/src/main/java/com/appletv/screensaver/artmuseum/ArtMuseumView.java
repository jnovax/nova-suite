package com.appletv.screensaver.artmuseum;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArtMuseumView extends FrameLayout {

    private static final long SLIDE_DURATION = 25_000L;
    private static final long CROSSFADE_DURATION = 1500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ImageView currentView;
    private ImageView nextView;
    private MuseumCardView cardView;

    private int currentIndex = 0;
    private boolean running = false;
    private ValueAnimator kenBurnsAnimator;

    private final Runnable nextSlideRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            loadAndTransitionNext();
        }
    };

    public ArtMuseumView(Context context) {
        this(context, null);
    }

    public ArtMuseumView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setBackgroundColor(0xFF000000);

        currentView = new ImageView(context);
        currentView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(currentView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        nextView = new ImageView(context);
        nextView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        nextView.setAlpha(0f);
        addView(nextView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        float density = getResources().getDisplayMetrics().density;
        cardView = new MuseumCardView(context);
        LayoutParams cardLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        cardLp.gravity = Gravity.BOTTOM | Gravity.START;
        cardLp.leftMargin = (int) (48 * density);
        cardLp.bottomMargin = (int) (40 * density);
        addView(cardView, cardLp);
    }

    public void start() {
        if (running) return;
        running = true;
        loadAndTransitionNext();
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(nextSlideRunnable);
        if (kenBurnsAnimator != null) {
            kenBurnsAnimator.cancel();
        }
        if (cardView != null) {
            cardView.dismiss();
        }
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

    private void loadAndTransitionNext() {
        if (!running) return;
        final int index = currentIndex++;
        final ArtPiece piece = ArtCatalog.getPiece(index);
        if (piece == null) return;

        executor.execute(() -> {
            Bitmap bitmap = decodeAsset("paintings/" + piece.filename);
            handler.post(() -> {
                if (!running || bitmap == null) return;
                applyTransition(bitmap, piece);
            });
        });
    }

    private void applyTransition(Bitmap bitmap, ArtPiece piece) {
        nextView.setImageBitmap(bitmap);
        nextView.setAlpha(0f);
        nextView.setScaleX(1.0f);
        nextView.setScaleY(1.0f);
        nextView.setTranslationX(0f);
        nextView.setTranslationY(0f);

        // Crossfade nextView in
        nextView.animate()
                .alpha(1f)
                .setDuration(CROSSFADE_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Swap views
                        ImageView temp = currentView;
                        currentView = nextView;
                        nextView = temp;
                        nextView.setAlpha(0f);

                        startKenBurns(currentView);
                    }
                })
                .start();

        cardView.displayPiece(piece);

        handler.removeCallbacks(nextSlideRunnable);
        handler.postDelayed(nextSlideRunnable, SLIDE_DURATION);
    }

    private void startKenBurns(ImageView view) {
        if (kenBurnsAnimator != null) kenBurnsAnimator.cancel();

        kenBurnsAnimator = ValueAnimator.ofFloat(0f, 1f);
        kenBurnsAnimator.setDuration(SLIDE_DURATION);
        kenBurnsAnimator.setInterpolator(new LinearInterpolator());
        kenBurnsAnimator.addUpdateListener(anim -> {
            float frac = anim.getAnimatedFraction();
            float scale = 1.0f + 0.08f * frac;
            view.setScaleX(scale);
            view.setScaleY(scale);
        });
        kenBurnsAnimator.start();
    }

    private Bitmap decodeAsset(String path) {
        AssetManager am = getContext().getAssets();
        try (InputStream is = am.open(path)) {
            return BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            return null;
        }
    }
}
