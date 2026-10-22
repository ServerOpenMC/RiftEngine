package fr.openmc.riftengine.core.converter.writers.items.specials.model;

import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.api.scanner.items.entry.ItemModel;
import fr.openmc.riftengine.api.utils.IdentifierUtils;
import fr.openmc.riftengine.core.converter.writers.PackWriter;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemModelTextureWriter implements PackWriter {
    private final List<ItemEntry> items;

    public ItemModelTextureWriter(List<ItemEntry> items) {
        this.items = items;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws Exception {
        Set<String> modelCreated = new HashSet<>();

        for (ItemEntry item : items) {
            if (!item.getModel().hasModel()) continue;

            ItemModel model = item.getModel();
            String modelName = model.modelId();

            if (modelName.startsWith("minecraft:")) continue;

            if (modelCreated.contains(modelName)) continue;
            modelCreated.add(modelName);

            for (String textureName : model.texturesPath(javaRootPath)) {
                Path javaTexturePath = IdentifierUtils.resolveTextureId(javaRootPath, item.getNamespace(), textureName);
                this.copyFile(javaTexturePath,
                        bedrockRootPath.resolve("textures/" + IdentifierUtils.toBedrockTexturePath(textureName)));
            }
        }
    }
}
