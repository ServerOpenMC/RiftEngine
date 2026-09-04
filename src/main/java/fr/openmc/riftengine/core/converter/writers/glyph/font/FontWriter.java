package fr.openmc.riftengine.core.converter.writers.glyph.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.openmc.riftengine.core.RiftRegistry;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.registry.glyphs.types.FontGlyph;
import fr.openmc.riftengine.core.utils.IdentifierUtils;
import fr.openmc.riftengine.core.utils.ScanUtils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * écrit pas réelement dans un dossier fonts dans le resourcepack bedorck,
 * aucun support de font sur bedrock, donc on passe par les glyphs.
 * et faut pas oublier de le modifier sur le coté du client
 */
public class FontWriter implements PackWriter {
    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        List<Path> fonts = ScanUtils.scanAllPath(javaRootPath, "font");

        for (Path fontFile : fonts) {
            if (isMinecraftFont(fontFile)) continue;
            if (!fontFile.getFileName().toString().endsWith(".json")) continue;

            JsonObject fontJson;
            try (Reader reader = Files.newBufferedReader(fontFile)) {
                fontJson = JsonParser.parseReader(reader).getAsJsonObject();
            }

            String fontNamespace = null;
            String fontId = null;
            File fontPng = null;
            JsonArray fontsChar = null;
            try {
                for (JsonElement c : fontJson.getAsJsonArray("providers")) {
                    JsonObject provider = c.getAsJsonObject();

                    // todo : support les autres providers (space, TTF, ...)
                    if (provider.get("type").getAsString().equals("bitmap")) {
                        String file = IdentifierUtils.normalizeId(provider.get("file").getAsString());

                        fontNamespace = file.split(":")[0];
                        fontId = file.split(":")[1]
                                .replace("font/", "")
                                .replace(".png", "");
                        fontPng = IdentifierUtils.resolveTextureId(javaRootPath, file).toFile();
                        fontsChar = provider.getAsJsonArray("chars");
                    }
                }
            } catch (Exception e) {
                throw new IOException("Erreur lors de la lecture du fichier font " + fontFile, e);
            }

            if (fontPng == null || fontsChar == null || fontNamespace == null || fontId == null) return;

            List<FontChar.InternalFontChar> positions = FontChar.extractCharPositions(fontsChar);

            String page = RiftRegistry.GLYPHS.nextGlyphPage();

            for (FontChar.InternalFontChar pos : positions) {
                String namespace = fontNamespace + ":" + fontId + ":" + pos.character();
                RiftRegistry.GLYPHS.register(new FontGlyph(namespace, page, pos.row(), pos.col()));
            }

            String fileName = "glyph_" + page + ".png";
            Path glyphPath = bedrockRootPath.resolve("font").resolve(fileName);
            Files.createDirectories(glyphPath.getParent());
            Files.copy(fontPng.toPath(), glyphPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean isMinecraftFont(Path fontFile) {
        Path namespaceDir = fontFile.getParent().getParent();
        if (namespaceDir == null) return false;
        return namespaceDir.getFileName().toString().equals("minecraft");
    }
}
