package fr.openmc.riftengine.core.converter.writers.glyph.icons;

import fr.openmc.core.bootstrap.integration.OMCLogger;
import fr.openmc.riftengine.core.RiftRegistry;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.registry.glyphs.GlyphsRegistry;
import fr.openmc.riftengine.core.registry.glyphs.types.IconGlyph;
import fr.openmc.riftengine.core.scanner.icons.IconEntry;
import fr.openmc.riftengine.core.scanner.icons.IconScanner;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class IconsWriter implements PackWriter {

    private final Path itemsAdderContentsPath;

    public IconsWriter(Path itemsAdderContentsPath) {
        this.itemsAdderContentsPath = itemsAdderContentsPath;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        IconScanner scanner = RiftRegistry.SCANNERS.ICONS;
        List<IconEntry> entries;
        try {
            entries = scanner.scan(itemsAdderContentsPath);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du scan des contents d'items adder " + e.getMessage(), e);
        }

        List<ResolvedIcon> resolved = new ArrayList<>();
        int skipped = 0;
        for (IconEntry entry : entries) {
            Path imagePath = entry.path().apply(javaRootPath);

            if (!Files.exists(imagePath)) {
                OMCLogger.errorFormatted("Image introuvable pour " + entry.namespacedId() + " dans le path " + imagePath + " (skip)");
                skipped++;
                continue;
            }

            BufferedImage image;
            try {
                image = ImageIO.read(imagePath.toFile());
            } catch (IOException e) {
                OMCLogger.errorFormatted("Erreur de lecture pour " + entry.namespacedId() + " (skip) : " + e.getMessage());
                skipped++;
                continue;
            }

            if (image == null) {
                OMCLogger.errorFormatted("Image non lisible pour " + entry.namespacedId() + " (skip)");
                skipped++;
                continue;
            }

            resolved.add(new ResolvedIcon(entry, image));
        }

        if (skipped > 0) {
            OMCLogger.errorFormatted("Image manquante pour " + skipped + " emoji(s)/icon(s) sur " + entries.size() + ".");
        }

        if (resolved.isEmpty()) return;

        Map<Integer, List<ResolvedIcon>> groupedByGroupSize = new TreeMap<>();
        for (ResolvedIcon r : resolved) {
            int maxDim = Math.max(r.image.getWidth(), r.image.getHeight());
            int bucket = IconsRegrouperUtils.pickBestGroup(maxDim);
            groupedByGroupSize.computeIfAbsent(bucket, b -> new ArrayList<>()).add(r);
        }

        for (Map.Entry<Integer, List<ResolvedIcon>> group : groupedByGroupSize.entrySet()) {
            Dimension size = new Dimension(group.getKey(), group.getKey());
            List<ResolvedIcon> icons = group.getValue();

            for (int slotPage = 0; slotPage < icons.size(); slotPage += GlyphsRegistry.MAX_PER_PAGE) {
                List<ResolvedIcon> iconPerPage = icons.subList(slotPage,
                        Math.min(slotPage + GlyphsRegistry.MAX_PER_PAGE, icons.size()));
                writePage(bedrockRootPath, size, iconPerPage);
            }
        }
    }

    private void writePage(Path bedrockRootPath, Dimension size, List<ResolvedIcon> icons) throws IOException {
        String page = RiftRegistry.GLYPHS.nextGlyphPage();

        int cellWidth = size.width;
        int cellHeight = size.height;

        BufferedImage pageImage = new BufferedImage(
                cellWidth * GlyphsRegistry.GRID_SIZE,
                cellHeight * GlyphsRegistry.GRID_SIZE,
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D imageEditable = pageImage.createGraphics();

        try {
            for (int i = 0; i < icons.size(); i++) {
                ResolvedIcon resolvedEmoji = icons.get(i);
                int row = i / GlyphsRegistry.GRID_SIZE;
                int col = i % GlyphsRegistry.GRID_SIZE;

                imageEditable.drawImage(resolvedEmoji.image, col * cellWidth, row * cellHeight, null);

                RiftRegistry.GLYPHS.register(new IconGlyph(
                        resolvedEmoji.entry.namespacedId(),
                        page,
                        row,
                        col
                ));
            }
        } finally {
            imageEditable.dispose();
        }

        String fileName = "glyph_" + page + ".png";
        Path glyphPath = bedrockRootPath.resolve("font").resolve(fileName);
        Files.createDirectories(glyphPath.getParent());
        ImageIO.write(pageImage, "png", glyphPath.toFile());
    }

    private record ResolvedIcon(IconEntry entry, BufferedImage image) {}
}
