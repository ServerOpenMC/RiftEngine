package fr.openmc.riftengine.core.converter.writers.items.specials.model;

import com.google.gson.JsonObject;
import fr.openmc.core.utils.JsonUtils;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.api.scanner.items.entry.ItemModel;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.resolvers.items.ItemResourceResolver;
import fr.openmc.riftengine.core.resolvers.items.ResolvedItemResource;

import java.nio.file.Path;
import java.util.List;

public class ItemModelAttachableWriter implements PackWriter {
    private final List<ItemEntry> items;
    private final ItemResourceResolver resolver;

    public ItemModelAttachableWriter(List<ItemEntry> items, ItemResourceResolver resolver) {
        this.items = items;
        this.resolver = resolver;
    }

    private static final String TEMPLATE_ATTACHABLE = """
            {
                "format_version": "1.21.50",
                "minecraft:attachable": {
                    "description": {
                        "identifier": "<identifier>",
                        "materials": {
                            "default": "entity_alphatest",
                            "enchanted": "entity_alphatest_glint"
                        },
                        "textures": {
                            "default": "<texture>",
                            "enchanted": "textures/misc/enchanted_item_glint"
                        },
                        "geometry": {
                            "default": "<geometry>"
                        },
                        "render_controllers": ["controller.render.item_default"]
                    }
                }
            }
            """;

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws Exception {
        for (ItemEntry item : items) {
            if (!item.getModel().hasModel()) continue;

            List<ResolvedItemResource> resolvedItems = resolver.resolve(javaRootPath, item);

            String bedrockIdentifier;
            if (resolvedItems.isEmpty()) {
                bedrockIdentifier = resolver.resolveBedrockIdentifier(item, ItemEntry.DEFAULT_STATE);
            } else {
                bedrockIdentifier = resolvedItems.getFirst().bedrockIdentifier();
            }

            ItemModel model = item.getModel();

            List<String> textures = model.texturesPath(javaRootPath);

            System.out.println("textures " + textures);

            if (textures.isEmpty()) continue;

            JsonObject root = JsonUtils.parseJson(TEMPLATE_ATTACHABLE
                            .replace("<identifier>", "geyser_custom:" + bedrockIdentifier)
                            .replace("<texture>", model.toBedrockAtlasPath())
                            .replace("<geometry>", model.toBedrockGeometryId()));


            Path outputFile = bedrockRootPath
                    .resolve("attachables")
                    .resolve(bedrockIdentifier + ".json");
            this.writeJsonFile(outputFile, root);
        }
    }
}
