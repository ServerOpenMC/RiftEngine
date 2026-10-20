package fr.openmc.riftengine.core.resolvers.mcmeta.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.openmc.core.utils.JsonUtils;
import fr.openmc.riftengine.core.RiftLog;

import java.nio.file.Path;

/**
 * basé sur <a href="https://minecraft.wiki/w/Resource_pack#GUI">...</a>
 */
public record ResolvedGuiMcMeta(
        String scallingType, // entre stretch (default), tile, ou nine_slice
        Integer widthFrame, // taille en px d'une frame
        Integer heightFrame, // hauteur en px d'une frame
        boolean stretchInner,
        Border border
) {
    private record Border(Integer left, Integer right, Integer top, Integer bottom) {
        public Border(Integer border) {
            this(border, border, border, border);
        }
    }
    public static ResolvedGuiMcMeta read(Path mcmetaPath) {
        JsonObject root = JsonUtils.loadJson(mcmetaPath);

        if (root == null) {
            RiftLog.ERROR_READING.send(mcmetaPath);
            return null;
        }

        JsonElement gui = root.get("gui");
        if (gui == null || !gui.isJsonObject()) {
            RiftLog.NULL.send("gui");
            return null;
        }

        JsonElement scaling = gui.getAsJsonObject().get("scaling");
        if (scaling == null || !scaling.isJsonObject()) {
            RiftLog.NULL.send("scaling");
            return null;
        }

        JsonObject scalingObj = scaling.getAsJsonObject();

        Border border = null;

        JsonElement borderElt = scalingObj.get("border");
        if (borderElt.isJsonObject()) {
            JsonObject borderObj = borderElt.getAsJsonObject();
            border = new Border(
                    JsonUtils.getInt(borderObj, "left", null),
                    JsonUtils.getInt(borderObj, "right", null),
                    JsonUtils.getInt(borderObj, "top", null),
                    JsonUtils.getInt(borderObj, "bottom", null)
            );
        } else if (borderElt.isJsonPrimitive()) {
            border = new Border(JsonUtils.getInt(scalingObj, "border", null));
        }

        return new ResolvedGuiMcMeta(
                JsonUtils.getString(scalingObj, "type", "stretch"),
                JsonUtils.getInt(scalingObj, "width", null),
                JsonUtils.getInt(scalingObj, "height",  null),
                JsonUtils.getBool(scalingObj, "stretch_inner", false),
                border
        );
    }
}
