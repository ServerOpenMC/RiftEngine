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
import java.util.Optional;
import java.util.Set;

public class ItemsTextureWriter implements PackWriter {
    private final List<ItemEntry> items;

    public ItemsTextureWriter(List<ItemEntry> items) {
        this.items = items;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        for (ItemEntry itemEntry : items) {
            Set<String> resources = itemEntry.getBestResourcesId();

            if (resources == null) {
                OMCLogger.warn("Item {} a pas de resource id", itemEntry.namespacedId());
                continue;
            }

            // todo: supporter la liste des textures pour les choses comment fishing rod et
            Optional<String> ressouceId = resources.stream().findFirst();
            if (ressouceId.isEmpty()) {
                OMCLogger.warn("Item {} a pas de resource id", itemEntry.namespacedId());
                continue;
            }

            if (ressouceId.get().split(":")[0].equals("minecraft")) continue;

            Set<Path> resources1 = itemEntry.getResourcePath().apply(javaRootPath);
            if (resources1 == null) {
                OMCLogger.warn("Item {} a aucune resource (Model ou texture)", itemEntry.namespacedId());
                continue;
            }
            Optional<Path> resourcePath = resources1.stream().findFirst();
            if (resourcePath.isEmpty()) {
                OMCLogger.warn("Item {} a aucune resource (Model ou texture)", itemEntry.namespacedId());
                continue;
            }

            Path reducedPath = PathUtils.getPathFromRoot(resourcePath.get(), "textures");
            if (reducedPath == null) {
                OMCLogger.warn("Item {} a un Path impossible a réduire {}", itemEntry.namespacedId(), resourcePath.toString());
                continue;
            }

            Path outputFile = bedrockRootPath.resolve(
                    IdentifierUtils.toBedrockTexturePath(reducedPath.toString()));
            Files.createDirectories(outputFile.getParent());
            Files.copy(resourcePath.get(), outputFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}