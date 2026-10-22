package fr.openmc.riftengine.core.converter.writers.items.specials.armor;

import fr.openmc.riftengine.api.scanner.equipements.EquipementEntry;
import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.api.scanner.items.entry.behaviours.armors.ArmorsBehaviour;
import fr.openmc.riftengine.core.RiftLog;
import fr.openmc.riftengine.core.converter.writers.PackWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class CustomArmorTextureWriter implements PackWriter {
    private final List<ItemEntry> items;

    public CustomArmorTextureWriter(List<ItemEntry> items) {
        this.items = items;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        for (ItemEntry item : items) {
            if (!item.getBehaviour().isArmor()) continue;
            if (!(item.getBehaviour() instanceof ArmorsBehaviour(EquipementEntry entry, _))) continue;
            if (entry == null) {
                RiftLog.ENTRY_EQUIPMENT_NULL.send(item.namespacedId());
                continue;
            }

            this.copyFile(entry.getLayer1Path(javaRootPath), entry.getLayer1BedrockPath(bedrockRootPath));
            this.copyFile(entry.getLayer2Path(javaRootPath), entry.getLayer2BedrockPath(bedrockRootPath));
        }
    }
}
