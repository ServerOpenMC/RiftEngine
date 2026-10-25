package fr.openmc.riftengine.core;

import fr.openmc.core.bootstrap.integration.OMCLogger;

public enum RiftLog {
    RIFT_DISABLE("RiftEngine désactivé", LogType.INFO),
    RIFT_PACK_INFO("Pack chargé avec succès (pack={})", LogType.INFO),
    REGISTRY_LOADED("Registre {} chargé pendant le runtime", LogType.SUCCESS),
    REGISTRY_DISABLE("Registre {} stoppé", LogType.SUCCESS),

    IMAGE_NOT_FOUND("Image introuvable pour {} dans le path {} (skip)", LogType.ERROR),
    ERROR_READING("Erreur de lecture pour {} (skip) : ", LogType.ERROR),
    ICON_TYPE_NOT_RECOGNIZED("Icon type {} : n'est pas reconnu", LogType.ERROR),
    ICON_SKIPPED("Image manquante pour {} emoji(s)/icon(s) sur {}.", LogType.INFO),
    DETECTED_AS("{} ({}x{}) détecté comme {}", LogType.INFO),
    SYMBOL_KEY_NOT_FOUND("Clé {} introuvable dans {}.", LogType.WARN),
    NUMBER_GLYPH_REGISTERED("{}/{} glyphs enregistré", LogType.INFO),

    ITEM_NOT_RESOURCE("Item {} : n'a pas de best resources", LogType.WARN),
    RESOURCE_NOT_FOUND("Item {} : a aucune resource", LogType.WARN),
    NUMBER_TOOLTIP_REGISTERED("{}/{} tooltip enregistré", LogType.INFO),
    GLYPH_NOT_FOUND("Glyph {} introuvable", LogType.WARN),
    FLIPBOOK_NULL("Flipbook est null pour ce tooltip {}", LogType.WARN),
    NULL("{} est null", LogType.WARN),

    PATH_CANT_REDUCED_TO_TEXTURES("resource {} ne peut pas etre réduite à un chemin de texture", LogType.WARN),
    ;

    public static void logBanner(String pluginVersion, String server, String javaVersion) {
        OMCLogger.info("\u001B[1;35m██████╗ ██╗███████╗████████╗    \u001B[0;95mRIFT v{}\u001B[0m", pluginVersion);
        OMCLogger.info("\u001B[1;35m██╔══██╗██║██╔════╝╚══██╔══╝    \u001B[0;90mPaper {}\u001B[0m", server);
        OMCLogger.info("\u001B[1;95m██████╔╝██║█████╗     ██║       \u001B[0;90mJava  {}\u001B[0m", javaVersion);
        OMCLogger.info("\u001B[1;95m██╔══██╗██║██╔══╝     ██║   \u001B[0m");
        OMCLogger.info("\u001B[1;35m██║  ██║██║██║        ██║   \u001B[0m");
        OMCLogger.info("\u001B[1;95m╚═╝  ╚═╝╚═╝╚═╝        ╚═╝   \u001B[0m");
        OMCLogger.info("");
    }

    private enum LogType {
        WARN,
        ERROR,
        DEBUG,
        INFO,
        SUCCESS
    }

    private final String message;
    private final LogType type;

    RiftLog(String message, LogType type) {
        this.message = message;
        this.type = type;
    }

    public void send(Object... args) {
        if (type == LogType.WARN) {
            OMCLogger.warnFormatted(message, args);
        } else if (type == LogType.ERROR) {
            OMCLogger.errorFormatted(message, args);
        } else if (type == LogType.DEBUG) {
            OMCLogger.debug(message, args);
        } else if (type == LogType.INFO) {
            OMCLogger.infoFormatted(message, args);
        } else if (type == LogType.SUCCESS) {
            OMCLogger.successFormatted(message, args);
        }
    }
}
