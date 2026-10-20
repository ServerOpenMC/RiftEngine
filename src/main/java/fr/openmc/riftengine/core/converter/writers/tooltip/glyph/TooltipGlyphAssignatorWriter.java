package fr.openmc.riftengine.core.converter.writers.tooltip.glyph;

import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.core.RiftLog;
import fr.openmc.riftengine.core.RiftRegistry;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.registry.glyphs.types.TooltipGlyph;
import fr.openmc.riftengine.core.resolvers.tooltip.ResolvedTooltipResource;
import fr.openmc.riftengine.core.resolvers.tooltip.TooltipResourceResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cette classe doit être justifiée.
 * Afin de detecter le tooltip sur un item dans JSON UI, il faut impérativement mettre un glyph dans l'item name
 * de l'item qui doit avoir un tooltip.
 */
public class TooltipGlyphAssignatorWriter implements PackWriter {
    private final List<ItemEntry> items;
    private final TooltipResourceResolver resolver;

    public TooltipGlyphAssignatorWriter(List<ItemEntry> items, TooltipResourceResolver resolver) {
        this.items = items;
        this.resolver = resolver;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        // todo: faire un TooltipScanner quand OpenMC sera divisé en module
        Map<String, ResolvedTooltipResource> tooltipResources = new HashMap<>();
        for (ItemEntry itemEntry : items) {
            ResolvedTooltipResource resource = resolver.resolve(javaRootPath, itemEntry);
            if (resource == null) continue;

            tooltipResources.put(resource.resourceId(), resource);
        }

        int indexPage = 0;
        for (ResolvedTooltipResource resource : tooltipResources.values()) {
            int row = indexPage / 16;
            int col = indexPage % 16;
            RiftRegistry.GLYPHS.register(new TooltipGlyph(resource.resourceId(), row, col, resource));
            indexPage++;
        }

        RiftLog.NUMBER_GLYPH_REGISTERED.send(indexPage + 1, 16*16);
    }
}