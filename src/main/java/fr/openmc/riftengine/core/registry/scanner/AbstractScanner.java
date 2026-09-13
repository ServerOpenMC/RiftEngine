package fr.openmc.riftengine.core.registry.scanner;

public abstract class AbstractScanner<T, P> {
    public abstract T scan(P param) throws Exception;
}
