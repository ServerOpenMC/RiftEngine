package fr.openmc.riftengine.core.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class YmlUtils {
    public static Map<String, Object> loadYml(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return new Yaml().load(in);
        } catch (Exception e) {
            throw new IOException("Erreur lors de la lecture du fichier yml " + path, e);
        }
    }

    public static int getInt(Object obj, int def) {
        if (obj == null) return def;
        if (obj instanceof Number n) return n.intValue();
        return Integer.parseInt(obj.toString());
    }

    public static boolean getBool(Object obj, boolean def) {
        if (obj == null) return def;
        if (obj instanceof Boolean b) return b;
        return Boolean.parseBoolean(obj.toString());
    }

    public static double getDouble(Object obj, double def) {
        if (obj == null) return def;
        if (obj instanceof Number n) return n.doubleValue();
        return Double.parseDouble(obj.toString());
    }
}
