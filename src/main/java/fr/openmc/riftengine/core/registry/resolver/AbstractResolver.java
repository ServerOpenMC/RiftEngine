package fr.openmc.riftengine.core.registry.resolver;

import java.nio.file.Path;

public abstract class AbstractResolver<I, O> {
    public abstract O resolve(Path javaRootPath, I input);
}
