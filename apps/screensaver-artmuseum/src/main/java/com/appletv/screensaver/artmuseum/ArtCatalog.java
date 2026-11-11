package com.appletv.screensaver.artmuseum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class ArtCatalog {

    private static final List<ArtPiece> PIECES = new ArrayList<>();

    static {
        PIECES.add(new ArtPiece(
                "starry_night",
                "starry_night.webp",
                "The Starry Night",
                "Vincent van Gogh",
                "1889",
                "Museum of Modern Art, New York"
        ));
        PIECES.add(new ArtPiece(
                "water_lilies",
                "water_lilies.webp",
                "Water Lilies",
                "Claude Monet",
                "1916",
                "Musée de l'Orangerie, Paris"
        ));
        PIECES.add(new ArtPiece(
                "mona_lisa",
                "mona_lisa.webp",
                "Mona Lisa",
                "Leonardo da Vinci",
                "1503",
                "Musée du Louvre, Paris"
        ));
        PIECES.add(new ArtPiece(
                "girl_with_pearl",
                "girl_with_pearl.webp",
                "Girl with a Pearl Earring",
                "Johannes Vermeer",
                "1665",
                "Mauritshuis, The Hague"
        ));
        PIECES.add(new ArtPiece(
                "great_wave",
                "great_wave.webp",
                "The Great Wave off Kanagawa",
                "Katsushika Hokusai",
                "1831",
                "Tokyo National Museum"
        ));
        PIECES.add(new ArtPiece(
                "the_kiss",
                "the_kiss.webp",
                "The Kiss",
                "Gustav Klimt",
                "1908",
                "Österreichische Galerie Belvedere, Vienna"
        ));
        PIECES.add(new ArtPiece(
                "sunflowers",
                "sunflowers.webp",
                "Sunflowers",
                "Vincent van Gogh",
                "1888",
                "National Gallery, London"
        ));
        PIECES.add(new ArtPiece(
                "grande_jatte",
                "grande_jatte.webp",
                "A Sunday on La Grande Jatte",
                "Georges Seurat",
                "1884",
                "Art Institute of Chicago"
        ));
        PIECES.add(new ArtPiece(
                "wanderer",
                "wanderer.webp",
                "Wanderer above the Sea of Fog",
                "Caspar David Friedrich",
                "1818",
                "Hamburger Kunsthalle"
        ));
        PIECES.add(new ArtPiece(
                "last_supper",
                "last_supper.webp",
                "The Last Supper",
                "Leonardo da Vinci",
                "1498",
                "Santa Maria delle Grazie, Milan"
        ));
        PIECES.add(new ArtPiece(
                "school_of_athens",
                "school_of_athens.webp",
                "The School of Athens",
                "Raphael",
                "1511",
                "Apostolic Palace, Vatican City"
        ));
        PIECES.add(new ArtPiece(
                "the_scream",
                "the_scream.webp",
                "The Scream",
                "Edvard Munch",
                "1893",
                "National Museum, Oslo"
        ));
    }

    private ArtCatalog() {}

    public static List<ArtPiece> getPieces() {
        return Collections.unmodifiableList(PIECES);
    }

    public static int getCount() {
        return PIECES.size();
    }

    public static ArtPiece getPiece(int index) {
        if (PIECES.isEmpty()) return null;
        int size = PIECES.size();
        int normalized = ((index % size) + size) % size;
        return PIECES.get(normalized);
    }

    /**
     * Generates a randomly shuffled playlist of indices [0..count-1].
     * If previousIndex >= 0 and count > 1, guarantees that the first item
     * does not repeat previousIndex so every screensaver launch starts fresh.
     */
    public static List<Integer> createShuffledIndices(Random random, int previousIndex) {
        int count = PIECES.size();
        List<Integer> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(i);
        }
        if (count <= 1) {
            return list;
        }
        Random rng = (random != null) ? random : new Random();
        Collections.shuffle(list, rng);
        if (previousIndex >= 0 && list.get(0) == previousIndex) {
            int swapIndex = 1 + rng.nextInt(count - 1);
            Collections.swap(list, 0, swapIndex);
        }
        return list;
    }
}
