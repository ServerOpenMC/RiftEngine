package fr.openmc.riftengine.core.converter;

import fr.openmc.core.bootstrap.integration.OMCLogger;
import fr.openmc.core.utils.text.DateUtils;
import fr.openmc.riftengine.core.converter.writers.PackWriter;

import java.nio.file.Path;
import java.time.LocalDateTime;

public class RegisteredPackWriter implements PackWriter {
    private final String name;
    private final PackWriter wrapped;

    public RegisteredPackWriter(String name, PackWriter wrapped) {
        this.name = name;
        this.wrapped = wrapped;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws Exception {
        LocalDateTime start = DateUtils.getLocalDateTime();
        OMCLogger.infoFormatted("Conversion : " + name);

        try {
            wrapped.write(bedrockRootPath, javaRootPath);

            long elapsed = -DateUtils.getDelayBetweenNowMillis(start);
            OMCLogger.successFormatted(name + " terminé en " + elapsed + " ms");
        } catch (Exception e) {
            OMCLogger.warnFormatted("Échec de " + name + " : " + e.getMessage(), e);
            throw e;
        }
    }
}
