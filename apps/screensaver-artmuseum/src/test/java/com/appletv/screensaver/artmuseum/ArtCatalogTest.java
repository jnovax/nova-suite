package com.appletv.screensaver.artmuseum;

import org.junit.Test;
import java.util.List;
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
}
