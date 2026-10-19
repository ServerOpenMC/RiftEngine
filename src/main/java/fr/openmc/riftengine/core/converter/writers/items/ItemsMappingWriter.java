package fr.openmc.riftengine.core.converter.writers.items;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.openmc.core.bootstrap.integration.OMCLogger;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.core.RiftPlugin;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.utils.PathUtils;
import org.bukkit.Material;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemsMappingWriter implements PackWriter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<ItemEntry> items;

    public ItemsMappingWriter(List<ItemEntry> items) {
        this.items = items;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws Exception {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", 2);

        Map<String, JsonArray> byMaterial = new HashMap<>();

        for (ItemEntry item : items) {
            Map<String, String> resources = item.getBestResourcesId();

            if  (resources == null) continue;

            if (resources.size() == 1) {
                // * Gestion des items a une texture
                byMaterial.computeIfAbsent(item.getMaterial().getKey().asString(), _ -> new JsonArray())
                        .add(handleUniqueTexture(item));
            } else {
                // * Gestion des items a plusieurs textures
                JsonObject def = new JsonObject();
                def.addProperty("type", "group");
                def.addProperty("model", item.getNamespace() + ":ia_auto/" + item.getKey());
                def.add("definitions", handleMultipleTexture(item));

                byMaterial.computeIfAbsent(item.getMaterial().getKey().asString(), _ -> new JsonArray())
                        .add(def);
            }
        }

        JsonObject itemsSetter = new JsonObject();
        byMaterial.forEach(itemsSetter::add);
        root.add("items", itemsSetter);

        Path outputFile = PathUtils.getGeyserPath(RiftPlugin.getInstance().getDataPath())
                .resolve("custom_mappings").resolve("ia-items-injected.json");
        Files.deleteIfExists(outputFile);
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private JsonObject handleUniqueTexture(ItemEntry item) {
        JsonObject def = new JsonObject();
        if (item.getCustomModelData() != null) {
            def.addProperty("type", "legacy");
            def.addProperty("custom_model_data", item.getCustomModelData());
        } else {
            def.addProperty("type", "definition");
            def.addProperty("model", item.getNamespace() + ":ia_auto/" + item.getKey());
        }

        def.addProperty("bedrock_identifier", item.getKey());

        if (item.getBehaviour().isItem()) {
            JsonObject options = new JsonObject();
            options.addProperty("icon", item.getKey());
            def.add("bedrock_options", options);
        }

        return def;
    }

    private JsonArray handleMultipleTexture(ItemEntry item) {
        JsonArray definitionsArray = new JsonArray();

        Map<String, String> bestResources = item.getBestResourcesId();
        if (bestResources == null) {
            OMCLogger.warn("Item {} : n'a pas de best resources", item.namespacedId());
            return definitionsArray;
        }

        for (var entry : bestResources.entrySet()) {
            String stateResource = entry.getKey();

            JsonObject defObject = new JsonObject();

            String namespaceId = item.namespacedId();

            if (!stateResource.equals(ItemEntry.DEFAULT_STATE))
                namespaceId += "_" + stateResource;

            defObject.addProperty("bedrock_identifier", namespaceId);

            JsonObject options = new JsonObject();
            options.addProperty("icon", namespaceId);
            options.addProperty("display_handheld", true);

            // y'a 5 types de conditions (broken, damaged, custom_model_data, has_component, fishing_rod_cast),
            // note pour plus tard si ajout elytra
            if (!stateResource.equals(ItemEntry.DEFAULT_STATE) && item.getMaterial().equals(Material.FISHING_ROD)) {
                options.addProperty("display_handheld", true);
                JsonObject predicate = new JsonObject();
                predicate.addProperty("type", "condition");
                predicate.addProperty("property", "fishing_rod_cast");
                defObject.add("predicate", predicate);
            }


            defObject.add("bedrock_options", options);

            definitionsArray.add(defObject);
        }

        return definitionsArray;
    }
}