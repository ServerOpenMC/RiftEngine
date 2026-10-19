package fr.openmc.riftengine.core.converter.writers.blocks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.openmc.riftengine.api.scanner.blocks.BlockStateScanner;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.api.scanner.items.entry.behaviours.IABehaviour;
import fr.openmc.riftengine.api.scanner.items.entry.behaviours.block.BlocksBehaviour;
import fr.openmc.riftengine.core.RiftPlugin;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.utils.IdentifierUtils;
import fr.openmc.riftengine.core.utils.PathUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlocksMappingWriter implements PackWriter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final List<ItemEntry> items;

    public BlocksMappingWriter(List<ItemEntry> items) {
        this.items = items;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws Exception {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", 1);

        Map<String, JsonObject> blockStatesByBlock = new HashMap<>();

        for (ItemEntry item : items) {
            IABehaviour behaviour = item.getBehaviour();
            if (!behaviour.isBlock()) continue;
            if (!(behaviour instanceof BlocksBehaviour blockBehaviour)) continue;
            Map<String, String> resources = item.getBestResourcesId();

            if (resources == null) continue;

            BlockStateScanner.CustomBlockState state = blockBehaviour.blockState();
            JsonObject def = new JsonObject();

            def.addProperty("name", item.getKey());
            def.addProperty("geometry", "minecraft:geometry.full_block");
            def.add("material_instances", createMaterialInstance(item, resources.keySet()));

            blockStatesByBlock.computeIfAbsent(state.javaBlock(), _ -> new JsonObject())
                    .add(state.state(), def);

        }

        JsonObject blocksSetter = new JsonObject();
        blockStatesByBlock.forEach((javaBlock, overrides) -> {
            JsonObject def = new JsonObject();
            def.addProperty("name", IdentifierUtils.removeNamespace(javaBlock));
            def.addProperty("only_override_states", true);
            def.add("state_overrides", overrides);
            blocksSetter.add(javaBlock, def);
        });
        root.add("blocks", blocksSetter);

        Path outputFile = PathUtils.getGeyserPath(RiftPlugin.getInstance().getDataPath())
                .resolve("custom_mappings").resolve("ia-blocks-injected.json");
        Files.deleteIfExists(outputFile);
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private JsonObject createMaterialInstance(ItemEntry item, Set<String> faceResources) {
        JsonObject materialInstance = new JsonObject();
        String fallback = faceResources.stream()
                .filter(face -> !face.equals("all")).findFirst()
                .orElse(faceResources.stream().findFirst().orElse(null));
        if (fallback == null) return materialInstance;

        materialInstance.add("*", material(textureKey(item, fallback)));

        faceResources.forEach(face -> {
            switch (face) {
                case "side" -> List.of("north", "south", "east", "west")
                        .forEach(f -> materialInstance.add(f, material(textureKey(item, f))));
                case "up", "down", "north", "south", "east", "west" -> materialInstance.add(face, material(textureKey(item, face)));
                default -> {}
            }
        });

        return materialInstance;
    }

    public static String textureKey(ItemEntry item, String state) {
        return state.equals(ItemEntry.DEFAULT_STATE) ? item.getKey() : (item.getKey() + "_" + state);
    }

    private JsonObject material(String texture) {
        JsonObject m = new JsonObject();
        m.addProperty("texture", texture);
        m.addProperty("render_method", "alpha_test");
        m.addProperty("face_dimming", true);
        m.addProperty("ambient_occlusion", true);
        return m;
    }
}