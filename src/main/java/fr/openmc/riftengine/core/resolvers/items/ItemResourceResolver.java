package fr.openmc.riftengine.core.resolvers.items;

import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.core.RiftError;
import fr.openmc.riftengine.core.registry.resolver.AbstractResolver;
import fr.openmc.riftengine.core.utils.IdentifierUtils;
import fr.openmc.riftengine.core.utils.PathUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemResourceResolver extends AbstractResolver<ItemEntry, List<ResolvedItemResource>> {

    @Override
    public List<ResolvedItemResource> resolve(Path javaRootPath, ItemEntry item) {
        Map<String, String> resources = item.getBestResourcesId();

        if (resources == null || resources.isEmpty()) {
            RiftError.ITEM_NOT_RESOURCE.send(item.namespacedId());
            return List.of();
        }

        Map<String, Path> bedrockTexturePaths = item.getResourcePath().apply(javaRootPath);

        List<ResolvedItemResource> resolved = new ArrayList<>();
        for (Map.Entry<String, String> entry : resources.entrySet()) {
            String state = entry.getKey();
            String resourceId = entry.getValue();
            Path bedrockTexturePath = bedrockTexturePaths.get(state);
            String bedrockIdentifier = state.equals(ItemEntry.DEFAULT_STATE) ? item.getKey() : item.getKey() + "_" + state;
            String bedrockTextureValue = null;

            if (resourceId.isEmpty()) {
                String folder = item.getBehaviour().isItem() ? "items" : "blocks";
                bedrockTextureValue = "textures/" + folder + "/" + item.getMaterial().name().toLowerCase();
            } else if (resourceId.startsWith("minecraft:")) {
                String path = IdentifierUtils.normalizeId(resourceId).split(":", 2)[1];
                bedrockTextureValue = IdentifierUtils.toBedrockTexturePath("textures/" + path);
            } else {
                Path resourcePath;
                if (bedrockTexturePaths.size() > 1) {
                    resourcePath = bedrockTexturePaths.get(state);
                } else if (bedrockTexturePaths.size() == 1) {
                    resourcePath = bedrockTexturePaths.values().stream()
                            .findFirst().get();
                } else {
                    RiftError.RESOURCE_NOT_FOUND.send(item.getKey());
                    return List.of();
                }

                Path reducedPath = PathUtils.getPathFromRoot(resourcePath, "textures");
                if (reducedPath != null) {
                    bedrockTextureValue = IdentifierUtils.toBedrockTexturePath(reducedPath.toString())
                            .replaceFirst("\\.[^/.]+$", "");
                }
            }

            if (bedrockTextureValue == null) {
                RiftError.RESOURCE_NOT_FOUND.send(item.getKey());
                return List.of();
            }

            resolved.add(new ResolvedItemResource(
                    item, state, resourceId,
                    bedrockIdentifier, bedrockTexturePath, bedrockTextureValue));
        }

        return resolved;
    }
}
