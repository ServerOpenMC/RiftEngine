package fr.openmc.riftengine.core.utils;

import java.io.File;
import java.nio.file.Path;

public class PathUtils {
    public static Path getPathFromRoot(Path path, String rootName) {
        Path reducedPath = null;
        for (Path iterPath : path) {
            if (reducedPath != null) {
                reducedPath = reducedPath.resolve(iterPath);
            }

            if (reducedPath == null && iterPath.getFileName().toString().equals(rootName))
                reducedPath = iterPath;

        }
        return reducedPath;
    }

    public static Path getGeyserPath(Path dataPath) {
        File pluginsDir = dataPath.toFile().getParentFile(); // * root/plugins
        File geyserDir = new File(pluginsDir, "Geyser-Spigot"); // * root/plugins/Geyser-Spigot

        return geyserDir.toPath();
    }
}
