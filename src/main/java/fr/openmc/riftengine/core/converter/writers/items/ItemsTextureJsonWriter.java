package fr.openmc.riftengine.core.converter.writers.items;

import com.google.gson.JsonObject;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.resolvers.items.ItemResourceResolver;
import fr.openmc.riftengine.core.resolvers.items.ResolvedItemResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Ecrit le item_texture.json qui sert grosso modo a relier les textures aux items (une ref = une resource)
 */
public class ItemsTextureJsonWriter implements PackWriter {
    private final List<ItemEntry> items;
    private final ItemResourceResolver resolver;

    public ItemsTextureJsonWriter(List<ItemEntry> items, ItemResourceResolver resolver) {
        this.items = items;
        this.resolver = resolver;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject textureData = new JsonObject();

        for (ItemEntry itemEntry : items) {
            List<ResolvedItemResource> resolvedItemResources = resolver.resolve(javaRootPath, itemEntry);

            for (ResolvedItemResource resource : resolvedItemResources) {
                JsonObject itemData = new JsonObject();
                itemData.addProperty("textures", resource.bedrockTexturePath());
                textureData.add(resource.bedrockIdentifier(), itemData);
            }
        }

        root.addProperty("texture_name", "atlas.items");
        root.add("texture_data", textureData);

        Path outputFile = bedrockRootPath.resolve("textures").resolve("item_texture.json");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, GSON.toJson(root), StandardCharsets.UTF_8);
    }
}
