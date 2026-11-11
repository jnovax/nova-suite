package com.appletv.screensaver.artmuseum;

import android.graphics.Bitmap;

public final class FastBlur {

    private static final int DEFAULT_THUMB_WIDTH = 80;
    private static final int DEFAULT_THUMB_HEIGHT = 45;
    private static final int DEFAULT_RADIUS = 5;

    private FastBlur() {}

    /**
     * Generates a soft, ambient blurred thumbnail from the source bitmap.
     * Bilinear scaling by ImageView to 1080p produces a creamy Gaussian-like glow.
     */
    public static Bitmap createAmbientBlur(Bitmap source) {
        return createAmbientBlur(source, DEFAULT_THUMB_WIDTH, DEFAULT_THUMB_HEIGHT, DEFAULT_RADIUS);
    }

    public static Bitmap createAmbientBlur(Bitmap source, int thumbWidth, int thumbHeight, int radius) {
        if (source == null || source.isRecycled() || thumbWidth <= 0 || thumbHeight <= 0) {
            return null;
        }

        Bitmap small = null;
        try {
            small = Bitmap.createScaledBitmap(source, thumbWidth, thumbHeight, true);
            // Ensure ARGB_8888 mutable bitmap for pixel manipulation
            Bitmap mutable = small.isMutable() ? small : small.copy(Bitmap.Config.ARGB_8888, true);
            if (mutable != small) {
                small.recycle();
                small = mutable;
            }

            int w = small.getWidth();
            int h = small.getHeight();
            int[] pixels = new int[w * h];
            small.getPixels(pixels, 0, w, 0, 0, w, h);

            boxBlur(pixels, w, h, radius);
            boxBlur(pixels, w, h, radius); // 2nd pass for gaussian-like softness

            small.setPixels(pixels, 0, w, 0, 0, w, h);
            return small;
        } catch (Throwable t) {
            if (small != null && !small.isRecycled()) {
                small.recycle();
            }
            return null;
        }
    }

    /**
     * Sliding-window O(N) 2D box blur.
     */
    public static void boxBlur(int[] pixels, int w, int h, int r) {
        if (pixels == null || w <= 0 || h <= 0 || r <= 0) return;
        r = Math.min(r, Math.max(1, Math.min(w - 1, h - 1)));
        if (r <= 0) return;

        int[] temp = new int[w * h];
        int win = 2 * r + 1;

        // 1. Horizontal pass
        for (int y = 0; y < h; y++) {
            int row = y * w;
            int rs = 0, gs = 0, bs = 0;

            for (int i = -r; i <= r; i++) {
                int idx = Math.min(w - 1, Math.max(0, i));
                int c = pixels[row + idx];
                rs += (c >> 16) & 0xFF;
                gs += (c >> 8) & 0xFF;
                bs += c & 0xFF;
            }

            for (int x = 0; x < w; x++) {
                temp[row + x] = (0xFF << 24) | ((rs / win) << 16) | ((gs / win) << 8) | (bs / win);
                int left = Math.min(w - 1, Math.max(0, x - r));
                int right = Math.min(w - 1, Math.max(0, x + r + 1));
                int cLeft = pixels[row + left];
                int cRight = pixels[row + right];

                rs += ((cRight >> 16) & 0xFF) - ((cLeft >> 16) & 0xFF);
                gs += ((cRight >> 8) & 0xFF) - ((cLeft >> 8) & 0xFF);
                bs += (cRight & 0xFF) - (cLeft & 0xFF);
            }
        }

        // 2. Vertical pass
        for (int x = 0; x < w; x++) {
            int rs = 0, gs = 0, bs = 0;

            for (int i = -r; i <= r; i++) {
                int idx = Math.min(h - 1, Math.max(0, i));
                int c = temp[idx * w + x];
                rs += (c >> 16) & 0xFF;
                gs += (c >> 8) & 0xFF;
                bs += c & 0xFF;
            }

            for (int y = 0; y < h; y++) {
                pixels[y * w + x] = (0xFF << 24) | ((rs / win) << 16) | ((gs / win) << 8) | (bs / win);
                int top = Math.min(h - 1, Math.max(0, y - r));
                int bottom = Math.min(h - 1, Math.max(0, y + r + 1));
                int cTop = temp[top * w + x];
                int cBottom = temp[bottom * w + x];

                rs += ((cBottom >> 16) & 0xFF) - ((cTop >> 16) & 0xFF);
                gs += ((cBottom >> 8) & 0xFF) - ((cTop >> 8) & 0xFF);
                bs += (cBottom & 0xFF) - (cTop & 0xFF);
            }
        }
    }
}
