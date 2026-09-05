package fr.openmc.riftengine.core.converter.writers.glyph.icons;

public class IconsRegrouperUtils {

    private final static int[] GROUP_SIZE = {16, 32, 64, 128, 256};

    /**
     * Utilitaire pour choisir la meilleur dimension dans lequel mettre l'emoji/icons
     * @param maxDim la dimension maximale que l'on peut mettre
     * @return la dimension la plus proche de maxDim
     */
    public static int pickBestGroup(int maxDim) {
        int close = GROUP_SIZE[0];
        int minDiff = Math.abs(maxDim - close);

        for (int size : GROUP_SIZE) {
            int diff = Math.abs(maxDim - size);

            if (diff < minDiff) {
                minDiff = diff;
                close = size;
            }
        }

        return close;
    }
}
