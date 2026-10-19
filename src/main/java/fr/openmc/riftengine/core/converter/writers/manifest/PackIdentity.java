package fr.openmc.riftengine.core.converter.writers.manifest;

import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static fr.openmc.riftengine.core.RiftPlugin.RP_VERSION;

@Getter
public class PackIdentity {

    private final String packName;
    private final UUID headerUuid;
    private final UUID moduleUuid;
    private final int[] version; // * [major, minor, patch]

    private PackIdentity(String packName, UUID headerUuid, UUID moduleUuid, int[] version) {
        this.packName = packName;
        this.headerUuid = headerUuid;
        this.moduleUuid = moduleUuid;
        this.version = version;
    }

    public static PackIdentity loadOrCreate(JavaPlugin plugin) throws IOException {
        File file = new File(plugin.getDataFolder(), "pack-identity.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (!config.contains("header-uuid")) {
            config.set("header-uuid", UUID.randomUUID().toString());
        }
        if (!config.contains("module-uuid")) {
            config.set("module-uuid", UUID.randomUUID().toString());
        }

        int major = config.getInt("version.major", RP_VERSION[0]);
        int minor = config.getInt("version.minor", RP_VERSION[1]);
        int patch = config.getInt("version.patch", 0);

        patch++;
        config.set("version.major", major);
        config.set("version.minor", minor);
        config.set("version.patch", patch);

        config.save(file);

        return new PackIdentity(
                "RiftPack",
                UUID.fromString(config.getString("header-uuid")),
                UUID.fromString(config.getString("module-uuid")),
                new int[]{major, minor, patch}
        );
    }
}
