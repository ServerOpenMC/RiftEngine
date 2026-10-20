package fr.openmc.riftengine.core.converter.writers.tooltip.glyph;

import fr.openmc.core.utils.YmlUtils;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.core.RiftRegistry;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.registry.glyphs.Glyph;
import fr.openmc.riftengine.core.registry.glyphs.types.TooltipGlyph;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 /**
 * Suite logique de TooltipGlyphAssignatorWriter.
 * On applique le glyph sur l'item qui doit avoir un tooltip.
 */
public class TooltipGlyphApplicatorWriter implements PackWriter {

    private final List<ItemEntry> items;
    private final Path itemsAdderContents;

    public TooltipGlyphApplicatorWriter(List<ItemEntry> items, Path itemsAdderContents) {
        this.items = items;
        this.itemsAdderContents = itemsAdderContents;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);
        options.setSplitLines(false);
        options.setWidth(Integer.MAX_VALUE);

        Yaml yaml = new Yaml(options);

        Map<Path, List<ItemEntry>> itemsByFile = new LinkedHashMap<>();

        for (ItemEntry itemEntry : items) {
            String sourceYml = itemEntry.getSourceYml().toString()
                    .replaceFirst("^plugins/ItemsAdder/contents/", "");

            Path ymlPath = itemsAdderContents.resolve(sourceYml).normalize();
            itemsByFile.computeIfAbsent(ymlPath, _ -> new ArrayList<>()).add(itemEntry);
        }

        for (Map.Entry<Path, List<ItemEntry>> fileEntry : itemsByFile.entrySet()) {
            Path ymlPath = fileEntry.getKey();

            if (!Files.isRegularFile(ymlPath)) continue;

            Map<String, Object> root = YmlUtils.loadYml(ymlPath);
            if (root == null) continue;

            Map<?, ?> itemsSection = YmlUtils.getMap(root, "items");
            if (itemsSection == null) continue;

            boolean modified = false;

            for (ItemEntry itemEntry : fileEntry.getValue()) {
                Optional<Glyph> glyph = RiftRegistry.GLYPHS.get(
                        itemEntry.getTooltipResourceId()
                );

                if (glyph.isEmpty() || !(glyph.get() instanceof TooltipGlyph tooltipGlyph)) continue;

                Object itemObject = itemsSection.get(itemEntry.getKey());
                if (!(itemObject instanceof Map<?, ?>)) continue;

                Map<String, Object> itemMap = (Map<String, Object>) itemObject;

                String key = itemMap.containsKey("display_name") ? "display_name" : "name";

                String name = YmlUtils.getString(itemMap, key, null);
                if (name == null) continue;

                String glyphStr = tooltipGlyph.getBedrockCharString();

                if (name.contains(glyphStr) || name.contains(String.valueOf(tooltipGlyph.getBedrockChar()))) continue;

                itemMap.put(key, name + glyphStr);
                modified = true;
            }

            if (!modified) continue;

            try (Writer writer = Files.newBufferedWriter(ymlPath)) {
                yaml.dump(root, writer);
            }

            String content = Files.readString(ymlPath);

            for (ItemEntry itemEntry : fileEntry.getValue()) {
                Optional<Glyph> glyph = RiftRegistry.GLYPHS.get(
                        itemEntry.getTooltipResourceId()
                );

                if (glyph.isEmpty()
                        || !(glyph.get() instanceof TooltipGlyph)) continue;

                content = content.replaceAll("\\\\\\\\(uF[0-9A-Fa-f]{3})", "\\\\$1");
            }

            Files.writeString(ymlPath, content);
        }
    }
}