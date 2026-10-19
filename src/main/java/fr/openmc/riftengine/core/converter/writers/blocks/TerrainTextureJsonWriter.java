package fr.openmc.riftengine.core.converter.writers.blocks;

import com.google.gson.JsonObject;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.api.scanner.items.entry.behaviours.IABehaviour;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.converter.writers.manifest.PackIdentity;
import fr.openmc.riftengine.core.resolvers.items.ItemResourceResolver;
import fr.openmc.riftengine.core.resolvers.items.ResolvedItemResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Ecrit le terrain_texture.json qui sert grosso modo a relier les textures aux items (une ref = une resource)
 */
public class TerrainTextureJsonWriter implements PackWriter {
    private final PackIdentity packIdentity;
    private final List<ItemEntry> items;
    private final ItemResourceResolver resolver;
    private static final String OUTPUT_FILE = "textures/terrain_texture.json";

    public TerrainTextureJsonWriter(PackIdentity packIdentity, List<ItemEntry> items, ItemResourceResolver resolver) {
        this.packIdentity = packIdentity;
        this.items = items;
        this.resolver = resolver;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject textureData = new JsonObject();

        for (ItemEntry itemEntry : items) {
            IABehaviour behaviour = itemEntry.getBehaviour();
            if (!behaviour.isBlock()) continue;

            List<ResolvedItemResource> resolvedItemResources = resolver.resolve(javaRootPath, itemEntry);

            for (ResolvedItemResource resource : resolvedItemResources) {
                JsonObject itemData = new JsonObject();
                itemData.addProperty("textures", resource.bedrockTexturePath());
                textureData.add(resource.bedrockIdentifier(), itemData);
            }
        }

        root.addProperty("resource_pack_name", packIdentity.getPackName());
        root.addProperty("texture_name", "atlas.terrain");
        root.addProperty("padding", 8);
        root.addProperty("num_mip_levels", 4);
        root.add("texture_data", textureData);

        Path outputFile = bedrockRootPath.resolve(OUTPUT_FILE);
        this.writeJsonFile(outputFile, root);
    }
}
