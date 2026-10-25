package fr.openmc.riftengine.core.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ImageUtils {
    public static BufferedImage read(Path path) throws IOException {
        return ImageIO.read(path.toFile());
    }

    public static void writePng(BufferedImage image, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        ImageIO.write(image, "png", target.toFile());
    }

    public static BufferedImage center(BufferedImage src) {
        return center(src, src.getWidth(), src.getHeight());
    }

    public static BufferedImage center(BufferedImage src, int canvasW, int canvasH) {
        int minX = src.getWidth(), minY = src.getHeight(), maxX = -1, maxY = -1;

        // * detecte les zones transparentes
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                if ((src.getRGB(x, y) >>> 24) != 0) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < 0) return src;

        int contentW = maxX - minX + 1;
        int contentH = maxY - minY + 1;
        int offsetX = (canvasW - contentW) / 2;
        int offsetY = (canvasH - contentH) / 2;

        BufferedImage result = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < contentH; y++) {
            for (int x = 0; x < contentW; x++) {
                result.setRGB(offsetX + x, offsetY + y, src.getRGB(minX + x, minY + y));
            }
        }
        return result;
    }

    /**
     * Grossomodo calcule la valeur y ou y'a une zone transparentes
     */
    public static int centerShiftY(BufferedImage src) {
        int minY = -1;
        int maxY = -1;

        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                if ((src.getRGB(x, y) >>> 24) != 0) {
                    if (minY == -1) minY = y;
                    maxY = y;
                    break;
                }
            }
        }

        if (minY == -1) return 0;

        int contentH = maxY - minY + 1;
        return (src.getHeight() - contentH) / 2 - minY;
    }

    public static int findGridLeft(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();

        long[] edge = new long[w];
        for (int x = 1; x < w; x++) {
            long s = 0;
            for (int y = 0; y < h; y++) s += Math.abs(lum(img, x, y) - lum(img, x - 1, y));
            edge[x] = s;
        }

        long[] score = new long[w];
        long best = -1;
        int bestX = -1;
        for (int p = 1; p + 162 < w; p++) {
            long s = 0;
            for (int k = 0; k <= 9; k++) s += edge[p + k * 18];
            score[p] = s;
            if (s > best) { best = s; bestX = p; }
        }
        if (bestX == -1) return -1;

        long rival = 0;
        for (int p = 1; p + 162 < w; p++) {
            int d = Math.floorMod(p - bestX, 18);
            if (Math.min(d, 18 - d) > 2) rival = Math.max(rival, score[p]);
        }
        return best >= rival * 1.15 ? bestX : -1;
    }

    private static int lum(BufferedImage img, int x, int y) {
        int argb = img.getRGB(x, y);
        if ((argb >>> 24) == 0) return 0;
        return (((argb >> 16) & 255) + ((argb >> 8) & 255) + (argb & 255)) / 3;
    }
}
