package fr.openmc.riftengine.core.resolvers.mcmeta.gui;

import fr.openmc.riftengine.core.registry.resolver.AbstractResolver;

import java.nio.file.Path;

public class GuiMcMetaResolver extends AbstractResolver<Path, ResolvedGuiMcMeta> {
    @Override
    public ResolvedGuiMcMeta resolve(Path javaRootPath, Path mcmeta) {
        return ResolvedGuiMcMeta.read(mcmeta);
    }
}