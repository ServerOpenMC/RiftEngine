package fr.openmc.riftengine.core.converter.writers.manifest;

import fr.openmc.riftengine.core.converter.writers.PackWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ManifestWriter implements PackWriter {
    public static final String PACK_NAME = "RiftPack";

    private final PackIdentity identity;

    public ManifestWriter(PackIdentity identity) {
        this.identity = identity;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        int[] v = identity.version();
        String versionJson = "[" + v[0] + ", " + v[1] + ", " + v[2] + "]";
        String manifest = """
            {
              "format_version": 2,
              "header": {
                "name": "%s",
                "description": "Généré par RiftEngine à partir de celui de Java",
                "uuid": "%s",
                "version": %s,
                "min_engine_version": [1, 21, 0]
              },
              "modules": [
                {
                  "type": "resources",
                  "uuid": "%s",
                  "version": %s
                }
              ]
            }
            """.formatted(PACK_NAME, identity.headerUuid(), versionJson, identity.moduleUuid(), versionJson);

        Files.writeString(bedrockRootPath.resolve("manifest.json"), manifest, StandardCharsets.UTF_8);

    }
}
