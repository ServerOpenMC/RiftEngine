package fr.openmc.riftengine.core.converter.writers.items;

import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.resolvers.items.ItemResourceResolver;
import fr.openmc.riftengine.core.resolvers.items.ResolvedItemResource;
import fr.openmc.riftengine.core.utils.IdentifierUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ItemsTextureWriter implements PackWriter {
    private final List<ItemEntry> items;
    private final ItemResourceResolver resolver;

    public ItemsTextureWriter(List<ItemEntry> items, ItemResourceResolver resolver) {
        this.items = items;
        this.resolver = resolver;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        for (ItemEntry itemEntry : items) {
            List<ResolvedItemResource> resources = resolver.resolve(javaRootPath, itemEntry);

            for (ResolvedItemResource resource : resources) {
                if (resource.isVanilla()) continue;

                Path outputFile = bedrockRootPath.resolve(
                        IdentifierUtils.toBedrockTexturePath(resource.bedrockTexturePath()));
                Files.createDirectories(outputFile.getParent());
                Files.copy(resource.sourcePath(), outputFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}