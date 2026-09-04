package fr.openmc.riftengine.core;

import fr.openmc.core.bootstrap.integration.OMCLogger;
import fr.openmc.riftengine.core.converter.ConverterManager;
import fr.openmc.riftengine.core.registry.glyphs.GlyphsRegistry;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.event.Event;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.option.PriorityOption;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class RiftPlugin extends JavaPlugin implements EventRegistrar {
    @Getter
    private static RiftPlugin instance;

    @Getter
    @Setter
    private static ResourcePack resourcePack;

    @Getter
    private RiftConfig riftConfig;

    private ConverterManager converterManager;

    public static final int[] RP_VERSION = new int[] {1,0};

    @Override
    public void onEnable() {
        instance = this;
        try {
            riftConfig = new RiftConfig(this, this.getConfig());
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier config.yml", e);
        }

        // * Registries internal
        RiftRegistry.initAll();

        // * Managers
        converterManager = new ConverterManager(this);
        updateBedrockResourcePack();

        // * Listeners
        registerEvent(SessionLoadResourcePacksEvent.class, this::onLoadResourcePacks);

        GlyphsRegistry glyphsRegistry = RiftRegistry.GLYPHS;
        OMCLogger.info("RiftEngine activé!");
        OMCLogger.infoFormatted(glyphsRegistry.size() + "/" + glyphsRegistry.maxSize() + " glyphs enregistré");
        OMCLogger.infoFormatted("Glyphs : " + glyphsRegistry.values());
    }

    @Override
    public void onDisable() {
        RiftRegistry.stopAll();

        OMCLogger.info("RiftEngine désactivé!");
    }

    public void updateBedrockResourcePack() {
        try {
            Path generatedBedrockPack = converterManager.generateConvertedPack();
            resourcePack = ResourcePack.builder(PackCodec.path(generatedBedrockPack)).build();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du resourcepack bedrock", e);
        }
    }

    public void onLoadResourcePacks(SessionLoadResourcePacksEvent event) {
        ResourcePack pack = RiftPlugin.getResourcePack();

        OMCLogger.infoFormatted("RiftEngine pack = " + pack);

        if (pack == null) return;

        event.register(pack, PriorityOption.HIGHEST);

        OMCLogger.successFormatted("RiftEngine: pack registered !");
    }

    public <T extends Event> void registerEvent(Class<T> listenerClass, Consumer<T> handler) {
        GeyserApi.api().eventBus().subscribe(this, listenerClass, handler);
    }
}
