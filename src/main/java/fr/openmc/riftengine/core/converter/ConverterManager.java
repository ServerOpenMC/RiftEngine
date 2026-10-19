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
import fr.openmc.riftengine.core.converter.writers.translations.TranslationInjector;
import fr.openmc.riftengine.core.converter.writers.ui.ScoreboardUiWriter;
import fr.openmc.riftengine.core.resolvers.items.ItemResourceResolver;
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
            ItemResourceResolver resolver = RiftRegistry.RESOLVERS.ITEM_RESOURCE_RESOLVER;

            register("Icônes", new IconWriter());
            register("Manifest", new ManifestWriter(identity));
            register("Traductions", new TranslationInjector());
            register("Polices", new FontWriter());

            register("Scoreboard",
                    new ScoreboardUiWriter(config.isHideScoreboardNumberBedrock()));

            register("Icônes ItemsAdder", new IconsWriter(itemsAdderContents));
            register("Mappings des symboles", new SymbolWriter());

            register("Textures des items", new ItemsTextureJsonWriter(items, resolver));
            register("Fichiers de textures", new ItemsTextureWriter(items, resolver));
            register("Mappings des items", new ItemsMappingWriter(items, resolver));

            register("Textures des blocs",
                    new TerrainTextureJsonWriter(identity, items, resolver));
            register("Mappings des blocs", new BlocksMappingWriter(items, resolver));

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
        File generatedDir = new File(outputDir, "generated.zip"); // * root/plugins/ItemsAdder/output/generated.zip

        return generatedDir.toPath();
    }

    public static Path getItemsAdderContents(JavaPlugin plugin) {
        File pluginsDir = plugin.getDataFolder().getParentFile(); // * root/plugins/
        File itemsAdderDir = new File(pluginsDir, "ItemsAdder"); // * root/plugins/ItemsAdder
        File contentDir = new File(itemsAdderDir, "contents"); // * root/plugins/ItemsAdder/output

        return contentDir.toPath();
    }
}
