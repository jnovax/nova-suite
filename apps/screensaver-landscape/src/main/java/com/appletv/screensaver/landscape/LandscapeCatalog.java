package com.appletv.screensaver.landscape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class LandscapeCatalog {

    private static final List<LandscapePiece> PIECES = new ArrayList<>();

    static {
        PIECES.add(new LandscapePiece(
                "hay_wain", "hay_wain.webp",
                "Xe chở cỏ khô", "The Hay Wain",
                "John Constable", "1821",
                "National Gallery, London", 1.778f
        ));
        PIECES.add(new LandscapePiece(
                "ninth_wave", "ninth_wave.webp",
                "Con sóng thứ chín", "The Ninth Wave",
                "Ivan Aivazovsky", "1850",
                "State Russian Museum, St. Petersburg", 1.778f
        ));
        PIECES.add(new LandscapePiece(
                "chalk_cliffs", "chalk_cliffs.webp",
                "Vách đá phấn trên đảo Rügen", "Chalk Cliffs on Rügen",
                "Caspar David Friedrich", "1818",
                "Kunstmuseum Winterthur", 1.778f
        ));
        PIECES.add(new LandscapePiece(
                "mont_sainte_victoire", "mont_sainte_victoire.webp",
                "Núi Sainte-Victoire", "Mont Sainte-Victoire",
                "Paul Cézanne", "1904",
                "Philadelphia Museum of Art", 1.778f
        ));
        PIECES.add(new LandscapePiece(
                "oxbow", "oxbow.webp",
                "Khúc quanh sông Oxbow", "The Oxbow",
                "Thomas Cole", "1836",
                "Metropolitan Museum of Art, New York", 1.778f
        ));
        PIECES.add(new LandscapePiece(
                "heart_of_andes", "heart_of_andes.webp",
                "Trái tim dãy Andes", "The Heart of the Andes",
                "Frederic Edwin Church", "1859",
                "Metropolitan Museum of Art, New York", 1.778f
        ));
        PIECES.add(new LandscapePiece(
                "niagara", "niagara.webp",
                "Thác Niagara", "Niagara",
                "Frederic Edwin Church", "1857",
                "National Gallery of Art, Washington D.C.", 1.778f
        ));
        PIECES.add(new LandscapePiece(
                "rocky_mountains", "rocky_mountains.webp",
                "Dãy Rocky", "The Rocky Mountains, Lander's Peak",
                "Albert Bierstadt", "1863",
                "Metropolitan Museum of Art, New York", 1.778f
        ));
        PIECES.add(new LandscapePiece(
                "flood_port_marly", "flood_port_marly.webp",
                "Lũ lụt ở Port-Marly", "Flood at Port-Marly",
                "Alfred Sisley", "1876",
                "Musée d'Orsay, Paris", 1.778f
        ));
        PIECES.add(new LandscapePiece(
                "bridge_narni", "bridge_narni.webp",
                "Cầu ở Narni", "The Bridge at Narni",
                "Camille Corot", "1826",
                "Musée du Louvre, Paris", 1.778f
        ));
    }

    private LandscapeCatalog() {}

    public static List<LandscapePiece> getPieces() {
        return Collections.unmodifiableList(PIECES);
    }

    public static int getCount() {
        return PIECES.size();
    }

    public static LandscapePiece getPiece(int index) {
        if (PIECES.isEmpty()) return null;
        int size = PIECES.size();
        int normalized = ((index % size) + size) % size;
        return PIECES.get(normalized);
    }

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
