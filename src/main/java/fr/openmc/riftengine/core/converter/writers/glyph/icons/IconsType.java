package fr.openmc.riftengine.core.converter.writers.glyph.icons;

import fr.openmc.riftengine.core.scanner.icons.IconEntry;

import java.awt.*;

public enum IconsType {
    PREFIX {
        @Override
        public boolean matches(int width, int height) {
            double aspectRatio = (double) Math.max(width, height) / Math.min(width, height);
            return aspectRatio > 2.0;
        }

        @Override
        public Dimension finalSize(IconEntry entry, int width, int height) {
            return new Dimension(width, height); // pas de resize
        }

        @Override
        public int yOffset(IconEntry entry, int height) {
            return 0;
        }
    },

    MENU_GUI {
        @Override
        public boolean matches(int width, int height) {
            return Math.max(width, height) > 128;
        }

        @Override
        public Dimension finalSize(IconEntry entry, int width, int height) {
            return new Dimension(width, height);
        }

        @Override
        public int yOffset(IconEntry entry, int height) {
            return 0;
        }
    },

    ICON {
        @Override
        public boolean matches(int width, int height) {
            return false; // à detecter manuellement via config/icons_type.yml
        }

        @Override
        public Dimension finalSize(IconEntry entry, int width, int height) {
            double scale = entry.scaleRatio() / (double) height;
            int w = Math.max(1, (int) Math.round(width * scale));
            int h = Math.max(1, (int) Math.round(height * scale));
            return new Dimension(w, h);
        }

        @Override
        public int yOffset(IconEntry entry, int height) {
            return entry.yPosition() != null ? -entry.yPosition() : DEFAULT_Y_OFFSET;
        }
    },

    EMOJI {
        @Override
        public boolean matches(int width, int height) {
            int maxDim = Math.max(width, height);
            return maxDim <= 128 && maxDim > 0;
        }

        @Override
        public Dimension finalSize(IconEntry entry, int width, int height) {
            double scale = yOffset(entry, height) / (double) height;
            int w = Math.max(1, (int) Math.round(width * scale));
            int h = Math.max(1, (int) Math.round(height * scale));
            return new Dimension(w, h);
        }

        @Override
        public int yOffset(IconEntry entry, int height) {
            return entry.yPosition() != null ? entry.yPosition() : DEFAULT_Y_OFFSET;
        }
    },

    EMOJI_16 {
        @Override
        public boolean matches(int width, int height) {
            return false; // a detecter manuellement via config/icons_type.yml
        }

        @Override
        public Dimension finalSize(IconEntry entry, int width, int height) {
            return new Dimension(16, 16);
        }

        @Override
        public int yOffset(IconEntry entry, int height) {
            return entry.yPosition() != null ? entry.yPosition() : DEFAULT_Y_OFFSET;
        }
    };

    private static final int DEFAULT_Y_OFFSET = 9;

    public abstract boolean matches(int width, int height);
    public abstract int yOffset(IconEntry entry, int height);
    public abstract Dimension finalSize(IconEntry entry, int width, int height);

    public static IconsType detect(int width, int height) {
        for (IconsType type : values()) {
            if (type.matches(width, height)) {
                return type;
            }
        }
        return EMOJI;
    }
}