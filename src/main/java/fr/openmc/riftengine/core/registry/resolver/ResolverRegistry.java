package fr.openmc.riftengine.core.registry.resolver;

import fr.openmc.core.bootstrap.registries.KeyedRegistry;
import fr.openmc.core.bootstrap.registries.Registry;
import fr.openmc.riftengine.core.resolvers.items.ItemResourceResolver;
import fr.openmc.riftengine.core.resolvers.mcmeta.animation.AnimationMcMetaResolver;
import fr.openmc.riftengine.core.resolvers.mcmeta.gui.GuiMcMetaResolver;
import fr.openmc.riftengine.core.resolvers.tooltip.TooltipResourceResolver;

public class ResolverRegistry extends Registry<String, AbstractResolver<?, ?>>
    implements KeyedRegistry<String, AbstractResolver<?, ?>> {

    public final ItemResourceResolver ITEM_RESOURCE_RESOLVER = register(new ItemResourceResolver());
    public final TooltipResourceResolver TOOLTIP_RESOURCE_RESOLVER = register(new TooltipResourceResolver());

    // todo: https://minecraft.wiki/w/Mcmeta faire les autres types de mcmeta si nécessaire
    public final AnimationMcMetaResolver ANIMATION_MCMETA_RESOLVER = register(new AnimationMcMetaResolver());
    public final GuiMcMetaResolver GUI_MCMETA_RESOLVER = register(new GuiMcMetaResolver());
    
    @Override
    public String key(AbstractResolver<?, ?> resolver) {
        return resolver.getClass().getSimpleName();
    }
}