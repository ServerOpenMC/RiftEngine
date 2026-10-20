package fr.openmc.riftengine.core.resolvers.tooltip;

import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.core.registry.resolver.AbstractResolver;

import java.nio.file.Path;

public class TooltipResourceResolver extends AbstractResolver<ItemEntry, ResolvedTooltipResource> {
    @Override
    public ResolvedTooltipResource resolve(Path javaRootPath, ItemEntry item) {
        String tooltipStyle = item.getTooltipResourceId();

        if (tooltipStyle == null) return null;

        return new ResolvedTooltipResource(tooltipStyle);
    }
}
