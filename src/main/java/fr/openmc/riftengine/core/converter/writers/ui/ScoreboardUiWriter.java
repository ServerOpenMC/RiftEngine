package fr.openmc.riftengine.core.converter.writers.ui;

import com.google.gson.JsonObject;
import fr.openmc.riftengine.core.converter.writers.PackWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScoreboardUiWriter implements PackWriter {
    private final boolean hideScoreboardNumberBedrock;

    public ScoreboardUiWriter(boolean hideScoreboardNumberBedrock) {
        this.hideScoreboardNumberBedrock = hideScoreboardNumberBedrock;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        if (!hideScoreboardNumberBedrock) return;

        JsonObject root = new JsonObject();

        JsonObject visible = new JsonObject();
        visible.addProperty("visible", false);
        root.add("scoreboard_sidebar_score", visible);

        Path outputFile = bedrockRootPath.resolve("ui").resolve("scoreboards.json");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, GSON.toJson(root), StandardCharsets.UTF_8);
    }
}
