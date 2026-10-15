package fr.openmc.riftengine.core.converter.writers.items;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.openmc.core.bootstrap.integration.OMCLogger;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.utils.IdentifierUtils;
import fr.openmc.riftengine.core.utils.PathUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ItemsTextureJsonWriter implements PackWriter {
    private final List<ItemEntry> items;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public ItemsTextureJsonWriter(List<ItemEntry> items) {
        this.items = items;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        JsonObject root = new JsonObject();

        JsonObject textureData = new JsonObject();

        for (ItemEntry itemEntry : items) {
            // todo: supporter la liste des textures pour les choses comment fishing rod et tt
            Set<String> bestResources = itemEntry.getBestResourcesId();
            if (bestResources == null) {
                OMCLogger.warn("Item {} : impossible de déterminer la texture", itemEntry.namespacedId());
                continue;
            }

            Optional<String> resourceId = bestResources.stream().findFirst();
            String texture = null;

            if (resourceId.isEmpty()) {
                texture = "textures/items/" + itemEntry.getMaterial().name().toLowerCase();;
            } else if (resourceId.get().startsWith("minecraft:")) {
                String path = IdentifierUtils.normalizeId(resourceId.get()).split(":", 2)[1];
                texture = IdentifierUtils.toBedrockTexturePath("textures/" + path);
            } else {
                Set<Path> resources = itemEntry.getResourcePath().apply(javaRootPath);
                if (resources == null) {
                    OMCLogger.warn("Item {} : impossible de déterminer la texture", itemEntry.namespacedId());
                    continue;
                }

                Optional<Path> resourcePath = resources.stream().findFirst();
                if (resourcePath.isEmpty()) {
                    // todo: en vrai faire un systeme plus propre d'erreur genre on def ttes les erreurs dans une classe, on les appelle ici et paf, au lieu de dupliquer du texte
                    OMCLogger.warn("Item {} : impossible de déterminer la texture", itemEntry.namespacedId());
                    continue;
                }
                Path reducedPath = resourcePath.isEmpty() ? null
                        : PathUtils.getPathFromRoot(resourcePath.get(), "textures");
                if (reducedPath != null) {
                    texture = IdentifierUtils.toBedrockTexturePath(reducedPath.toString())
                            .replaceFirst("\\.[^/.]+$", "");
                }
            }

            if (texture == null) {
                OMCLogger.warn("Item {} : impossible de déterminer la texture", itemEntry.namespacedId());
                continue;
            }

            JsonObject itemData = new JsonObject();
            itemData.addProperty("textures", texture);
            textureData.add(itemEntry.namespacedId(), itemData);
        }

        root.addProperty("texture_name", "atlas.items");
        root.add("texture_data", textureData);

        Path outputFile = bedrockRootPath.resolve("textures").resolve("item_texture.json");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, GSON.toJson(root), StandardCharsets.UTF_8);
    }
}
