package fr.openmc.riftengine.core.converter.writers.tooltip;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.openmc.core.utils.JsonUtils;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.core.RiftLog;
import fr.openmc.riftengine.core.RiftRegistry;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.registry.glyphs.Glyph;
import fr.openmc.riftengine.core.resolvers.mcmeta.animation.AnimationMcMetaResolver;
import fr.openmc.riftengine.core.resolvers.mcmeta.gui.GuiMcMetaResolver;
import fr.openmc.riftengine.core.resolvers.mcmeta.gui.ResolvedGuiMcMeta;
import fr.openmc.riftengine.core.resolvers.tooltip.ResolvedTooltipResource;
import fr.openmc.riftengine.core.resolvers.tooltip.TooltipResourceResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UiCommonTooltipWriter implements PackWriter {
    private final List<ItemEntry> items;
    private final TooltipResourceResolver tooltipResolver;
    private final GuiMcMetaResolver guiMcMetaResolver;
    private final AnimationMcMetaResolver animationMcMetaResolver;
    private static final String OUTPUT_FILE = "ui/ui_common.json";

    public UiCommonTooltipWriter(List<ItemEntry> items,
                                 TooltipResourceResolver tooltipResolver,
                                 GuiMcMetaResolver guiMcMetaResolver,
                                 AnimationMcMetaResolver animationMcMetaResolver) {
        this.items = items;
        this.tooltipResolver = tooltipResolver;
        this.animationMcMetaResolver = animationMcMetaResolver;
        this.guiMcMetaResolver = guiMcMetaResolver;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        // todo: faire un TooltipScanner quand OpenMC sera divisé en module
        Map<String, ResolvedTooltipResource> tooltipResources = new HashMap<>();
        for (ItemEntry itemEntry : items) {
            ResolvedTooltipResource resource = tooltipResolver.resolve(javaRootPath, itemEntry);
            if (resource == null) continue;

            tooltipResources.put(resource.resourceId(), resource);
        }

        String namespace = "common";
        String element = "omc_hover_text";

        JsonObject root = new JsonObject();
        root.addProperty("namespace", namespace);

        // * génération des animations des frames des tooltips
        handleTooltipAnimation(javaRootPath, root, tooltipResources);

        tooltipResources.put("default", new ResolvedTooltipResource("default"));

        // * génération des tooltips coté JSON UI
        handleTooltipDetection(javaRootPath, tooltipResources, root, namespace, element);

        // * on mets la template pour "injecter" notre tooltip
        handleTooltipInjection(root, namespace, element);


        Path outputFile = bedrockRootPath.resolve(OUTPUT_FILE);
        this.writeJsonFile(outputFile, root);
    }

    private void handleTooltipAnimation(Path javaRootPath, JsonObject root, Map<String, ResolvedTooltipResource> tooltipResources) throws IOException {
        for (ResolvedTooltipResource resource : tooltipResources.values()) {
            String animationKey = resource.getKey() + "_frame" + "_flipbook";
            ResolvedTooltipResource.FlipbookAnimation flipbookAnimation = resource.getFrameFlipbookAnimation(
                    animationMcMetaResolver, guiMcMetaResolver, javaRootPath);

            // * écrit le jsonobject pour les animations
            ResolvedGuiMcMeta guiMeta = guiMcMetaResolver.resolve(javaRootPath, resource.frameMCMetaPath(javaRootPath));
            if (guiMeta != null && flipbookAnimation != null) {
                JsonObject animation = writeAnimation(flipbookAnimation, animationKey, guiMeta);
                root.add(animationKey, animation);
            }
        }
    }

    private final String TEMPLATE_TOOLTIP = """
            {
                "$hover_text_binding_name|default": "#hover_text",
                "type": "panel",
                "size": ["100%c", "100%c"],
                "layer": 5,
                "follows_cursor": true,
                "offset": [12, -12],
                "bindings": [
                    {
                        "binding_name": "$hover_text_binding_name",
                        "binding_name_override": "#hover_text",
                        "binding_type": "collection",
                        "binding_collection_name": "$item_collection_name"
                    },
                    {
                        "binding_type": "view",
                        "source_property_name": "(not (#hover_text = ''))",
                        "target_property_name": "#visible"
                    }
                ]
            }""";

    private final String TEMPLATE_BASE_TOOLTIP = """
            {
                "type": "panel",
                "size": ["100%c", "100%c"],
                "anchor_from": "top_left",
                "anchor_to": "top_left"
            }""";

    private final String TEMPLATE_BINDINGS = """
            [
                {
                    "binding_name": "$hover_text_binding_name",
                    "binding_name_override": "#hover_text",
                    "binding_type": "collection",
                    "binding_collection_name": "$item_collection_name"
                },
                {
                    "binding_type": "view",
                    "source_property_name": "<condition_view>",
                    "target_property_name": "#visible"
                }
            ]
            """;

    private final String TEMPLATE_DEFAULT_CONTROLS = """
            [
                {
                    "border": {
                        "type": "image",
                        "texture": "textures/ui/tooltip_default_background",
                        "color": [0.3, 0.0, 1.0],
                        "layer": 0,
                        "anchor_from": "top_left",
                        "anchor_to": "top_left",
                        "size": ["100%sm", "100%sm"],
                        "controls": [
                        {
                            "fill": {
                                  "type": "image",
                                  "texture": "textures/ui/tooltip_default_background",
                                  "color": [0.235, 0.0, 0.235],
                                  "alpha": 0.94,
                                  "layer": 1,
                                  "anchor_from": "center",
                                  "anchor_to": "center",
                                  "size": ["100% - 6px", "100% - 6px"]
                                }
                              }
                            ]
                          }
                        },
                        {
                          "content": {
                            "type": "panel",
                            "layer": 3,
                            "size": ["100%c + 12px", "100%c + 8px"],
                            "anchor_from": "top_left",
                            "anchor_to": "top_left",
                            "controls": [
                              {
                                "tooltip_text": {
                                  "type": "label",
                                  "text": "#text",
                                  "size": ["default", "default"],
                                  "anchor_from": "top_left",
                                  "anchor_to": "top_left",
                                  "offset": [6, 4],
                                  "bindings": [
                                    {
                                      "binding_name": "$hover_text_binding_name",
                                      "binding_name_override": "#hover_text",
                                      "binding_type": "collection",
                                      "binding_collection_name": "$item_collection_name"
                                    },
                                    {
                                      "binding_type": "view",
                                      "source_property_name": "<final_name>",
                                      "target_property_name": "#text"
                                    }
                                  ]
                                }
                            }
                        ]
                    }
                }
            ]
            """;

    private final String TEMPLATE_CONTROLS_BACKGROUND_TOOLTIP = """
            {
                "type": "image",
                "texture": "<textures>",
                "nineslice_size": [9, 23, 9, 18],
                "layer": 0,
                "anchor_from": "top_left",
                "anchor_to": "top_left",
                "size": ["100%sm + 10px", "100%sm + 14px"]
            }
            """;

    private final String TEMPLATE_CONTROLS_FRAME_TOOLTIP = """
            {
                "type": "image",
                "texture": "<textures>",
                "uv": [0, 0],
                "uv_size": [<width>, <height>],
                "nineslice_size": 20,
                "layer": 1,
                "anchor_from": "center",
                "anchor_to": "center",
                "size": ["100%", "100%"]
            }
            """;

    private final String TEMPLATE_CONTROLS_CONTENT = """
            {
                "type": "panel",
                "layer": 3,
                "size": ["100%c + 12px", "100%c + 8px"],
                "offset": [5, 7],
                "anchor_from": "top_left",
                "anchor_to": "top_left",
                "controls": [
                    {
                        "tooltip_text": {
                            "type": "label",
                            "text": "#text",
                            "size": ["default", "default"],
                            "anchor_from": "top_left",
                            "anchor_to": "top_left",
                            "offset": [6, 4],
                            "bindings": [
                                {
                                    "binding_name": "$hover_text_binding_name",
                                    "binding_name_override": "#hover_text",
                                    "binding_type": "collection",
                                    "binding_collection_name": "$item_collection_name"
                                },
                                {
                                    "binding_type": "view",
                                    "source_property_name": "(#hover_text - '<bedrock_char>')",
                                    "target_property_name": "#text"
                                }
                            ]
                        }
                    }
                ]
            }
            """;

    private void handleTooltipDetection(Path javaRootPath,
                                        Map<String, ResolvedTooltipResource> tooltipResources,
                                        JsonObject root,
                                        String namespace,
                                        String element) throws IOException {
        JsonObject elementTooltip = JsonUtils.parseJson(TEMPLATE_TOOLTIP);
        JsonArray controls = new JsonArray();

        for (ResolvedTooltipResource resource : tooltipResources.values()) {
            Optional<Glyph> glyph = RiftRegistry.GLYPHS.get(resource.resourceId());

            if (glyph.isEmpty() && !resource.resourceId().equals("default")) {
                RiftLog.GLYPH_NOT_FOUND.send(resource.resourceId());
                continue;
            }

            JsonObject tooltip = JsonUtils.parseJson(TEMPLATE_BASE_TOOLTIP);

            // * création de l'object binding
            if (resource.getKey().equals("default")) {
                String condition = "(#hover_text = #hover_text)";

                for (ResolvedTooltipResource resourceCondition : tooltipResources.values()) {
                    Optional<Glyph> glyphCondition = RiftRegistry.GLYPHS.get(resourceCondition.resourceId());

                    if (glyphCondition.isEmpty()) {
                        RiftLog.GLYPH_NOT_FOUND.send(resourceCondition.resourceId());
                        continue;
                    }

                    String character = glyphCondition.get().getBedrockCharString();
                    String marker = "'" + character + "'";
                    condition = "(" + condition
                            + " and ((#hover_text - " + marker
                            + ") = #hover_text))";
                }

                tooltip.add("bindings", JsonUtils.parseJsonArray(
                        TEMPLATE_BINDINGS.replace("<condition_view>", condition)));
            } else {
                tooltip.add("bindings", JsonUtils.parseJsonArray(TEMPLATE_BINDINGS.replace("<condition_view>",
                        "(not ((#hover_text - '" + glyph.get().getBedrockCharString() + "') = #hover_text))")));
            }

            // * création de l'object controls
            if (resource.getKey().equals("default")) {
                String expression = "#hover_text";

                for (ResolvedTooltipResource resourceCondition : tooltipResources.values()) {
                    Optional<Glyph> glyphCondition = RiftRegistry.GLYPHS.get(resourceCondition.resourceId());

                    if (glyphCondition.isEmpty()) {
                        RiftLog.GLYPH_NOT_FOUND.send(resourceCondition.resourceId());
                        continue;
                    }

                    String character = glyphCondition.get().getBedrockCharString();

                    expression = "(" + expression + " - '"
                            + character + "')";

                    tooltip.add("controls", JsonUtils.parseJsonArray(TEMPLATE_DEFAULT_CONTROLS.replace("<final_name>",
                            expression)));
                }
            } else {
                ResolvedTooltipResource.FlipbookAnimation animation = resource.getFrameFlipbookAnimation(animationMcMetaResolver, guiMcMetaResolver, javaRootPath);
                JsonArray controlsTooltip = new JsonArray();

                // * écriture du background
                JsonObject backgroundResource = new JsonObject();
                JsonObject inBackgroundResource = JsonUtils.parseJson(TEMPLATE_CONTROLS_BACKGROUND_TOOLTIP
                        .replace("<textures>", resource.backgroundImageBedrockPath(javaRootPath).toString()));

                String width = "0";
                String height = "0";
                if (animation != null) {
                    width = String.valueOf(animation.widthFrame());
                    height = String.valueOf(animation.heightFrame());
                } else {
                    RiftLog.FLIPBOOK_NULL.send(resource.resourceId());
                }

                JsonArray backgroundControls = new JsonArray();
                JsonObject frame = JsonUtils.parseJson(TEMPLATE_CONTROLS_FRAME_TOOLTIP
                            .replace("<textures>", resource.frameImageBedrockPath(javaRootPath).toString())
                            .replace("<width>", width)
                            .replace("<height>", height));

                if (animation != null) {
                    frame.add("anims", JsonUtils.parseJsonArray("[@"+ namespace +"." + resource.getKey() + "_frame_flipbook]"));
                }

                JsonObject frameObj = new JsonObject();
                frameObj.add(resource.getKey() + "_frame", frame);
                backgroundControls.add(frameObj);
                inBackgroundResource.add("controls", backgroundControls);
                backgroundResource.add("bg_" + resource.getKey(), inBackgroundResource);
                controlsTooltip.add(backgroundResource);

                // * écriture du content
                JsonObject contentResource = new JsonObject();
                JsonObject inContentResource = JsonUtils.parseJson(TEMPLATE_CONTROLS_CONTENT
                        .replace("<bedrock_char>", glyph.get().getBedrockCharString()));

                contentResource.add("content", inContentResource);

                controlsTooltip.add(contentResource);

                tooltip.add("controls", controlsTooltip);
            }

            JsonObject control = new JsonObject();
            control.add("varient_" + resource.getKey(), tooltip);
            controls.add(control);
        }

        elementTooltip.add("controls", controls);

        root.add(element, elementTooltip);
    }

    private final String TEMPLATE_HIGHLIGHT_SLOT_PANEL = """
            {
                "type": "panel",
                "controls": [
                  {
                    "highlight@common.highlight_slot": {
                      "controls": [
                        {
                          "hover_text@%s": {
                            "layer": 29
                          }
                        }
                      ],
                      "bindings": [
                        {
                          "binding_name": "#show_persistent_bundle_hover_text",
                          "binding_name_override": "#visible"
                        }
                      ]
                    }
                  },
                  {
                    "white_border@common.white_border_slot": {
                      "bindings": [
                        {
                          "binding_name": "#show_persistent_bundle_hover_text",
                          "binding_name_override": "#visible"
                        }
                      ]
                    }
                  }
                ]
              }
            """;

    private void handleTooltipInjection(JsonObject root, String namespace, String element) {
        JsonObject highlightSlotPanel = JsonUtils.parseJson(
                TEMPLATE_HIGHLIGHT_SLOT_PANEL.formatted(namespace + "." + element));
        root.add("highlight_slot_panel", highlightSlotPanel);
    }

    /**
     * "anim_type": "flip_book",
     * "frame_count": 15,
     * "frame_step": 100,
     * "fps": 6.67,
     * "reversible": false,
     * "easing": "linear",
     * "next": "@common.legendary_frame_flipbook"
     */
    private JsonObject writeAnimation(ResolvedTooltipResource.FlipbookAnimation flipbookAnim, String animationKey, ResolvedGuiMcMeta guiMeta) {
        JsonObject flipbook = new JsonObject();
        flipbook.addProperty("anim_type", "flip_book");
        flipbook.addProperty("frame_count", flipbookAnim.frameCount());
        flipbook.addProperty("frame_step", guiMeta.widthFrame());
        flipbook.addProperty("fps", flipbookAnim.fps());
        flipbook.addProperty("reversible", false);
        flipbook.addProperty("easing", "linear");
        flipbook.addProperty("next", "@common." + animationKey);
        return flipbook;
    }
}
