package fr.openmc.riftengine.core.converter.writers.items.specials.armor;

import com.google.gson.JsonObject;
import fr.openmc.core.utils.JsonUtils;
import fr.openmc.riftengine.api.scanner.equipements.EquipementEntry;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.api.scanner.items.entry.behaviours.armors.ArmorType;
import fr.openmc.riftengine.api.scanner.items.entry.behaviours.armors.ArmorsBehaviour;
import fr.openmc.riftengine.core.RiftLog;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.resolvers.items.ItemResourceResolver;
import fr.openmc.riftengine.core.resolvers.items.ResolvedItemResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class CustomArmorAttachableWriter implements PackWriter {
    private final List<ItemEntry> items;
    private final ItemResourceResolver resolver;

    public CustomArmorAttachableWriter(List<ItemEntry> items, ItemResourceResolver resolver) {
        this.items = items;
        this.resolver = resolver;
    }

    private final String TEMPLATE_ATTACHABLE = """
            {
                "format_version": "1.8.0",
                "minecraft:attachable": {
                    "description": {
                        "identifier": "<identifier>",
                        "materials": {
                            "default": "armor",
                            "enchanted": "armor_enchanted"
                        },
                        "textures": {
                            "default": "<textures>",
                            "enchanted": "textures/misc/enchanted_actor_glint",
                            "baby": "<textures>"
                        },
                        "geometry": {
                            "default": "geometry.humanoid.armor.<type>",
                            "baby": "geometry.humanoid.baby.armor.<type>"
                        },
                        "scripts": {
                            // Prevents parts of the player's skin from sticking through the armor
                            "parent_setup": "v.<type_2>_layer_visible = false;",
                            "pre_animation": [
                                // Variable used by the render controller to determine whether the "baby" geometry and texture should be used
                                "v.use_baby_geo = q.is_baby && c.owning_entity->v.use_dedicated_baby_armor;"
                            ]
                        },
                        "render_controllers": ["controller.render.armor.v2"]
                    }
                }
            }
            """;

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        for (ItemEntry item : items) {
            if (!item.getBehaviour().isArmor()) continue;
            if (!(item.getBehaviour() instanceof ArmorsBehaviour(
                    EquipementEntry entry,
                    ArmorType type
            ))) continue;
            if (entry == null) {
                RiftLog.ENTRY_EQUIPMENT_NULL.send(item.namespacedId());
                continue;
            }

            List<ResolvedItemResource> resolvedItems = resolver.resolve(javaRootPath, item);

            String bedrockIdentifier;
            if (resolvedItems.isEmpty()) {
                bedrockIdentifier = resolver.resolveBedrockIdentifier(item, "null");
            } else {
                bedrockIdentifier = resolvedItems.getFirst().bedrockIdentifier();
            }

            if (bedrockIdentifier == null) continue;

            JsonObject root = JsonUtils.parseJson(TEMPLATE_ATTACHABLE
                    .replace("<identifier>", "geyser_custom:" + bedrockIdentifier)
                    .replace("<textures>", "textures/" + type.selectLayer(
                            entry.layer1Path(), entry.layer2Path()
                    ))
                    .replace("<type>", type.getType())
                    .replace("<type_2>", type.getShortType())
            );


            Path outputFile = bedrockRootPath
                    .resolve("attachables")
                    .resolve(bedrockIdentifier + ".json");
            this.writeJsonFile(outputFile, root);
        }
    }
}
