package com.appletv.launcher;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AppAdapterLayoutTest {

    @Test
    public void fiveColumnsYieldsExactThreeHundredPixelIconAt1080p() {
        float density = 2.0f; // 1080p base at 320dpi: 1dp = 2px
        int screenWidthPx = 1920;
        int safeMarginPx = 90 * 2; // left + right safe area (45dp * 2 * 2) = 180px
        int innerWidthPx = screenWidthPx - safeMarginPx; // 1740px

        int tileWidth = AppAdapter.tileWidthPx(innerWidthPx, 5, density);
        assertEquals(300, tileWidth);

        int tileHeight = AppAdapter.tileHeightPx(tileWidth);
        assertEquals(180, tileHeight);
    }

    @Test
    public void sixColumnsShrinksProportionallyMaintainingFiveThreeAspectRatio() {
        float density = 2.0f;
        int innerWidthPx = 1740;

        int tileWidth = AppAdapter.tileWidthPx(innerWidthPx, 6, density);
        assertEquals(242, tileWidth);

        int tileHeight = AppAdapter.tileHeightPx(tileWidth);
        assertEquals(145, tileHeight);
        // Aspect ratio 5:3: 242 * 3 / 5 = 145.2 -> 145
    }

    @Test
    public void cornerRadiusMatchesSpec() {
        float density = 2.0f;
        float radius = AppAdapter.cornerRadius(300, density);
        assertEquals(24.0f, radius, 0.001f);
    }

    @Test
    public void focusedDimensionsMatchAppleTvSpec() {
        int unfocusedW = 300;
        int unfocusedH = 180;
        float focusedW = unfocusedW * AppAdapter.FOCUS_SCALE;
        float focusedH = unfocusedH * AppAdapter.FOCUS_SCALE;

        assertEquals(370f, focusedW, 0.5f);
        assertEquals(222f, focusedH, 0.5f);
    }

    @Test
    public void dockTilesHideBottomLabelWhileGridTilesShowIt() {
        int dockSize = 5;
        for (int pos = 0; pos < dockSize; pos++) {
            org.junit.Assert.assertFalse(AppAdapter.shouldShowTileName(pos, dockSize));
        }
        org.junit.Assert.assertTrue(AppAdapter.shouldShowTileName(5, dockSize));
        org.junit.Assert.assertTrue(AppAdapter.shouldShowTileName(12, dockSize));
    }

    @Test
    public void dockSitsNearBottomAndKeepsFirstGridRowOffScreenYetCompact() {
        float density = 2.0f;
        int gridHeightPx = 1000;
        int tileHeightPx = 180; // 90dp * 2
        int paddingTop = DockDecoration.computeDockPaddingTopPx(gridHeightPx, tileHeightPx, density);

        // Platter bottom = paddingTop + outRect.top (20px) + itemPadTop (20px) + tileH (180px) + padV (20px)
        int platterBottom = paddingTop + Math.round((2 * DockDecoration.PAD_V_DP + 10f) * density) + tileHeightPx;
        // Dock platter sits 20dp (40px) above bottom edge
        assertEquals(gridHeightPx - Math.round(DockDecoration.DOCK_BOTTOM_MARGIN_DP * density), platterBottom);

        // First grid row card top when scrollY = 0 must be >= gridHeightPx (off-screen)
        int row1CardTop = platterBottom
                + Math.round(DockDecoration.PAD_V_DP * density)
                + Math.round(DockDecoration.FIRST_ROW_TOP_GAP_DP * density)
                + Math.round(10f * density);
        assertTrue(row1CardTop >= gridHeightPx);
        // Yet the visual gap from dock platter bottom to row 1 card top is compact (<= 36dp = 72px, down from 168px)
        assertTrue(row1CardTop - platterBottom <= Math.round(36f * density));
    }
}
