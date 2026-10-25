package fr.openmc.riftengine.core.converter.writers.items;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.core.RiftPlugin;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.resolvers.items.ItemResourceResolver;
import fr.openmc.riftengine.core.resolvers.items.ResolvedItemResource;
import fr.openmc.riftengine.core.utils.PathUtils;
import org.bukkit.Material;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemsMappingWriter implements PackWriter {
    private final List<ItemEntry> items;
    private final ItemResourceResolver resolver;
    private static final String OUTPUT_FILE = "custom_mappings/ia-items-injected.json";

    public ItemsMappingWriter(List<ItemEntry> items, ItemResourceResolver resolver) {
        this.items = items;
        this.resolver = resolver;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws Exception {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", 2);

        Map<String, JsonArray> byMaterial = new HashMap<>();

        for (ItemEntry item : items) {
            List<ResolvedItemResource> resources = resolver.resolve(javaRootPath, item);

            String material = item.getMaterial().getKey().asString();

            if (material.equals("minecraft:tripwire"))
                material = "minecraft:tripwire_hook";

            JsonArray definitions = byMaterial.computeIfAbsent(
                    material,
                    _ -> new JsonArray()
            );

            if (resources.size() == 1) {
                // * Gestion des items a une texture
                definitions.add(handleUniqueTexture(item, resolver, resources.getFirst()));
            } else if (resources.size() > 1 && item.getBehaviour().isItem()) {
                // * Gestion des items a plusieurs textures
                JsonObject def = new JsonObject();
                def.addProperty("type", "group");
                def.addProperty("model", resources.getFirst().modelResourceId());
                def.add("definitions", handleMultipleTexture(item, resources));

                definitions.add(def);
            } else if (resources.size() > 1 && !item.getBehaviour().isItem()) {
                // * Gestion des non items (ex block qui sont géré par le block mapping)
                JsonObject def = new JsonObject();
                String bedrockIdentifier = resources.getFirst().bedrockIdentifier();
                def.addProperty("type", "definition");
                def.addProperty("model", resources.getFirst().modelResourceId());
                def.addProperty("bedrock_identifier", bedrockIdentifier);

                if (item.getBehaviour().isItem()) {
                    JsonObject options = new JsonObject();
                    options.addProperty("icon", bedrockIdentifier);
                    def.add("bedrock_options", options);
                }

                definitions.add(def);
            } else {
                // * Gestion des items qui n'a pas de textures (donc dans notre cas model)
                definitions.add(handleUniqueTexture(item, resolver, null));
            }
        }

        JsonObject itemsSetter = new JsonObject();
        byMaterial.forEach(itemsSetter::add);
        root.add("items", itemsSetter);

        Path outputFile = PathUtils.getGeyserPath(RiftPlugin.getInstance().getDataPath())
                .resolve(OUTPUT_FILE);
        Files.deleteIfExists(outputFile);
        this.writeJsonFile(outputFile, root);
        this.writeJsonFile(RiftPlugin.getInstance().getOutputDir().resolve(OUTPUT_FILE), root);
    }

    private JsonObject handleUniqueTexture(ItemEntry item, ItemResourceResolver resolver, ResolvedItemResource resource) {
        JsonObject def = new JsonObject();
        if (item.getCustomModelData() != null) {
            def.addProperty("type", "legacy");
            def.addProperty("custom_model_data", item.getCustomModelData());
        } else if (resource != null) {
            def.addProperty("type", "definition");
            def.addProperty("model", resource.modelResourceId());
        }

        String bedrockIdentifier = resolver.resolveBedrockIdentifier(item);
        if (resource != null)
            bedrockIdentifier = resource.bedrockIdentifier();
        def.addProperty("bedrock_identifier", bedrockIdentifier);

        if (item.getBehaviour().isItem()) {
            JsonObject options = new JsonObject();
            options.addProperty("icon", bedrockIdentifier);
            def.add("bedrock_options", options);
        }

        return def;
    }

    private JsonArray handleMultipleTexture(ItemEntry item, List<ResolvedItemResource> resources) {
        JsonArray definitionsArray = new JsonArray();

        for (ResolvedItemResource resource : resources) {
            JsonObject defObject = new JsonObject();

            defObject.addProperty("bedrock_identifier", resource.bedrockIdentifier());

            JsonObject options = new JsonObject();
            options.addProperty("icon", resource.bedrockIdentifier());
            options.addProperty("display_handheld", true);

            // y'a 5 types de conditions (broken, damaged, custom_model_data, has_component, fishing_rod_cast),
            // note pour plus tard si ajout elytra
            if (!resource.state().equals(ItemEntry.DEFAULT_STATE) && item.getMaterial().equals(Material.FISHING_ROD)) {
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