package fr.openmc.riftengine.core.converter.writers.glyph.chestgui;

import fr.openmc.riftengine.core.converter.writers.PackWriter;

import java.io.IOException;
import java.nio.file.Path;

public class ChestGuiTextureWriter implements PackWriter {
    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        for (ResolvedChestGui chestGui : ChestScreenWriter.chestGuis) {
            this.copyFile(chestGui.javaTexturePath(javaRootPath),
                    bedrockRootPath.resolve(chestGui.bedrockTexturePath(javaRootPath)));
        }
    }
}
