package com.appletv.launcher;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight 4K Wallpaper slideshow screensaver engine.
 * Reuses local wallpapers in /sdcard/Android/data/com.appletv.launcher/files/wallpapers/.
 * All animations stop immediately when detached.
 */
final class ScreensaverView extends FrameLayout {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final List<File> files = new ArrayList<>();
    private ExecutorService decoder;
    private View clock;
    private GradientDrawable glow;
    private final int[] colors = {0xFF2B5CFF, 0xFF00A39A, 0xFF8A3FFC, 0xFFE0661B, 0xFFD6336C};
    private int colorIndex;
    private ValueAnimator colorAnimation;
    private ImageView displayed;
    private boolean running;
    private int generation;
    private int imageIndex;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (!running || clock == null) return;
            clock.animate().alpha(0f).setDuration(900).withEndAction(() -> {
                if (!running) return;
                placeClock();
                clock.animate().alpha(1f).setDuration(1200).start();
            }).start();
            int from = colors[colorIndex];
            colorIndex = (colorIndex + 1) % colors.length;
            colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), from, colors[colorIndex]);
            colorAnimation.setDuration(8000);
            colorAnimation.addUpdateListener(a -> setGlow((int) a.getAnimatedValue()));
            colorAnimation.start();
            handler.postDelayed(this, 60000);
        }
    };

    private final Runnable slide = new Runnable() {
        @Override public void run() {
            if (!running || files.isEmpty()) return;
            File file = files.get(imageIndex++ % files.size());
            final int currentGeneration = generation;
            decoder.execute(() -> {
                Bitmap bitmap = decode(file);
                handler.post(() -> {
                    if (!running || generation != currentGeneration) {
                        if (bitmap != null) bitmap.recycle();
                        return;
                    }
                    if (bitmap != null) showImage(bitmap);
                    handler.postDelayed(this, 20000);
                });
            });
        }
    };

    ScreensaverView(Context context) {
        super(context);
        setBackgroundColor(0xff050508);

        LayoutInflater.from(context).inflate(R.layout.dream_clock, this, true);
        getChildAt(0).setBackgroundResource(0);
        clock = findViewById(R.id.dreamContent);
        View ambient = findViewById(R.id.dreamAmbient);
        glow = new GradientDrawable();
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        ambient.setBackground(glow);
        ambient.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> glow.setGradientRadius(Math.max(1, (r - l) * .55f)));
        setGlow(colors[0]);

        if (!ScreensaverPreferences.overlay(context)) {
            clock.setVisibility(GONE);
        }

        File dir = Config.dir(context);
        if (dir != null) {
            File[] list = new File(dir, "wallpapers").listFiles((d, name) ->
                    !name.startsWith(".") && name.toLowerCase(Locale.US).endsWith(".jpg"));
            if (list != null) {
                Arrays.sort(list);
                files.addAll(Arrays.asList(list));
            }
            File current = new File(dir, Config.WALLPAPER_NAME);
            if (files.isEmpty() && current.isFile()) {
                files.add(current);
            }
        }
    }

    void start() {
        if (running) return;
        running = true;
        generation++;
        decoder = Executors.newSingleThreadExecutor();
        if (clock != null) clock.post(() -> { if (running) placeClock(); });
        handler.postDelayed(clockTick, 60000);
        handler.post(slide);
    }

    void stop() {
        running = false;
        generation++;
        handler.removeCallbacksAndMessages(null);
        if (clock != null) clock.animate().cancel();
        if (colorAnimation != null) colorAnimation.cancel();
        if (displayed != null) displayed.animate().cancel();
        if (decoder != null) decoder.shutdownNow();
    }

    private void setGlow(int color) {
        if (glow == null) return;
        int c = color & 0x00ffffff;
        glow.setColors(new int[]{0x40000000 | c, 0x14000000 | c, c});
    }

    private void placeClock() {
        if (clock == null) return;
        int maxX = Math.max(0, getWidth() - clock.getWidth());
        int maxY = Math.max(0, getHeight() - clock.getHeight());
        float x = maxX * (.1f + .8f * random.nextFloat());
        float y = maxY * (.1f + .8f * random.nextFloat());
        clock.setX(x);
        clock.setY(y);
        if (getWidth() > 0 && getHeight() > 0) {
            glow.setGradientCenter((x + clock.getWidth() / 2f) / getWidth(), (y + clock.getHeight() / 2f) / getHeight());
        }
    }

    private Bitmap decode(File file) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), o);
            int w = o.outWidth, h = o.outHeight;
            if (w <= 0 || h <= 0) return null;
            int targetW = Math.min(1920, getResources().getDisplayMetrics().widthPixels);
            int targetH = Math.min(1080, getResources().getDisplayMetrics().heightPixels);
            int sample = WallpaperSize.sampleSize(w, h, Math.max(1, targetW), Math.max(1, targetH));
            while (!WallpaperSize.accepted(w / sample, h / sample)) sample *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = sample;
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            int[] scale = WallpaperSize.coverScale(w / sample, h / sample, Math.max(1, targetW), Math.max(1, targetH));
            if (scale != null) {
                o.inScaled = true;
                o.inDensity = scale[0];
                o.inTargetDensity = scale[1];
            }
            return BitmapFactory.decodeFile(file.getPath(), o);
        } catch (RuntimeException | OutOfMemoryError e) {
            return null;
        }
    }

    private void showImage(Bitmap bitmap) {
        ImageView old = displayed;
        ImageView image = new ImageView(getContext());
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageBitmap(bitmap);
        image.setAlpha(0f);
        int imageLayer = old == null ? 0 : indexOfChild(old) + 1;
        addView(image, imageLayer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        displayed = image;
        image.animate().alpha(1f).setDuration(1500).withEndAction(() -> {
            if (old != null) removeView(old);
            if (running) image.animate().scaleX(1.05f).scaleY(1.05f).setDuration(18500).start();
        }).start();
    }
}
