package fr.openmc.riftengine.core.converter.writers.items.specials.model;

import com.google.gson.JsonObject;
import fr.openmc.core.utils.JsonUtils;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.api.scanner.items.entry.ItemModel;
import fr.openmc.riftengine.api.utils.IdentifierUtils;
import fr.openmc.riftengine.core.converter.writers.PackWriter;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemModelGeometryWriter implements PackWriter {
    private final List<ItemEntry> items;

    public ItemModelGeometryWriter(List<ItemEntry> items) {
        this.items = items;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws Exception {
        Set<String> modelCreated = new HashSet<>();

        for (ItemEntry item : items) {
            if (!item.getModel().hasModel()) continue;

            ItemModel model = item.getModel();
            String modelName = model.modelId();

            if (modelCreated.contains(modelName)) continue;
            modelCreated.add(modelName);

            Path targetJson = IdentifierUtils.resolveModelId(javaRootPath, modelName);
            JsonObject rootModelJson = JsonUtils.loadJson(targetJson);

            if (rootModelJson == null) continue;

            JsonObject rootModelGeoJson = JavaModelToGeometryConverter.convert(
                    rootModelJson,
                    model.toBedrockGeometryId(),
                    true
            );

            Path output = bedrockRootPath.resolve(model.toBedrockModelPath());
            this.writeJsonFile(output, rootModelGeoJson);
        }
    }
}
