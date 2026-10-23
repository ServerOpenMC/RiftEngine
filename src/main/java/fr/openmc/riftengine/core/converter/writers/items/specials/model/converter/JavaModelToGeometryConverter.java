package fr.openmc.riftengine.core.converter.writers.items.specials.model.converter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Convertit un modèle Java Edition (JSON Blockbench) en géométrie Bedrock (.geo.json).
 *
 * Classe généré par IA, je précise. Assez complexe de définir les formules et de les appliquer sans
 * s'aider d'une IA
 */
public final class JavaModelToGeometryConverter {

    private static final double CENTRE_X = 8.0;
    private static final double CENTRE_Y = 0.0;
    private static final double CENTRE_Z = 8.0;

    /**
     * @param javaModel le JSON du modèle Java
     * @param geometryId ex. "geometry.omc_dream.orb.orb_cloud"
     * @param itemBinding true pour ajouter le binding d'item tenu en main (attachable)
     * @param atlas l'atlas de textures (donc le regroupement des textures)
     * @return le contenu du .geo.json, ou null si aucun élément exploitable
     */
    public static JsonObject convert(JsonObject javaModel, String geometryId, boolean itemBinding,
                                     ModelTextureAtlas atlas) {
        if (!javaModel.has("elements") || !javaModel.get("elements").isJsonArray()) return null;

        int texW = 16;
        int texH = 16;
        if (javaModel.has("texture_size") && javaModel.get("texture_size").isJsonArray()) {
            JsonArray ts = javaModel.getAsJsonArray("texture_size");
            if (ts.size() >= 2) {
                texW = ts.get(0).getAsInt();
                texH = ts.get(1).getAsInt();
            }
        }
        if (atlas != null) {
            texW = atlas.width();
            texH = atlas.height();
        }
        double scaleU = texW / 16.0;
        double scaleV = texH / 16.0;

        JsonArray cubes = new JsonArray();
        for (JsonElement el : javaModel.getAsJsonArray("elements")) {
            if (!el.isJsonObject()) continue;
            JsonObject element = el.getAsJsonObject();
            if (!element.has("from") || !element.has("to")) continue;
            cubes.add(convertElement(element, scaleU, scaleV, atlas));
        }
        if (cubes.isEmpty()) return null;

        JsonArray bones = new JsonArray();
        bones.add(bone(ROOT_BONE, null, itemBinding, null));
        bones.add(bone(ROOT_BONE + "_x", ROOT_BONE, false, null));
        bones.add(bone(ROOT_BONE + "_y", ROOT_BONE + "_x", false, null));
        bones.add(bone(ROOT_BONE + "_z", ROOT_BONE + "_y", false, cubes));

        JsonObject description = new JsonObject();
        description.addProperty("identifier", geometryId);
        description.addProperty("visible_bounds_width", 4.0);
        description.addProperty("visible_bounds_height", 4.0);
        description.add("visible_bounds_offset", toArray(0, 0.75, 0));
        description.addProperty("texture_width", texW);
        description.addProperty("texture_height", texH);

        JsonObject geoEntry = new JsonObject();
        geoEntry.add("description", description);
        geoEntry.add("bones", bones);

        JsonArray geoArray = new JsonArray();
        geoArray.add(geoEntry);

        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.21.0");
        root.add("minecraft:geometry", geoArray);
        return root;
    }

    private static JsonObject convertElement(JsonObject element, double scaleU, double scaleV, ModelTextureAtlas atlas) {
        JsonArray from = element.getAsJsonArray("from");
        JsonArray to = element.getAsJsonArray("to");

        double rawFromX = from.get(0).getAsDouble();
        double rawFromY = from.get(1).getAsDouble();
        double rawFromZ = from.get(2).getAsDouble();
        double rawToX = to.get(0).getAsDouble();
        double rawToY = to.get(1).getAsDouble();
        double rawToZ = to.get(2).getAsDouble();

        double fromX = rawFromX - CENTRE_X;
        double fromY = rawFromY - CENTRE_Y;
        double fromZ = rawFromZ - CENTRE_Z;
        double toX = rawToX - CENTRE_X;
        double toY = rawToY - CENTRE_Y;
        double toZ = rawToZ - CENTRE_Z;

        double originX = Math.min(fromX, toX);
        double originY = Math.min(fromY, toY);
        double originZ = Math.min(fromZ, toZ);
        double sizeX = Math.max(fromX, toX) - originX;
        double sizeY = Math.max(fromY, toY) - originY;
        double sizeZ = Math.max(fromZ, toZ) - originZ;

        originX = -(originX + sizeX);
        int invertedAxes = (rawFromX > rawToX ? 1 : 0) + (rawFromY > rawToY ? 1 : 0) + (rawFromZ > rawToZ ? 1 : 0);
        if (invertedAxes % 2 == 1) {
            originX += sizeX;  originY += sizeY;  originZ += sizeZ;
            sizeX = -sizeX;    sizeY = -sizeY;    sizeZ = -sizeZ;
        }

        JsonObject cube = new JsonObject();
        cube.add("origin", toArray(originX, originY, originZ));
        cube.add("size", toArray(sizeX, sizeY, sizeZ));

        if (element.has("rotation") && element.get("rotation").isJsonObject()) {
            JsonObject javaRot = element.getAsJsonObject("rotation");
            double angle = javaRot.has("angle") ? javaRot.get("angle").getAsDouble() : 0;
            String axis = javaRot.has("axis") ? javaRot.get("axis").getAsString() : "y";

            if (angle != 0) {
                JsonArray rotation = new JsonArray();
                switch (axis) {
                    case "x" -> {
                        rotation.add(-angle);
                        rotation.add(0);
                        rotation.add(0);
                    }
                    case "y" -> {
                        rotation.add(0);
                        rotation.add(-angle);
                        rotation.add(0);
                    }
                    case "z" -> {
                        rotation.add(0);
                        rotation.add(0);
                        rotation.add(angle);
                    }
                    default -> {
                        rotation.add(0);
                        rotation.add(0);
                        rotation.add(0);
                    }
                }
                cube.add("rotation", rotation);

                if (javaRot.has("origin") && javaRot.get("origin").isJsonArray()) {
                    JsonArray o = javaRot.getAsJsonArray("origin");
                    double px = -(o.get(0).getAsDouble() - CENTRE_X);
                    double py = o.get(1).getAsDouble() - CENTRE_Y;
                    double pz = o.get(2).getAsDouble() - CENTRE_Z;
                    if (px != 0 || py != 0 || pz != 0) cube.add("pivot", toArray(px, py, pz));
                }
            }
        }

        if (element.has("faces") && element.get("faces").isJsonObject()) {
            JsonObject javaFaces = element.getAsJsonObject("faces");
            JsonObject bedrockUV = new JsonObject();

            for (String faceName : new String[]{"north", "east", "south", "west", "up", "down"}) {
                if (!javaFaces.has(faceName) || !javaFaces.get(faceName).isJsonObject()) continue;
                JsonObject javaFace = javaFaces.getAsJsonObject(faceName);

                double[] uv;
                if (javaFace.has("uv") && javaFace.get("uv").isJsonArray() && javaFace.getAsJsonArray("uv").size() >= 4) {
                    JsonArray a = javaFace.getAsJsonArray("uv");
                    uv = new double[]{
                            a.get(0).getAsDouble(), a.get(1).getAsDouble(),
                            a.get(2).getAsDouble(), a.get(3).getAsDouble()
                    };
                } else {
                    uv = defaultFaceUV(faceName, rawFromX, rawFromY, rawFromZ, rawToX, rawToY, rawToZ);
                }

                int faceRotation = 0;
                if (javaFace.has("rotation") && javaFace.get("rotation").isJsonPrimitive()) {
                    int normalized = ((javaFace.get("rotation").getAsInt() % 360) + 360) % 360;
                    if (normalized % 90 == 0) faceRotation = normalized;
                }

                boolean flip = faceName.equals("up") || faceName.equals("down");
                double sU = scaleU, sV = scaleV, offU = 0, offV = 0;
                if (atlas != null) {
                    String ref = javaFace.has("texture") ? javaFace.get("texture").getAsString() : "#0";
                    ModelTextureAtlas.Sprite s = atlas.sprite(ref);
                    if (s != null) {
                        sU = s.width() / 16.0;  sV = s.height() / 16.0;
                        offU = s.x();           offV = s.y();
                    }
                }
                bedrockUV.add(faceName, convertFaceUV(uv, flip, sU, sV, offU, offV, faceRotation));
            }
            if (bedrockUV.size() > 0) cube.add("uv", bedrockUV);
        }
        return cube;
    }

    private static double[] defaultFaceUV(String face, double fx, double fy, double fz, double tx, double ty, double tz) {
        return switch (face) {
            case "north" -> new double[]{16 - tx, 16 - ty, 16 - fx, 16 - fy};
            case "south" -> new double[]{fx, 16 - ty, tx, 16 - fy};
            case "west" -> new double[]{fz, 16 - ty, tz, 16 - fy};
            case "east" -> new double[]{16 - tz, 16 - ty, 16 - fz, 16 - fy};
            case "up" -> new double[]{fx, fz, tx, tz};
            case "down" -> new double[]{fx, 16 - tz, tx, 16 - fz};
            default -> new double[]{0, 0, 16, 16};
        };
    }

    private static JsonObject convertFaceUV(double[] uv, boolean flipUpDown, double scaleU, double scaleV,
                                            double offU, double offV, int rotation) {
        double u1 = uv[0] * scaleU + offU;
        double v1 = uv[1] * scaleV + offV;
        double u2 = uv[2] * scaleU + offU;
        double v2 = uv[3] * scaleV + offV;

        JsonArray uvArr = new JsonArray();
        JsonArray sizeArr = new JsonArray();
        if (flipUpDown) {
            uvArr.add(round4(u2));
            uvArr.add(round4(v2));
            sizeArr.add(round4(u1 - u2));
            sizeArr.add(round4(v1 - v2));
        } else {
            uvArr.add(round4(u1));
            uvArr.add(round4(v1));
            sizeArr.add(round4(u2 - u1));
            sizeArr.add(round4(v2 - v1));
        }

        JsonObject face = new JsonObject();
        face.add("uv", uvArr);
        face.add("uv_size", sizeArr);
        if (rotation != 0) face.addProperty("uv_rotation", rotation);
        return face;
    }

    private static JsonArray toArray(double x, double y, double z) {
        JsonArray arr = new JsonArray();
        arr.add(round4(x));
        arr.add(round4(y));
        arr.add(round4(z));
        return arr;
    }

    public static final String ROOT_BONE = "bone";

    private static JsonObject bone(String name, String parent, boolean binding, JsonArray cubes) {
        JsonObject b = new JsonObject();
        b.addProperty("name", name);
        if (parent != null) b.addProperty("parent", parent);
        if (binding) b.addProperty("binding", "c.item_slot == 'head' ? 'head' : q.item_slot_to_bone_name(c.item_slot)");
        b.add("pivot", toArray(0, 8, 0));
        if (cubes != null) b.add("cubes", cubes);
        return b;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}