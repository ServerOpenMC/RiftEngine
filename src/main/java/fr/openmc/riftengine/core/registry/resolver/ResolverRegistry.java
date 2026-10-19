package fr.openmc.riftengine.core.registry.resolver;

import fr.openmc.core.bootstrap.registries.KeyedRegistry;
import fr.openmc.core.bootstrap.registries.Registry;
import fr.openmc.riftengine.core.resolvers.items.ItemResourceResolver;

public class ResolverRegistry extends Registry<String, AbstractResolver<?, ?>>
    implements KeyedRegistry<String, AbstractResolver<?, ?>> {

    public final ItemResourceResolver ITEM_RESOURCE_RESOLVER = register(new ItemResourceResolver());

    @Override
    public String key(AbstractResolver<?, ?> resolver) {
        return resolver.getClass().getSimpleName();
    }
}