package fr.openmc.riftengine.core.converter.writers.glyph.chestgui;

import fr.openmc.riftengine.api.scanner.icons.IconEntry;
import fr.openmc.riftengine.core.utils.PathUtils;

import java.nio.file.Path;

public record ResolvedChestGui(IconEntry entry, int width, int height) {
    public String key() {
        return entry.key();
    }

    public Path javaTexturePath(Path javaRootPath) {
        return entry.path().apply(javaRootPath);
    }

    public Path bedrockTexturePath(Path javaRootPath) {
        return PathUtils.getPathFromRoot(javaTexturePath(javaRootPath), "textures")                                                                                        ;
    }
}
