package fr.openmc.riftengine.core.converter.writers.blocks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.openmc.core.bootstrap.integration.OMCLogger;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.api.scanner.items.entry.behaviours.IABehaviour;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.converter.writers.manifest.ManifestWriter;
import fr.openmc.riftengine.core.utils.IdentifierUtils;
import fr.openmc.riftengine.core.utils.PathUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Ecrit le terrain_texture.json qui sert grosso modo a relier les textures aux items (une ref = une resource)
 */
public class TerrainTextureJsonWriter implements PackWriter {
    private final List<ItemEntry> items;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public TerrainTextureJsonWriter(List<ItemEntry> items) {
        this.items = items;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        JsonObject root = new JsonObject();

        JsonObject textureData = new JsonObject();

        for (ItemEntry itemEntry : items) {
            IABehaviour behaviour = itemEntry.getBehaviour();
            if (!behaviour.isBlock())  {
                OMCLogger.warn("Item {} : n'a pas block", itemEntry.namespacedId());
                continue;
            }
            System.out.println("Item : " + itemEntry.namespacedId());

            Map<String, String> bestResources = itemEntry.getBestResourcesId();
            if (bestResources == null) {
                OMCLogger.warn("Item {} : n'a pas de best resources", itemEntry.namespacedId());
                continue;
            }

            for (var entry : bestResources.entrySet()) {
                String[] textureDataStr = getTextureData(itemEntry, entry.getKey(), entry.getValue(), javaRootPath);

                if (textureDataStr == null) {
                    OMCLogger.warn("Item {} : textureDataStr n'a pas de contenu", itemEntry.namespacedId());
                    continue;
                }

                JsonObject itemData = new JsonObject();
                itemData.addProperty("textures", textureDataStr[1]);
                textureData.add(textureDataStr[0], itemData);
            }
        }

        root.addProperty("resource_pack_name", ManifestWriter.PACK_NAME);
        root.addProperty("texture_name", "atlas.terrain");
        root.addProperty("padding", 8);
        root.addProperty("num_mip_levels", 4);
        root.add("texture_data", textureData);

        Path outputFile = bedrockRootPath.resolve("textures").resolve("terrain_texture.json");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    /**
     * Retourne les id du namespace et de la resource
     * (en fonction de si y'a des textures multiples ect fishing rod)
     * Note a moi meme:
     * - le state normal est skip, a ma vue c'est le state par défaut
     */
    public String[] getTextureData(ItemEntry itemEntry, String stateResource, String resourceId, Path javaRootPath) {
        String texture = null;
        String namespace = itemEntry.getKey();

        if (resourceId.isEmpty()) {
            texture = "textures/blocks/" + itemEntry.getMaterial().name().toLowerCase();
        } else if (resourceId.startsWith("minecraft:")) {
            String path = IdentifierUtils.normalizeId(resourceId).split(":", 2)[1];
            texture = IdentifierUtils.toBedrockTexturePath("textures/" + path);
        } else {
            Map<String, Path> resources = itemEntry.getResourcePath().apply(javaRootPath);
            if (resources == null) {
                OMCLogger.warn("Item {} : impossible de déterminer la texture", itemEntry.namespacedId());
                return null;
            }

            if (resources.isEmpty()) {
                OMCLogger.warn("Item {} : impossible de déterminer la texture", itemEntry.namespacedId());
                return null;
            }

            Path resourcePath;
            if (resources.size() > 1) {
                resourcePath = resources.get(stateResource);
                if (!stateResource.equals(ItemEntry.DEFAULT_STATE))
                    namespace = itemEntry.getKey() + "_" + stateResource;
            } else {
                resourcePath = resources.values().stream().findFirst().get();
            }

            Path reducedPath = PathUtils.getPathFromRoot(resourcePath, "textures");
            if (reducedPath != null) {
                texture = IdentifierUtils.toBedrockTexturePath(reducedPath.toString())
                        .replaceFirst("\\.[^/.]+$", "");
            }
        }

        if (texture == null) {
            OMCLogger.warn("Item {} : impossible de déterminer la texture", itemEntry.namespacedId());
            return null;
        }

        return new String[] {namespace, texture};
    }
}
