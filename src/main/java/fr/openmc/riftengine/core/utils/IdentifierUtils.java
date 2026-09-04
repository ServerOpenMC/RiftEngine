package fr.openmc.riftengine.core.utils;

import java.nio.file.Path;

public class IdentifierUtils {
    /**
     * Normalise un identifiant (qui n'a pas un namespace devant
     * @param id l'id (font/default.png, minecraft:font/default.png)
     * @return l'id final
     */
    public static String normalizeId(String id) {
        return normalizeId(id, "minecraft");
    }

    /**
     * Normalise un identifiant (qui n'a pas un namespace devant
     * @param id l'id (font/default.png, minecraft:font/default.png)
     * @param defaultNamespace le namespace par défaut à utiliser, si on est dans un fichier d'items adder par ex
     * @return l'id final
     */
    public static String normalizeId(String id, String defaultNamespace) {
        if (id.split(":").length == 2) return id;

        return defaultNamespace + ":" + id;
    }

    /**
     * Résout un identifiant en un chemin de fichier dans les assets
     */
    public static Path resolveTextureId(Path rootPath, String id) {
        String normalizedId = normalizeId(id);

        String[] split = normalizedId.split(":");
        if (split.length == 2)
           return rootPath
                .resolve("assets")
                .resolve(normalizedId.split(":")[0])
                .resolve("textures")
                .resolve(normalizedId.split(":")[1]);
        else return rootPath
                .resolve("assets")
                .resolve("minecraft")
                .resolve("textures")
                .resolve(normalizedId.split(":")[0]);
    }
}
