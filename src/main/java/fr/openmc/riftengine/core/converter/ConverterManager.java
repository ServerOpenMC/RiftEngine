package fr.openmc.riftengine.core.converter;

import fr.openmc.core.OMCRegistry;
import fr.openmc.core.bootstrap.integration.OMCLogger;
import fr.openmc.core.utils.FilesUtils;
import fr.openmc.core.utils.ZipUtils;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.core.RiftConfig;
import fr.openmc.riftengine.core.RiftPlugin;
import fr.openmc.riftengine.core.RiftRegistry;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.converter.writers.blocks.BlocksMappingWriter;
import fr.openmc.riftengine.core.converter.writers.blocks.TerrainTextureJsonWriter;
import fr.openmc.riftengine.core.converter.writers.glyph.font.FontWriter;
import fr.openmc.riftengine.core.converter.writers.glyph.icons.IconsWriter;
import fr.openmc.riftengine.core.converter.writers.glyph.icons.SymbolWriter;
import fr.openmc.riftengine.core.converter.writers.items.ItemsMappingWriter;
import fr.openmc.riftengine.core.converter.writers.items.ItemsTextureJsonWriter;
import fr.openmc.riftengine.core.converter.writers.items.ItemsTextureWriter;
import fr.openmc.riftengine.core.converter.writers.manifest.IconWriter;
import fr.openmc.riftengine.core.converter.writers.manifest.ManifestWriter;
import fr.openmc.riftengine.core.converter.writers.manifest.PackIdentity;
import fr.openmc.riftengine.core.converter.writers.tooltip.UiCommonTooltipWriter;
import fr.openmc.riftengine.core.converter.writers.tooltip.glyph.TooltipGlyphApplicatorWriter;
import fr.openmc.riftengine.core.converter.writers.tooltip.glyph.TooltipGlyphAssignatorWriter;
import fr.openmc.riftengine.core.converter.writers.tooltip.textures.TooltipTextureWriter;
import fr.openmc.riftengine.core.converter.writers.translations.TranslationInjector;
import fr.openmc.riftengine.core.converter.writers.ui.ScoreboardUiWriter;
import fr.openmc.riftengine.core.resolvers.items.ItemResourceResolver;
import fr.openmc.riftengine.core.resolvers.mcmeta.animation.AnimationMcMetaResolver;
import fr.openmc.riftengine.core.resolvers.mcmeta.gui.GuiMcMetaResolver;
import fr.openmc.riftengine.core.resolvers.tooltip.TooltipResourceResolver;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConverterManager {

    private final RiftPlugin plugin;

    private final List<PackWriter> writers = new ArrayList<>();

    public ConverterManager(RiftPlugin plugin) {
        this.plugin = plugin;
        RiftConfig config = plugin.getRiftConfig();
        Path itemsAdderContents = getItemsAdderContents(plugin);

        try {
            PackIdentity identity = PackIdentity.loadOrCreate(plugin);
            List<ItemEntry> items = OMCRegistry.SCANNERS.ITEMS.scan(itemsAdderContents);
            ItemResourceResolver itemResolver = RiftRegistry.RESOLVERS.ITEM_RESOURCE_RESOLVER;
            TooltipResourceResolver tooltipResolver = RiftRegistry.RESOLVERS.TOOLTIP_RESOURCE_RESOLVER;
            GuiMcMetaResolver guiResolver = RiftRegistry.RESOLVERS.GUI_MCMETA_RESOLVER;
            AnimationMcMetaResolver animResolver = RiftRegistry.RESOLVERS.ANIMATION_MCMETA_RESOLVER;

            register("Icônes", new IconWriter());
            register("Manifest", new ManifestWriter(identity));
            register("Traductions", new TranslationInjector());
            register("Polices", new FontWriter());

            register("Scoreboard",
                    new ScoreboardUiWriter(config.isHideScoreboardNumberBedrock()));

            register("Icônes ItemsAdder", new IconsWriter(itemsAdderContents));
            register("Mappings des symboles", new SymbolWriter());

            register("Textures des items", new ItemsTextureJsonWriter(items, itemResolver));
            register("Fichiers de textures", new ItemsTextureWriter(items, itemResolver));
            register("Mappings des items", new ItemsMappingWriter(items, itemResolver));

            register("Textures des tooltips", new TooltipTextureWriter(items, guiResolver, tooltipResolver));
            register("Assignations des glyphs pour les tooltips",
                    new TooltipGlyphAssignatorWriter(items, tooltipResolver));
            register("Application des glyphs sur les items",
                    new TooltipGlyphApplicatorWriter(items, itemsAdderContents));
            register("Génération du JSON UI pour les tooltips",
                    new UiCommonTooltipWriter(items, tooltipResolver, guiResolver, animResolver));

            register("Textures des blocs",
                    new TerrainTextureJsonWriter(identity, items, itemResolver));
            register("Mappings des blocs", new BlocksMappingWriter(items, itemResolver));

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors d'initialisation du ConverterManager", e);
        }
    }

    private void register(String name, PackWriter writer) {
        writers.add(new RegisteredPackWriter(name, writer));
    }

    /**
     * Prends un pack java et le convertit en pack bedrock
     */
    public Path generateConvertedPack() throws Exception {
        Path javaPackPath = getJavaPackPath(RiftPlugin.getInstance());

        Path outputDir = plugin.getDataFolder().toPath().resolve("output");
        FilesUtils.deleteDirectory(outputDir.toFile());
        Files.createDirectories(outputDir);

        Path workDir = outputDir.resolve("internal");
        Path javaExtractDir = workDir.resolve("java");
        Path bedrockBuildDir = workDir.resolve("bedrock");
        Files.createDirectories(bedrockBuildDir);

        ZipUtils.unzip(javaPackPath, javaExtractDir);

        for (PackWriter writer : writers) {
            writer.write(bedrockBuildDir, javaExtractDir);
        }

        Path outputPack = outputDir.resolve("bedrock_pack.mcpack");
        Files.deleteIfExists(outputPack);
        ZipUtils.zip(bedrockBuildDir, outputPack);

        OMCLogger.successFormatted("Pack bedrock généré : " + outputPack);
        return outputPack;
    }

    public static Path getJavaPackPath(JavaPlugin plugin) {
        File pluginsDir = plugin.getDataFolder().getParentFile(); // * root/plugins/
        File itemsAdderDir = new File(pluginsDir, "ItemsAdder"); // * root/plugins/ItemsAdder
        File outputDir = new File(itemsAdderDir, "output"); // * root/plugins/ItemsAdder/output
        File clientPacksDir = new File(outputDir, "client_packs"); // * root/plugins/ItemsAdder/output/client_packs/

        return FilesUtils.listFileNames(clientPacksDir.toPath().toAbsolutePath().toString()).stream()
                .filter(name -> name.contains(".zip") && name.contains("26_3") && name.contains("fast_compression"))
                .findFirst()
                .map(name -> clientPacksDir.toPath().resolve(name))
                .orElse(null);
    }

    public static Path getItemsAdderContents(JavaPlugin plugin) {
        File pluginsDir = plugin.getDataFolder().getParentFile(); // * root/plugins/
        File itemsAdderDir = new File(pluginsDir, "ItemsAdder"); // * root/plugins/ItemsAdder
        File contentDir = new File(itemsAdderDir, "contents"); // * root/plugins/ItemsAdder/output

        return contentDir.toPath();
    }
}
