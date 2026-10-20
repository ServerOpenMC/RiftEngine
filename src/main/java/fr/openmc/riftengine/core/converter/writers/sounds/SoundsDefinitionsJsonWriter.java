package fr.openmc.riftengine.core.converter.writers.sounds;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.openmc.core.OMCRegistry;
import fr.openmc.riftengine.api.scanner.sounds.SoundEntry;
import fr.openmc.riftengine.core.converter.writers.PackWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Ecrit le sounds_definitions.json qui sert grosso modo a relier les textures aux items (une ref = une resource)
 */
public class SoundsDefinitionsJsonWriter implements PackWriter {
    private static final String OUTPUT_FILE = "sounds/sound_definitions.json";

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        List<SoundEntry> sounds = OMCRegistry.SCANNERS.SOUNDS.scan(javaRootPath);
        JsonObject definitions = new JsonObject();

        for (SoundEntry entry : sounds) {
            JsonArray array = new JsonArray();
            for (String resourceId : entry.soundResourcesId()) {
                if (resourceId == null) continue;
                array.add(entry.toBedrockPath("minecraft", resourceId));
            }
            if (array.isEmpty()) continue;

            JsonObject def = new JsonObject();
            def.addProperty("category", "player");
            def.add("sounds", array);
            definitions.add(entry.toBedrockEventId(), def);
        }

        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.21.50");
        root.add("sound_definitions", definitions);

        Path outputFile = bedrockRootPath.resolve(OUTPUT_FILE);
        this.writeJsonFile(outputFile, root);
    }
}
