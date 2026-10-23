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

    private final String TEMPLATE_ATTACHABLE = """
            {
                "format_version": "1.21.50",
                "minecraft:attachable": {
                    "description": {
                        "identifier": "<identifier>",
                        "materials": {
                            "default": "<material>",
                            "enchanted": "entity_alphatest_glint"
                        },
                        "textures": {
                            "default": "<texture>",
                            "enchanted": "textures/misc/enchanted_item_glint"
                        },
                        "geometry": {
                            "default": "<geometry>"
                        },
                        "scripts": {
                            "pre_animation": [
                                "v.main_hand = c.item_slot == 'main_hand';",
                                "v.off_hand = c.item_slot == 'off_hand';",
                                "v.head = c.item_slot == 'head';"
                            ],
                            "animate": [
                                {"thirdperson_main_hand": "v.main_hand && !c.is_first_person"},
                                {"thirdperson_off_hand": "v.off_hand && !c.is_first_person"},
                                {"thirdperson_head": "v.head && !c.is_first_person"},
                                {"firstperson_main_hand": "v.main_hand && c.is_first_person"},
                                {"firstperson_off_hand": "v.off_hand && c.is_first_person"},
                                {"firstperson_head": "c.is_first_person && v.head"}
                            ]
                        },
                        "animations": {
                            "thirdperson_main_hand": "<animation>.thirdperson_main_hand",
                            "thirdperson_off_hand": "<animation>.thirdperson_off_hand",
                            "thirdperson_head": "<animation>.head",
                            "firstperson_main_hand": "<animation>.firstperson_main_hand",
                            "firstperson_off_hand": "<animation>.firstperson_off_hand",
                            "firstperson_head": "<animation>.disable"
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

            if (bedrockIdentifier == null) continue;

            ItemModel model = item.getModel();

            List<String> textures = model.texturesPath(javaRootPath);

            System.out.println("textures " + textures);

            if (textures.isEmpty()) continue;

            JsonObject root = JsonUtils.parseJson(TEMPLATE_ATTACHABLE
                    .replace("<identifier>", "geyser_custom:" + bedrockIdentifier)
                    .replace("<material>", "entity_alphablend")
                    .replace("<texture>", model.toBedrockAtlasPath())
                    .replace("<geometry>", model.toBedrockGeometryId())
                    .replace("<animation>", model.toBedrockAnimationId())
            );

            Path outputFile = bedrockRootPath
                    .resolve("attachables")
                    .resolve(bedrockIdentifier + ".json");
            this.writeJsonFile(outputFile, root);
        }
    }
}
