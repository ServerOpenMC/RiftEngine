package fr.openmc.riftengine.core.converter.writers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public interface PackWriter {
    Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping() // fix des = qui était en caractere unicode
            .create();

    void write(Path bedrockRootPath, Path javaRootPath) throws Exception;

    default void writeJsonFile(Path outputFile, JsonObject root) throws IOException {
        this.writeFile(outputFile, GSON.toJson(root));
    }

    default void writeFile(Path outputFile, String content) throws IOException {
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, content, StandardCharsets.UTF_8);
    }

    default void copyFile(Path toCopy, Path where) throws IOException {
        Files.createDirectories(where.getParent());
        Files.copy(toCopy, where, StandardCopyOption.REPLACE_EXISTING);
    }
}
