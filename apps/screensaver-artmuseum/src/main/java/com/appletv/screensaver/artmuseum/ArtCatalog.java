package com.appletv.screensaver.artmuseum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
                "night_watch",
                "night_watch.webp",
                "The Night Watch",
                "Rembrandt van Rijn",
                "1642",
                "Rijksmuseum, Amsterdam"
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
                "dance_class",
                "dance_class.webp",
                "The Dance Class",
                "Edgar Degas",
                "1874",
                "Musée d'Orsay, Paris"
        ));
        PIECES.add(new ArtPiece(
                "birth_of_venus",
                "birth_of_venus.webp",
                "The Birth of Venus",
                "Sandro Botticelli",
                "1485",
                "Uffizi Gallery, Florence"
        ));
        PIECES.add(new ArtPiece(
                "creation_of_adam",
                "creation_of_adam.webp",
                "The Creation of Adam",
                "Michelangelo",
                "1512",
                "Sistine Chapel, Vatican City"
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
}
