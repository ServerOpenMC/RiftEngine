package fr.openmc.riftengine.core.converter.writers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Path;

public interface PackWriter {
    Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping() // fix des = qui était en caractere unicode
            .create();

    void write(Path bedrockRootPath, Path javaRootPath) throws Exception;
}
