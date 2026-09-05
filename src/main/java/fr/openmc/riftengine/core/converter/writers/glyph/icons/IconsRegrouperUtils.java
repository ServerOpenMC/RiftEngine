package fr.openmc.riftengine.core.converter.writers.glyph.icons;

public class IconsRegrouperUtils {

    private final static int[] GROUP_SIZE = {8, 16, 24, 32, 48, 64, 96, 128, 192, 256};

    /**
     * Utilitaire pour choisir la meilleur dimension dans lequel mettre l'emoji/icons
     * @param width la largeur de l'emoji
     * @param height la hauteur de l'emoji
     * @return la dimension la plus proche de maxDim
     */
    public static int pickBestGroup(int width, int height) {
        int maxDim = Math.max(width, height);
        return pickBestSize(maxDim);
    }

    /**
     * Utilitaire pour choisir la meilleur dimension dans lequel mettre l'emoji/icons
     * @param maxDim la dimension maximale que l'on peut mettre
     * @return la dimension la plus proche de maxDim
     */
    public static int pickBestSize(int maxDim) {
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
