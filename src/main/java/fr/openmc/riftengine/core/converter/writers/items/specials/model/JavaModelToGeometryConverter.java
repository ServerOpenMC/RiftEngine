package fr.openmc.riftengine.core.converter.writers.items.specials.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Convertit un modèle Java Edition (JSON Blockbench) en géométrie Bedrock (.geo.json).
 * Classe pure : ne touche à aucun fichier, renvoie un JsonObject.
 *
 * Limites : un seul bone (groupes aplatis), une seule texture (UV mis à l'échelle via "texture_size").
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
     * @return le contenu du .geo.json, ou null si aucun élément exploitable
     */
    public static JsonObject convert(JsonObject javaModel, String geometryId, boolean itemBinding) {
        if (!javaModel.has("elements") || !javaModel.get("elements").isJsonArray()) return null;

        int texW = 16, texH = 16;
        if (javaModel.has("texture_size") && javaModel.get("texture_size").isJsonArray()) {
            JsonArray ts = javaModel.getAsJsonArray("texture_size");
            if (ts.size() >= 2) {
                texW = ts.get(0).getAsInt();
                texH = ts.get(1).getAsInt();
            }
        }
        double scaleU = texW / 16.0;
        double scaleV = texH / 16.0;

        JsonArray cubes = new JsonArray();
        for (JsonElement el : javaModel.getAsJsonArray("elements")) {
            if (!el.isJsonObject()) continue;
            JsonObject element = el.getAsJsonObject();
            if (!element.has("from") || !element.has("to")) continue;
            cubes.add(convertElement(element, scaleU, scaleV));
        }
        if (cubes.isEmpty()) return null;

        JsonObject bone = new JsonObject();
        bone.addProperty("name", "bone");
        if (itemBinding)
            bone.addProperty("binding", "q.item_slot_to_bone_name(context.item_slot)");
        double[] pivot = bonePivot(javaModel);
        if (pivot[0] != 0 || pivot[1] != 0 || pivot[2] != 0)
            bone.add("pivot", toArray(pivot[0], pivot[1], pivot[2]));
        bone.add("cubes", cubes);

        JsonArray bones = new JsonArray();
        bones.add(bone);

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

    private static double[] bonePivot(JsonObject javaModel) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        boolean found = false;
        for (JsonElement el : javaModel.getAsJsonArray("elements")) {
            if (!el.isJsonObject()) continue;
            JsonObject e = el.getAsJsonObject();
            if (!e.has("from") || !e.has("to")) continue;
            JsonArray f = e.getAsJsonArray("from"), t = e.getAsJsonArray("to");
            double fx = f.get(0).getAsDouble() - CENTRE_X;
            double fy = f.get(1).getAsDouble() - CENTRE_Y;
            double fz = f.get(2).getAsDouble() - CENTRE_Z;

            double tx = t.get(0).getAsDouble() - CENTRE_X;
            double ty = t.get(1).getAsDouble() - CENTRE_Y;
            double tz = t.get(2).getAsDouble() - CENTRE_Z;

            minX = Math.min(minX, Math.min(fx, tx));
            minY = Math.min(minY, Math.min(fy, ty));
            minZ = Math.min(minZ, Math.min(fz, tz));
            maxX = Math.max(maxX, Math.max(fx, tx));
            maxY = Math.max(maxY, Math.max(fy, ty));
            maxZ = Math.max(maxZ, Math.max(fz, tz));
            found = true;
        }
        if (!found) return new double[]{0, 0, 0};
        return new double[]{-((minX + maxX) / 2.0), (minY + maxY) / 2.0, (minZ + maxZ) / 2.0};
    }

    private static JsonObject convertElement(JsonObject element, double scaleU, double scaleV) {
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
                        rotation.add(angle);
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
                bedrockUV.add(faceName, convertFaceUV(uv, flip, scaleU, scaleV, faceRotation));
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

    private static JsonObject convertFaceUV(double[] uv, boolean flipUpDown, double scaleU, double scaleV, int rotation) {
        double u1 = uv[0] * scaleU, v1 = uv[1] * scaleV;
        double u2 = uv[2] * scaleU, v2 = uv[3] * scaleV;

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

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}