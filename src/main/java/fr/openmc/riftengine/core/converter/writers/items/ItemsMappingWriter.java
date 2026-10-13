package fr.openmc.riftengine.core.converter.writers.items;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.core.RiftPlugin;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.utils.PathUtils;

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
            JsonObject def = new JsonObject();

            if (item.getCustomModelData() != null) {
                def.addProperty("type", "legacy");
                def.addProperty("custom_model_data", item.getCustomModelData());
            } else {
                def.addProperty("type", "definition");
                def.addProperty("model", item.getNamespace() + ":ia_auto/" + item.getKey());
            }

            def.addProperty("bedrock_identifier", item.namespacedId());

            JsonObject options = new JsonObject();
            options.addProperty("icon", item.namespacedId());
            def.add("bedrock_options", options);

            byMaterial.computeIfAbsent(item.getMaterial().getKey().asString(), _ -> new JsonArray())
                    .add(def);
        }

        JsonObject itemsSetter = new JsonObject();
        byMaterial.forEach(itemsSetter::add);
        root.add("items", itemsSetter);

        Path outputFile = PathUtils.getGeyserPath(RiftPlugin.getInstance().getDataPath())
                .resolve("custom_mappings").resolve("ia-injected.json");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private ItemEntry getByNamespace(String namespacedId) {
        return items.stream().
                filter(item -> item.namespacedId().equals(namespacedId))
                .findFirst().orElse(null);
    }
}