package fr.openmc.riftengine.core.converter.writers.tooltip.textures;

import fr.openmc.riftengine.core.resolvers.mcmeta.gui.ResolvedGuiMcMeta;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Compense un problème, les animations de frames, doit etre en horizontal au lieu de vertical, on change d'orientation puis
 * on copie la texture ou elle doit etre
 */
public class FrameTextureConverter {
    public static void convert(ResolvedGuiMcMeta gui,
                               Path source,
                               Path destination) throws IOException {
        BufferedImage src = ImageIO.read(source.toFile());

        int width = src.getWidth();
        int height = src.getHeight();

        int realWidth = gui.widthFrame() != null ? gui.widthFrame() :
                (gui.heightFrame() != null ? gui.heightFrame() : Math.min(width, height));
        int realHeight = gui.heightFrame() != null ? gui.heightFrame() :
                (gui.widthFrame() != null ? gui.widthFrame() : Math.min(width, height));

        int cols = width / realWidth;
        int numberFrame = cols * (height / realHeight);

        BufferedImage out = new BufferedImage(realWidth * numberFrame, realHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = out.createGraphics();
        try {
            for (int i = 0; i < numberFrame; i++) {
                graphics.drawImage(src.getSubimage(
                                (i % cols) * realWidth,
                                (i / cols) * realHeight,
                                realWidth,
                                realHeight),
                        i * realWidth,
                        0,
                        null);
            }
        } finally {
            graphics.dispose();
        }

        if (destination.getParent() != null)
            Files.createDirectories(destination.getParent());
        ImageIO.write(out, "png", destination.toFile());
    }
}
