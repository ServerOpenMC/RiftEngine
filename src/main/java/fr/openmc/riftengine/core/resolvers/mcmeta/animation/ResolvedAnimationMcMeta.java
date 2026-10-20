package fr.openmc.riftengine.core.resolvers.mcmeta.animation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.openmc.core.utils.JsonUtils;
import fr.openmc.riftengine.core.RiftLog;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * basé sur <a href="https://minecraft.wiki/w/Resource_pack#Texture_animation">...</a>
 */
public record ResolvedAnimationMcMeta(
        boolean interpolate,
        Integer widthFrame, // taille en px d'une frame
        Integer heightFrame, // hauteur en px d'une frame
        int frameTime, // en ticks
        List<Integer> frames,
        List<Integer> frameTimes
) {
    public static ResolvedAnimationMcMeta read(Path mcmetaPath) {
        JsonObject root = JsonUtils.loadJson(mcmetaPath);

        if (root == null) {
            RiftLog.ERROR_READING.send(mcmetaPath);
            return null;
        }

        JsonElement animation = root.get("animation");
        if (animation == null || !animation.isJsonObject()) {
            RiftLog.NULL.send("animation");
            return null;
        }

        JsonObject anim = root.getAsJsonObject("animation");

        int frametime = Math.max(1, JsonUtils.getInt(anim, "frametime", 1));

        List<Integer> frames = new ArrayList<>();
        List<Integer> frameTimes = new ArrayList<>();
        if (anim.has("frames") && anim.get("frames").isJsonArray()) {
            for (JsonElement element : anim.getAsJsonArray("frames")) {
                if (element.isJsonObject()) {
                    JsonObject f = element.getAsJsonObject();
                    frames.add(JsonUtils.getInt(f, "index", 0));
                    frameTimes.add(Math.max(1, JsonUtils.getInt(f, "time", frametime)));
                }
            }
        }

        return new ResolvedAnimationMcMeta(
                anim.has("interpolate") && anim.get("interpolate").getAsBoolean(),
                JsonUtils.getInt(anim, "width", 0),
                JsonUtils.getInt(anim, "height", 0),
                frametime,
                frames,
                frameTimes);
    }
}
