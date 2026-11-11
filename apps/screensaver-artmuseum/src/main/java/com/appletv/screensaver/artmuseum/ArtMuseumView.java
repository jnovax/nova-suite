package com.appletv.screensaver.artmuseum;

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
import android.view.Gravity;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArtMuseumView extends FrameLayout {

    private static final String PREFS_NAME = "artmuseum_prefs";
    private static final String KEY_LAST_INDEX = "last_piece_index";

    private static final long SLIDE_DURATION = 25_000L;
    private static final long CROSSFADE_DURATION = 1500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Random random = new Random();
    private final List<Integer> playlist = new ArrayList<>();

    private ImageView currentView;
    private ImageView nextView;
    private MuseumCardView cardView;

    private int playlistPosition = 0;
    private int lastShownIndex = -1;
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

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            lastShownIndex = prefs.getInt(KEY_LAST_INDEX, -1);
        } catch (Exception ignored) {
        }

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

    private void initPlaylist() {
        playlist.clear();
        playlist.addAll(ArtCatalog.createShuffledIndices(random, lastShownIndex));
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
        handler.removeCallbacks(nextSlideRunnable);
        if (kenBurnsAnimator != null) {
            kenBurnsAnimator.cancel();
        }
        if (currentView != null) {
            currentView.animate().cancel();
        }
        if (nextView != null) {
            nextView.animate().cancel();
        }
        if (cardView != null) {
            cardView.dismiss();
        }
        playlist.clear();
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
        if (playlist.isEmpty() || playlistPosition >= playlist.size()) {
            initPlaylist();
        }
        if (playlist.isEmpty()) return;

        final int index = playlist.get(playlistPosition++);
        lastShownIndex = index;

        try {
            getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_LAST_INDEX, index)
                    .apply();
        } catch (Exception ignored) {
        }

        final ArtPiece piece = ArtCatalog.getPiece(index);
        if (piece == null) return;

        executor.execute(() -> {
            Bitmap bitmap = decodeAsset("paintings/" + piece.filename);
            handler.post(() -> {
                if (!running || bitmap == null) {
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                    return;
                }
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
