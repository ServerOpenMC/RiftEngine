package fr.openmc.riftengine.core.converter.writers.items.specials.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.openmc.core.utils.JsonUtils;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.api.scanner.items.entry.ItemModel;
import fr.openmc.riftengine.api.utils.IdentifierUtils;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.converter.writers.items.specials.model.converter.JavaModelToGeometryConverter;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * les bedrock_root/animations/ servent a positioner les items correctement.
 * Code fait main et les méthodes handAnimation et dérivés ont été faite via IA.
 */
public class ItemModelAnimationWriter implements PackWriter {
    private static final String ROOT = JavaModelToGeometryConverter.ROOT_BONE;
    private static final double HEAD_SCALE = 0.625;

    // * Échelle du model en 1ère personne d'apres des repos de Geyser et autres.
    private static final double FIRST_PERSON_ROOT_SCALE = 1.5;

    private final List<ItemEntry> items;

    public ItemModelAnimationWriter(List<ItemEntry> items) {
        this.items = items;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws Exception {
        Set<String> modelCreated = new HashSet<>();

        for (ItemEntry item : items) {
            if (!item.getModel().hasModel()) continue;

            ItemModel model = item.getModel();
            String modelName = model.modelId();

            if (!modelCreated.add(modelName)) continue;

            JsonObject rootModelJson = JsonUtils.loadJson(IdentifierUtils.resolveModelId(javaRootPath, modelName));
            if (rootModelJson == null) continue;

            JsonObject display = JsonUtils.getJsonObject(rootModelJson, "display", new JsonObject());

            String id = model.toBedrockAnimationId();
            JsonObject animations = new JsonObject();
            animations.add(id + ".thirdperson_main_hand", handAnimation(display, "thirdperson_righthand", false, true));
            animations.add(id + ".thirdperson_off_hand", handAnimation(display, "thirdperson_lefthand", false, true));
            animations.add(id + ".head", headAnimation(display));
            animations.add(id + ".firstperson_main_hand", handAnimation(display, "firstperson_righthand", true, true));
            animations.add(id + ".firstperson_off_hand", handAnimation(display, "firstperson_lefthand", true, false));
            animations.add(id + ".disable", disableAnimation());

            JsonObject root = new JsonObject();
            root.addProperty("format_version", "1.8.0");
            root.add("animations", animations);

            this.writeJsonFile(bedrockRootPath.resolve(model.toBedrockAnimationPath()), root);
        }
    }

    private JsonObject handAnimation(JsonObject display, String slot, boolean firstPerson, boolean negateX) {
        double[] rot = vec(display, slot, "rotation");
        double[] pos = vec(display, slot, "translation");
        double[] scl = vec(display, slot, "scale");

        JsonObject bones = new JsonObject();

        JsonObject root = new JsonObject();
        if (firstPerson) {
            root.add("rotation", array(90, 60, -40));
            root.add("position", array(4, 10, 4));
            root.addProperty("scale", FIRST_PERSON_ROOT_SCALE);
        } else {
            root.add("rotation", array(90, 0, 0));
            root.add("position", array(0, 13, -3));
        }
        bones.add(ROOT, root);

        JsonObject x = new JsonObject();
        if (rot != null) {
            x.add("rotation", array(-rot[0], 0, 0));
        } else if (firstPerson) {
            x.add("rotation", array(0.1, 0.1, 0.1));
        }
        if (pos != null) {
            double k = firstPerson ? 1.0 / FIRST_PERSON_ROOT_SCALE : 1.0;
            x.add("position", array(
                    (negateX ? -pos[0] : pos[0]) * k,
                    pos[1] * k,
                    (firstPerson ? -pos[2] : pos[2]) * k));
        }
        if (scl != null) x.add("scale", array(scl[0], scl[1], scl[2]));
        if (x.size() > 0) bones.add(ROOT + "_x", x);

        addYZ(bones, rot);
        return animation(bones);
    }

    private JsonObject headAnimation(JsonObject display) {
        double[] rot = vec(display, "head", "rotation");
        double[] pos = vec(display, "head", "translation");
        double[] scl = vec(display, "head", "scale");

        JsonObject bones = new JsonObject();

        JsonObject root = new JsonObject();
        root.add("position", array(0, 19.5, 0));
        bones.add(ROOT, root);

        JsonObject x = new JsonObject();
        if (rot != null) x.add("rotation", array(-rot[0], 0, 0));
        if (pos != null) {
            x.add("position", array(-pos[0] * HEAD_SCALE, pos[1] * HEAD_SCALE, pos[2] * HEAD_SCALE));
        }
        if (scl != null) {
            x.add("scale", array(scl[0] * HEAD_SCALE, scl[1] * HEAD_SCALE, scl[2] * HEAD_SCALE));
        } else {
            x.addProperty("scale", HEAD_SCALE);
        }
        bones.add(ROOT + "_x", x);

        addYZ(bones, rot);
        return animation(bones);
    }

    private JsonObject disableAnimation() {
        JsonObject root = new JsonObject();
        root.addProperty("scale", 0);

        JsonObject bones = new JsonObject();
        bones.add(ROOT, root);

        JsonObject animation = animation(bones);
        animation.addProperty("override_previous_animation", true);
        return animation;
    }

    private void addYZ(JsonObject bones, double[] rot) {
        if (rot == null) return;

        JsonObject y = new JsonObject();
        y.add("rotation", array(0, -rot[1], 0));
        bones.add(ROOT + "_y", y);

        JsonObject z = new JsonObject();
        z.add("rotation", array(0, 0, rot[2]));
        bones.add(ROOT + "_z", z);
    }

    private JsonObject animation(JsonObject bones) {
        JsonObject animation = new JsonObject();
        animation.addProperty("loop", true);
        animation.add("bones", bones);
        return animation;
    }

    private double[] vec(JsonObject display, String slot, String field) {
        JsonObject transform = JsonUtils.getJsonObject(display, slot, null);
        if (transform == null) return null;
        JsonArray a = JsonUtils.getJsonArray(transform, field, null);
        if (a == null || a.size() < 3) return null;
        return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    private JsonArray array(double x, double y, double z) {
        JsonArray a = new JsonArray();
        a.add(round4(x));
        a.add(round4(y));
        a.add(round4(z));
        return a;
    }

    private double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
