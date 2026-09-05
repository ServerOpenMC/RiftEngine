package fr.openmc.riftengine.core.registry.glyphs.types;

import fr.openmc.riftengine.core.registry.glyphs.Glyph;
import fr.openmc.riftengine.core.scanner.icons.IconEntry;
import lombok.Getter;

@Getter
public class IconGlyph extends Glyph {
    private final IconEntry icon;

    public IconGlyph(String namespacedId, String page, int row, int col, IconEntry icon) {
        super(namespacedId, page, row, col);
        this.icon = icon;
    }
}