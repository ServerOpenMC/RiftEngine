package fr.openmc.riftengine.core.converter.writers.glyph.icons;

import fr.openmc.core.OMCRegistry;
import fr.openmc.riftengine.api.scanner.icons.IconEntry;
import fr.openmc.riftengine.api.scanner.icons.IconScanner;
import fr.openmc.riftengine.core.RiftLog;
import fr.openmc.riftengine.core.RiftPlugin;
import fr.openmc.riftengine.core.RiftRegistry;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.converter.writers.glyph.chestgui.ChestScreenWriter;
import fr.openmc.riftengine.core.converter.writers.glyph.chestgui.ResolvedChestGui;
import fr.openmc.riftengine.core.registry.glyphs.GlyphsRegistry;
import fr.openmc.riftengine.core.registry.glyphs.types.IconGlyph;

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

/**
 * Writer qui gère la prise en charge des icons/emojis ("font_images" d'items adder)
 */
public class IconsWriter implements PackWriter {

    private final Path itemsAdderContentsPath;

    public IconsWriter(Path itemsAdderContentsPath) {
        this.itemsAdderContentsPath = itemsAdderContentsPath;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        IconsTypeConfig forcedType = IconsTypeConfig.load(RiftPlugin.getInstance().CONFIG_FOLDER
                .resolve("icons_types.yml")); // todo: degeu ça

        IconScanner scanner = OMCRegistry.SCANNERS.ICONS;
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
                RiftLog.IMAGE_NOT_FOUND.send(entry.namespacedId(), imagePath);
                skipped++;
                continue;
            }

            BufferedImage image;
            try {
                image = ImageIO.read(imagePath.toFile());
            } catch (IOException e) {
                RiftLog.ERROR_READING.send(entry.namespacedId());
                skipped++;
                continue;
            }

            if (image == null) {
                RiftLog.ERROR_READING.send(entry.namespacedId());
                skipped++;
                continue;
            }

            IconsType type = forcedType.get(entry.namespacedId());
            if (type == null) {
                type = IconsType.detect(image.getWidth(), image.getHeight());
            }

            if (type.equals(IconsType.MENU_GUI)) {
                ChestScreenWriter.chestGuis.add(new ResolvedChestGui(entry, image.getWidth(), image.getHeight()));
                continue;
            }

            resolved.add(new ResolvedIcon(entry, image, type));
        }

        if (skipped > 0) {
            RiftLog.ICON_SKIPPED.send(skipped, entries.size());
        }

        if (resolved.isEmpty()) return;

        Map<Integer, List<ResolvedIcon>> groupedByGroupSize = new TreeMap<>();
        for (ResolvedIcon r : resolved) {
            Dimension finalSize = r.type().finalSize(r.entry(), r.image().getWidth(), r.image().getHeight());
            int targetFinalSize = Math.max(finalSize.width, finalSize.height);

            Integer group = IconsRegrouperUtils.pickBestSize(targetFinalSize);
            groupedByGroupSize.computeIfAbsent(group, _ -> new ArrayList<>()).add(r);
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
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D imageEditable = pageImage.createGraphics();
        imageEditable.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        imageEditable.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        try {
            for (int i = 0; i < icons.size(); i++) {
                ResolvedIcon resolvedEmoji = icons.get(i);
                int row = i / GlyphsRegistry.GRID_SIZE;
                int col = i % GlyphsRegistry.GRID_SIZE;

                IconsType type = resolvedEmoji.type();
                RiftLog.DETECTED_AS.send(resolvedEmoji.entry().namespacedId(), resolvedEmoji.image().getWidth(), resolvedEmoji.image().getHeight(), type);

                Rectangle placement = centerImage(resolvedEmoji.image(), cellWidth, cellHeight, resolvedEmoji.entry(), type);
                imageEditable.drawImage(
                        resolvedEmoji.image,
                        col * cellWidth + placement.x,
                        row * cellHeight + placement.y,
                        placement.width,
                        placement.height,
                        null
                );

                RiftRegistry.GLYPHS.register(new IconGlyph(
                        resolvedEmoji.entry.namespacedId(),
                        page,
                        row,
                        col,
                        resolvedEmoji.entry
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

    private Rectangle centerImage(BufferedImage image, int cellWidth, int cellHeight, IconEntry entry, IconsType type) {
        Dimension finalSize = type.finalSize(entry, image.getWidth(), image.getHeight());
        int w = Math.min(cellWidth, finalSize.width);
        int h = Math.min(cellHeight, finalSize.height);

        int x = (cellWidth - w) / 2;
        int y = (cellHeight - h) / 2;

        if (entry.yPosition() != null) {
            double yHeight = type.yOffset(entry, h) / (double) h;
            y += (int) Math.round(entry.yPosition() * yHeight);
            y = Math.max(0, Math.min(y, cellHeight - h));
        }

        return new Rectangle(x, y, w, h);
    }

    private record ResolvedIcon(IconEntry entry, BufferedImage image, IconsType type) {}
}
