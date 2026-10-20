package fr.openmc.riftengine.core.converter.writers.manifest;

import fr.openmc.riftengine.core.converter.writers.PackWriter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class IconWriter implements PackWriter {
    private static final String OUTPUT_FILE = "pack_icon.png";

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/pack_icon.png")) {
            if (is == null) throw new IOException("Resource introuvable : pack_icon.png");

            Files.copy(is, bedrockRootPath.resolve(OUTPUT_FILE), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
