package fr.openmc.riftengine.core.registry.glyphs.types;

import fr.openmc.riftengine.core.registry.glyphs.Glyph;
import fr.openmc.riftengine.core.registry.glyphs.GlyphsRegistry;
import fr.openmc.riftengine.core.resolvers.tooltip.ResolvedTooltipResource;
import lombok.Getter;

@Getter
public class TooltipGlyph extends Glyph {
    private final ResolvedTooltipResource tooltipResource;

    public TooltipGlyph(String namespacedId, int row, int col, ResolvedTooltipResource tooltipResource) {
        super(namespacedId, GlyphsRegistry.OTHER_PAGE, row, col);
        this.tooltipResource = tooltipResource;
    }
}