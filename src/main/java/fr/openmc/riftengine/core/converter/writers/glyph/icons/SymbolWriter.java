package fr.openmc.riftengine.core.converter.writers.glyph.icons;

import fr.openmc.core.bootstrap.integration.OMCLogger;
import fr.openmc.riftengine.core.RiftPlugin;
import fr.openmc.riftengine.core.RiftRegistry;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.listeners.LoadAfterItemsAdderListener;
import fr.openmc.riftengine.core.registry.glyphs.Glyph;
import fr.openmc.riftengine.core.registry.glyphs.types.IconGlyph;
import fr.openmc.riftengine.core.utils.YmlUtils;
import org.bukkit.Bukkit;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writer qui prends en charge l'assignation des characteres des glyphs, et qui donne les mappings a items adder
 */
public class SymbolWriter implements PackWriter {

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        Map<Path, List<IconGlyph>> iconByYml = new LinkedHashMap<>();

        for (Glyph glyph : RiftRegistry.GLYPHS.values()) {
            if (!(glyph instanceof IconGlyph iconGlyph)) continue;
            iconByYml.computeIfAbsent(iconGlyph.getIcon().sourceYml(), _ -> new ArrayList<>()).add(iconGlyph);
        }

        if (iconByYml.isEmpty()) return;

        Yaml yaml = new Yaml();

        for (Map.Entry<Path, List<IconGlyph>> fileEntry : iconByYml.entrySet()) {
            Path ymlFile = fileEntry.getKey();
            List<IconGlyph> icons = fileEntry.getValue();

            Map<String, Object> root = YmlUtils.loadYml(ymlFile);

            Map<String, Object> fontImagesMap = (Map<String, Object>) root.get("font_images");

            boolean modified = false;
            for (IconGlyph icon : icons) {
                Object entryObj = fontImagesMap.get(icon.getIcon().key());
                if (!(entryObj instanceof Map<?, ?> entryMapRaw)) {
                    OMCLogger.warnFormatted("Clé " + icon.getIcon().key() + " introuvable dans " + ymlFile + ".");
                    continue;
                }

                Map<String, Object> entryMap = (Map<String, Object>) entryMapRaw;

                entryMap.put("symbol", String.valueOf(icon.getBedrockChar()));
                modified = true;
            }

            if (!modified) continue;

            try (Writer writer = Files.newBufferedWriter(ymlFile)) {
                yaml.dump(root, writer);
            }
        }

        LoadAfterItemsAdderListener.actionToLaunch.add(this::reloadItemsAdder);
    }

    private void reloadItemsAdder() {
        Bukkit.getScheduler().runTask(RiftPlugin.getInstance(), () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "iazip")
        );
    }
}
