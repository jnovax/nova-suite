package com.appletv.screensaver.artmuseum;

import org.junit.Test;
import static org.junit.Assert.*;

public class FastBlurTest {

    @Test
    public void testBoxBlurEmptyOrInvalid() {
        // Should not crash on invalid inputs
        FastBlur.boxBlur(null, 0, 0, 0);
        FastBlur.boxBlur(new int[0], 0, 0, 5);
        FastBlur.boxBlur(new int[10], 5, 2, 0);
    }

    @Test
    public void testBoxBlurSinglePixel() {
        int[] pixels = { 0xFF123456 };
        FastBlur.boxBlur(pixels, 1, 1, 2);
        assertEquals(0xFF123456, pixels[0]);
    }

    @Test
    public void testBoxBlurSmoothsStepGradient() {
        int w = 10;
        int h = 10;
        int[] pixels = new int[w * h];
        // Half black, half white
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixels[y * w + x] = (x < 5) ? 0xFF000000 : 0xFFFFFFFF;
            }
        }

        FastBlur.boxBlur(pixels, w, h, 2);

        // Pixels near the boundary (x=4 and x=5) should be smoothed between black and white
        int midLeft = pixels[5 * w + 4] & 0xFF;
        int midRight = pixels[5 * w + 5] & 0xFF;

        assertTrue("Boundary pixel should be grey, not 0: " + midLeft, midLeft > 0 && midLeft < 255);
        assertTrue("Boundary pixel should be grey, not 255: " + midRight, midRight > 0 && midRight < 255);
    }
}
