package fr.openmc.riftengine.core.converter.writers.items.specials.model;

import com.google.gson.JsonObject;
import fr.openmc.core.utils.JsonUtils;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.api.scanner.items.entry.ItemModel;
import fr.openmc.riftengine.api.utils.IdentifierUtils;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.converter.writers.items.specials.model.converter.ModelTextureAtlas;

import javax.imageio.ImageIO;
import java.nio.file.Files;
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

            JsonObject rootModelJson = JsonUtils.loadJson(IdentifierUtils.resolveModelId(javaRootPath, modelName));
            if (rootModelJson == null) continue;

            ModelTextureAtlas atlas = ModelTextureAtlas.build(javaRootPath, rootModelJson, model.namespace());
            System.out.println("atlas de " + item.namespacedId() + " " + atlas);
            if (atlas == null) continue;

            Path output = bedrockRootPath.resolve(model.toBedrockAtlasPath() + ".png");
            Files.createDirectories(output.getParent());
            ImageIO.write(atlas.getImage(), "png", output.toFile());

        }
    }
}
