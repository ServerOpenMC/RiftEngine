package fr.openmc.riftengine.core.converter.writers.items;

import fr.openmc.core.bootstrap.integration.OMCLogger;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.utils.IdentifierUtils;
import fr.openmc.riftengine.core.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ItemsTextureWriter implements PackWriter {
    private final List<ItemEntry> items;

    public ItemsTextureWriter(List<ItemEntry> items) {
        this.items = items;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        for (ItemEntry itemEntry : items) {
            // todo: supporter la liste des textures pour les choses comment fishing rod et
            String ressouceId = itemEntry.getBestResourcesId().stream().findFirst().get();
            if (ressouceId == null) {
                OMCLogger.warn("Item {} a pas de resource id", itemEntry.namespacedId());
                continue;
            }

            if (ressouceId.split(":")[0].equals("minecraft")) continue;

            Path resourcePath = itemEntry.getResourcePath().apply(javaRootPath).stream().findFirst().get();
            if (resourcePath == null) {
                OMCLogger.warn("Item {} a aucune resource (Model ou texture)", itemEntry.namespacedId());
                continue;
            }

            Path reducedPath = PathUtils.getPathFromRoot(resourcePath, "textures");
            if (reducedPath == null) {
                OMCLogger.warn("Item {} a un Path impossible a réduire {}", itemEntry.namespacedId(), resourcePath.toString());
                continue;
            }

            Path outputFile = bedrockRootPath.resolve(
                    IdentifierUtils.toBedrockTexturePath(reducedPath.toString()));
            Files.createDirectories(outputFile.getParent());
            Files.copy(resourcePath, outputFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}