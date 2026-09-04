package fr.openmc.riftengine.core;


import fr.openmc.core.bootstrap.integration.OMCLogger;
import fr.openmc.core.bootstrap.registries.LifecycleRegistry;
import fr.openmc.core.bootstrap.registries.RegistryContext;
import fr.openmc.core.bootstrap.registries.RegistryLoadingType;
import fr.openmc.riftengine.core.registry.glyphs.GlyphsRegistry;
import fr.openmc.riftengine.core.registry.mapping.MappingRegistry;
import fr.openmc.riftengine.core.registry.scanner.ScannerRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RiftRegistry {
    // * Registre globaux
    public static MappingRegistry MAPPINGS;
    public static GlyphsRegistry GLYPHS;
    public static ScannerRegistry SCANNERS;

    private static final List<LifecycleRegistry> LOADED = new ArrayList<>();

    private static final List<RegistryContext> ALL = List.of(
            new RegistryContext(
                    () -> MAPPINGS = new MappingRegistry(),
                    RegistryLoadingType.RUNTIME),
            new RegistryContext(
                    () -> GLYPHS = new GlyphsRegistry(),
                    RegistryLoadingType.RUNTIME),
            new RegistryContext(
                    () -> SCANNERS = new ScannerRegistry(),
                    RegistryLoadingType.RUNTIME)
    );

    private RiftRegistry() {}

    public static void initAll() {
        for (RegistryContext ctx : RiftRegistry.ALL) {
            if (isNotTyped(ctx, RegistryLoadingType.RUNTIME)) continue;

            LifecycleRegistry r = load(ctx);

            r.init();
            OMCLogger.successFormatted("Registre {} chargé pendant le runtime", r.getClass().getSimpleName());
        }
    }

    public static void stopAll() {
        for (LifecycleRegistry r : LOADED) {
            r.stop();
            OMCLogger.successFormatted("Registre {} stoppé", r.getClass().getSimpleName());
        }
        LOADED.clear();
    }

    private static LifecycleRegistry load(RegistryContext ctx) {
        LifecycleRegistry registry = ctx.registry().get();
        LOADED.add(registry);
        return registry;
    }

    private static boolean isNotTyped(RegistryContext ctx, RegistryLoadingType type) {
        return Arrays.stream(ctx.loadingTypes()).noneMatch(t -> t == type);
    }

    private static boolean isTyped(RegistryContext ctx, RegistryLoadingType type) {
        return Arrays.stream(ctx.loadingTypes()).anyMatch(t -> t == type);
    }
}