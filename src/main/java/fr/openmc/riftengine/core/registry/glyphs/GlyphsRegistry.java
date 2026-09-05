package fr.openmc.riftengine.core.registry.glyphs;

import fr.openmc.core.bootstrap.registries.KeyedRegistry;
import fr.openmc.core.bootstrap.registries.Registry;

import java.util.ArrayList;
import java.util.List;

public class GlyphsRegistry extends Registry<String, Glyph>
    implements KeyedRegistry<String, Glyph> {
    public static final int GRID_SIZE = 16;
    public static final int MAX_PER_PAGE = GRID_SIZE * GRID_SIZE;

    private final List<String> SLOT_GLYPH = new ArrayList<>(List.of(
            "E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9", "EA", "EB", "EC", "ED", "EE", "EF",
            "F0", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8"));
    private int nextPageIndex = 0;

    public String nextGlyphPage() {
        if (nextPageIndex >= SLOT_GLYPH.size()) {
            throw new IllegalStateException("Plus de page de glyphs disponible (max " + SLOT_GLYPH.size() + " pages atteint).");
        }
        return SLOT_GLYPH.get(nextPageIndex++);
    }

    @Override
    public void init() {

    }

    @Override
    public String key(Glyph glyph) {
        return glyph.getNamespacedId();
    }

    public int size() {
        return entries.size();
    }

    /**
     * Donne la taille maximale du registre, car on a 23 slots de glyph (EO a F8 inclu),
     * et dans chaque png on a une grille de 16x16
     * @return 24 * 16 * 16 = 6144
     */
    public int maxSize() {
        return SLOT_GLYPH.size() * 16*16;
    }
}