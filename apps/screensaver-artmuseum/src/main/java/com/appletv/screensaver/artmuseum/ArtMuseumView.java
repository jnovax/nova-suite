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
import android.util.Log;
import android.view.Gravity;
import android.view.View;
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

    private SlideHolder currentSlide;
    private SlideHolder nextSlide;
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
            Log.i("ArtMuseumView", "init: loaded lastShownIndex=" + lastShownIndex);
        } catch (Exception e) {
            Log.w("ArtMuseumView", "Failed to read prefs", e);
        }

        currentSlide = new SlideHolder(context);
        addView(currentSlide.container);

        nextSlide = new SlideHolder(context);
        nextSlide.container.setAlpha(0f);
        addView(nextSlide.container);

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
        Log.i("ArtMuseumView", "initPlaylist: lastShownIndex=" + lastShownIndex + ", playlist=" + playlist);
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
        if (currentSlide != null) {
            currentSlide.container.animate().cancel();
        }
        if (nextSlide != null) {
            nextSlide.container.animate().cancel();
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

        final ArtPiece piece = ArtCatalog.getPiece(index);
        Log.i("ArtMuseumView", "loadAndTransitionNext: index=" + index + ", title=" + (piece != null ? piece.title : "null"));

        try {
            getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_LAST_INDEX, index)
                    .apply();
        } catch (Exception ignored) {
        }
        if (piece == null) return;

        executor.execute(() -> {
            Bitmap fullBitmap = decodeAsset("paintings/" + piece.filename);
            if (fullBitmap == null) return;
            Bitmap ambientBitmap = FastBlur.createAmbientBlur(fullBitmap);

            handler.post(() -> {
                if (!running) {
                    fullBitmap.recycle();
                    if (ambientBitmap != null) ambientBitmap.recycle();
                    return;
                }
                applyTransition(fullBitmap, ambientBitmap, piece);
            });
        });
    }

    private void applyTransition(Bitmap fullBitmap, Bitmap ambientBitmap, ArtPiece piece) {
        nextSlide.setContent(fullBitmap, ambientBitmap);
        nextSlide.resetTransform();

        // Crossfade nextSlide in
        nextSlide.container.animate()
                .alpha(1f)
                .setDuration(CROSSFADE_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Swap slides
                        SlideHolder temp = currentSlide;
                        currentSlide = nextSlide;
                        nextSlide = temp;
                        nextSlide.container.setAlpha(0f);

                        startKenBurns(currentSlide);
                    }
                })
                .start();

        cardView.displayPiece(piece);

        handler.removeCallbacks(nextSlideRunnable);
        handler.postDelayed(nextSlideRunnable, SLIDE_DURATION);
    }

    private void startKenBurns(SlideHolder slide) {
        if (kenBurnsAnimator != null) kenBurnsAnimator.cancel();

        kenBurnsAnimator = ValueAnimator.ofFloat(0f, 1f);
        kenBurnsAnimator.setDuration(SLIDE_DURATION);
        kenBurnsAnimator.setInterpolator(new LinearInterpolator());
        kenBurnsAnimator.addUpdateListener(anim -> {
            float frac = anim.getAnimatedFraction();
            // Foreground artwork breathes subtly from 1.0f to 1.035f (100% visible, never cropped)
            float fgScale = 1.0f + 0.035f * frac;
            slide.fgArtwork.setScaleX(fgScale);
            slide.fgArtwork.setScaleY(fgScale);

            // Ambient background breathes slightly more from 1.0f to 1.05f
            float bgScale = 1.0f + 0.05f * frac;
            slide.ambientBg.setScaleX(bgScale);
            slide.ambientBg.setScaleY(bgScale);
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

    private static class SlideHolder {
        final FrameLayout container;
        final ImageView ambientBg;
        final View scrimView;
        final ImageView fgArtwork;

        SlideHolder(Context context) {
            container = new FrameLayout(context);
            container.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            ambientBg = new ImageView(context);
            ambientBg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            container.addView(ambientBg, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            scrimView = new View(context);
            scrimView.setBackgroundColor(0x99050508);
            container.addView(scrimView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            fgArtwork = new ImageView(context);
            fgArtwork.setScaleType(ImageView.ScaleType.FIT_CENTER);
            float density = context.getResources().getDisplayMetrics().density;
            int padH = (int) (48 * density);
            int padV = (int) (28 * density);
            fgArtwork.setPadding(padH, padV, padH, padV);
            container.addView(fgArtwork, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }

        void setContent(Bitmap artwork, Bitmap ambient) {
            fgArtwork.setImageBitmap(artwork);
            ambientBg.setImageBitmap(ambient);
        }

        void resetTransform() {
            container.setAlpha(0f);
            fgArtwork.setScaleX(1.0f);
            fgArtwork.setScaleY(1.0f);
            ambientBg.setScaleX(1.0f);
            ambientBg.setScaleY(1.0f);
        }
    }
}
