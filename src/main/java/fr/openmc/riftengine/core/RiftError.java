package fr.openmc.riftengine.core;

import fr.openmc.core.bootstrap.integration.OMCLogger;

public enum RiftError {
    ITEM_NOT_RESOURCE("Item {} : n'a pas de best resources", ErrorType.WARN),
    RESOURCE_NOT_FOUND("Item {} : a aucune resource", ErrorType.WARN)

    ;

    private enum ErrorType {
        WARN,
        ERROR,
        DEBUG,
        INFO
    }

    private final String message;
    private final ErrorType type;

    RiftError(String message, ErrorType type) {
        this.message = message;
        this.type = type;
    }

    public void send(Object... args) {
        if (type == ErrorType.WARN) {
            OMCLogger.warn(message, args);
        } else if (type == ErrorType.ERROR) {
            OMCLogger.error(message, args);
        } else if (type == ErrorType.DEBUG) {
            OMCLogger.debug(message, args);
        } else if (type == ErrorType.INFO) {
            OMCLogger.info(message, args);
        }
    }
}
