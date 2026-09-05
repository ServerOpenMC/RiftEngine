package fr.openmc.riftengine.core.converter;

import fr.openmc.core.bootstrap.integration.OMCLogger;
import fr.openmc.core.utils.FilesUtils;
import fr.openmc.riftengine.core.RiftConfig;
import fr.openmc.riftengine.core.RiftPlugin;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.converter.writers.glyph.font.FontWriter;
import fr.openmc.riftengine.core.converter.writers.glyph.icons.IconsWriter;
import fr.openmc.riftengine.core.converter.writers.glyph.icons.SymbolWriter;
import fr.openmc.riftengine.core.converter.writers.manifest.IconWriter;
import fr.openmc.riftengine.core.converter.writers.manifest.ManifestWriter;
import fr.openmc.riftengine.core.converter.writers.manifest.PackIdentity;
import fr.openmc.riftengine.core.converter.writers.translations.TranslationInjector;
import fr.openmc.riftengine.core.converter.writers.ui.ScoreboardUiWriter;
import fr.openmc.riftengine.core.utils.ZipUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConverterManager {

    private final RiftPlugin plugin;

    private final List<PackWriter> writers = new ArrayList<>();
    private final PackIdentity identity;

    public ConverterManager(RiftPlugin plugin) {
        this.plugin = plugin;
        RiftConfig config = plugin.getRiftConfig();
        Path itemsAdderContents = getItemsAdderContents(plugin);

        try {
            identity = PackIdentity.loadOrCreate(plugin);
            writers.addAll(List.of(
                    new IconWriter(),
                    new ManifestWriter(identity),
                    new TranslationInjector(),
                    new FontWriter(),
                    new ScoreboardUiWriter(config.isHideScoreboardNumberBedrock()),
                    new IconsWriter(itemsAdderContents),
                    new SymbolWriter()
            ));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors d'initialisation du ConverterManager", e);
        }
    }

    /**
     * Prends un pack java et le convertit en pack bedrock
     */
    public Path generateConvertedPack() throws IOException {
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
