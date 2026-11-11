package com.appletv.screensaver.landscape;

import org.junit.Test;
import java.util.List;
import java.util.Random;
import static org.junit.Assert.*;

public class LandscapeCatalogTest {

    @Test
    public void testCatalogHasExactThirteenPieces() {
        assertEquals(13, LandscapeCatalog.getCount());
        assertEquals(13, LandscapeCatalog.getPieces().size());
    }

    @Test
    public void testPiecesHaveValidMetadata() {
        for (int i = 0; i < LandscapeCatalog.getCount(); i++) {
            LandscapePiece piece = LandscapeCatalog.getPiece(i);
            assertNotNull(piece);
            assertNotNull(piece.id);
            assertFalse(piece.id.isEmpty());
            assertTrue(piece.filename.endsWith(".webp"));
            assertNotNull(piece.titleVi);
            assertFalse(piece.titleVi.isEmpty());
            assertNotNull(piece.titleEn);
            assertFalse(piece.titleEn.isEmpty());
            assertNotNull(piece.artist);
            assertFalse(piece.artist.isEmpty());
            assertNotNull(piece.year);
            assertFalse(piece.year.isEmpty());
            assertTrue("Aspect ratio must be > 1.0 for landscape paintings", piece.aspectRatio > 1.0f);
        }
    }

    @Test
    public void testGetPieceNormalization() {
        LandscapePiece first = LandscapeCatalog.getPiece(0);
        LandscapePiece wrapPositive = LandscapeCatalog.getPiece(13);
        LandscapePiece wrapNegative = LandscapeCatalog.getPiece(-13);
        assertEquals(first.id, wrapPositive.id);
        assertEquals(first.id, wrapNegative.id);
    }

    @Test
    public void testShuffleDoesNotRepeatLastShownIndex() {
        Random random = new Random(42);
        for (int prev = 0; prev < LandscapeCatalog.getCount(); prev++) {
            for (int run = 0; run < 10; run++) {
                List<Integer> shuffled = LandscapeCatalog.createShuffledIndices(random, prev);
                assertEquals(13, shuffled.size());
                assertNotEquals("First item of shuffled playlist must not be previous index", (Integer) prev, shuffled.get(0));
            }
        }
    }
}
