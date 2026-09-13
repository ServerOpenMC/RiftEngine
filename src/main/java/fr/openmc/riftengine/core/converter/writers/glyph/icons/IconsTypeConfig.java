package fr.openmc.riftengine.core.converter.writers.glyph.icons;

import fr.openmc.core.bootstrap.integration.OMCLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class IconsTypeConfig {

    private final Map<String, IconsType> overrides;

    private IconsTypeConfig(Map<String, IconsType> overrides) {
        this.overrides = overrides;
    }

    public static IconsTypeConfig load(Path configPath) throws IOException {
        Map<String, IconsType> overrides = new HashMap<>();

        if (!Files.exists(configPath)) return new IconsTypeConfig(overrides);

        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(configPath)) {
            Object parsed = yaml.load(in);
            if (!(parsed instanceof Map<?, ?> root)) return new IconsTypeConfig(overrides);

            Object section = root.get("icon-type-overrides");
            if (!(section instanceof Map<?, ?> overridesMap)) return new IconsTypeConfig(overrides);

            for (Map.Entry<?, ?> entry : overridesMap.entrySet()) {
                String namespacedId = String.valueOf(entry.getKey());
                String typeName = String.valueOf(entry.getValue());

                try {
                    overrides.put(namespacedId, IconsType.valueOf(typeName.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    OMCLogger.errorFormatted("Type inconnu pour " + namespacedId + " : " + typeName + " (skip)");
                }
            }
        }

        return new IconsTypeConfig(overrides);
    }

    public IconsType get(String namespacedId) {
        return overrides.get(namespacedId);
    }
}
