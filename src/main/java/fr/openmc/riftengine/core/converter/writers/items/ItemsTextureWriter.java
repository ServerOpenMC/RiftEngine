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
import java.util.Map;

public class ItemsTextureWriter implements PackWriter {
    private final List<ItemEntry> items;

    public ItemsTextureWriter(List<ItemEntry> items) {
        this.items = items;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        for (ItemEntry itemEntry : items) {
            Map<String, String> bestResources = itemEntry.getBestResourcesId();
            if (bestResources == null) {
                OMCLogger.warn("Item {} : n'a pas de best resources", itemEntry.namespacedId());
                continue;
            }

            for (var entry : bestResources.entrySet()) {
                String stateResource = entry.getKey();
                String resourceId = entry.getValue();

                if (resourceId.isEmpty()) {
                    OMCLogger.warn("Item {} a pas de resource id", itemEntry.namespacedId());
                    continue;
                }

                if (resourceId.split(":")[0].equals("minecraft")) continue;

                Map<String, Path> resources = itemEntry.getResourcePath().apply(javaRootPath);
                if (resources == null) {
                    OMCLogger.warn("Item {} a aucune resource (Model ou texture)", itemEntry.namespacedId());
                    continue;
                }

                Path resourcePath = resources.get(stateResource);
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
}