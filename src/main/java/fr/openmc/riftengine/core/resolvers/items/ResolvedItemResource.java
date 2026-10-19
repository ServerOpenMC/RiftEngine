package fr.openmc.riftengine.core.resolvers.items;

import fr.openmc.riftengine.api.scanner.items.ItemEntry;

import java.nio.file.Path;

public record ResolvedItemResource(ItemEntry item,
                                   String state, // bottom, top, side
                                   String resourceId, // ex aywenite (sans le namespace devant), cloud_fishing_rod_cast
                                   String modelResourceId, // ex omc_dream:ia_auto/dream_boot
                                   String bedrockIdentifier, //
                                   Path sourcePath,
                                   String bedrockTexturePath // ex textures/aywenite
                                   ) {
    public String bedrockIdWithState(String state) {
        return state.equals(ItemEntry.DEFAULT_STATE) ? item.getKey() : (item.getKey() + "_" + state);
    }

    public boolean isVanilla() {
        return resourceId.startsWith("minecraft:");
    }
}
