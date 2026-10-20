package fr.openmc.riftengine.core.resolvers.mcmeta.animation;

import fr.openmc.riftengine.core.registry.resolver.AbstractResolver;

import java.nio.file.Path;


public class AnimationMcMetaResolver extends AbstractResolver<Path, ResolvedAnimationMcMeta> {
    @Override
    public ResolvedAnimationMcMeta resolve(Path javaRootPath, Path mcmeta) {
        return ResolvedAnimationMcMeta.read(mcmeta);
    }
}