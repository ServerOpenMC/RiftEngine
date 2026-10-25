package fr.openmc.riftengine.core.resolvers.items;

import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.api.utils.IdentifierUtils;
import fr.openmc.riftengine.core.RiftLog;
import fr.openmc.riftengine.core.registry.resolver.AbstractResolver;
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
            RiftLog.ITEM_NOT_RESOURCE.send(item.namespacedId());
            return List.of();
        }

        Map<String, Path> bedrockTexturePaths = item.getResourcePath().apply(javaRootPath);

        List<ResolvedItemResource> resolved = new ArrayList<>();
        for (Map.Entry<String, String> entry : resources.entrySet()) {
            String state = entry.getKey();
            String resourceId = entry.getValue();
            Path bedrockTexturePath = bedrockTexturePaths.get(state);
            String bedrockIdentifier = resolveBedrockIdentifier(item, state);
            String bedrockTextureValue = null;
            String modelResourceId = item.getNamespace() + ":ia_auto/" + item.getKey();

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
                    RiftLog.RESOURCE_NOT_FOUND.send(item.getKey());
                    return List.of();
                }

                Path reducedPath = PathUtils.getPathFromRoot(resourcePath, "textures");
                if (reducedPath != null) {
                    bedrockTextureValue = IdentifierUtils.toBedrockTexturePath(reducedPath.toString()).toString()
                            .replaceFirst("\\.[^/.]+$", "");
                }
            }

            if (bedrockTextureValue == null) {
                RiftLog.RESOURCE_NOT_FOUND.send(item.getKey());
                return List.of();
            }

            resolved.add(new ResolvedItemResource(
                    item, state, resourceId, modelResourceId,
                    bedrockIdentifier, bedrockTexturePath, bedrockTextureValue));
        }

        return resolved;
    }

    public String resolveBedrockIdentifier(ItemEntry item, String state) {
        return state.equals(ItemEntry.DEFAULT_STATE) ? resolveBedrockIdentifier(item) : item.getKey() + "_" + state;

    }

    public String resolveBedrockIdentifier(ItemEntry item) {
        return item.getKey();

    }
}
