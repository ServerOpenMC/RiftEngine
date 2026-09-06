package fr.openmc.riftengine.core.converter.writers.glyph.icons;

public class IconsRegrouperUtils {

    private final static int[] GROUP_SIZE = {8, 16, 20, 24, 32, 48, 64, 96, 128, 192, 256, 512};

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
        for (int size : GROUP_SIZE) {
            if (maxDim <= size) {
                return size;
            }
        }

        return GROUP_SIZE[GROUP_SIZE.length - 1];
    }
}
