package com.appletv.screensaver.artmuseum;

import org.junit.Test;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import static org.junit.Assert.*;

public class ArtCatalogTest {

    @Test
    public void testCatalogNotEmpty() {
        List<ArtPiece> pieces = ArtCatalog.getPieces();
        assertNotNull(pieces);
        assertTrue("Catalog should contain at least 10 classical masterpieces", pieces.size() >= 10);
    }

    @Test
    public void testPieceMetadataIntegrity() {
        for (ArtPiece piece : ArtCatalog.getPieces()) {
            assertNotNull(piece.id);
            assertFalse(piece.id.trim().isEmpty());
            assertNotNull(piece.filename);
            assertTrue("Filename should be webp or jpg", piece.filename.endsWith(".webp") || piece.filename.endsWith(".jpg"));
            assertNotNull(piece.title);
            assertFalse(piece.title.trim().isEmpty());
            assertNotNull(piece.artist);
            assertFalse(piece.artist.trim().isEmpty());
            assertNotNull(piece.year);
            assertFalse(piece.year.trim().isEmpty());
            assertNotNull(piece.museum);
            assertFalse(piece.museum.trim().isEmpty());
        }
    }

    @Test
    public void testGetPieceWraparound() {
        int count = ArtCatalog.getCount();
        assertTrue(count > 0);
        ArtPiece first = ArtCatalog.getPiece(0);
        ArtPiece wrapped = ArtCatalog.getPiece(count);
        assertEquals(first.id, wrapped.id);

        ArtPiece negativeWrapped = ArtCatalog.getPiece(-1);
        ArtPiece last = ArtCatalog.getPiece(count - 1);
        assertEquals(last.id, negativeWrapped.id);
    }

    @Test
    public void testCreateShuffledIndicesContainsAllPiecesWithoutDuplicates() {
        int count = ArtCatalog.getCount();
        List<Integer> shuffled = ArtCatalog.createShuffledIndices(new Random(), -1);
        assertNotNull(shuffled);
        assertEquals(count, shuffled.size());

        Set<Integer> unique = new HashSet<>(shuffled);
        assertEquals("Shuffled list must contain every index exactly once", count, unique.size());
        for (int i = 0; i < count; i++) {
            assertTrue("Shuffled list missing index " + i, unique.contains(i));
        }
    }

    @Test
    public void testCreateShuffledIndicesDoesNotStartWithPreviousIndex() {
        int count = ArtCatalog.getCount();
        Random rng = new Random(42);

        // Test with different previous indices
        int[] testPreviousIndices = {0, 1, 5, count - 1};
        for (int prev : testPreviousIndices) {
            for (int trial = 0; trial < 100; trial++) {
                List<Integer> shuffled = ArtCatalog.createShuffledIndices(rng, prev);
                assertNotEquals("Startup image must not repeat previous image (prev=" + prev + ")",
                        (Integer) prev, shuffled.get(0));
            }
        }
    }

    @Test
    public void testCreateShuffledIndicesRandomnessDistribution() {
        Set<Integer> startingIndices = new HashSet<>();
        Random rng = new Random();
        for (int i = 0; i < 50; i++) {
            List<Integer> shuffled = ArtCatalog.createShuffledIndices(rng, -1);
            startingIndices.add(shuffled.get(0));
        }
        // Across 50 runs with 12 items, we expect multiple different starting artworks
        assertTrue("Starting artwork must be distributed across catalog, not always index 0",
                startingIndices.size() > 1);
    }

    @Test
    public void testCreateShuffledIndicesHandlesNullRandomAndNegativePrevious() {
        List<Integer> shuffled = ArtCatalog.createShuffledIndices(null, -1);
        assertNotNull(shuffled);
        assertEquals(ArtCatalog.getCount(), shuffled.size());
    }
}
