package fr.openmc.riftengine.core.converter.writers.items.specials.model.converter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.openmc.core.utils.JsonUtils;
import fr.openmc.riftengine.api.utils.IdentifierUtils;
import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Adapte les textures d'un modèle car bedrock ne supporte pas les plusieures textures par modeles :/
 *
 * Classe généré par IA :{
 * todo: la refaire à ma sauce, un ImageUtils car le probleme pour faire un atlas je l'avais aussi dans FrameTextureConverter
 */
@Getter
public final class ModelTextureAtlas {

    public record Sprite(int x, int y, int width, int height) {
    }

    private final BufferedImage image;
    private final Map<String, Sprite> spritesByKey;

    private ModelTextureAtlas(BufferedImage image, Map<String, Sprite> spritesByKey) {
        this.image = image;
        this.spritesByKey = spritesByKey;
    }

    public int width() {
        return image.getWidth();
    }

    public int height() {
        return image.getHeight();
    }

    public Sprite sprite(String ref) {
        if (ref == null) return null;
        return spritesByKey.get(ref.startsWith("#") ? ref.substring(1) : ref);
    }

    public static ModelTextureAtlas build(Path javaRootPath, JsonObject model, String defaultNamespace) throws IOException {
        JsonObject textures = JsonUtils.getJsonObject(model, "textures", null);

        if (textures == null) return null;

        Map<String, String> keyToTexture = new LinkedHashMap<>();
        for (String key : textures.keySet()) {
            if (key.equals("particle")) continue;
            String id = resolveAlias(textures, key, defaultNamespace);
            if (id != null) keyToTexture.put(key, id);
        }

        Map<String, BufferedImage> images = new LinkedHashMap<>();
        for (String id : new LinkedHashSet<>(keyToTexture.values())) {
            BufferedImage img = loadFirstFrame(javaRootPath, id);
            if (img != null) images.put(id, img);
        }
        if (images.isEmpty()) return null;

        int totalWidth = 0, maxHeight = 0;
        for (BufferedImage img : images.values()) {
            totalWidth += img.getWidth();
            maxHeight = Math.max(maxHeight, img.getHeight());
        }

        BufferedImage atlas = new BufferedImage(totalWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = atlas.createGraphics();
        Map<String, Sprite> spriteByTexture = new HashMap<>();
        int x = 0;
        for (Map.Entry<String, BufferedImage> entry : images.entrySet()) {
            BufferedImage img = entry.getValue();
            g.drawImage(img, x, 0, null);
            spriteByTexture.put(entry.getKey(), new Sprite(x, 0, img.getWidth(), img.getHeight()));
            x += img.getWidth();
        }
        g.dispose();

        Map<String, Sprite> spritesByKey = new HashMap<>();
        for (Map.Entry<String, String> entry : keyToTexture.entrySet()) {
            Sprite sprite = spriteByTexture.get(entry.getValue());
            if (sprite != null) spritesByKey.put(entry.getKey(), sprite);
        }
        return new ModelTextureAtlas(atlas, spritesByKey);
    }

    private static String resolveAlias(JsonObject textures, String key, String defaultNamespace) {
        JsonElement element = textures.get(key);
        for (int i = 0; i < 10 && element != null && element.isJsonPrimitive(); i++) {
            String value = element.getAsString();
            if (!value.startsWith("#"))
                return IdentifierUtils.normalizeId(value.startsWith("_") ? value.substring(1) : value, defaultNamespace);
            element = textures.get(value.substring(1));
        }
        return null;
    }

    private static BufferedImage loadFirstFrame(Path javaRootPath, String textureId) throws IOException {
        Path png = IdentifierUtils.resolveTextureId(javaRootPath, textureId);
        if (!Files.exists(png)) return null;

        BufferedImage img = ImageIO.read(png.toFile());
        if (img == null) return null;

        Path meta = IdentifierUtils.resolveMetaId(javaRootPath, textureId, ".png");
        if (!Files.exists(meta)) return img;

        JsonObject metaJson;
        try {
            metaJson = JsonUtils.loadJson(meta);
        } catch (RuntimeException e) {
            return img;
        }
        if (metaJson == null || !metaJson.has("animation")) return img;

        JsonObject animation = JsonUtils.getJsonObject(metaJson, "animation", new JsonObject());
        int frameWidth = JsonUtils.getInt(animation, "width", img.getWidth());
        int frameHeight = JsonUtils.getInt(animation, "height", frameWidth);

        frameWidth = Math.min(frameWidth, img.getWidth());
        frameHeight = Math.min(frameHeight, img.getHeight());
        return img.getSubimage(0, 0, frameWidth, frameHeight);
    }
}
