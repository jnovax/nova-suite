package com.appletv.screensaver.landscape;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LandscapeView extends FrameLayout {

    private static final String TAG = "LandscapeView";
    private static final String PREFS_NAME = "landscape_prefs";
    private static final String KEY_LAST_INDEX = "last_piece_index";

    private static final long SLIDE_DURATION = 28_000L;
    private static final long CROSSFADE_DURATION = 2500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Random random = new Random();
    private final List<Integer> playlist = new ArrayList<>();

    private SlideHolder currentSlide;
    private SlideHolder nextSlide;
    private LandscapeInfoCardView cardView;

    private int playlistPosition = 0;
    private int lastShownIndex = -1;
    private boolean running = false;
    private int generation = 0;
    private boolean transitioning = false;
    private ValueAnimator kenBurnsAnimator;

    private final Runnable nextSlideRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            loadAndTransitionNext();
        }
    };

    public LandscapeView(Context context) {
        this(context, null);
    }

    public LandscapeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setBackgroundColor(0xFF000000);

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            lastShownIndex = prefs.getInt(KEY_LAST_INDEX, -1);
        } catch (Exception e) {
            Log.w(TAG, "Failed reading preferences: " + e.getMessage());
        }

        currentSlide = new SlideHolder(context);
        addView(currentSlide.imageView);

        nextSlide = new SlideHolder(context);
        nextSlide.imageView.setAlpha(0f);
        addView(nextSlide.imageView);

        float density = getResources().getDisplayMetrics().density;
        cardView = new LandscapeInfoCardView(context);
        LayoutParams cardLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        cardLp.gravity = Gravity.BOTTOM | Gravity.START;
        cardLp.leftMargin = (int) (48 * density);
        cardLp.bottomMargin = (int) (40 * density);
        addView(cardView, cardLp);
    }

    private void initPlaylist() {
        playlist.clear();
        playlist.addAll(LandscapeCatalog.createShuffledIndices(random, lastShownIndex));
        playlistPosition = 0;
    }

    public void start() {
        if (running) return;
        running = true;
        initPlaylist();
        loadAndTransitionNext();
    }

    public void stop() {
        running = false;
        generation++;
        transitioning = false;
        handler.removeCallbacks(nextSlideRunnable);
        if (kenBurnsAnimator != null) {
            kenBurnsAnimator.cancel();
        }
        if (currentSlide != null) {
            currentSlide.imageView.animate().setListener(null);
            currentSlide.imageView.animate().cancel();
        }
        if (nextSlide != null) {
            nextSlide.imageView.animate().setListener(null);
            nextSlide.imageView.animate().cancel();
        }
        if (cardView != null) {
            cardView.dismiss();
        }
        playlist.clear();
        currentSlide.recycle();
        nextSlide.recycle();
    }

    public void next() {
        if (transitioning) {
            nextSlide.imageView.animate().setListener(null);
            nextSlide.imageView.animate().cancel();
            nextSlide.imageView.setAlpha(1f);
            finishTransition();
        }
        handler.removeCallbacks(nextSlideRunnable);
        loadAndTransitionNext();
    }

    public boolean handleRemoteKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            next();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                   keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                   keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                   keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            if (cardView != null) {
                cardView.showBriefly();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (executor.isShutdown()) executor = Executors.newSingleThreadExecutor();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        executor.shutdownNow();
        super.onDetachedFromWindow();
    }

    private void loadAndTransitionNext() {
        if (!running) return;
        if (playlist.isEmpty() || playlistPosition >= playlist.size()) {
            initPlaylist();
        }
        if (playlist.isEmpty()) return;

        final int requestGeneration = ++generation;
        final int index = playlist.get(playlistPosition++);
        lastShownIndex = index;

        final LandscapePiece piece = LandscapeCatalog.getPiece(index);
        if (piece == null) return;

        try {
            getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_LAST_INDEX, index)
                    .apply();
        } catch (Exception ignored) {}

        executor.execute(() -> {
            Bitmap bitmap = decodeAsset("landscapes/" + piece.filename);
            if (bitmap == null) {
                handler.post(() -> {
                    if (running && requestGeneration == generation) {
                        handler.postDelayed(nextSlideRunnable, SLIDE_DURATION);
                    }
                });
                return;
            }

            handler.post(() -> {
                if (!running || requestGeneration != generation) {
                    bitmap.recycle();
                    return;
                }
                applyTransition(bitmap, piece);
            });
        });
    }

    private void applyTransition(Bitmap bitmap, LandscapePiece piece) {
        nextSlide.imageView.animate().setListener(null);
        nextSlide.imageView.animate().cancel();
        nextSlide.setImage(bitmap);
        nextSlide.imageView.bringToFront();
        cardView.bringToFront();
        nextSlide.resetTransform();

        // Directional Ken Burns setup
        // If wider than 16:9, drift horizontally. If taller, drift vertically.
        // For 16:9 images, randomly alternate horizontal or vertical motion for cinematic variety.
        final boolean isPanorama = (piece.aspectRatio > 1.78f) ||
                (Math.abs(piece.aspectRatio - 1.778f) < 0.05f && random.nextBoolean());
        final boolean reverseMotion = random.nextBoolean();

        startKenBurns(nextSlide, isPanorama, reverseMotion);

        // 2.5s Crossfade
        transitioning = true;
        nextSlide.imageView.animate()
                .alpha(1f)
                .setDuration(CROSSFADE_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        finishTransition();
                    }
                })
                .start();

        cardView.displayPiece(piece);

        handler.removeCallbacks(nextSlideRunnable);
        handler.postDelayed(nextSlideRunnable, SLIDE_DURATION);
    }

    private void finishTransition() {
        transitioning = false;
        SlideHolder temp = currentSlide;
        currentSlide = nextSlide;
        nextSlide = temp;
        nextSlide.imageView.setAlpha(0f);
        nextSlide.recycle();
    }

    private void startKenBurns(SlideHolder slide, boolean isPanorama, boolean reverse) {
        if (kenBurnsAnimator != null) {
            kenBurnsAnimator.cancel();
        }

        final float startScale = isPanorama ? 1.01f : 1.02f;
        final float endScale = isPanorama ? 1.05f : 1.07f;
        final float maxPan = isPanorama ? 0.08f : 0.06f; // 4% drift

        kenBurnsAnimator = ValueAnimator.ofFloat(0f, 1f);
        kenBurnsAnimator.setDuration(SLIDE_DURATION);
        kenBurnsAnimator.setInterpolator(new LinearInterpolator());
        kenBurnsAnimator.addUpdateListener(anim -> {
            float frac = anim.getAnimatedFraction();
            float scale = startScale + (endScale - startScale) * frac;
            slide.imageView.setScaleX(scale);
            slide.imageView.setScaleY(scale);

            float drift = (reverse ? (1f - frac) : frac) - 0.5f; // -0.5 to +0.5
            if (isPanorama) {
                // Wide image: drift horizontally
                float tx = slide.imageView.getWidth() * Math.min(maxPan, scale - 1f) * drift;
                slide.imageView.setTranslationX(tx);
                slide.imageView.setTranslationY(0f);
            } else {
                // Taller image: drift vertically
                float ty = slide.imageView.getHeight() * Math.min(maxPan, scale - 1f) * drift;
                slide.imageView.setTranslationY(ty);
                slide.imageView.setTranslationX(0f);
            }
        });
        kenBurnsAnimator.start();
    }

    private Bitmap decodeAsset(String path) {
        AssetManager am = getContext().getAssets();
        try (InputStream is = am.open(path)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.RGB_565; // High performance, 50% less RAM
            return BitmapFactory.decodeStream(is, null, opts);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding asset " + path, e);
            return null;
        }
    }

    private static class SlideHolder {
        final ImageView imageView;
        private Bitmap currentBitmap;

        SlideHolder(Context context) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        void setImage(Bitmap bm) {
            recycle();
            currentBitmap = bm;
            imageView.setImageBitmap(bm);
        }

        void resetTransform() {
            imageView.setAlpha(0f);
            imageView.setScaleX(1.02f);
            imageView.setScaleY(1.02f);
            imageView.setTranslationX(0f);
            imageView.setTranslationY(0f);
        }

        void recycle() {
            if (currentBitmap != null && !currentBitmap.isRecycled()) {
                imageView.setImageBitmap(null);
                currentBitmap.recycle();
                currentBitmap = null;
            }
        }
    }
}
