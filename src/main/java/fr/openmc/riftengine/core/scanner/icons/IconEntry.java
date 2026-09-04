package fr.openmc.riftengine.core.scanner.icons;

import fr.openmc.riftengine.core.utils.IdentifierUtils;
import fr.openmc.riftengine.core.utils.YmlUtils;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

public record IconEntry(
        String namespace,
        String key,
        Function<Path, Path> path,
        int permission,
        boolean showInGui,
        double scaleRatio,
        int yPosition,
        Path sourceYml
) {
    public static IconEntry from(Path sourceYml, String namespace, String key, Map<?, ?> data) {
        return new IconEntry(
                namespace,
                key,
                javaRoot -> IdentifierUtils.resolveTextureId(javaRoot,
                        IdentifierUtils.normalizeId(String.valueOf(data.get("path")), namespace)),
                YmlUtils.getInt(data.get("permission"), 0),
                YmlUtils.getBool(data.get("show_in_gui"), true),
                YmlUtils.getDouble(data.get("scale_ratio"), 9),
                YmlUtils.getInt(data.get("y_position"), 8),
                sourceYml
        );
    }

    public String namespacedId() {
        return namespace + ":" + key;
    }
}