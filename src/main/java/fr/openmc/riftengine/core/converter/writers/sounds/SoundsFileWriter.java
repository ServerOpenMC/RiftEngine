package fr.openmc.riftengine.core.converter.writers.sounds;

import fr.openmc.core.OMCRegistry;
import fr.openmc.riftengine.api.scanner.sounds.SoundEntry;
import fr.openmc.riftengine.core.converter.writers.PackWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class SoundsFileWriter implements PackWriter {
    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        List<SoundEntry> sounds = OMCRegistry.SCANNERS.SOUNDS.scan(javaRootPath);

        for (SoundEntry soundEntry : sounds) {
            for (String soundResourceId : soundEntry.soundResourcesId()) {
                Path soundsPath = javaRootPath.resolve(soundEntry.toJavaPath("minecraft", soundResourceId) + ".ogg");
                Path dest = bedrockRootPath.resolve(soundEntry.toBedrockPath("minecraft", soundResourceId) + ".ogg");

                this.copyFile(soundsPath, dest);
            }
        }
    }
}
