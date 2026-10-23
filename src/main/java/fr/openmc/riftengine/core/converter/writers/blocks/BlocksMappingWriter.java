package fr.openmc.riftengine.core.converter.writers.blocks;

import com.google.gson.JsonObject;
import fr.openmc.riftengine.api.scanner.blocks.BlockStateScanner;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.api.scanner.items.entry.behaviours.IABehaviour;
import fr.openmc.riftengine.api.scanner.items.entry.behaviours.block.BlocksBehaviour;
import fr.openmc.riftengine.api.utils.IdentifierUtils;
import fr.openmc.riftengine.core.RiftPlugin;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.resolvers.items.ItemResourceResolver;
import fr.openmc.riftengine.core.resolvers.items.ResolvedItemResource;
import fr.openmc.riftengine.core.utils.PathUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlocksMappingWriter implements PackWriter {
    private final List<ItemEntry> items;
    private final ItemResourceResolver resolver;
    private static final String OUTPUT_FILE = "custom_mappings/ia-blocks-injected.json";

    public BlocksMappingWriter(List<ItemEntry> items, ItemResourceResolver resolver) {
        this.items = items;
        this.resolver = resolver;
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

            List<ResolvedItemResource> resources = resolver.resolve(javaRootPath, item);

            String parentModel = item.getGraphics().parent();
            BlockStateScanner.CustomBlockState state = blockBehaviour.blockState();

            JsonObject def = new JsonObject();
            def.addProperty("name", item.getKey());
            def.addProperty("geometry", getGeometryBlockFromParent(parentModel));
            def.add("material_instances", createMaterialInstance(resources));

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

        Path outputFile = PathUtils
                .getGeyserPath(RiftPlugin.getInstance().getDataPath())
                .resolve(OUTPUT_FILE);

        Files.deleteIfExists(outputFile);
        this.writeJsonFile(outputFile, root);
        this.writeJsonFile(RiftPlugin.getInstance().getOutputDir().resolve(OUTPUT_FILE), root);
    }

    private JsonObject createMaterialInstance(List<ResolvedItemResource> faceResources) {
        JsonObject materialInstance = new JsonObject();

        ResolvedItemResource fallback = faceResources.stream()
                .filter(resourceFace -> !resourceFace.state().equals("all"))
                .findFirst()
                .orElse(null);

        if (fallback == null) return materialInstance;

        materialInstance.add("*", materialObj(fallback.bedrockIdentifier()));

        faceResources.forEach(face -> {
            switch (face.state()) {
                case "side" -> List.of("north", "south", "east", "west")
                        .forEach(f -> materialInstance.add(f, materialObj(face.bedrockIdWithState(f))));
                case "up", "down", "north", "south", "east", "west" -> materialInstance.add(face.state(), materialObj(face.bedrockIdentifier()));
                default -> {}
            }
        });

        return materialInstance;
    }

    private JsonObject materialObj(String texture) {
        JsonObject m = new JsonObject();
        m.addProperty("texture", texture);
        m.addProperty("render_method", "alpha_test");
        m.addProperty("face_dimming", true);
        m.addProperty("ambient_occlusion", true);
        return m;
    }

    private String getGeometryBlockFromParent(String parentModel) {
        String defaultGeometry = "minecraft:geometry.full_block";
        if (parentModel == null) return defaultGeometry;
        String parent = IdentifierUtils.normalizeId(parentModel);

        if (!parent.startsWith("minecraft:")) return defaultGeometry;
        if (parentModel.contains("block/cross"))
            return "minecraft:geometry.cross";
        return defaultGeometry;
    }
}