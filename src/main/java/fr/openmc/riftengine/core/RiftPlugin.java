package fr.openmc.riftengine.core;

import fr.openmc.core.CommandsManager;
import fr.openmc.core.bootstrap.integration.OMCLogger;
import fr.openmc.riftengine.core.commands.GlyphCommand;
import fr.openmc.riftengine.core.converter.ConverterManager;
import fr.openmc.riftengine.core.listeners.LoadAfterItemsAdderListener;
import fr.openmc.riftengine.core.registry.glyphs.GlyphsRegistry;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// todo: clean les writer et tt qui néccesite du Yml a adapter au max avec nos YmlUtils dans le plugin OpenMC
public class RiftPlugin extends JavaPlugin implements EventRegistrar {
    @Getter
    private static RiftPlugin instance;

    @Getter
    @Setter
    private static ResourcePack resourcePack;

    @Getter
    private RiftConfig riftConfig;
    public final Path CONFIG_FOLDER = getDataFolder().toPath().resolve("config");

    private final List<Object> commands = new ArrayList<>(List.of(
            new GlyphCommand()
    ));

    private ConverterManager converterManager;
    public static final int[] RP_VERSION = new int[] {1,0};

    @Override
    public void onEnable() {
        instance = this;
        logLoadMessage(this);

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
        registerListener(
                new LoadAfterItemsAdderListener()
        );

        // * Commands
        for (Object command : commands) {
            CommandsManager.getHandler().register(command);
        }

        GlyphsRegistry glyphsRegistry = RiftRegistry.GLYPHS;
        RiftLog.NUMBER_GLYPH_REGISTERED.send(glyphsRegistry.size(), glyphsRegistry.maxSize());
    }

    @Override
    public void onDisable() {
        RiftRegistry.stopAll();

        RiftLog.RIFT_DISABLE.send();
    }

    public Path getOutputDir() {
        return this.getDataFolder().toPath().resolve("output");
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

        if (pack == null) return;

        event.register(pack, PriorityOption.HIGHEST);

        RiftLog.RIFT_PACK_INFO.send(pack);
    }

    public void registerListener(Listener... listeners) {
        for (Listener listener : listeners)
            this.getServer().getPluginManager().registerEvents(listener, this);
    }

    public <T extends Event> void registerEvent(Class<T> listenerClass, Consumer<T> handler) {
        GeyserApi.api().eventBus().subscribe(this, listenerClass, handler);
    }

    public static void logLoadMessage(RiftPlugin plugin) {
        RiftLog.logBanner(
                plugin.getPluginMeta().getVersion(),
                Bukkit.getName() + " " + Bukkit.getVersion(),
                System.getProperty("java.version")
        );

        for (String dependency : plugin.getPluginMeta().getPluginDependencies()) {
            logPluginStatus(dependency, false);
        }

        for (String dependency : plugin.getPluginMeta().getPluginSoftDependencies()) {
            logPluginStatus(dependency, true);
        }
    }

    /**
     * Log l'état d'une dépendance (requise ou optionnelle).
     *
     * @param name Nom du plugin dépendance
     * @param optional True si la dépendance est optionnelle
     */
    private static void logPluginStatus(String name, boolean optional) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        boolean enabled = plugin != null && plugin.isEnabled();

        String icon = enabled ? "✔" : "✘";
        String color = enabled ? "\u001B[32m" : "\u001B[31m";
        String version = enabled ? " v" + plugin.getPluginMeta().getVersion() : "";
        String label = optional ? " (facultatif)" : "";

        OMCLogger.info("  {}{} {}{}{}\u001B[0m", color, icon, name, version, label);
    }
}
