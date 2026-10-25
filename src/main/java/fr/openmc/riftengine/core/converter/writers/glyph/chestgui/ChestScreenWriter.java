package fr.openmc.riftengine.core.converter.writers.glyph.chestgui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.openmc.core.utils.JsonUtils;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ChestScreenWriter implements PackWriter {
    public static final List<ResolvedChestGui> chestGuis = new ArrayList<>();
    private static final String OUTPUT_FILE = "ui/chest_screen.json";
    public static final String PREFIX = "rift";
    private final String namespace = "chest";

    private final String TEMPLATE_CHEST_SCREEN = """
            {
              "namespace": <namespace>,
            
              "chest_label": {
                "type": "label",
                "layer": 2,
                "offset": [7, -1],
                "size": ["90%", "default"],
                "anchor_from": "top_left",
                "anchor_to": "top_left",
                "text": "$container_title",
                "color": "$title_text_color",
                "localize": "$localize_title",
                "enable_profanity_filter": true,
                "$atext": "$container_title",
                "visible": "($atext - '<prefix>:' = $atext)"
              }
            }
            """;

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        JsonObject root = JsonUtils.parseJson(TEMPLATE_CHEST_SCREEN
                .replace("<namespace>", namespace)
                .replace("<prefix>", PREFIX));

        this.handleChestGui(javaRootPath, root);
        this.handlePanelTopHalf(root);


        Path outputFile = bedrockRootPath.resolve(OUTPUT_FILE);
        this.writeJsonFile(outputFile, root);
    }

    private final String TEMPLATE_CHEST_GUI = """
            {
                "type": "image",
                "texture": "<texture_path>",
                "size": [<width>, <height>],
                "keep_ratio": false,
                "offset": [<offset_x>, <offset_y>],
                "anchor_from": "top_middle",
                "anchor_to": "top_middle",
                "layer": 4,
                "$atext": "$container_title",
                "visible": "<visible_condition>"
            }
            """;

    private void handleChestGui(Path javaRootPath, JsonObject root) throws IOException {
        for (ResolvedChestGui chestGui : chestGuis) {
            BufferedImage src = ImageUtils.read(chestGui.javaTexturePath(javaRootPath));
            BufferedImage centered = ImageUtils.center(src);

            int shift = ImageUtils.centerShiftY(src);
            int offsetY = 5 - chestGui.entry().yPosition() - shift;

            int gridLeft = ImageUtils.findGridLeft(centered);
            int offsetX = gridLeft == -1 ? 0 : 7 - gridLeft - (176 - centered.getWidth()) / 2;

            root.add(PREFIX + "_" + chestGui.key(), JsonUtils.parseJson(TEMPLATE_CHEST_GUI
                    .replace("<texture_path>", chestGui.bedrockTexturePath(javaRootPath).toString())
                    .replace("<width>", String.valueOf(chestGui.width()))
                    .replace("<height>", String.valueOf(chestGui.height()))
                    .replace("<offset_y>", String.valueOf(offsetY))
                    .replace("<offset_x>", String.valueOf(offsetX))
                    .replace("<visible_condition>",
                            "(not ($atext - '" + PREFIX + ":" + chestGui.key() + "' = $atext))")));
        }
    }

    private final String TEMPLATE_PANEL_TOP_HALF = """
            {
                "modifications": [
                  {
                    "array_name": "controls",
                    "operation": "insert_front",
                    "value": []
                  }
                ]
              }
            """;

    private void handlePanelTopHalf(JsonObject root) {
        JsonObject panelTopHalf = JsonUtils.parseJson(TEMPLATE_PANEL_TOP_HALF);

        JsonArray modifications = panelTopHalf.getAsJsonArray("modifications");

        for (JsonElement modification : modifications) {
            if (!(modification instanceof JsonObject obj)) continue;

            JsonArray value = obj.getAsJsonArray("value");

            for (ResolvedChestGui chestGui : chestGuis) {
                JsonObject chestGuiValue = new JsonObject();
                chestGuiValue.add(PREFIX + "_" + chestGui.key()
                        + "@" + namespace + "." + PREFIX + "_" + chestGui.key(), new JsonObject());
                value.add(chestGuiValue);
            }
        }

        for (String prefix : new String[] {"large_chest", "small_chest"}) {
            root.add(prefix + "_panel_top_half", panelTopHalf);
        }
    }
}
