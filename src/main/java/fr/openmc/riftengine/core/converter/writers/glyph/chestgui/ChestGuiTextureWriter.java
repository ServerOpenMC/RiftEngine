package fr.openmc.riftengine.core.converter.writers.glyph.chestgui;

import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class ChestGuiTextureWriter implements PackWriter {
    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        for (ResolvedChestGui chestGui : ChestScreenWriter.chestGuis) {
            BufferedImage src = ImageUtils.read(chestGui.javaTexturePath(javaRootPath));
            Path target = bedrockRootPath.resolve(chestGui.bedrockTexturePath(javaRootPath));

            ImageUtils.writePng(ImageUtils.center(src), target);
        }
    }
}
